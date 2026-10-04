import type {Solution} from '@/api/solution'

/** Author preference alone cannot indicate public access after moderation or problem changes. */
export const solutionVisibilityLabel = (solution: Pick<Solution, 'effectiveVisibility'>): string => {
  switch (solution.effectiveVisibility) {
    case 'PUBLIC': return '公开'
    case 'AUTHOR_ONLY': return '仅本人可见'
    case 'DELETED': return '已删除'
    default: return '未确认'
  }
}
