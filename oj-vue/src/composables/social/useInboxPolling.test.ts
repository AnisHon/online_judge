import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {createPinia, setActivePinia} from 'pinia'

const mocked = vi.hoisted(() => ({
  version: 1,
  status: 'authenticated',
  userId: '42' as string | null,
  getNotificationPage: vi.fn(),
  getUnreadNotificationCount: vi.fn(),
  markNotificationRead: vi.fn(),
  markNotificationsReadThrough: vi.fn(),
}))

vi.mock('@/api/notification', () => ({
  getNotificationPage: mocked.getNotificationPage,
  getUnreadNotificationCount: mocked.getUnreadNotificationCount,
  markNotificationRead: mocked.markNotificationRead,
  markNotificationsReadThrough: mocked.markNotificationsReadThrough,
}))
vi.mock('@/stores/useToken', () => ({
  useToken: () => ({
    authStatus: mocked.status,
    hasToken: () => mocked.status === 'authenticated',
    getSessionVersion: () => mocked.version,
  }),
}))
vi.mock('@/stores/useUserStore', () => ({
  useUserStore: () => ({user: mocked.userId === null ? null : {userId: mocked.userId}}),
}))

import {createInboxPollingController, INBOX_POLL_INTERVAL_MS} from './useInboxPolling'
import {useInboxStore} from '@/stores/useInboxStore'
import {maximumObservedNotificationId} from '@/utils/notificationDisplay'
import type {InboxNotification} from '@/api/notification'

const eventTarget = () => {
  const listeners = new Map<string, Set<() => void>>()
  return {
    addEventListener(type: string, listener: () => void) {
      const set = listeners.get(type) ?? new Set()
      set.add(listener)
      listeners.set(type, set)
    },
    removeEventListener(type: string, listener: () => void) {
      listeners.get(type)?.delete(listener)
    },
    dispatch(type: string) {
      listeners.get(type)?.forEach(listener => listener())
    },
  }
}

const pollingHarness = (refreshUnreadCount: (guard: () => boolean) => Promise<void>) => {
  const documentTarget = eventTarget()
  const windowTarget = eventTarget()
  const documentLike = Object.assign(documentTarget, {visibilityState: 'visible' as DocumentVisibilityState})
  const environment = {
    document: documentLike,
    window: windowTarget,
    setTimeout: (callback: () => void, delay: number) => globalThis.setTimeout(callback, delay) as unknown as number,
    clearTimeout: (timer: number) => globalThis.clearTimeout(timer as unknown as ReturnType<typeof setTimeout>),
  }
  const clearInbox = vi.fn()
  const controller = createInboxPollingController({environment, refreshUnreadCount, clearInbox})
  return {controller, documentLike, documentTarget, windowTarget, clearInbox}
}

const flushMicrotasks = async () => {
  await Promise.resolve()
  await Promise.resolve()
  await Promise.resolve()
}

const deferred = <T,>() => {
  let resolve!: (value: T) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return {promise, resolve, reject}
}

const item = (notificationId: string): InboxNotification => ({
  notificationId,
  type: 'SOLUTION_LIKED',
  actor: null,
  solutionId: '101',
  commentId: null,
  action: null,
  reason: null,
  occurredAt: '2026-10-03T12:00:00+08:00',
  readAt: null,
})

describe('inbox polling controller', () => {
  beforeEach(() => {
    vi.useFakeTimers()
  })
  afterEach(() => vi.useRealTimers())

  it('polls immediately and schedules the next request 30 seconds after completion', async () => {
    const refresh = vi.fn(async () => undefined)
    const {controller} = pollingHarness(refresh)
    controller.syncSession({version: 1, userId: '42'})
    controller.start()
    await flushMicrotasks()
    expect(refresh).toHaveBeenCalledTimes(1)

    await vi.advanceTimersByTimeAsync(INBOX_POLL_INTERVAL_MS - 1)
    expect(refresh).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(1)
    await flushMicrotasks()
    expect(refresh).toHaveBeenCalledTimes(2)
    controller.dispose()
  })

  it('pauses while hidden and requests immediately when visible again', async () => {
    const refresh = vi.fn(async () => undefined)
    const {controller, documentLike, documentTarget} = pollingHarness(refresh)
    controller.syncSession({version: 1, userId: '42'})
    controller.start()
    await flushMicrotasks()
    documentLike.visibilityState = 'hidden'
    documentTarget.dispatch('visibilitychange')
    await vi.advanceTimersByTimeAsync(90_000)
    expect(refresh).toHaveBeenCalledTimes(1)

    documentLike.visibilityState = 'visible'
    documentTarget.dispatch('visibilitychange')
    await flushMicrotasks()
    expect(refresh).toHaveBeenCalledTimes(2)
    controller.dispose()
  })

  it('coalesces repeated focus refreshes while one request is in flight', async () => {
    const response = deferred<void>()
    const refresh = vi.fn(() => response.promise)
    const {controller, windowTarget} = pollingHarness(refresh)
    controller.syncSession({version: 1, userId: '42'})
    controller.start()
    await flushMicrotasks()
    windowTarget.dispatch('focus')
    windowTarget.dispatch('focus')
    await flushMicrotasks()
    expect(refresh).toHaveBeenCalledTimes(1)
    response.resolve()
    await flushMicrotasks()
    await vi.advanceTimersByTimeAsync(INBOX_POLL_INTERVAL_MS)
    await flushMicrotasks()
    expect(refresh).toHaveBeenCalledTimes(2)
    controller.dispose()
  })

  it('invalidates a response after logout and clears the in-memory inbox', async () => {
    const response = deferred<void>()
    const applied: string[] = []
    const refresh = async (guard: () => boolean) => {
      await response.promise
      if (guard()) applied.push('count')
    }
    const {controller, clearInbox} = pollingHarness(refresh)
    controller.syncSession({version: 1, userId: '42'})
    controller.start()
    await flushMicrotasks()
    controller.syncSession(null)
    response.resolve()
    await flushMicrotasks()
    expect(applied).toEqual([])
    expect(clearInbox).toHaveBeenCalled()
    expect(vi.getTimerCount()).toBe(0)
    controller.dispose()
  })

  it('backs off after an unavailable service instead of retrying rapidly', async () => {
    const refresh = vi.fn(async () => { throw new Error('offline') })
    const {controller} = pollingHarness(refresh)
    controller.syncSession({version: 1, userId: '42'})
    controller.start()
    await flushMicrotasks()
    expect(refresh).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(INBOX_POLL_INTERVAL_MS)
    expect(refresh).toHaveBeenCalledTimes(1)
    await vi.advanceTimersByTimeAsync(INBOX_POLL_INTERVAL_MS)
    await flushMicrotasks()
    expect(refresh).toHaveBeenCalledTimes(2)
    controller.dispose()
  })

  it('does not keep polling after the app subtree is deactivated', async () => {
    const refresh = vi.fn(async () => undefined)
    const {controller} = pollingHarness(refresh)
    controller.syncSession({version: 1, userId: '42'})
    controller.start()
    await flushMicrotasks()
    controller.stop()
    await vi.advanceTimersByTimeAsync(90_000)
    expect(refresh).toHaveBeenCalledTimes(1)
    controller.dispose()
  })
})

describe('inbox session isolation and read-all boundary', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    mocked.version = 1
    mocked.status = 'authenticated'
    mocked.userId = '42'
    mocked.getNotificationPage.mockReset().mockResolvedValue({data: [], currentPage: 1, pageSize: 20, totalRecords: 0})
    mocked.getUnreadNotificationCount.mockReset().mockResolvedValue(0)
    mocked.markNotificationRead.mockReset().mockResolvedValue(undefined)
    mocked.markNotificationsReadThrough.mockReset().mockResolvedValue(2)
  })

  it('uses only the maximum ID observed in loaded messages and leaves a newer message unread', async () => {
    const store = useInboxStore()
    store.notifications = [item('2098755579163770881'), item('2098755579163770882')]
    const throughId = maximumObservedNotificationId(store.notifications)
    expect(throughId).toBe('2098755579163770882')

    const response = deferred<number>()
    mocked.markNotificationsReadThrough.mockReturnValue(response.promise)
    const operation = store.markAllRead(throughId!)
    await flushMicrotasks()
    expect(mocked.markNotificationsReadThrough).toHaveBeenCalledWith(throughId)
    store.notifications = [...store.notifications, item('2098755579163770883')]
    response.resolve(2)
    expect(await operation).toBe(true)
    expect(store.notifications.map(row => row.readAt !== null)).toEqual([true, true, false])
  })

  it('rejects a throughId that is not the currently observed maximum', async () => {
    const store = useInboxStore()
    store.notifications = [item('2098755579163770881')]
    expect(await store.markAllRead('9223372036854775807')).toBe(false)
    expect(mocked.markNotificationsReadThrough).not.toHaveBeenCalled()
  })

  it('discards unread-count responses from a previous account session', async () => {
    const store = useInboxStore()
    const response = deferred<number>()
    mocked.getUnreadNotificationCount.mockReturnValue(response.promise)
    const request = store.refreshUnreadCount()
    mocked.version = 2
    mocked.userId = '99'
    response.resolve(12)
    await request
    expect(store.unreadCount).toBe(0)
  })
})
