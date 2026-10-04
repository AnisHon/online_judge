import type {IdType} from '@/api/common'
import type {PagedResponse} from '@/api/pagedType'
import type {CommentState} from './index'
import {ApiError, service, type AjaxResult} from '@/utils/http'

export interface AdminComment {
  commentId: IdType
  solutionId: IdType
  userId: IdType
  rootId: IdType | null
  parentId: IdType | null
  replyToUserId: IdType | null
  content: string
  state: CommentState
  deletedBy: IdType | null
  deletedAt: string | null
  likeCount: string
  replyCount: string
  createdAt: string
  actionId: IdType | null
  operatorId: IdType | null
  action: string | null
  reason: string | null
  actionAt: string | null
}

export const getAdminComments = async (solutionId: IdType, currentPage = 1,
                                       state?: CommentState, signal?: AbortSignal): Promise<PagedResponse<AdminComment>> => {
  const {data} = await service.get<PagedResponse<AdminComment>, AjaxResult<PagedResponse<AdminComment>>>(
    '/content-api/comment/admin/page', {params: {solutionId, currentPage, pageSize: 20, state},
      signal, localForbidden: true})
  return data
}

export const deleteCommentAdmin = async (id: IdType, reason: string, signal?: AbortSignal): Promise<void> => {
  const {data} = await service.post<{reason: string}, AjaxResult<{deleted: boolean}>>(
    `/content-api/comment/admin/${encodeURIComponent(id)}/delete`, {reason}, {signal, localForbidden: true})
  if (data?.deleted !== true) throw new ApiError('评论未删除，请刷新后重试', 409)
}
