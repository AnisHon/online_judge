import type {IdType} from '@/api/common'
import type {PagedResponse} from '@/api/pagedType'
import type {LikeResult, ModerationAction} from '@/api/solution'
import {service, type AjaxResult} from '@/utils/http'

export interface CommentAuthor {
  userId: IdType
  userName?: string
  nikeName?: string
  specialRoles?: string[]
}

export type CommentState = 'VISIBLE' | 'AUTHOR_DELETED' | 'ADMIN_DELETED'

export interface CommentVo {
  commentId: IdType
  solutionId: IdType
  rootId: IdType | null
  parentId: IdType | null
  author: CommentAuthor | null
  replyTo: CommentAuthor | null
  content: string | null
  state: CommentState
  likeCount: string | null
  likedByMe: boolean
  replyCount: string | null
  createdAt: string
  canDelete: boolean
}

export interface CommentPage {
  currentPage: number
  pageSize: number
  totalRecords: number
  data: CommentVo[]
}

export interface CommentCreateRequest {
  content: string
  clientRequestId: string
}

const pageSize = 20

export const getSolutionComments = async (solutionId: IdType, currentPage = 1,
                                         signal?: AbortSignal): Promise<PagedResponse<CommentVo>> => {
  const {data} = await service.get<PagedResponse<CommentVo>, AjaxResult<PagedResponse<CommentVo>>>(
    `/content-api/solution/${encodeURIComponent(solutionId)}/comments`,
    {params: {currentPage, pageSize}, signal, localForbidden: true}
  )
  return data
}

export const createSolutionComment = async (solutionId: IdType, request: CommentCreateRequest,
                                           signal?: AbortSignal): Promise<CommentVo> => {
  const {data} = await service.post<CommentCreateRequest, AjaxResult<CommentVo>>(
    `/content-api/solution/${encodeURIComponent(solutionId)}/comments`, request,
    {signal, localForbidden: true}
  )
  return data
}

export const getCommentReplies = async (rootId: IdType, currentPage = 1,
                                        signal?: AbortSignal): Promise<PagedResponse<CommentVo>> => {
  const {data} = await service.get<PagedResponse<CommentVo>, AjaxResult<PagedResponse<CommentVo>>>(
    `/content-api/comment/${encodeURIComponent(rootId)}/replies`,
    {params: {currentPage, pageSize}, signal, localForbidden: true}
  )
  return data
}

export const createCommentReply = async (parentId: IdType, request: CommentCreateRequest,
                                         signal?: AbortSignal): Promise<CommentVo> => {
  const {data} = await service.post<CommentCreateRequest, AjaxResult<CommentVo>>(
    `/content-api/comment/${encodeURIComponent(parentId)}/replies`, request,
    {signal, localForbidden: true}
  )
  return data
}

export const likeComment = async (id: IdType, signal?: AbortSignal): Promise<LikeResult> => {
  const {data} = await service.put<LikeResult, AjaxResult<LikeResult>>(
    `/content-api/comment/${encodeURIComponent(id)}/like`, undefined,
    {signal, localForbidden: true}
  )
  return data
}

export const unlikeComment = async (id: IdType, signal?: AbortSignal): Promise<LikeResult> => {
  const {data} = await service.delete<LikeResult, AjaxResult<LikeResult>>(
    `/content-api/comment/${encodeURIComponent(id)}/like`,
    {signal, localForbidden: true}
  )
  return data
}

export const deleteOwnComment = async (id: IdType): Promise<boolean> => {
  const {data} = await service.delete<{deleted: boolean}, AjaxResult<{deleted: boolean}>>(
    `/content-api/comment/${encodeURIComponent(id)}`, {localForbidden: true}
  )
  return data?.deleted === true
}

export const getCommentModeration = async (id: IdType,
                                           signal?: AbortSignal): Promise<ModerationAction[]> => {
  const {data} = await service.get<ModerationAction[], AjaxResult<ModerationAction[]>>(
    `/content-api/comment/${encodeURIComponent(id)}/moderation`, {signal, localForbidden: true}
  )
  return Array.isArray(data) ? data : []
}
