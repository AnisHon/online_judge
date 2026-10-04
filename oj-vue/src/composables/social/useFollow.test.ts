import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {effectScope, reactive, ref, type EffectScope} from 'vue'

const api = vi.hoisted(() => ({
  getFollowSummary: vi.fn(), followUser: vi.fn(), unfollowUser: vi.fn(),
}))
vi.mock('@/api/follow', async importOriginal => ({
  ...await importOriginal<typeof import('@/api/follow')>(), ...api,
}))
vi.mock('@/utils/http', () => ({service: {}}))
vi.mock('@/stores/useToken', () => ({useToken: () => token}))
vi.mock('@/stores/useUserStore', () => ({useUserStore: () => user}))
import {useFollow} from './useFollow'
import {isFollowUserId, type FollowSummary} from '@/api/follow'

const token = reactive({sessionVersion: 1, authStatus: 'authenticated'})
const user = reactive({user: {userId: '42'}})
const scopes: EffectScope[] = []
const summary = (userId = '51', following = false): FollowSummary =>
  ({userId, following, followingCount: 3, followersCount: following ? 5 : 4})
const deferred = <T,>() => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no })
  return {promise, resolve, reject}
}
const flush = async () => { await Promise.resolve(); await Promise.resolve(); await Promise.resolve() }
const harness = (id = '51') => {
  const target = ref<string | null>(id)
  const scope = effectScope()
  scopes.push(scope)
  const follow = scope.run(() => useFollow(target))!
  return {target, scope, follow}
}

describe('profile follow state', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    token.sessionVersion = 1
    token.authStatus = 'authenticated'
    user.user.userId = '42'
    api.getFollowSummary.mockImplementation(async (id: string) => summary(id))
    api.followUser.mockImplementation(async (id: string) => summary(id, true))
    api.unfollowUser.mockImplementation(async (id: string) => summary(id, false))
  })
  afterEach(() => { scopes.splice(0).forEach(scope => scope.stop()) })

  it('uses one flight for repeated clicks, confirmed counts, then DELETE for unfollow', async () => {
    const {follow} = harness()
    await flush()
    const response = deferred<FollowSummary>()
    api.followUser.mockReturnValueOnce(response.promise)
    const first = follow.toggle()
    const second = follow.toggle()
    expect(first).toBe(second)
    expect(api.followUser).toHaveBeenCalledTimes(1)
    expect(follow.pending.value).toBe(true)
    expect(follow.summary.value).toEqual(summary())
    response.resolve(summary('51', true))
    await first
    expect(follow.pending.value).toBe(false)
    expect(follow.summary.value).toEqual(summary('51', true))
    await follow.toggle()
    expect(api.unfollowUser).toHaveBeenCalledTimes(1)
    expect(follow.summary.value?.following).toBe(false)
  })

  it('retains confirmed state when mutation fails and allows retry', async () => {
    const {follow} = harness()
    await flush()
    api.followUser.mockRejectedValueOnce(new Error('private backend exception'))
    await follow.toggle()
    expect(follow.summary.value).toEqual(summary())
    expect(follow.error.value).toBe('操作未完成，请重试')
    expect(follow.pending.value).toBe(false)
    await follow.toggle()
    expect(follow.summary.value?.following).toBe(true)
    expect(follow.error.value).toBe('')
  })

  it('drops late A responses after A → B → A', async () => {
    const old = deferred<FollowSummary>()
    api.getFollowSummary.mockReturnValueOnce(old.promise)
    const {target, follow} = harness()
    const signal = api.getFollowSummary.mock.calls[0][1] as AbortSignal
    target.value = '52'
    target.value = '51'
    await flush()
    old.resolve({...summary(), followersCount: 999})
    await flush()
    expect(signal.aborted).toBe(true)
    expect(follow.summary.value).toEqual(summary())
  })

  it('drops a pending mutation when the target changes', async () => {
    const {target, follow} = harness()
    await flush()
    const old = deferred<FollowSummary>()
    api.followUser.mockReturnValueOnce(old.promise)
    const flight = follow.toggle()
    target.value = '52'
    await flush()
    old.resolve(summary('51', true))
    await flight
    expect(follow.summary.value).toEqual(summary('52'))
    expect(follow.pending.value).toBe(false)
  })

  it('hides the self-follow action while allowing own statistics', async () => {
    const {follow} = harness('42')
    await flush()
    expect(follow.available.value).toBe(true)
    expect(follow.canFollow.value).toBe(false)
    await follow.toggle()
    expect(api.followUser).not.toHaveBeenCalled()
  })

  it.each(['0', '', '-1', 'no-user', '9223372036854775808'])('hides invalid target %s', async id => {
    const {follow} = harness(id)
    await flush()
    expect(follow.available.value).toBe(false)
    expect(follow.canFollow.value).toBe(false)
    expect(api.getFollowSummary).not.toHaveBeenCalled()
  })

  it('blocks ID0 viewer and preserves 19-digit Long IDs', async () => {
    user.user.userId = '0'
    const {follow} = harness('2098755579163770881')
    await flush()
    expect(follow.canFollow.value).toBe(false)
    expect(api.getFollowSummary.mock.calls[0][0]).toBe('2098755579163770881')
    expect(isFollowUserId('9223372036854775807')).toBe(true)
  })

  it('clears on logout and rejects old mutation responses across sessions', async () => {
    const {follow} = harness()
    await flush()
    const old = deferred<FollowSummary>()
    api.followUser.mockReturnValueOnce(old.promise)
    const flight = follow.toggle()
    token.authStatus = 'unauthenticated'
    token.sessionVersion++
    old.resolve(summary('51', true))
    await flight
    expect(follow.summary.value).toBeNull()
    expect(follow.canFollow.value).toBe(false)
    token.sessionVersion++
    token.authStatus = 'authenticated'
    await flush()
    expect(follow.summary.value).toEqual(summary())
  })

  it('aborts on scope disposal and ignores response without starting more requests', async () => {
    const old = deferred<FollowSummary>()
    api.getFollowSummary.mockReturnValueOnce(old.promise)
    const {scope, target, follow} = harness()
    const signal = api.getFollowSummary.mock.calls[0][1] as AbortSignal
    scope.stop()
    target.value = '52'
    old.resolve(summary())
    await flush()
    expect(signal.aborted).toBe(true)
    expect(follow.summary.value).toBeNull()
    expect(api.getFollowSummary).toHaveBeenCalledTimes(1)
  })

  it('reports summary errors locally and retries without a polling timer', async () => {
    api.getFollowSummary.mockRejectedValueOnce(new Error('unavailable'))
    const {follow} = harness()
    await flush()
    expect(follow.error.value).toBe('关注信息加载失败，请重试')
    expect(follow.summary.value).toBeNull()
    await follow.refresh()
    expect(follow.summary.value).toEqual(summary())
    expect(api.getFollowSummary).toHaveBeenCalledTimes(2)
  })
})
