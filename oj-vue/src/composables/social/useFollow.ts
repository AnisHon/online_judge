import {computed, onScopeDispose, ref, toValue, watch, type MaybeRefOrGetter} from 'vue'
import {followUser, getFollowSummary, isFollowUserId, unfollowUser, type FollowSummary} from '@/api/follow'
import {useToken} from '@/stores/useToken'
import {useUserStore} from '@/stores/useUserStore'

export const useFollow = (targetId: MaybeRefOrGetter<string | null>) => {
  const token = useToken()
  const user = useUserStore()
  const summary = ref<FollowSummary | null>(null)
  const loading = ref(false)
  const pending = ref(false)
  const error = ref('')
  const target = computed(() => toValue(targetId))
  const available = computed(() => isFollowUserId(target.value) && token.authStatus === 'authenticated')
  const canFollow = computed(() => available.value && isFollowUserId(user.user?.userId)
    && target.value !== user.user?.userId)
  let generation = 0
  let disposed = false
  let controller: AbortController | undefined
  let flight: Promise<void> | undefined

  const refresh = async () => {
    if (!available.value || loading.value || pending.value) return
    const id = target.value as string
    const request = ++generation
    controller?.abort()
    controller = new AbortController()
    loading.value = true
    error.value = ''
    try {
      const result = await getFollowSummary(id, controller.signal)
      if (!disposed && request === generation) summary.value = result
    } catch {
      if (!disposed && request === generation) error.value = '关注信息加载失败，请重试'
    } finally {
      if (!disposed && request === generation) loading.value = false
    }
  }
  const toggle = (): Promise<void> => {
    if (flight) return flight
    if (!canFollow.value || loading.value || !summary.value) return Promise.resolve()
    const id = target.value as string
    const request = ++generation
    const operation = summary.value.following ? unfollowUser : followUser
    controller?.abort()
    controller = new AbortController()
    const signal = controller.signal
    pending.value = true
    error.value = ''
    flight = (async () => {
      try {
        const result = await operation(id, signal)
        if (!disposed && request === generation) summary.value = result
      } catch {
        if (!disposed && request === generation) error.value = '操作未完成，请重试'
      } finally {
        if (!disposed && request === generation) {
          pending.value = false
          flight = undefined
        }
      }
    })()
    return flight
  }
  watch([target, () => token.sessionVersion, () => token.authStatus, () => user.user?.userId], () => {
    generation++
    controller?.abort()
    flight = undefined
    summary.value = null
    pending.value = false
    loading.value = false
    error.value = ''
    void refresh()
  }, {immediate: true, flush: 'sync'})
  onScopeDispose(() => {
    disposed = true
    generation++
    controller?.abort()
  })
  return {summary, loading, pending, error, available, canFollow, refresh, toggle}
}
