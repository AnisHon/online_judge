import {describe, expect, it, vi} from 'vitest'
import {moderationActionLabel, moderationPermissions, moderationReasonError, useModerationForm} from './moderationForm'

const base = ['system:backend:access', 'problem:solution:list']
describe('management permission combinations', () => {
  it('uses permissions, rejects missing ancestors, and honors the deny policy', () => {
    expect(moderationPermissions([])).toMatchObject({list: false, edit: false, remove: false, comments: false})
    expect(moderationPermissions(base)).toMatchObject({list: true, edit: false, remove: false, comments: false})
    expect(moderationPermissions(['problem:solution:add', 'system:backend:access']))
      .toMatchObject({add: true, edit: false, list: false})
    expect(moderationPermissions(['problem:comment:list', 'problem:comment:remove']))
      .toMatchObject({comments: false, removeComment: false})
    expect(moderationPermissions([...base, 'problem:comment:remove']))
      .toMatchObject({comments: false, removeComment: false})
    expect(moderationPermissions([...base, 'problem:comment:list']))
      .toMatchObject({comments: true, removeComment: false})
    const full = [...base, 'problem:solution:add', 'problem:solution:edit', 'problem:solution:remove',
      'problem:comment:list', 'problem:comment:remove']
    expect(Object.values(moderationPermissions(full)).every(Boolean)).toBe(true)
    expect(moderationPermissions([...full, 'policy:solution:deny']))
      .toMatchObject({list: true, comments: true, edit: false, remove: false, removeComment: false, add: false})
  })
})

describe('processing reason and submission lifecycle', () => {
  it('distinguishes comment DELETE audits from solution DELETE audits', () => {
    expect(moderationActionLabel('DELETE', 'COMMENT')).toBe('删除评论')
    expect(moderationActionLabel('DELETE', 'SOLUTION')).toBe('删除题解')
  })
  it('validates empty, unicode, length and illegal characters like the backend', () => {
    expect(moderationReasonError(' \n ')).not.toBe('')
    expect(moderationReasonError('🙂'.repeat(500))).toBe('')
    expect(moderationReasonError('🙂'.repeat(501))).not.toBe('')
    expect(moderationReasonError('理由\u0000')).not.toBe('')
    expect(moderationReasonError('\uD800')).not.toBe('')
    expect(moderationReasonError('原因\n补充\t说明')).toBe('')
  })

  it('runs once during repeat clicks and normalizes the submitted reason', async () => {
    const form = useModerationForm()
    form.reason.value = '  原因\r\n补充  '
    let finish!: () => void
    const request = vi.fn((_reason: string, _signal: AbortSignal) => new Promise<void>(resolve => { finish = resolve }))
    const first = form.submit(true, request)
    expect(await form.submit(true, request)).toBe(false)
    expect(request).toHaveBeenCalledTimes(1)
    expect(request.mock.calls[0][0]).toBe('原因\n补充')
    finish()
    expect(await first).toBe(true)
    expect(form.pending.value).toBe(false)
  })

  it('retains the reason on 403/409, hides internal exceptions, and supports retry', async () => {
    const form = useModerationForm()
    form.reason.value = '请重新整理内容'
    const request = vi.fn().mockRejectedValue({code: 403, message: 'SQL internal details'})
    expect(await form.submit(true, request)).toBe(false)
    expect(form.reason.value).toBe('请重新整理内容')
    expect(form.error.value).toContain('已保留')
    request.mockRejectedValue({code: 409})
    expect(await form.submit(true, request)).toBe(false)
    expect(form.error.value).toContain('状态已变化')
    request.mockRejectedValue(new Error('SQL internal details'))
    await form.submit(true, request)
    expect(form.error.value).not.toContain('SQL')
    request.mockResolvedValue(undefined)
    expect(await form.submit(true, request)).toBe(true)
  })

  it('rejects invalid/unauthorized requests before sending', async () => {
    const form = useModerationForm()
    const request = vi.fn()
    expect(await form.submit(true, request)).toBe(false)
    form.reason.value = '原因'
    expect(await form.submit(false, request)).toBe(false)
    expect(request).not.toHaveBeenCalled()
  })

  it('closing/replacing a dialog aborts the old request without updating the new draft', async () => {
    const form = useModerationForm()
    form.reason.value = '旧原因'
    let reject!: (value: unknown) => void
    let signal!: AbortSignal
    const old = form.submit(true, (_reason, current) => {
      signal = current
      return new Promise<void>((_resolve, fail) => { reject = fail })
    })
    form.reset()
    form.reason.value = '新原因'
    reject({code: 403})
    expect(await old).toBe(false)
    expect(signal.aborted).toBe(true)
    expect(form.reason.value).toBe('新原因')
    expect(form.error.value).toBe('')
    expect(form.pending.value).toBe(false)
  })
})
