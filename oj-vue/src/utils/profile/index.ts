import type {IdType} from '@/api/common'

/** Profile 页面统一使用的 ID 展示规则，完整值应通过 title/aria-label 暴露。 */
export const shortProfileId = (value?: IdType | null, maxLength = 16): string => {
    if (value === undefined || value === null || value === '') return '—'
    const text = String(value)
    if (text.length <= maxLength) return text
    return `${text.slice(0, 7)}…${text.slice(-5)}`
}

export const formatProfileDate = (value?: string | Date | null): string => {
    if (!value) return '—'
    const date = value instanceof Date ? value : new Date(value)
    return Number.isNaN(date.getTime())
        ? '—'
        : date.toLocaleDateString('zh-CN', {year: 'numeric', month: '2-digit', day: '2-digit'})
}
