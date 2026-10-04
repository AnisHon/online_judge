import type {IdType} from '@/api/common'
import type {PagedResponse} from '@/api/pagedType'
import {service, type AjaxResult} from '@/utils/http'

export interface FollowSummary {
  userId: IdType
  following: boolean
  followersCount: number
  followingCount: number
}
export interface FollowUserSummary {
  userId: IdType
  userName: string
  nikeName: string
  specialRoles: string[]
}
export type FollowListType = 'following' | 'followers'

// Validate Long IDs without passing them through Number.
export const isFollowUserId = (value: unknown): value is string =>
  typeof value === 'string' && /^[1-9]\d{0,18}$/.test(value)
  && (value.length < 19 || value <= '9223372036854775807')

const endpoint = (userId: IdType) => {
  if (!isFollowUserId(userId)) throw new Error('用户标识无效')
  return `/user-api/follow/${userId}`
}
const normalizeSummary = (value: FollowSummary, userId: IdType): FollowSummary => {
  if (!value || value.userId !== userId || typeof value.following !== 'boolean'
    || !Number.isSafeInteger(value.followersCount) || value.followersCount < 0
    || !Number.isSafeInteger(value.followingCount) || value.followingCount < 0) {
    throw new Error('关注信息暂不可用')
  }
  return value
}
export const getFollowSummary = async (userId: IdType, signal?: AbortSignal): Promise<FollowSummary> => {
  const response = await service.get<FollowSummary, AjaxResult<FollowSummary>>(`${endpoint(userId)}/summary`, {signal, localForbidden: true})
  return normalizeSummary(response.data, userId)
}
export const followUser = async (userId: IdType, signal?: AbortSignal): Promise<FollowSummary> => {
  const response = await service.put<FollowSummary, AjaxResult<FollowSummary>>(endpoint(userId), undefined, {signal, localForbidden: true})
  return normalizeSummary(response.data, userId)
}
export const unfollowUser = async (userId: IdType, signal?: AbortSignal): Promise<FollowSummary> => {
  const response = await service.delete<FollowSummary, AjaxResult<FollowSummary>>(endpoint(userId), {signal, localForbidden: true})
  return normalizeSummary(response.data, userId)
}
export const getFollowUsersPage = async (userId: IdType, type: FollowListType, currentPage: number,
                                       signal?: AbortSignal): Promise<PagedResponse<FollowUserSummary>> => {
  const response = await service.get<PagedResponse<FollowUserSummary>, AjaxResult<PagedResponse<FollowUserSummary>>>(
    `${endpoint(userId)}/${type}`, {params: {currentPage, pageSize: 20}, signal, localForbidden: true}
  )
  const page = response.data
  if (!page || !Array.isArray(page.data) || !Number.isSafeInteger(page.totalRecords) || page.totalRecords < 0) {
    throw new Error('用户列表暂不可用')
  }
  return {
    ...page,
    data: page.data.filter(item => item && isFollowUserId(item.userId)).map(item => ({
      userId: item.userId,
      userName: typeof item.userName === 'string' ? item.userName : '',
      nikeName: typeof item.nikeName === 'string' ? item.nikeName : '',
      specialRoles: Array.isArray(item.specialRoles) ? item.specialRoles.filter(role => typeof role === 'string') : [],
    })),
  }
}
