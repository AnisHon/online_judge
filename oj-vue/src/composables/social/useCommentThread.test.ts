import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {effectScope, ref, type EffectScope} from 'vue'

const api = vi.hoisted(() => ({
  getSolutionComments: vi.fn(), getCommentReplies: vi.fn(), createSolutionComment: vi.fn(),
  createCommentReply: vi.fn(), deleteOwnComment: vi.fn(),
}))
vi.mock('@/api/comment', () => api)

import {createCommentRequestIdentity, useCommentThread} from './useCommentThread'
import type {CommentVo} from '@/api/comment'

const scopes: EffectScope[] = []
const deferred = <T,>() => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no })
  return {promise, resolve, reject}
}
const flush = async () => { await Promise.resolve(); await Promise.resolve(); await Promise.resolve(); await Promise.resolve() }
const comment = (id: string, overrides: Partial<CommentVo> = {}): CommentVo => ({
  commentId: id, solutionId: '101', rootId: null, parentId: null,
  author: {userId: '5', nikeName: '同学'}, replyTo: null, content: 'hello', state: 'VISIBLE',
  likeCount: '0', likedByMe: false, replyCount: '0', createdAt: '2026-10-04T12:00:00', canDelete: false,
  ...overrides,
})
const page = (data: CommentVo[], currentPage = 1, totalRecords = data.length) => ({currentPage, pageSize: 20, totalRecords, data})
const harness = (id = '101') => {
  const solutionId = ref<string | null>(id)
  const scope = effectScope()
  scopes.push(scope)
  const thread = scope.run(() => useCommentThread(solutionId))!
  return {solutionId, scope, thread}
}

describe('solution comment thread state', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    api.getSolutionComments.mockResolvedValue(page([comment('201')]))
    api.getCommentReplies.mockResolvedValue(page([comment('202', {rootId: '201', parentId: '201'})]))
    api.createSolutionComment.mockImplementation(async (solutionId: string, request: {content: string}) => comment('203', {solutionId, content: request.content}))
    api.createCommentReply.mockImplementation(async (_parentId: string, request: {content: string}) => comment('204', {rootId: '201', parentId: '202', content: request.content}))
    api.deleteOwnComment.mockResolvedValue(true)
  })
  afterEach(() => { scopes.splice(0).forEach(scope => scope.stop()) })

  it('loads root and reply pages independently and appends only the requested page', async () => {
    api.getSolutionComments.mockResolvedValue(page([comment('201')], 1, 21))
    const {thread} = harness()
    await flush()
    expect(api.getSolutionComments).toHaveBeenCalledWith('101', 1, expect.any(AbortSignal))
    await thread.loadReplies('201')
    api.getCommentReplies.mockResolvedValueOnce(page([comment('205', {rootId: '201', parentId: '201'})], 2, 21))
    await thread.loadReplies('201', true)
    expect(api.getCommentReplies.mock.calls.map(call => call[1])).toEqual([1, 2])
    expect(thread.replyState('201').data.map(item => item.commentId)).toEqual(['202', '205'])
    await thread.loadRoots(true)
    expect(api.getSolutionComments.mock.calls.map(call => call[1])).toEqual([1, 2])
  })

  it('shares one in-flight root request and keeps the existing list on a later page failure', async () => {
    const {thread} = harness()
    await flush()
    const next = deferred<ReturnType<typeof page>>()
    api.getSolutionComments.mockReturnValueOnce(next.promise)
    const first = thread.loadRoots(true)
    const second = thread.loadRoots(true)
    expect(first).toBe(second)
    expect(api.getSolutionComments).toHaveBeenCalledTimes(2)
    next.reject({code: 503, message: 'SQL password leaked'})
    await first
    expect(thread.roots.value.map(item => item.commentId)).toEqual(['201'])
    expect(thread.rootsError.value).toBe('操作暂时未完成，请稍后重试')
  })

  it('drops an old solution response when the route changes and aborts its request', async () => {
    const old = deferred<ReturnType<typeof page>>()
    let oldSignal: AbortSignal | undefined
    api.getSolutionComments.mockImplementation((id: string, _page: number, signal: AbortSignal) => {
      if (id === '101') { oldSignal = signal; return old.promise }
      return Promise.resolve(page([comment('301', {solutionId: id})]))
    })
    const {solutionId, thread} = harness()
    solutionId.value = '102'
    await flush()
    old.resolve(page([comment('old')]))
    await flush()
    expect(oldSignal?.aborted).toBe(true)
    expect(thread.roots.value.map(item => item.commentId)).toEqual(['301'])
  })

  it('uses UUID identity through retry and rotates it after edited content or success', () => {
    const ids = ['id-one', 'id-two', 'id-three']
    const identity = createCommentRequestIdentity(() => ids.shift()!)
    const first = identity.begin('normalized body')
    expect(identity.begin('normalized body')).toBe(first)
    identity.edit('edited body')
    expect(identity.begin('edited body')).toBe('id-two')
    expect(identity.begin('edited body')).toBe('id-two')
    identity.complete()
    expect(identity.current()).toBe('id-three')
  })

  it('does not apply an idempotent create twice and retains comment state after deletion', async () => {
    const {thread} = harness()
    await flush()
    const created = comment('203', {content: 'new'})
    api.createSolutionComment.mockResolvedValue(created)
    api.getSolutionComments.mockResolvedValue(page([created, comment('201')], 1, 2))
    const first = await thread.createRoot('new', 'same-uuid')
    await thread.createRoot('new', 'same-uuid')
    expect(api.createSolutionComment).toHaveBeenCalledTimes(2)
    expect(thread.roots.value.filter(item => item.commentId === first?.commentId)).toHaveLength(1)
    expect(thread.totalRecords.value).toBe(2)
    const own = comment('205', {canDelete: true})
    thread.roots.value = [own]
    await thread.deleteComment(own)
    expect(thread.roots.value[0]).toMatchObject({state: 'AUTHOR_DELETED', content: null, canDelete: false})
  })

  it('aborts page requests on scope disposal', async () => {
    const pending = deferred<ReturnType<typeof page>>()
    let signal: AbortSignal | undefined
    api.getSolutionComments.mockImplementation((_id: string, _page: number, requestSignal: AbortSignal) => {
      signal = requestSignal
      return pending.promise
    })
    const {scope, thread} = harness()
    scope.stop()
    pending.resolve(page([comment('late')]))
    await flush()
    expect(signal?.aborted).toBe(true)
    expect(thread.roots.value).toEqual([])
  })
})
