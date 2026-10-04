import {describe, expect, it, vi} from 'vitest'
import {activityLevel, activityWeeks} from './heatmap'
import {getProfileActivity} from '@/api/profile'
import {get} from '@/utils/http'

vi.mock('@/utils/http', () => ({get: vi.fn(), ApiError: class extends Error {}}))
vi.mock('@/api/contest', () => ({ContestType: {CONTEST: 1, HOMEWORK: 2}}))
vi.mock('@/stores/useToken', () => ({useToken: () => ({getSessionVersion: () => 0})}))

describe('profile activity heatmap', () => {
  it('aligns Sundays and pads both boundaries without adding dates', () => {
    const days = [{date: '2026-10-03', count: 1}, {date: '2026-10-04', count: 2}]
    const weeks = activityWeeks(days)
    expect(weeks).toHaveLength(2)
    expect(weeks[0].slice(0, 6)).toEqual(Array(6).fill(null))
    expect(weeks[0][6]).toEqual(days[0])
    expect(weeks[1][0]).toEqual(days[1])
    expect(weeks.flat().filter(Boolean)).toEqual(days)
    expect(activityWeeks([])).toEqual([])
  })

  it('uses fixed color thresholds', () => {
    expect([0, 1, 2, 3, 5, 6, 9, 10, 100].map(activityLevel)).toEqual([0, 1, 1, 2, 2, 3, 3, 4, 4])
  })

  it('normalizes leap-year aggregates, fills zeros and rejects dates outside the window', async () => {
    vi.mocked(get).mockResolvedValue({data: {userId: '42', heatmap: {
      startDate: '2023-03-01', endDate: '2024-02-29', timeZone: 'Asia/Shanghai',
      days: [{date: '2023-03-01', count: 2}, {date: '2024-02-29', count: 12},
        {date: '2024-03-01', count: 999}, {date: '2023-03-02', count: -1},
        {date: '2023-03-03', count: Infinity}, {date: '2023-02-29', count: 999}],
    }}} as never)
    const activity = await getProfileActivity('42')
    expect(activity.heatmap?.days).toHaveLength(366)
    expect(activity.heatmap?.days[0]).toEqual({date: '2023-03-01', count: 2})
    expect(activity.heatmap?.days[365]).toEqual({date: '2024-02-29', count: 12})
    expect(activity.heatmap?.days.reduce((sum, day) => sum + day.count, 0)).toBe(14)
  })

  it.each([undefined,
    {startDate: '2023-01-01', endDate: '2026-01-01', timeZone: 'Asia/Shanghai'},
    {startDate: '2023-02-29', endDate: '2024-02-28', timeZone: 'Asia/Shanghai'},
    {startDate: '2023-03-01', endDate: '2024-02-29', timeZone: 'UTC'},
  ])('ignores old or malformed heatmaps: %j', async heatmap => {
    vi.mocked(get).mockResolvedValue({data: {userId: '42', heatmap}} as never)
    expect((await getProfileActivity('42')).heatmap).toBeUndefined()
  })
})
