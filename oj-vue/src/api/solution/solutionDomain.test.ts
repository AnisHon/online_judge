import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'

const http = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  query: vi.fn(),
  remove: vi.fn(),
  direct: {get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn()}
}))

vi.mock('@/utils/http.ts', () => ({
  get: http.get,
  post: http.post,
  put: http.put,
  query: http.query,
  service: http.direct,
  ApiError: class ApiError extends Error {
    status: number
    constructor(message: string, status: number) {
      super(message)
      this.status = status
    }
  }
}))

vi.mock('@/utils/simpleCRUD.ts', () => ({remove: http.remove}))

import {
  addSolution,
  deleteSolution,
  deleteSolutionAdmin,
  editSolution,
  getSolution,
  getSolutionAdmin,
  listSolution,
  listSolutionAdmin,
  lowDown,
  recentSolution,
  topUp
} from './index'

describe('solution APIs use the content domain while preserving existing operation paths', () => {
  beforeEach(() => {
    http.get.mockResolvedValue({data: {solutionId: '9007199254740993'}})
    http.query.mockResolvedValue({data: {data: []}})
    http.post.mockResolvedValue({data: true})
    http.put.mockResolvedValue({data: true})
    http.remove.mockResolvedValue(undefined)
    http.direct.get.mockResolvedValue({data: {solutionId: '9007199254740993', data: []}})
    http.direct.post.mockImplementation(async path => ({data: path.endsWith('/delete') ? {deleted: true} : true}))
    http.direct.put.mockResolvedValue({data: true})
    http.direct.delete.mockResolvedValue({data: true})
  })

  afterEach(() => vi.clearAllMocks())

  it('uses content-api for detail, admin detail, list, CRUD, delete, top-up and recent', async () => {
    const form = {title: 'solution', private_: false, content: 'body'}
    const page = {currentPage: 1, pageSize: 10, asc: false}

    await getSolution('9007199254740993')
    await getSolutionAdmin('9007199254740993')
    await listSolution(page)
    await listSolutionAdmin(page)
    await addSolution(form)
    await addSolution(form, true)
    await editSolution({...form, solutionId: '9007199254740993'})
    await editSolution({...form, solutionId: '9007199254740993'}, true)
    await deleteSolution('9007199254740993')
    await deleteSolutionAdmin('9007199254740993', '重复内容')
    await topUp('9007199254740993')
    await lowDown('9007199254740993')
    await recentSolution()

    expect(http.get.mock.calls.map(([path]) => path)).toEqual([
      '/content-api/solution',
      '/content-api/solution/recent'
    ])
    expect(http.query.mock.calls.map(([path]) => path)).toEqual([
      '/content-api/solution/list'
    ])
    expect(http.post.mock.calls.map(([path]) => path)).toEqual([
      '/content-api/solution'
    ])
    expect(http.direct.get.mock.calls.map(([path]) => path)).toEqual([
      '/content-api/solution/admin/9007199254740993', '/content-api/solution/admin/list'
    ])
    expect(http.direct.post).toHaveBeenCalledWith('/content-api/solution/admin/9007199254740993/delete',
      {reason: '重复内容'}, {signal: undefined, localForbidden: true})
    expect(http.put.mock.calls.map(([path]) => path)).toEqual([
      '/content-api/solution'
    ])
    expect(http.direct.put.mock.calls.map(([path]) => path)).toEqual([
      '/content-api/solution/admin',
      '/content-api/solution/admin/topUp/9007199254740993',
      '/content-api/solution/admin/lowDown/9007199254740993'
    ])
    expect(http.direct.post).toHaveBeenCalledWith('/content-api/solution/admin', form, {localForbidden: true})
    expect(http.remove).not.toHaveBeenCalled()
    expect(http.direct.delete).toHaveBeenCalledWith('/content-api/solution/9007199254740993', {localForbidden: true})
    expect(JSON.stringify([
      http.get.mock.calls, http.query.mock.calls, http.post.mock.calls,
      http.put.mock.calls, http.remove.mock.calls
    ])).not.toContain('/problem-api/solution')
  })

  it('rejects an ID on create, and does not turn a failed deletion/moderation into success', async () => {
    const {moderateSolution} = await import('./index')
    await expect(addSolution({solutionId: '2098755579163770881', title: 'x', content: 'x', private_: false}, true))
      .rejects.toMatchObject({status: 400})
    expect(http.post).not.toHaveBeenCalled()
    http.direct.post.mockResolvedValue({data: {deleted: false}})
    await expect(deleteSolutionAdmin('2098755579163770881', '原因')).rejects.toMatchObject({status: 409})
    http.direct.delete.mockResolvedValue({data: false})
    await expect(deleteSolution('2098755579163770881')).rejects.toMatchObject({status: 409})
    http.direct.post.mockResolvedValue({data: {moderationState: 'NORMAL'}})
    await expect(moderateSolution('2098755579163770881', 'AUTHOR_ONLY', '原因')).rejects.toMatchObject({status: 409})
    await expect(moderateSolution('2098755579163770881', 'RESTORE', '原因')).resolves.toBeUndefined()
  })

  it('uses T09/T12 solution interaction endpoints without changing legacy content APIs', async () => {
    const {getSolutionModeration, likeSolution, setSolutionCommentsState, unlikeSolution} = await import('./index')
    await likeSolution('2098755579163770881')
    await unlikeSolution('2098755579163770881')
    await setSolutionCommentsState('2098755579163770881', false)
    await getSolutionModeration('2098755579163770881')
    expect(http.direct.put).toHaveBeenNthCalledWith(1, '/content-api/solution/2098755579163770881/like', undefined,
      {signal: undefined, localForbidden: true})
    expect(http.direct.delete).toHaveBeenCalledWith('/content-api/solution/2098755579163770881/like',
      {signal: undefined, localForbidden: true})
    expect(http.direct.put).toHaveBeenNthCalledWith(2, '/content-api/solution/2098755579163770881/comments-state',
      {open: false}, {localForbidden: true})
    expect(http.direct.get).toHaveBeenCalledWith('/content-api/solution/2098755579163770881/moderation',
      {signal: undefined, localForbidden: true})
  })
})
