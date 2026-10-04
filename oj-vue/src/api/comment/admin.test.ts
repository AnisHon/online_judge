import {beforeEach, describe, expect, it, vi} from 'vitest'

const service = vi.hoisted(() => ({get: vi.fn(), post: vi.fn()}))
vi.mock('@/utils/http', () => ({service, ApiError: class extends Error {
  constructor(message: string, readonly code: number) { super(message) }
}}))
import {deleteCommentAdmin, getAdminComments} from './admin'

describe('administration comment contract', () => {
  beforeEach(() => { vi.clearAllMocks(); service.get.mockResolvedValue({data: {data: []}}) })
  it('scopes comments to the current solution and preserves Long IDs', async () => {
    const id = '2098755579163770881'
    await getAdminComments(id, 2, 'ADMIN_DELETED')
    expect(service.get).toHaveBeenCalledWith('/content-api/comment/admin/page', {
      params: {solutionId: id, currentPage: 2, pageSize: 20, state: 'ADMIN_DELETED'},
      signal: undefined, localForbidden: true,
    })
    service.post.mockResolvedValue({data: {deleted: true}})
    await deleteCommentAdmin(id, '原因')
    expect(service.post).toHaveBeenCalledWith(`/content-api/comment/admin/${id}/delete`,
      {reason: '原因'}, {signal: undefined, localForbidden: true})
  })
  it('rejects false or missing deletion results and propagates forbidden errors', async () => {
    for (const data of [{deleted: false}, null, {}]) {
      service.post.mockResolvedValue({data})
      await expect(deleteCommentAdmin('2098755579163770881', '原因')).rejects.toMatchObject({code: 409})
    }
    service.post.mockRejectedValue({code: 403})
    await expect(deleteCommentAdmin('2098755579163770881', '原因')).rejects.toMatchObject({code: 403})
  })
})
