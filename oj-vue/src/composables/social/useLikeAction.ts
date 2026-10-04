import {computed, onScopeDispose, ref, toValue, watch, type MaybeRefOrGetter} from 'vue'
import type {IdType} from '@/api/common'
import {likeComment, unlikeComment} from '@/api/comment'
import {likeSolution, unlikeSolution, type LikeResult} from '@/api/solution'
import {commentUiError} from '@/utils/commentText'

export type LikeResource = 'solution' | 'comment'

export const useLikeAction = (
  resource: LikeResource,
  id: MaybeRefOrGetter<IdType | null | undefined>,
  initialLiked: MaybeRefOrGetter<boolean | null | undefined>,
  initialCount: MaybeRefOrGetter<string | number | null | undefined>,
) => {
  const liked = ref(Boolean(toValue(initialLiked)))
  const likeCount = ref<string | number | null>(toValue(initialCount) ?? null)
  const pending = ref(false)
  const error = ref('')
  let generation = 0
  let disposed = false
  let controller: AbortController | null = null
  let flight: Promise<boolean> | null = null

  const idValue = computed(() => toValue(id))
  const active = (version: number, targetId: IdType | null | undefined) =>
    !disposed && generation === version && idValue.value === targetId

  watch([idValue, () => toValue(initialLiked), () => toValue(initialCount)], ([targetId, nextLiked, nextCount]) => {
    generation++
    controller?.abort()
    controller = null
    flight = null
    pending.value = false
    error.value = ''
    liked.value = Boolean(nextLiked)
    likeCount.value = nextCount ?? null
    if (!targetId) liked.value = false
  })

  const toggle = (): Promise<boolean> => {
    if (flight) return flight
    const targetId = idValue.value
    if (!targetId || disposed) return Promise.resolve(false)
    const version = generation
    const nextLiked = !liked.value
    const abortController = new AbortController()
    controller = abortController
    pending.value = true
    error.value = ''
    const mutate = resource === 'solution'
      ? (nextLiked ? likeSolution : unlikeSolution)
      : (nextLiked ? likeComment : unlikeComment)
    const request = mutate(targetId, abortController.signal).then((result: LikeResult) => {
      if (!active(version, targetId)) return false
      liked.value = result.liked
      likeCount.value = result.likeCount ?? null
      return true
    }).catch(reason => {
      if (active(version, targetId) && !abortController.signal.aborted) {
        error.value = commentUiError(reason)
      }
      return false
    }).finally(() => {
      if (active(version, targetId)) {
        pending.value = false
        controller = null
        flight = null
      }
    })
    flight = request
    return request
  }

  onScopeDispose(() => {
    disposed = true
    generation++
    controller?.abort()
    controller = null
  })

  return {liked, likeCount, pending, error, toggle}
}
