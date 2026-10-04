import type {PagedResponse} from '@/api/pagedType'
import type {FollowUserSummary} from '@/api/follow'
import {isFollowUserId} from '@/api/follow'
import {service, type AjaxResult} from '@/utils/http'

export interface RankEntry {
  rank: string
  rowPosition: string
  userId: string
  user: FollowUserSummary | null
  score: string
  correctCount: number
  answeredCount: number
  handedIn: boolean
}
export interface FinalRank {
  contestId: string
  state: 'WAITING' | 'BUILDING' | 'READY' | 'ERROR'
  version: string
  sourceMode: 'CURRENT' | 'LEGACY' | null
  ruleVersion: string | null
  generatedAt: string | null
  pendingCount: string
  ranking: PagedResponse<RankEntry> | null
}
const path = (id: string, admin: boolean) => {
  if (!isFollowUserId(id)) throw new Error('比赛标识无效')
  return `/problem-api/contest/${admin ? 'admin/' : ''}${id}/final-rank`
}
export async function fetchFinalRank(id: string, admin: boolean, page: number): Promise<FinalRank> {
  const response = await service.get<FinalRank, AjaxResult<FinalRank>>(path(id, admin), {
    params: {currentPage: page, pageSize: 20}, localForbidden: true,
  })
  const value = response.data
  if (!value || value.contestId !== id || !['WAITING', 'BUILDING', 'READY', 'ERROR'].includes(value.state)
    || typeof value.version !== 'string' || (value.ranking && !Array.isArray(value.ranking.data))) {
    throw new Error('成绩暂不可用')
  }
  // Do not retain adminBuildInfo/internal failure details in presentation state.
  return {contestId: value.contestId, state: value.state, version: value.version,
    sourceMode: value.sourceMode, ruleVersion: value.ruleVersion, generatedAt: value.generatedAt,
    pendingCount: value.pendingCount, ranking: value.ranking}
}
export async function rebuildFinalRank(id: string): Promise<void> {
  await service.post(path(id, true) + '/rebuild', undefined, {localForbidden: true})
}
