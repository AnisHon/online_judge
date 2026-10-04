import {ref} from 'vue'
import {commentTextError, normalizeCommentText} from '@/utils/commentText'

export type ModerationMode = 'AUTHOR_ONLY' | 'RESTORE' | 'SOLUTION_DELETE' | 'COMMENT_DELETE'

export const moderationPermissions = (auths: readonly string[]) => {
  const has = (value: string) => auths.includes(value)
  const list = has('system:backend:access') && has('problem:solution:list')
  const allowed = !has('policy:solution:deny')
  const comments = list && has('problem:comment:list')
  return {list, edit: list && allowed && has('problem:solution:edit'),
    add: has('system:backend:access') && allowed && has('problem:solution:add'),
    remove: list && allowed && has('problem:solution:remove'), comments,
    removeComment: comments && allowed && has('problem:comment:remove')}
}

export const moderationReasonError = (raw: string): string => {
  const value = normalizeCommentText(raw)
  if (!value) return '请填写处理原因'
  if (Array.from(value).length > 500) return '处理原因不能超过 500 个字符'
  if (commentTextError(value)) return '处理原因包含不支持的字符'
  return ''
}

export const moderationError = (error: unknown): string => {
  const code = (error as {code?: number})?.code
  if (code === 403) return '当前账号没有此操作权限，填写内容已保留'
  if (code === 404) return '内容已不可访问，请刷新后重试'
  if (code === 409) return '内容状态已变化，请刷新后重试'
  if (code === 400) return '处理未完成，请检查填写内容'
  return '操作暂时未完成，请稍后重试'
}

/** A closed/replaced dialog cannot publish a late response or clear a newer draft. */
export const useModerationForm = () => {
  const reason = ref('')
  const pending = ref(false)
  const error = ref('')
  let generation = 0
  let controller: AbortController | null = null
  const reset = () => {
    generation++
    controller?.abort()
    controller = null
    pending.value = false
    reason.value = ''
    error.value = ''
  }
  const submit = async (allowed: boolean,
                        request: (reason: string, signal: AbortSignal) => Promise<void>): Promise<boolean> => {
    if (pending.value) return false
    if (!allowed) { error.value = '当前账号没有此操作权限'; return false }
    error.value = moderationReasonError(reason.value)
    if (error.value) return false
    const version = generation
    const current = new AbortController()
    controller = current
    pending.value = true
    try {
      await request(normalizeCommentText(reason.value), current.signal)
      return version === generation && !current.signal.aborted
    } catch (failure) {
      if (version === generation && !current.signal.aborted) error.value = moderationError(failure)
      return false
    } finally {
      if (version === generation) { pending.value = false; controller = null }
    }
  }
  return {reason, pending, error, reset, submit}
}

export const moderationActionLabel = (action: string, targetType?: string) => action === 'DELETE'
  ? targetType === 'COMMENT' ? '删除评论' : '删除题解' : ({
  AUTHOR_ONLY: '限制为仅作者可见', RESTORE: '解除审核限制', DELETE: '删除题解', ADMIN_DELETE: '删除评论',
}[action] || '管理处理')
