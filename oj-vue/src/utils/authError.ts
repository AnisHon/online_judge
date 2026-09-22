import {ApiError} from '@/utils/http'

/** 认证页面只展示稳定的用户提示，不把网关/服务端内部异常原文带到前台。 */
export function authErrorMessage(error: unknown, fallback: string) {
  if (error instanceof ApiError) {
    if (error.code === 401) return '用户名、密码或验证码错误'
    if (error.code === 403) return '当前账号没有执行此操作的权限'
    if (error.code === 0 || error.code >= 500) return fallback
    return error.message || fallback
  }
  if (error instanceof Error && error.message) return error.message
  if (typeof error === 'string' && error) return error
  return fallback
}
