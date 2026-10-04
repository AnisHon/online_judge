import type {ProfileActivityDay} from '@/api/profile'

export const activityLevel = (count: number) => count <= 0 ? 0 : count <= 2 ? 1 : count <= 5 ? 2 : count <= 9 ? 3 : 4

// UTC 仅用于无时区日历运算；日期本身由服务端按北京时间确定。
export const activityWeeks = (days: ProfileActivityDay[]): (ProfileActivityDay | null)[][] => {
  if (!days.length) return []
  const offset = new Date(`${days[0].date}T00:00:00Z`).getUTCDay()
  if (!Number.isFinite(offset)) return []
  const cells: (ProfileActivityDay | null)[] = [
    ...Array<null>(offset).fill(null), ...days.slice(0, 366),
  ]
  while (cells.length % 7) cells.push(null)
  return Array.from({length: cells.length / 7}, (_, index) => cells.slice(index * 7, index * 7 + 7))
}
