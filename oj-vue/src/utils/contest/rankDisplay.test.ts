import {describe, expect, it, vi} from 'vitest'
const current = vi.hoisted(() => ({auths: [] as string[]}))
vi.mock('@/stores/useUserStore', () => ({useUserStore: () => ({getAuths: () => current.auths})}))
import {hasPerm} from '@/utils/authUtil'
import {rankAvatarId, rankStatusText, rankUserName, shouldPollRank} from './rankDisplay'
import type {FinalRank, RankEntry} from '@/api/contest/rank'

describe('final rank presentation', () => {
  it('uses server ranks and decimal scores without changing ties or Long IDs', () => {
    const entries = [{rank: '1', score: '100.00', userId: '2098755579163770881'},
      {rank: '1', score: '100.00', userId: '2098755579163770882'}] as RankEntry[]
    entries.forEach(row => {expect(rankUserName(row)).toBe('未知用户'); expect(rankAvatarId(row)).toBeNull()})
    expect(entries.map(row => row.rank)).toEqual(['1', '1'])
    expect(entries[0].userId).toBe('2098755579163770881')
  })
  it('shows safe legacy/pending/error labels and stops ready/error-old-version polling', () => {
    const value = {state: 'READY', sourceMode: 'LEGACY', ranking: null} as FinalRank
    expect(rankStatusText(value, false)).toBe('历史成绩')
    expect(shouldPollRank(value)).toBe(false)
    expect(rankStatusText({...value, state: 'WAITING'}, false)).toBe('成绩正在结算')
    expect(shouldPollRank({...value, state: 'ERROR'})).toBe(true)
    expect(shouldPollRank({...value, state: 'ERROR', ranking: {data: []}} as unknown as FinalRank)).toBe(false)
    expect(rankStatusText(null, true)).toBe('成绩暂不可用，请稍后重试')
  })
  it('uses authority strings, not role names', () => {
    current.auths = ['admin', 'super-admin']
    expect(hasPerm('problem:contest:rank')).toBe(false)
    current.auths = ['problem:contest:rank']
    expect(hasPerm('problem:contest:rank')).toBe(true)
  })
})
