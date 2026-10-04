import {computed, onScopeDispose, reactive, ref, toValue, watch, type MaybeRefOrGetter} from 'vue'
import type {IdType} from '@/api/common'
import {
  createCommentReply,
  createSolutionComment,
  deleteOwnComment,
  getCommentReplies,
  getSolutionComments,
  type CommentPage,
  type CommentVo,
} from '@/api/comment'
import {commentUiError, createCommentRequestIdentity, normalizeCommentText} from '@/utils/commentText'

const PAGE_SIZE = 20
const validId = (id: unknown): id is string => typeof id === 'string' && /^[1-9]\d{0,18}$/.test(id)
  && (id.length < 19 || id <= '9223372036854775807')

export interface ReplyPageState extends CommentPage {
  loaded: boolean
  loading: boolean
  error: string
}

const emptyPage = (): ReplyPageState => ({
  currentPage: 0, pageSize: PAGE_SIZE, totalRecords: 0, data: [], loaded: false, loading: false, error: '',
})

const increment = (raw: string | null | undefined, delta: 1 | -1): string => {
  try {
    const next = BigInt(raw || '0') + BigInt(delta)
    return (next < 0n ? 0n : next).toString()
  } catch (_) {
    return delta > 0 ? '1' : '0'
  }
}

export const useCommentThread = (solutionId: MaybeRefOrGetter<IdType | null | undefined>) => {
  const solutionIdValue = computed(() => toValue(solutionId))
  const roots = ref<CommentVo[]>([])
  const currentPage = ref(0)
  const pageSize = ref(PAGE_SIZE)
  const totalRecords = ref(0)
  const rootsLoaded = ref(false)
  const rootsLoading = ref(false)
  const rootsError = ref('')
  const replies = reactive<Record<string, ReplyPageState>>({})
  const changeRevision = ref(0)
  const lastChangeDelta = ref<1 | -1>(1)

  let generation = 0
  let disposed = false
  let rootsController: AbortController | null = null
  let rootsFlight: Promise<void> | null = null
  const replyControllers = new Map<string, AbortController>()
  const replyFlights = new Map<string, Promise<void>>()
  const mutationFlights = new Map<string, Promise<CommentVo | null | boolean>>()
  const appliedCreateRequests = new Set<string>()
  const deletedCommentIds = new Set<string>()

  const active = (version: number, id: IdType | null | undefined) =>
    !disposed && generation === version && solutionIdValue.value === id

  const abortRequests = () => {
    rootsController?.abort()
    rootsController = null
    replyControllers.forEach(controller => controller.abort())
    replyControllers.clear()
    replyFlights.clear()
    rootsFlight = null
  }

  const resetForSolution = () => {
    generation++
    abortRequests()
    roots.value = []
    currentPage.value = 0
    pageSize.value = PAGE_SIZE
    totalRecords.value = 0
    rootsLoaded.value = false
    rootsLoading.value = false
    rootsError.value = ''
    Object.keys(replies).forEach(key => delete replies[key])
    mutationFlights.clear()
    appliedCreateRequests.clear()
    deletedCommentIds.clear()
    changeRevision.value = 0
    lastChangeDelta.value = 1
    if (validId(solutionIdValue.value)) void loadRoots()
  }

  const loadRoots = (append = false): Promise<void> => {
    const id = solutionIdValue.value
    if (!validId(id) || disposed) return Promise.resolve()
    if (rootsFlight) return rootsFlight
    const page = append ? currentPage.value + 1 : 1
    const version = generation
    const requestController = new AbortController()
    rootsController = requestController
    rootsLoading.value = true
    rootsError.value = ''
    const request = getSolutionComments(id, page, requestController.signal).then(result => {
      if (!active(version, id) || requestController.signal.aborted) return
      const data = Array.isArray(result?.data) ? result.data : []
      if (append) {
        const seen = new Set(roots.value.map(item => item.commentId))
        roots.value = [...roots.value, ...data.filter(item => !seen.has(item.commentId))]
      } else {
        roots.value = data
      }
      data.forEach(item => ensureReplyState(item.commentId))
      currentPage.value = result.currentPage
      pageSize.value = result.pageSize
      totalRecords.value = result.totalRecords
      rootsLoaded.value = true
    }).catch(reason => {
      if (active(version, id) && !requestController.signal.aborted) rootsError.value = commentUiError(reason)
    }).finally(() => {
      if (active(version, id) && rootsFlight === request) {
        rootsLoading.value = false
        if (rootsController === requestController) rootsController = null
        rootsFlight = null
      }
    })
    rootsFlight = request
    return request
  }

  const ensureReplyState = (rootId: IdType) => {
    if (!replies[rootId]) replies[rootId] = emptyPage()
  }
  const replyState = (rootId: IdType): ReplyPageState => replies[rootId] || emptyPage()

  const loadReplies = (rootId: IdType, append = false, force = false): Promise<void> => {
    if (!validId(rootId) || disposed || !validId(solutionIdValue.value)) return Promise.resolve()
    ensureReplyState(rootId)
    const state = replies[rootId]
    if (!append && state.loaded && !force) return Promise.resolve()
    const existing = replyFlights.get(rootId)
    if (existing) return existing
    const id = solutionIdValue.value
    const page = append ? state.currentPage + 1 : 1
    const version = generation
    const requestController = new AbortController()
    replyControllers.set(rootId, requestController)
    state.loading = true
    state.error = ''
    const request = getCommentReplies(rootId, page, requestController.signal).then(result => {
      if (!active(version, id) || requestController.signal.aborted) return
      const data = Array.isArray(result?.data) ? result.data : []
      if (append) {
        const seen = new Set(state.data.map(item => item.commentId))
        state.data = [...state.data, ...data.filter(item => !seen.has(item.commentId))]
      } else {
        state.data = data
      }
      state.currentPage = result.currentPage
      state.pageSize = result.pageSize
      state.totalRecords = result.totalRecords
      state.loaded = true
    }).catch(reason => {
      if (active(version, id) && !requestController.signal.aborted) state.error = commentUiError(reason)
    }).finally(() => {
      if (active(version, id) && replyFlights.get(rootId) === request) {
        state.loading = false
        if (replyControllers.get(rootId) === requestController) replyControllers.delete(rootId)
        replyFlights.delete(rootId)
      }
    })
    replyFlights.set(rootId, request)
    return request
  }

  const applyCreatedRoot = (comment: CommentVo, requestId: string) => {
    if (appliedCreateRequests.has(requestId)) return
    appliedCreateRequests.add(requestId)
    rootsController?.abort()
    rootsController = null
    rootsFlight = null
    rootsLoading.value = false
    roots.value = [comment, ...roots.value.filter(item => item.commentId !== comment.commentId)].slice(0, pageSize.value)
    currentPage.value = 1
    totalRecords.value = Math.max(1, totalRecords.value + (rootsLoaded.value ? 1 : 0))
    rootsLoaded.value = true
    ensureReplyState(comment.commentId)
    void loadRoots()
    changeRevision.value++
    lastChangeDelta.value = 1
  }

  const applyCreatedReply = (rootId: IdType, comment: CommentVo, requestId: string) => {
    if (appliedCreateRequests.has(requestId)) return
    appliedCreateRequests.add(requestId)
    const updateCount = (list: CommentVo[]) => list.map(item => item.commentId === rootId
      ? {...item, replyCount: increment(item.replyCount, 1)} : item)
    roots.value = updateCount(roots.value)
    Object.values(replies).forEach(state => { state.data = updateCount(state.data) })
    const requestController = replyControllers.get(rootId)
    requestController?.abort()
    replyControllers.delete(rootId)
    replyFlights.delete(rootId)
    const state = replies[rootId]
    if (state) {
      if (!state.loaded) {
        state.loaded = true
        state.currentPage = 1
        state.pageSize = PAGE_SIZE
        state.totalRecords = 1
        state.data = [comment]
      } else {
        state.totalRecords++
        if (state.currentPage >= 1 && state.data.length < state.pageSize
          && !state.data.some(item => item.commentId === comment.commentId)) state.data = [...state.data, comment]
      }
      void loadReplies(rootId, false, true)
    }
    changeRevision.value++
    lastChangeDelta.value = 1
  }

  const createRoot = (rawContent: string, clientRequestId: string): Promise<CommentVo | null> => {
    const id = solutionIdValue.value
    if (!validId(id) || disposed) return Promise.resolve(null)
    const key = `root:${id}:${clientRequestId}`
    const current = mutationFlights.get(key)
    if (current) return current as Promise<CommentVo | null>
    const version = generation
    const request = createSolutionComment(id, {
      content: normalizeCommentText(rawContent), clientRequestId,
    }).then(comment => {
      if (!active(version, id)) return null
      applyCreatedRoot(comment, clientRequestId)
      return comment
    }).finally(() => { if (mutationFlights.get(key) === request) mutationFlights.delete(key) })
    mutationFlights.set(key, request)
    return request
  }

  const createReply = (rootId: IdType, parentId: IdType, rawContent: string,
                       clientRequestId: string): Promise<CommentVo | null> => {
    const id = solutionIdValue.value
    if (!validId(id) || !validId(rootId) || !validId(parentId) || disposed) return Promise.resolve(null)
    const key = `reply:${id}:${rootId}:${parentId}:${clientRequestId}`
    const current = mutationFlights.get(key)
    if (current) return current as Promise<CommentVo | null>
    const version = generation
    const request = createCommentReply(parentId, {
      content: normalizeCommentText(rawContent), clientRequestId,
    }).then(comment => {
      if (!active(version, id)) return null
      applyCreatedReply(rootId, comment, clientRequestId)
      return comment
    }).finally(() => { if (mutationFlights.get(key) === request) mutationFlights.delete(key) })
    mutationFlights.set(key, request)
    return request
  }

  const replaceWithTombstone = (list: CommentVo[], id: IdType): {data: CommentVo[]; changed: boolean} => {
    let changed = false
    const data = list.map(item => {
      if (item.commentId !== id || item.state !== 'VISIBLE') return item
      changed = true
      return {...item, state: 'AUTHOR_DELETED' as const, content: null, canDelete: false}
    })
    return {data, changed}
  }

  const deleteComment = (comment: CommentVo): Promise<boolean> => {
    if (!comment?.commentId || disposed) return Promise.resolve(false)
    const key = `delete:${solutionIdValue.value}:${comment.commentId}`
    const current = mutationFlights.get(key)
    if (current) return current as Promise<boolean>
    const id = solutionIdValue.value
    const version = generation
    const request = deleteOwnComment(comment.commentId).then(deleted => {
      if (!deleted || !active(version, id) || deletedCommentIds.has(comment.commentId)) return false
      let changed = false
      let result = replaceWithTombstone(roots.value, comment.commentId)
      roots.value = result.data
      changed ||= result.changed
      Object.keys(replies).forEach(rootId => {
        result = replaceWithTombstone(replies[rootId].data, comment.commentId)
        replies[rootId].data = result.data
        changed ||= result.changed
      })
      if (changed) {
        deletedCommentIds.add(comment.commentId)
        if (comment.rootId) {
          const rootId = comment.rootId
          const updateCount = (list: CommentVo[]) => list.map(item => item.commentId === rootId
            ? {...item, replyCount: increment(item.replyCount, -1)} : item)
          roots.value = updateCount(roots.value)
          Object.values(replies).forEach(state => { state.data = updateCount(state.data) })
          if (replies[rootId]) replies[rootId].totalRecords = Math.max(0, replies[rootId].totalRecords - 1)
        }
        changeRevision.value++
        lastChangeDelta.value = -1
      }
      return changed
    }).finally(() => { if (mutationFlights.get(key) === request) mutationFlights.delete(key) })
    mutationFlights.set(key, request)
    return request
  }

  watch(solutionIdValue, resetForSolution, {immediate: true})
  onScopeDispose(() => {
    disposed = true
    generation++
    abortRequests()
    mutationFlights.clear()
  })

  return {
    roots, currentPage, pageSize, totalRecords, rootsLoaded, rootsLoading, rootsError,
    replies, changeRevision, lastChangeDelta, loadRoots, loadReplies, replyState, createRoot, createReply, deleteComment,
  }
}

export {createCommentRequestIdentity}
