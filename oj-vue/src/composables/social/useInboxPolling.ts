import {onActivated, onBeforeUnmount, onDeactivated, onMounted, watch} from 'vue'
import {useToken} from '@/stores/useToken'
import {useUserStore} from '@/stores/useUserStore'
import {useInboxStore} from '@/stores/useInboxStore'

export const INBOX_POLL_INTERVAL_MS = 30_000
const MAX_BACKOFF_MS = 5 * 60_000

export interface InboxPollingSession {
  version: number
  userId: string
}

type Listener = () => void

interface PollingDocument {
  visibilityState: DocumentVisibilityState
  addEventListener(type: 'visibilitychange', listener: Listener): void
  removeEventListener(type: 'visibilitychange', listener: Listener): void
}

interface PollingWindow {
  addEventListener(type: 'focus', listener: Listener): void
  removeEventListener(type: 'focus', listener: Listener): void
}

export interface InboxPollingEnvironment {
  document: PollingDocument
  window: PollingWindow
  setTimeout(callback: () => void, delay: number): number
  clearTimeout(timer: number): void
}

interface InboxPollingDependencies {
  environment: InboxPollingEnvironment
  refreshUnreadCount(guard: () => boolean): Promise<void>
  clearInbox(): void
}

/** Injectable controller keeps visibility, single-flight and timing behavior testable without a DOM. */
export const createInboxPollingController = (dependencies: InboxPollingDependencies) => {
  const {environment, refreshUnreadCount, clearInbox} = dependencies
  let active = false
  let disposed = false
  let listenersAttached = false
  let sessionKey: string | null = null
  let generation = 0
  let timer: number | null = null
  let failures = 0
  let inFlight: {generation: number; promise: Promise<void>} | null = null

  const isVisible = () => environment.document.visibilityState === 'visible'
  const canPoll = () => !disposed && active && !!sessionKey && isVisible()

  const clearTimer = () => {
    if (timer !== null) {
      environment.clearTimeout(timer)
      timer = null
    }
  }

  const scheduleNext = (delay: number, expectedGeneration: number) => {
    clearTimer()
    if (!canPoll() || expectedGeneration !== generation) return
    timer = environment.setTimeout(() => {
      timer = null
      void pollNow()
    }, delay)
  }

  const pollNow = (): Promise<void> => {
    if (!canPoll()) return Promise.resolve()
    const requestGeneration = generation
    const existing = inFlight
    if (existing?.generation === requestGeneration) return existing.promise

    const current = () => canPoll() && requestGeneration === generation
    const promise = Promise.resolve()
      .then(() => refreshUnreadCount(current))
      .then(() => {
        failures = 0
      })
      .catch(() => {
        failures++
      })
      .finally(() => {
        if (inFlight?.promise === promise) inFlight = null
        if (!current()) return
        const delay = failures === 0
          ? INBOX_POLL_INTERVAL_MS
          : Math.min(INBOX_POLL_INTERVAL_MS * (2 ** Math.min(failures, 4)), MAX_BACKOFF_MS)
        scheduleNext(delay, requestGeneration)
      })
    inFlight = {generation: requestGeneration, promise}
    return promise
  }

  const handleVisibilityChange = () => {
    if (!active || disposed) return
    if (isVisible()) void pollNow()
    else clearTimer()
  }

  const handleFocus = () => {
    if (isVisible()) void pollNow()
  }

  const attachListeners = () => {
    if (listenersAttached) return
    environment.document.addEventListener('visibilitychange', handleVisibilityChange)
    environment.window.addEventListener('focus', handleFocus)
    listenersAttached = true
  }

  const detachListeners = () => {
    if (!listenersAttached) return
    environment.document.removeEventListener('visibilitychange', handleVisibilityChange)
    environment.window.removeEventListener('focus', handleFocus)
    listenersAttached = false
  }

  const syncSession = (session: InboxPollingSession | null) => {
    const nextKey = session && session.userId ? `${session.version}:${session.userId}` : null
    if (nextKey === sessionKey) return
    sessionKey = nextKey
    generation++
    failures = 0
    inFlight = null
    clearTimer()
    clearInbox()
    if (sessionKey && active) void pollNow()
  }

  const start = () => {
    if (disposed || active) return
    active = true
    attachListeners()
    if (sessionKey) void pollNow()
  }

  const stop = () => {
    if (!active) return
    active = false
    generation++
    inFlight = null
    clearTimer()
    detachListeners()
  }

  const dispose = () => {
    stop()
    disposed = true
    clearTimer()
    detachListeners()
  }

  return {syncSession, start, stop, dispose, pollNow}
}

const browserEnvironment = (): InboxPollingEnvironment | null => {
  if (typeof window === 'undefined' || typeof document === 'undefined') return null
  return {
    document,
    window,
    setTimeout: (callback, delay) => window.setTimeout(callback, delay),
    clearTimeout: timer => window.clearTimeout(timer),
  }
}

/** Mount this composable once from App.vue; menu instances only display the shared badge. */
export const useInboxPolling = () => {
  const token = useToken()
  const user = useUserStore()
  const inbox = useInboxStore()
  const environment = browserEnvironment()
  if (!environment) return

  const controller = createInboxPollingController({
    environment,
    refreshUnreadCount: guard => inbox.refreshUnreadCount(guard),
    clearInbox: () => inbox.clear(),
  })

  const currentSession = (): InboxPollingSession | null => {
    const userId = user.user?.userId
    if (token.authStatus !== 'authenticated' || !token.hasToken() || userId === undefined || userId === null) return null
    return {version: token.getSessionVersion(), userId: String(userId)}
  }

  watch(
    () => [token.authStatus, token.getSessionVersion(), user.user?.userId] as const,
    () => controller.syncSession(currentSession()),
    {immediate: true, flush: 'sync'}
  )

  onMounted(controller.start)
  onActivated(controller.start)
  onDeactivated(controller.stop)
  onBeforeUnmount(controller.dispose)
}
