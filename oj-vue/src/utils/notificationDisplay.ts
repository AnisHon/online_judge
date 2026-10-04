import dayjs from 'dayjs'
import type {InboxNotification} from '@/api/notification'
import type {RouteLocationNamedRaw} from 'vue-router'

const MAX_LONG_ID = '9223372036854775807'

const solutionModerationCopy: Record<string, string> = {
  AUTHOR_ONLY: '管理员已将你的题解调整为仅自己可见',
  RESTORE: '管理员已恢复你的题解',
  DELETE: '管理员已删除你的题解',
}

const copyByType: Record<string, string> = {
  SOLUTION_PUBLISHED: '发布了新的题解',
  SOLUTION_LIKED: '赞了你的题解',
  SOLUTION_COMMENTED: '评论了你的题解',
  COMMENT_REPLIED: '回复了你的评论',
  COMMENT_LIKED: '赞了你的评论',
  COMMENT_MODERATED: '管理员已删除你的评论',
}

const targetTypes = new Set([
  'SOLUTION_PUBLISHED', 'SOLUTION_LIKED', 'SOLUTION_COMMENTED', 'COMMENT_REPLIED',
  'COMMENT_LIKED', 'SOLUTION_MODERATED', 'COMMENT_MODERATED',
])

export const isValidNotificationId = (value: unknown): value is string => {
  if (typeof value !== 'string' || !/^[1-9][0-9]{0,18}$/.test(value)) return false
  return compareDecimalIds(value, MAX_LONG_ID) <= 0
}

/** Compares canonical positive decimal IDs without converting them to JS Number. */
export const compareDecimalIds = (left: string, right: string): number => {
  const a = left.replace(/^0+(?=\d)/, '')
  const b = right.replace(/^0+(?=\d)/, '')
  if (a.length !== b.length) return a.length < b.length ? -1 : 1
  return a === b ? 0 : a < b ? -1 : 1
}

export const maximumObservedNotificationId = (items: Pick<InboxNotification, 'notificationId'>[]): string | null => {
  let maximum: string | null = null
  for (const item of items) {
    const id = String(item.notificationId)
    if (isValidNotificationId(id) && (maximum === null || compareDecimalIds(id, maximum) > 0)) maximum = id
  }
  return maximum
}

export const notificationMessage = (notification: Pick<InboxNotification, 'type' | 'action'>): string => {
  if (notification.type === 'SOLUTION_MODERATED') {
    return solutionModerationCopy[String(notification.action || '').toUpperCase()] || '管理员处理了你的题解'
  }
  return copyByType[notification.type] || '你收到了一条新消息'
}

export const notificationActorName = (notification: Pick<InboxNotification, 'type' | 'actor'>): string => {
  if (!notification.actor) {
    if (notification.type === 'SOLUTION_PUBLISHED') return '你关注的用户'
    if (notification.type === 'SOLUTION_MODERATED' || notification.type === 'COMMENT_MODERATED') return '管理员'
    return '其他用户'
  }
  return notification.actor.nikeName?.trim() || notification.actor.userName?.trim() || '其他用户'
}

export const notificationTarget = (
  notification: Pick<InboxNotification, 'type' | 'solutionId' | 'commentId'>
): RouteLocationNamedRaw | null => {
  if (!targetTypes.has(notification.type)) return null
  const solutionId = notification.solutionId == null ? '' : String(notification.solutionId)
  if (!isValidNotificationId(solutionId)) return null
  const isComment = notification.type === 'SOLUTION_COMMENTED'
    || notification.type === 'COMMENT_REPLIED'
    || notification.type === 'COMMENT_LIKED'
    || notification.type === 'COMMENT_MODERATED'
  const commentId = notification.commentId == null ? '' : String(notification.commentId)
  return {
    name: 'solution',
    params: {id: solutionId},
    ...(isComment && isValidNotificationId(commentId) ? {query: {commentId}} : {}),
  }
}

export const formatNotificationTime = (value: string): string => {
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD HH:mm') : '时间未知'
}

export const isModerationNotification = (type: string): boolean =>
  type === 'SOLUTION_MODERATED' || type === 'COMMENT_MODERATED'

export const notificationReason = (notification: Pick<InboxNotification, 'type' | 'reason'>): string =>
  isModerationNotification(notification.type) && typeof notification.reason === 'string'
    ? notification.reason.trim()
    : ''
