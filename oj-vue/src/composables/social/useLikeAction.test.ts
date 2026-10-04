import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {effectScope, ref, type EffectScope} from 'vue'

const api = vi.hoisted(() => ({likeSolution: vi.fn(), unlikeSolution: vi.fn(), likeComment: vi.fn(), unlikeComment: vi.fn()}))
vi.mock('@/api/solution', () => api)
vi.mock('@/api/comment', () => api)

import {useLikeAction} from './useLikeAction'

const scopes: EffectScope[] = []
const deferred = <T,>() => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no })
  return {promise, resolve, reject}
}
const flush = async () => { await Promise.resolve(); await Promise.resolve(); await Promise.resolve() }
const harness = (id = '9007199254740993', resource: 'solution' | 'comment' = 'solution') => {
  const target = ref<string | null>(id)
  const initialLiked = ref(false)
  const initialCount = ref<string | null>('7')
  const scope = effectScope()
  scopes.push(scope)
  const like = scope.run(() => useLikeAction(resource, target, initialLiked, initialCount))!
  return {target, initialLiked, initialCount, scope, like}
}

describe('single-flight like action', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.likeSolution.mockResolvedValue({liked: true, likeCount: '8'})
    api.unlikeSolution.mockResolvedValue({liked: false, likeCount: '7'})
    api.likeComment.mockResolvedValue({liked: true, likeCount: '8'})
    api.unlikeComment.mockResolvedValue({liked: false, likeCount: '7'})
  })
  afterEach(() => { scopes.splice(0).forEach(scope => scope.stop()) })

  it('uses PUT for like, shares a pending request, then DELETEs on the next action', async () => {
    const {like} = harness()
    const response = deferred<{liked: boolean; likeCount: string}>()
    api.likeSolution.mockReturnValueOnce(response.promise)
    const first = like.toggle()
    const repeated = like.toggle()
    expect(first).toBe(repeated)
    expect(like.pending.value).toBe(true)
    expect(api.likeSolution).toHaveBeenCalledTimes(1)
    response.resolve({liked: true, likeCount: '8'})
    await first
    expect(like.liked.value).toBe(true)
    expect(like.likeCount.value).toBe('8')
    await like.toggle()
    expect(api.unlikeSolution).toHaveBeenCalledTimes(1)
  })

  it('keeps confirmed state after failure and exposes only a safe retry message', async () => {
    api.likeComment.mockRejectedValueOnce({code: 503, message: 'database credentials'})
    const {like} = harness('41', 'comment')
    expect(await like.toggle()).toBe(false)
    expect(like.liked.value).toBe(false)
    expect(like.error.value).toBe('操作暂时未完成，请稍后重试')
    expect(like.pending.value).toBe(false)
    expect(await like.toggle()).toBe(true)
    expect(like.liked.value).toBe(true)
  })

  it('ignores a late result after changing the target and preserves Long IDs as strings', async () => {
    const response = deferred<{liked: boolean; likeCount: string}>()
    api.likeSolution.mockReturnValueOnce(response.promise)
    const {target, like} = harness()
    const request = like.toggle()
    target.value = '2098755579163770881'
    await flush()
    response.resolve({liked: true, likeCount: '999'})
    await request
    expect(api.likeSolution.mock.calls[0][0]).toBe('9007199254740993')
    expect(like.liked.value).toBe(false)
    expect(like.likeCount.value).toBe('7')
  })

  it('aborts an active request when its scope is disposed', async () => {
    let signal: AbortSignal | undefined
    api.likeSolution.mockImplementation((_id: string, requestSignal: AbortSignal) => {
      signal = requestSignal
      return new Promise(() => {})
    })
    const {scope, like} = harness()
    void like.toggle()
    scope.stop()
    expect(signal?.aborted).toBe(true)
  })
})
