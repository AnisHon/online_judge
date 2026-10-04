import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
vi.mock('@/api/contest/rank', () => ({fetchFinalRank: vi.fn(), rebuildFinalRank: vi.fn()}))
vi.mock('@/stores/useToken', () => ({useToken: vi.fn()}))
vi.mock('@/utils/authUtil', () => ({hasPerm: vi.fn()}))
import {createFinalRankController, type RankEnvironment} from './useFinalRank'
import type {FinalRank} from '@/api/contest/rank'

const value = (state: FinalRank['state'] = 'WAITING', version = '1'): FinalRank => ({
  contestId: '2098755579163770881', state, version, sourceMode: 'CURRENT', ruleVersion: '1',
  generatedAt: null, pendingCount: '0', ranking: null,
})
const target = () => {
  const listeners = new Map<string, Set<() => void>>()
  return {
    addEventListener(type: string, callback: () => void) {const set = listeners.get(type) || new Set(); set.add(callback); listeners.set(type, set)},
    removeEventListener(type: string, callback: () => void) {listeners.get(type)?.delete(callback)},
    emit(type: string) {listeners.get(type)?.forEach(callback => callback())},
  }
}
let disposers: Array<() => void> = []
const harness = () => {
  const doc = {...target(), visibilityState: 'visible'}
  const win = target()
  const session = {version: 1, authenticated: true, id: '2098755579163770881'}
  const fetch = vi.fn(async () => value())
  const rebuild = vi.fn(async () => {})
  const controller = createFinalRankController({id: () => session.id, admin: () => false,
    session: () => session.version, authenticated: () => session.authenticated, fetch, rebuild,
    environment: {document: doc, window: win, setTimeout: globalThis.setTimeout, clearTimeout: globalThis.clearTimeout} as unknown as RankEnvironment})
  disposers.push(controller.stop)
  return {controller, doc, win, session, fetch, rebuild}
}
const flush = async () => {await vi.advanceTimersByTimeAsync(0)}
describe('final rank lifecycle', () => {
  beforeEach(() => vi.useFakeTimers())
  afterEach(() => {disposers.forEach(stop => stop()); disposers = []; vi.useRealTimers()})
  it('polls pending every 30 seconds, pauses hidden, resumes immediately and stops READY', async () => {
    const h = harness(); h.controller.start(); await flush()
    await vi.advanceTimersByTimeAsync(30_000); expect(h.fetch).toHaveBeenCalledTimes(2)
    h.doc.visibilityState = 'hidden'; h.doc.emit('visibilitychange')
    await vi.advanceTimersByTimeAsync(90_000); expect(h.fetch).toHaveBeenCalledTimes(2)
    h.fetch.mockResolvedValue(value('READY')); h.doc.visibilityState = 'visible'; h.win.emit('focus'); await flush()
    expect(h.fetch).toHaveBeenCalledTimes(3)
    await vi.advanceTimersByTimeAsync(60_000); expect(h.fetch).toHaveBeenCalledTimes(3)
    h.controller.stop(); h.win.emit('focus'); expect(vi.getTimerCount()).toBe(0)
  })
  it('shares requests and rejects repeated focus requests', async () => {
    const a = harness(), b = harness()
    let resolve!: (result: FinalRank) => void
    a.fetch.mockImplementation(() => new Promise(r => {resolve = r}))
    a.controller.start(); b.controller.start(); a.win.emit('focus'); a.win.emit('focus')
    expect(a.fetch).toHaveBeenCalledTimes(1); expect(b.fetch).not.toHaveBeenCalled()
    resolve(value()); await flush(); expect(b.controller.result.value?.state).toBe('WAITING')
  })
  it('ignores late data on route switch, logout and panel close', async () => {
    const h = harness(); let resolve!: (result: FinalRank) => void
    h.fetch.mockImplementationOnce(() => new Promise(r => {resolve = r}))
    h.controller.start(); h.session.authenticated = false; h.session.version++; h.controller.reset()
    resolve(value()); await flush(); expect(h.controller.result.value).toBeNull(); expect(vi.getTimerCount()).toBe(0)
    h.session.authenticated = true; h.session.id = '2098755579163770882'
    h.fetch.mockResolvedValue({...value(), contestId: h.session.id}); h.controller.reset(); await flush()
    expect(h.controller.result.value?.contestId).toBe(h.session.id)
    h.controller.stop(); await vi.advanceTimersByTimeAsync(90_000); expect(vi.getTimerCount()).toBe(0)
  })
  it('preserves old rows on failure and resets pagination only when version changes', async () => {
    const h = harness(); const page = {currentPage: 1, pageSize: 20, totalRecords: 45, data: []}
    h.fetch.mockResolvedValue({...value('READY'), ranking: page}); h.controller.start(); await flush()
    h.controller.setPage(2); await flush(); expect(h.controller.page.value).toBe(2)
    h.fetch.mockRejectedValueOnce(new Error('SQL must never reach UI')); await h.controller.refresh()
    expect(h.controller.result.value?.ranking).toEqual(page); expect(h.controller.failed.value).toBe(true)
    h.fetch.mockResolvedValue({...value('READY', '2'), ranking: page}); await h.controller.refresh()
    expect(h.controller.page.value).toBe(1); expect(h.controller.result.value?.version).toBe('2')
  })
  it('polls first ERROR and does not submit unauthorized or duplicate rebuilds', async () => {
    const h = harness(); h.fetch.mockResolvedValue(value('ERROR')); h.controller.start(); await flush()
    await vi.advanceTimersByTimeAsync(30_000); expect(h.fetch).toHaveBeenCalledTimes(2)
    await h.controller.rebuild(false); expect(h.rebuild).not.toHaveBeenCalled()
    let resolve!: () => void; h.rebuild.mockImplementationOnce(() => new Promise<void>(r => {resolve = r}))
    const first = h.controller.rebuild(true); await h.controller.rebuild(true)
    expect(h.rebuild).toHaveBeenCalledTimes(1); resolve(); await first
  })
})
