import type {FinalRank, RankEntry} from '@/api/contest/rank'

export const shouldPollRank = (value: FinalRank | null) => !!value
  && (value.state === 'WAITING' || value.state === 'BUILDING' || (value.state === 'ERROR' && !value.ranking))
export const rankStatusText = (value: FinalRank | null, failed: boolean) => {
  if (failed || value?.state === 'ERROR') return '成绩暂不可用，请稍后重试'
  if (value?.state === 'WAITING' || value?.state === 'BUILDING') return '成绩正在结算'
  return value?.sourceMode === 'LEGACY' ? '历史成绩' : '最终成绩'
}
export const rankUserName = (row: RankEntry) => row.user?.nikeName || row.user?.userName || '未知用户'
export const rankAvatarId = (row: RankEntry) => row.user?.userId || null
