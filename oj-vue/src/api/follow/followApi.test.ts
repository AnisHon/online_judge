import {beforeEach, describe, expect, it, vi} from 'vitest'
const service = vi.hoisted(() => ({get: vi.fn(), put: vi.fn(), delete: vi.fn()}))
vi.mock('@/utils/http', () => ({service}))
import {followUser, getFollowSummary, getFollowUsersPage, unfollowUser} from './index'

const id = '2098755579163770881'
const summary = {userId: id, following: false, followersCount: 2, followingCount: 1}
describe('follow API contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    service.get.mockResolvedValue({data: summary})
    service.put.mockResolvedValue({data: {...summary, following: true}})
    service.delete.mockResolvedValue({data: summary})
  })
  it('preserves Long ID strings and uses existing GET/PUT/DELETE endpoints without payloads', async () => {
    const signal = new AbortController().signal
    expect(await getFollowSummary(id, signal)).toEqual(summary)
    await followUser(id, signal)
    await unfollowUser(id, signal)
    expect(service.get).toHaveBeenCalledWith(`/user-api/follow/${id}/summary`, {signal, localForbidden: true})
    expect(service.put).toHaveBeenCalledWith(`/user-api/follow/${id}`, undefined, {signal, localForbidden: true})
    expect(service.delete).toHaveBeenCalledWith(`/user-api/follow/${id}`, {signal, localForbidden: true})
  })
  it('requests only one page of safe summaries, filters deleted IDs, does not expose extra data', async () => {
    service.get.mockResolvedValue({data: {currentPage: 2, pageSize: 20, totalRecords: 30, data: [
      {userId: id, userName: 'student', nikeName: '同学', specialRoles: ['教师'], email: 'private', auths: ['private']},
      null, {userId: null}, {userId: 9007199254740992},
    ]}})
    const page = await getFollowUsersPage(id, 'followers', 2)
    expect(service.get).toHaveBeenCalledWith(`/user-api/follow/${id}/followers`,
      {params: {currentPage: 2, pageSize: 20}, signal: undefined, localForbidden: true})
    expect(page.data).toEqual([{userId: id, userName: 'student', nikeName: '同学', specialRoles: ['教师']}])
    expect(page.totalRecords).toBe(30)
  })
  it('supports empty lists and rejects malformed summary without replacing confirmed state', async () => {
    service.get.mockResolvedValueOnce({data: {currentPage: 1, pageSize: 20, totalRecords: 0, data: []}})
    expect((await getFollowUsersPage(id, 'following', 1)).data).toEqual([])
    service.get.mockResolvedValueOnce({data: {...summary, userId: '52'}})
    await expect(getFollowSummary(id)).rejects.toThrow('关注信息暂不可用')
    service.get.mockResolvedValueOnce({data: null})
    await expect(getFollowSummary(id)).rejects.toThrow('关注信息暂不可用')
  })
})
