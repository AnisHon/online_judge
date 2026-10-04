import {beforeEach, describe, expect, it, vi} from 'vitest'
const service = vi.hoisted(() => ({get: vi.fn(), post: vi.fn()}))
vi.mock('@/utils/http', () => ({service}))
import {fetchFinalRank, rebuildFinalRank} from './rank'
const id = '2098755579163770881'
const value = {contestId: id, state: 'WAITING', version: '0', pendingCount: '0', ranking: null,
  sourceMode: null, ruleVersion: null, generatedAt: null}
describe('final rank API contract', () => {
  beforeEach(() => {vi.clearAllMocks(); service.get.mockResolvedValue({data: value})})
  it('uses exact public/admin endpoints and keeps identifiers strings', async () => {
    expect(await fetchFinalRank(id, false, 2)).toEqual(value)
    expect(service.get).toHaveBeenCalledWith(`/problem-api/contest/${id}/final-rank`,
      {params: {currentPage: 2, pageSize: 20}, localForbidden: true})
    await fetchFinalRank(id, true, 1); await rebuildFinalRank(id)
    expect(service.post).toHaveBeenCalledWith(`/problem-api/contest/admin/${id}/final-rank/rebuild`, undefined, {localForbidden: true})
  })
  it('discards internal diagnosis and rejects wrong-contest/malformed responses', async () => {
    service.get.mockResolvedValueOnce({data: {...value, adminBuildInfo: {lastError: 'private SQL'}}})
    expect(await fetchFinalRank(id, true, 1)).not.toHaveProperty('adminBuildInfo')
    service.get.mockResolvedValueOnce({data: {...value, contestId: '42'}})
    await expect(fetchFinalRank(id, false, 1)).rejects.toThrow('成绩暂不可用')
    await expect(fetchFinalRank('9e18', false, 1)).rejects.toThrow('比赛标识无效')
  })
})
