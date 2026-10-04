import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {AxiosError, type AxiosAdapter} from 'axios'

const mocks = vi.hoisted(() => ({replace: vi.fn(async () => undefined), error: vi.fn(), clear: vi.fn()}))
vi.mock('@/router', () => ({default: {currentRoute: {value: {name: 'profile'}}, replace: mocks.replace}}))
vi.mock('element-plus', () => ({ElNotification: {error: mocks.error, warning: vi.fn(), success: vi.fn()}}))
vi.mock('@/stores/useToken', () => ({useToken: () => ({
  token: 'in-memory-test-token', getSessionVersion: () => 1, hasToken: () => true, clearToken: mocks.clear,
})}))
vi.mock('@/utils/authSession', () => ({
  refreshAccessToken: vi.fn(), SessionChangedError: class extends Error {},
}))
import {service, ApiError} from '@/utils/http'
import {followUser} from './index'

const previousAdapter = service.defaults.adapter
const forbiddenAdapter = (httpStatus: number): AxiosAdapter => async config => {
  const response = {config, status: httpStatus, statusText: 'Forbidden', headers: {},
    data: {code: 403, message: 'forbidden', data: null}}
  if (httpStatus === 403) throw new AxiosError('Forbidden', 'ERR_BAD_REQUEST', config, undefined, response)
  return response
}

describe('follow local permission failure with shared HTTP client', () => {
  beforeEach(() => vi.clearAllMocks())
  afterEach(() => {service.defaults.adapter = previousAdapter})

  it.each([200, 403])('keeps follow failures inline for HTTP %s while preserving Authorization', async status => {
    const adapter = forbiddenAdapter(status)
    service.defaults.adapter = vi.fn(adapter)
    await expect(followUser('51')).rejects.toBeInstanceOf(ApiError)
    expect(mocks.error).not.toHaveBeenCalled()
    expect(mocks.replace).not.toHaveBeenCalled()
    expect(mocks.clear).not.toHaveBeenCalled()
    const request = vi.mocked(service.defaults.adapter as AxiosAdapter).mock.calls[0][0]
    expect(request.headers.get('Authorization')).toBe('Bearer in-memory-test-token')
  })

  it('leaves ordinary request permission handling unchanged', async () => {
    service.defaults.adapter = forbiddenAdapter(403)
    await expect(service.get('/ordinary-api')).rejects.toBeInstanceOf(ApiError)
    expect(mocks.error).toHaveBeenCalledWith('拒绝访问')
    expect(mocks.replace).toHaveBeenCalledWith({name: '403'})
  })
})
