import {ref, watch, onMounted, onActivated, onDeactivated, onBeforeUnmount, type Ref} from 'vue'
import {fetchFinalRank, rebuildFinalRank, type FinalRank} from '@/api/contest/rank'
import {shouldPollRank} from '@/utils/contest/rankDisplay'
import {useToken} from '@/stores/useToken'
import {hasPerm} from '@/utils/authUtil'

// Share in-flight requests, not cached rankings or authorization decisions.
const flights = new Map<string, Promise<FinalRank>>()
const rebuilds = new Map<string, Promise<void>>()
function singleFlight<T>(map: Map<string, Promise<T>>, key: string, run: () => Promise<T>): Promise<T> {
  const existing = map.get(key)
  if (existing) return existing
  const promise = run().finally(() => { if (map.get(key) === promise) map.delete(key) })
  map.set(key, promise)
  return promise
}
export interface RankEnvironment {
  document: Pick<Document, 'visibilityState' | 'addEventListener' | 'removeEventListener'>
  window: Pick<Window, 'addEventListener' | 'removeEventListener'>
  setTimeout: typeof window.setTimeout
  clearTimeout: typeof window.clearTimeout
}
export function createFinalRankController(options: {
  id: () => string; admin: () => boolean; session: () => number; authenticated: () => boolean
  environment: RankEnvironment
  fetch?: typeof fetchFinalRank; rebuild?: typeof rebuildFinalRank
}) {
  const result = ref<FinalRank | null>(null), page = ref(1), loading = ref(false)
  const failed = ref(false), rebuilding = ref(false)
  const env = options.environment
  let active = false, generation = 0, timer: number | undefined
  let request: Promise<void> | undefined
  const clearTimer = () => { if (timer !== undefined) env.clearTimeout(timer); timer = undefined }
  const visible = () => active && env.document.visibilityState === 'visible' && options.authenticated()
  const schedule = () => {
    clearTimer()
    if (visible() && shouldPollRank(result.value)) timer = env.setTimeout(() => { void refresh() }, 30_000)
  }
  const refresh = (): Promise<void> => {
    clearTimer()
    if (!visible()) return Promise.resolve()
    if (request) return request
    const id = options.id(), session = options.session(), seq = generation, requestedPage = page.value
    const valid = () => active && seq === generation && id === options.id() && session === options.session() && options.authenticated()
    loading.value = true
    const work = (async () => {
      try {
        const next = await singleFlight(flights, `${session}:${options.admin()}:${id}:${requestedPage}`,
          () => (options.fetch || fetchFinalRank)(id, options.admin(), requestedPage))
        if (!valid()) return
        if (result.value && next.version !== result.value.version && requestedPage !== 1) {
          page.value = 1
          const first = await singleFlight(flights, `${session}:${options.admin()}:${id}:1`,
            () => (options.fetch || fetchFinalRank)(id, options.admin(), 1))
          if (!valid()) return
          result.value = first
        } else {
          result.value = next.ranking || !result.value?.ranking ? next : {...next, ranking: result.value.ranking}
        }
        failed.value = false
      } catch { if (valid()) failed.value = true }
      finally { if (valid()) {loading.value = false; schedule()} }
    })()
    request = work
    void work.finally(() => { if (request === work) request = undefined })
    return work
  }
  const reset = () => {
    generation++; clearTimer(); request = undefined
    result.value = null; page.value = 1; failed.value = false; loading.value = false; rebuilding.value = false
    if (active) void refresh()
  }
  const wake = () => { if (visible()) void refresh(); else clearTimer() }
  const start = () => { if (active) return; active = true; env.document.addEventListener('visibilitychange', wake); env.window.addEventListener('focus', wake); void refresh() }
  const stop = () => {
    if (!active) return
    active = false; generation++; request = undefined; loading.value = false; clearTimer()
    env.document.removeEventListener('visibilitychange', wake); env.window.removeEventListener('focus', wake)
  }
  const rebuild = async (allowed: boolean) => {
    if (!allowed || rebuilding.value || !visible()) return
    const id = options.id(), session = options.session(), seq = generation
    rebuilding.value = true
    try {
      await singleFlight(rebuilds, `${session}:${id}`, () => (options.rebuild || rebuildFinalRank)(id))
      if (seq === generation && session === options.session()) await refresh()
    } catch { if (seq === generation && session === options.session()) failed.value = true }
    finally { if (seq === generation) rebuilding.value = false }
  }
  const setPage = (value: number) => {
    if (!Number.isSafeInteger(value) || value < 1 || page.value === value || loading.value) return
    page.value = value; void refresh()
  }
  return {result, page, loading, failed, rebuilding, refresh, rebuild, setPage, reset, start, stop}
}
export function useFinalRank(id: Ref<string>, admin: Ref<boolean>) {
  const token = useToken()
  const authorized = () => token.hasToken() && (!admin.value || hasPerm('problem:contest:rank'))
  const controller = createFinalRankController({id: () => id.value, admin: () => admin.value,
    session: () => token.sessionVersion, authenticated: authorized,
    environment: {document, window, setTimeout: window.setTimeout.bind(window), clearTimeout: window.clearTimeout.bind(window)}})
  watch(() => [id.value, admin.value, token.sessionVersion, authorized()], controller.reset)
  onMounted(controller.start); onActivated(controller.start)
  onDeactivated(controller.stop); onBeforeUnmount(controller.stop)
  return controller
}
