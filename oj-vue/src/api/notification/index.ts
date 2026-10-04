import type {PagedResponse, PagedType} from '@/api/pagedType'
import type {IdType} from '@/api/common'
import {service, type AjaxResult} from '@/utils/http'

export interface NotificationActor {
  userId: IdType
  userName: string
  nikeName: string
  specialRoles: string[]
}

export type CommunityNotificationType =
  | 'SOLUTION_PUBLISHED'
  | 'SOLUTION_LIKED'
  | 'SOLUTION_COMMENTED'
  | 'COMMENT_REPLIED'
  | 'COMMENT_LIKED'
  | 'SOLUTION_MODERATED'
  | 'COMMENT_MODERATED'

export interface InboxNotification {
  notificationId: IdType
  type: CommunityNotificationType | string
  actor: NotificationActor | null
  solutionId: IdType | null
  commentId: IdType | null
  action: string | null
  reason: string | null
  occurredAt: string
  readAt: string | null
}

export interface NotificationPageQuery extends PagedType {
  unreadOnly: boolean
}

export type NotificationPage = PagedResponse<InboxNotification>

export const getNotificationPage = async (query: NotificationPageQuery): Promise<NotificationPage> => {
  const response = await service.get<NotificationPage, AjaxResult<NotificationPage>>(
    '/user-api/notification/page', {params: query}
  )
  return response.data
}

export const getUnreadNotificationCount = async (): Promise<number> => {
  const response = await service.get<{count: number | string}, AjaxResult<{count: number | string}>>(
    '/user-api/notification/unread-count'
  )
  const count = Number(response.data?.count)
  if (!Number.isSafeInteger(count) || count < 0) throw new Error('未读消息数量暂不可用')
  return count
}

export const markNotificationRead = async (notificationId: IdType): Promise<void> => {
  await service.put<{read: boolean}, AjaxResult<{read: boolean}>>(
    `/user-api/notification/${encodeURIComponent(notificationId)}/read`
  )
}

export const markNotificationsReadThrough = async (throughId: IdType): Promise<number> => {
  const response = await service.put<{throughId: IdType}, AjaxResult<{updatedCount: number}>>(
    '/user-api/notification/read-all', {throughId}
  )
  return response.data?.updatedCount ?? 0
}
