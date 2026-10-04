import {describe, expect, it} from 'vitest'
import type {InboxNotification} from '@/api/notification'
import {
  compareDecimalIds,
  formatNotificationTime,
  isValidNotificationId,
  maximumObservedNotificationId,
  notificationActorName,
  notificationMessage,
  notificationReason,
  notificationTarget,
} from './notificationDisplay'

const notification = (overrides: Partial<InboxNotification> = {}): InboxNotification => ({
  notificationId: '2098755579163770881',
  type: 'SOLUTION_PUBLISHED',
  actor: {userId: '2098755579163770882', userName: 'writer', nikeName: '作者', specialRoles: []},
  solutionId: '2098755579163770883',
  commentId: null,
  action: null,
  reason: null,
  occurredAt: '2026-10-03T12:30:00+08:00',
  readAt: null,
  ...overrides,
})

describe('notification display', () => {
  it.each([
    ['SOLUTION_PUBLISHED', null, '发布了新的题解'],
    ['SOLUTION_LIKED', null, '赞了你的题解'],
    ['SOLUTION_COMMENTED', null, '评论了你的题解'],
    ['COMMENT_REPLIED', null, '回复了你的评论'],
    ['COMMENT_LIKED', null, '赞了你的评论'],
    ['SOLUTION_MODERATED', 'AUTHOR_ONLY', '管理员已将你的题解调整为仅自己可见'],
    ['SOLUTION_MODERATED', 'RESTORE', '管理员已恢复你的题解'],
    ['SOLUTION_MODERATED', 'DELETE', '管理员已删除你的题解'],
    ['COMMENT_MODERATED', 'DELETE', '管理员已删除你的评论'],
    ['FUTURE_EVENT', '<img src=x onerror=alert(1)>', '你收到了一条新消息'],
  ] as const)('uses fixed ordinary-user copy for %s/%s', (type, action, copy) => {
    const value = notification({type, action})
    expect(notificationMessage(value)).toBe(copy)
    expect(notificationMessage(value)).not.toMatch(/[<>]/)
  })

  it('treats moderation reasons as plain text and ignores them for other event types', () => {
    const reason = '<script>alert(1)</script> 请修改内容'
    expect(notificationReason(notification({type: 'SOLUTION_MODERATED', reason}))).toBe(reason)
    expect(notificationReason(notification({type: 'SOLUTION_LIKED', reason}))).toBe('')
  })

  it('uses a safe fallback identity and does not substitute the current account', () => {
    expect(notificationActorName(notification({actor: null}))).toBe('你关注的用户')
    expect(notificationActorName(notification({type: 'COMMENT_LIKED', actor: null}))).toBe('其他用户')
    expect(notificationActorName(notification({actor: {
      userId: '9', userName: '<img>', nikeName: '', specialRoles: [],
    }}))).toBe('<img>')
  })

  it('preserves long IDs and only adds comment location for comment notifications', () => {
    const longSolutionId = '2098755579163770883'
    expect(notificationTarget(notification({solutionId: longSolutionId}))).toEqual({
      name: 'solution', params: {id: longSolutionId},
    })
    expect(notificationTarget(notification({type: 'COMMENT_REPLIED', commentId: '2098755579163770884'}))).toEqual({
      name: 'solution',
      params: {id: '2098755579163770883'},
      query: {commentId: '2098755579163770884'},
    })
    expect(notificationTarget(notification({type: 'SOLUTION_LIKED', commentId: '7'}))).not.toHaveProperty('query')
    expect(notificationTarget(notification({solutionId: 'not-an-id'}))).toBeNull()
    expect(notificationTarget(notification({type: 'FUTURE_EVENT'}))).toBeNull()
    expect(notificationTarget(notification({type: 'COMMENT_REPLIED', commentId: '0'}))).not.toHaveProperty('query')
  })

  it('compares and finds the maximum observed ID without Number precision loss', () => {
    expect(compareDecimalIds('2098755579163770881', '2098755579163770882')).toBe(-1)
    expect(compareDecimalIds('10000000000000000000', '999999999999999999')).toBe(1)
    expect(maximumObservedNotificationId([
      {notificationId: '2098755579163770881'},
      {notificationId: '2098755579163770883'},
      {notificationId: '2098755579163770882'},
      {notificationId: '9223372036854775808'},
      {notificationId: '0'},
    ])).toBe('2098755579163770883')
    expect(maximumObservedNotificationId([])).toBeNull()
  })

  it('rejects malformed and out-of-range IDs but accepts the signed BIGINT maximum', () => {
    expect(isValidNotificationId('9223372036854775807')).toBe(true)
    expect(isValidNotificationId('9223372036854775808')).toBe(false)
    expect(isValidNotificationId('01')).toBe(false)
    expect(isValidNotificationId(123)).toBe(false)
  })

  it('formats valid timestamps and falls back for invalid input', () => {
    expect(formatNotificationTime('2026-10-03T12:30:00+08:00')).toBe('2026-10-03 12:30')
    expect(formatNotificationTime('not-a-date')).toBe('时间未知')
  })
})
