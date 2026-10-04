import {beforeEach, describe, expect, it, vi} from 'vitest'

const service = vi.hoisted(() => ({get: vi.fn(), post: vi.fn(), put: vi.fn(), delete: vi.fn()}))
vi.mock('@/utils/http', () => ({service}))

import {
  createCommentReply, createSolutionComment, deleteOwnComment, getCommentModeration,
  getCommentReplies, getSolutionComments, likeComment, unlikeComment,
} from './index'

const solutionId = '2098755579163770881'
const commentId = '2098755579163770999'

describe('content comment API contract', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    service.get.mockResolvedValue({data: {currentPage: 1, pageSize: 20, totalRecords: 0, data: []}})
    service.post.mockResolvedValue({data: {commentId}})
    service.put.mockResolvedValue({data: {liked: true, likeCount: '1'}})
    service.delete.mockResolvedValue({data: {liked: false, likeCount: '0'}})
  })

  it('uses content-api, preserves Long IDs, page state and idempotency request body', async () => {
    const signal = new AbortController().signal
    const body = {content: '<script>literal</script>', clientRequestId: 'a3d66c4a-1c08-4ec5-a5a7-dfbecf11d28e'}
    await getSolutionComments(solutionId, 2, signal)
    await createSolutionComment(solutionId, body)
    await getCommentReplies(commentId, 3, signal)
    await createCommentReply(commentId, body)
    await deleteOwnComment(commentId)
    await getCommentModeration(commentId)

    expect(service.get).toHaveBeenNthCalledWith(1, `/content-api/solution/${solutionId}/comments`, {
      params: {currentPage: 2, pageSize: 20}, signal, localForbidden: true,
    })
    expect(service.post).toHaveBeenNthCalledWith(1, `/content-api/solution/${solutionId}/comments`, body, {
      signal: undefined, localForbidden: true,
    })
    expect(service.get).toHaveBeenNthCalledWith(2, `/content-api/comment/${commentId}/replies`, {
      params: {currentPage: 3, pageSize: 20}, signal, localForbidden: true,
    })
    expect(service.post).toHaveBeenNthCalledWith(2, `/content-api/comment/${commentId}/replies`, body, {
      signal: undefined, localForbidden: true,
    })
    expect(service.delete).toHaveBeenNthCalledWith(1, `/content-api/comment/${commentId}`, {localForbidden: true})
    expect(service.get).toHaveBeenNthCalledWith(3, `/content-api/comment/${commentId}/moderation`, {
      signal: undefined, localForbidden: true,
    })
  })

  it('sets comment likes by PUT/DELETE and never exposes a toggle API', async () => {
    const signal = new AbortController().signal
    await likeComment(commentId, signal)
    await unlikeComment(commentId, signal)
    expect(service.put).toHaveBeenCalledWith(`/content-api/comment/${commentId}/like`, undefined, {
      signal, localForbidden: true,
    })
    expect(service.delete).toHaveBeenNthCalledWith(1, `/content-api/comment/${commentId}/like`, {
      signal, localForbidden: true,
    })
  })
})
