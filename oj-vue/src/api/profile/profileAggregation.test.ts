import {beforeEach, describe, expect, it, vi} from 'vitest'

const state = vi.hoisted(() => ({get: vi.fn(), version: 0}))
vi.mock('@/utils/http', () => ({get: state.get, ApiError: class extends Error {
  constructor(message: string, public code: number) { super(message) }
}}))
vi.mock('@/api/contest', () => ({ContestType: {CONTEST: 1, HOMEWORK: 2}}))
vi.mock('@/stores/useToken', () => ({useToken: () => ({getSessionVersion: () => state.version})}))
import {getProfile, getProfileActivity, StaleProfileResponseError} from './index'

const id = '2098755579163770881'
const solution = {solutionId: '2098755579163770882', problemId: '2098755579163770883',
  title: '题解', effectiveVisibility: 'AUTHOR_ONLY', private_: false}
const response = (url: string) => {
  if (url.startsWith('/user-api')) return {data: {userId: id, userName: 'test'}}
  if (url.startsWith('/problem-api')) return {data: {userId: id, owner: true, solvedCount: 3, attemptedCount: 5}}
  return {data: {userId: id, solutions: [solution], solutionsTruncated: true}}
}

describe('profile three-domain aggregation', () => {
  beforeEach(() => {
    state.version = 0
    state.get.mockReset().mockImplementation(async (url: string) => response(url))
  })

  it('starts all requests together, preserves long IDs and takes content solutions only', async () => {
    const resolvers: Array<() => void> = []
    state.get.mockImplementation((url: string) => new Promise(resolve =>
      resolvers.push(() => resolve(response(url)))))
    const request = getProfile(id)
    expect(state.get).toHaveBeenCalledTimes(3)
    expect(state.get.mock.calls.map(([url]) => url)).toEqual([
      `/user-api/user/profile/${id}`, `/problem-api/profile/${id}`, `/content-api/profile/${id}/solutions`
    ])
    resolvers.forEach(resolve => resolve())
    const result = await request
    expect(result.user.userId).toBe(id)
    expect(result.activity.solutions[0]).toMatchObject(solution)
    expect(result.activity.solutionsTruncated).toBe(true)
    expect(result.activity.solutionsError).toBeUndefined()
  })

  it('keeps practice and contests with an explicit local error when content is offline', async () => {
    state.get.mockImplementation(async (url: string) => {
      if (url.startsWith('/content-api')) throw new Error('503')
      if (url.startsWith('/problem-api')) return {data: {...response(url).data,
        solutions: [{...solution, title: 'must not reuse old private cached solution'}]}}
      return response(url)
    })
    const result = await getProfile(id)
    expect(result.activity.solvedCount).toBe(3)
    expect(result.activity.attemptedCount).toBe(5)
    expect(result.activity.solutions).toEqual([])
    expect(result.activity.solutionsError).toBeTruthy()
  })

  it('normalizes legacy problem responses with removed fields without throwing', async () => {
    const activity = await getProfileActivity(id)
    expect(activity.solutions).toEqual([])
    expect(activity.solutionsTruncated).toBe(false)
  })

  it('discards responses from an old account session', async () => {
    const request = getProfile(id)
    state.version++
    await expect(request).rejects.toBeInstanceOf(StaleProfileResponseError)
  })

  it('discards old route/request generations', async () => {
    let current = true
    const request = getProfile(id, () => current)
    current = false
    await expect(request).rejects.toBeInstanceOf(StaleProfileResponseError)
  })

  it('mismatched content identity is a local error, not reused content', async () => {
    state.get.mockImplementation(async (url: string) => url.startsWith('/content-api')
      ? {data: {userId: '99', solutions: [solution]}} : response(url))
    const result = await getProfile(id)
    expect(result.activity.solutionsError).toBeTruthy()
    expect(result.activity.solutions).toEqual([])
  })
})
