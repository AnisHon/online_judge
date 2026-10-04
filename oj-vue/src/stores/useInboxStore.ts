import {computed, ref} from 'vue'
import {defineStore} from 'pinia'
import type {IdType} from '@/api/common'
import {
  getNotificationPage,
  getUnreadNotificationCount,
  markNotificationRead,
  markNotificationsReadThrough,
  type InboxNotification,
} from '@/api/notification'
import {useToken} from '@/stores/useToken'
import {useUserStore} from '@/stores/useUserStore'
import {compareDecimalIds, isValidNotificationId, maximumObservedNotificationId} from '@/utils/notificationDisplay'

interface SessionSnapshot {
  version: number
  userId: string
  epoch: number
}

interface InFlightCount {
  key: string
  promise: Promise<number>
}

export const useInboxStore = defineStore('inbox', () => {
  const notifications = ref<InboxNotification[]>([])
  const unreadCount = ref(0)
  const currentPage = ref(1)
  const pageSize = ref(20)
  const totalRecords = ref(0)
  const unreadOnly = ref(false)
  const hasLoadedPage = ref(false)
  const isLoadingPage = ref(false)
  const pendingReadIds = ref<string[]>([])
  const isMarkingAllRead = ref(false)

  const tokenStore = useToken()
  const userStore = useUserStore()
  let epoch = 0
  let pageRequestVersion = 0
  let unreadRequest: InFlightCount | null = null

  const unreadBadge = computed(() => unreadCount.value > 99 ? '99+' : String(unreadCount.value))

  const getSnapshot = (): SessionSnapshot | null => {
    const userId = userStore.user?.userId
    if (tokenStore.authStatus !== 'authenticated' || !tokenStore.hasToken() || userId === undefined || userId === null) {
      return null
    }
    return {version: tokenStore.getSessionVersion(), userId: String(userId), epoch}
  }

  const isSnapshotCurrent = (snapshot: SessionSnapshot, guard: () => boolean = () => true) => {
    const current = getSnapshot()
    return !!current
      && snapshot.version === current.version
      && snapshot.userId === current.userId
      && snapshot.epoch === current.epoch
      && guard()
  }

  const clear = () => {
    epoch++
    pageRequestVersion++
    unreadRequest = null
    notifications.value = []
    unreadCount.value = 0
    currentPage.value = 1
    totalRecords.value = 0
    unreadOnly.value = false
    hasLoadedPage.value = false
    isLoadingPage.value = false
    pendingReadIds.value = []
    isMarkingAllRead.value = false
  }

  const loadPage = async (page = currentPage.value, onlyUnread = unreadOnly.value): Promise<boolean> => {
    const snapshot = getSnapshot()
    if (!snapshot) return false
    const requestVersion = ++pageRequestVersion
    isLoadingPage.value = true
    try {
      const result = await getNotificationPage({currentPage: page, pageSize: pageSize.value, unreadOnly: onlyUnread})
      if (!isSnapshotCurrent(snapshot) || requestVersion !== pageRequestVersion) return false
      notifications.value = Array.isArray(result.data) ? result.data : []
      currentPage.value = result.currentPage || page
      pageSize.value = result.pageSize || pageSize.value
      totalRecords.value = result.totalRecords || 0
      unreadOnly.value = onlyUnread
      hasLoadedPage.value = true
      return true
    } finally {
      if (isSnapshotCurrent(snapshot) && requestVersion === pageRequestVersion) isLoadingPage.value = false
    }
  }

  const refreshUnreadCount = async (guard: () => boolean = () => true): Promise<void> => {
    const snapshot = getSnapshot()
    if (!snapshot) return
    const requestKey = `${snapshot.version}:${snapshot.userId}:${snapshot.epoch}`
    let request = unreadRequest
    if (!request || request.key !== requestKey) {
      const promise = getUnreadNotificationCount()
      request = {key: requestKey, promise}
      unreadRequest = request
      promise.finally(() => {
        if (unreadRequest?.promise === promise) unreadRequest = null
      }).catch(() => undefined)
    }
    const count = await request.promise
    if (isSnapshotCurrent(snapshot, guard)) unreadCount.value = count
  }

  const refreshUnreadCountQuietly = async (snapshot: SessionSnapshot) => {
    if (!isSnapshotCurrent(snapshot)) return
    try {
      await refreshUnreadCount(() => isSnapshotCurrent(snapshot))
    } catch {
      // The successful read action remains successful if the badge refresh is temporarily unavailable.
    }
  }

  const markRead = async (notificationId: IdType): Promise<boolean> => {
    const snapshot = getSnapshot()
    const id = String(notificationId)
    if (!snapshot || pendingReadIds.value.includes(id)) return false
    pendingReadIds.value = [...pendingReadIds.value, id]
    try {
      await markNotificationRead(id)
      if (!isSnapshotCurrent(snapshot)) return false
      const row = notifications.value.find(item => String(item.notificationId) === id)
      if (row && !row.readAt) {
        row.readAt = new Date().toISOString()
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
      await refreshUnreadCountQuietly(snapshot)
      return true
    } finally {
      if (isSnapshotCurrent(snapshot)) pendingReadIds.value = pendingReadIds.value.filter(item => item !== id)
    }
  }

  const markAllRead = async (throughId: IdType): Promise<boolean> => {
    const snapshot = getSnapshot()
    const observedMaximum = maximumObservedNotificationId(notifications.value)
    if (!snapshot || isMarkingAllRead.value || !isValidNotificationId(String(throughId))
      || observedMaximum !== String(throughId)) return false
    isMarkingAllRead.value = true
    try {
      await markNotificationsReadThrough(String(throughId))
      if (!isSnapshotCurrent(snapshot)) return false
      const readAt = new Date().toISOString()
      notifications.value = notifications.value.map(item =>
        isValidNotificationId(String(item.notificationId))
        && compareDecimalIds(String(item.notificationId), String(throughId)) <= 0
        && !item.readAt
          ? {...item, readAt}
          : item
      )
      await refreshUnreadCountQuietly(snapshot)
      return true
    } finally {
      if (isSnapshotCurrent(snapshot)) isMarkingAllRead.value = false
    }
  }

  return {
    notifications,
    unreadCount,
    unreadBadge,
    currentPage,
    pageSize,
    totalRecords,
    unreadOnly,
    hasLoadedPage,
    isLoadingPage,
    pendingReadIds,
    isMarkingAllRead,
    loadPage,
    refreshUnreadCount,
    markRead,
    markAllRead,
    clear,
  }
})
