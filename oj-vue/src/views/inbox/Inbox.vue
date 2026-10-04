<template>
  <main class="inbox-page">
    <header class="inbox-header">
      <div>
        <span class="inbox-header__eyebrow">PERSONAL INBOX</span>
        <h1>消息</h1>
        <p>与你的题解和评论相关的动态会出现在这里。</p>
      </div>
      <div class="inbox-header__actions">
        <el-button :loading="refreshing" @click="refresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-button
          v-if="inbox.unreadCount > 0 && maximumId"
          type="primary"
          plain
          :loading="inbox.isMarkingAllRead"
          @click="markAllRead"
        >
          全部标为已读
        </el-button>
      </div>
    </header>

    <section class="inbox-panel" aria-label="个人消息列表">
      <div class="inbox-toolbar">
        <div class="inbox-filters" role="tablist" aria-label="消息筛选">
          <button
            v-for="filter in filters"
            :key="filter.value ? 'unread' : 'all'"
            type="button"
            role="tab"
            :aria-selected="inbox.unreadOnly === filter.value"
            :class="{'inbox-filters__item--active': inbox.unreadOnly === filter.value}"
            class="inbox-filters__item"
            @click="selectFilter(filter.value)"
          >
            {{ filter.label }}
            <span v-if="filter.value && inbox.unreadCount" class="inbox-filters__count">{{ inbox.unreadBadge }}</span>
          </button>
        </div>
        <span v-if="inbox.totalRecords > 0" class="inbox-total">共 {{ inbox.totalRecords }} 条</span>
      </div>

      <div v-if="pageError" class="inbox-state inbox-state--error" role="alert">
        <span>{{ pageError }}</span>
        <el-button link type="primary" @click="loadPage">重试</el-button>
      </div>
      <div v-else-if="actionMessage" class="inbox-inline-message" role="status">{{ actionMessage }}</div>

      <div v-if="inbox.isLoadingPage && !inbox.hasLoadedPage" class="inbox-skeleton" aria-label="正在加载消息">
        <div v-for="index in 4" :key="index" class="inbox-skeleton__row">
          <el-skeleton animated>
            <template #template>
              <div class="inbox-skeleton__shape"><el-skeleton-item variant="circle" /><div><el-skeleton-item variant="text" /><el-skeleton-item variant="text" /></div></div>
            </template>
          </el-skeleton>
        </div>
      </div>
      <el-empty
        v-else-if="inbox.hasLoadedPage && inbox.notifications.length === 0"
        :description="inbox.unreadOnly ? '没有未读消息' : '暂时没有消息'"
      />
      <div v-else-if="inbox.notifications.length" class="inbox-list">
        <NotificationItem
          v-for="notification in inbox.notifications"
          :key="notification.notificationId"
          :notification="notification"
          :pending="inbox.pendingReadIds.includes(String(notification.notificationId))"
          @open="openNotification"
          @mark-read="markOneRead"
        />
      </div>

      <el-pagination
        v-if="inbox.totalRecords > inbox.pageSize"
        class="inbox-pagination"
        background
        layout="prev, pager, next"
        :current-page="inbox.currentPage"
        :page-size="inbox.pageSize"
        :total="inbox.totalRecords"
        @current-change="changePage"
      />
    </section>
  </main>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from 'vue'
import {useRouter} from 'vue-router'
import {Refresh} from '@element-plus/icons-vue'
import type {InboxNotification} from '@/api/notification'
import NotificationItem from '@/components/NotificationList/NotificationItem.vue'
import {useInboxStore} from '@/stores/useInboxStore'
import {maximumObservedNotificationId, notificationTarget} from '@/utils/notificationDisplay'

const inbox = useInboxStore()
const router = useRouter()
const refreshing = ref(false)
const pageError = ref('')
const actionMessage = ref('')
const filters = [{label: '全部', value: false}, {label: '未读', value: true}] as const
const maximumId = computed(() => maximumObservedNotificationId(inbox.notifications))

const loadPage = async (page = 1, unreadOnly = inbox.unreadOnly) => {
  pageError.value = ''
  actionMessage.value = ''
  try {
    await inbox.loadPage(page, unreadOnly)
  } catch (error) {
    pageError.value = error instanceof Error ? error.message : '消息加载失败，请稍后重试'
  }
}

const refresh = async () => {
  refreshing.value = true
  pageError.value = ''
  try {
    const results = await Promise.allSettled([
      inbox.loadPage(inbox.currentPage, inbox.unreadOnly),
      inbox.refreshUnreadCount(),
    ])
    if (results[0].status === 'rejected') {
      pageError.value = results[0].reason instanceof Error ? results[0].reason.message : '消息加载失败，请稍后重试'
    }
    if (results[1].status === 'rejected') {
      actionMessage.value = '未读数量暂时无法更新，请稍后重试。'
    }
  } finally {
    refreshing.value = false
  }
}

const selectFilter = (unreadOnly: boolean) => {
  if (inbox.unreadOnly !== unreadOnly) void loadPage(1, unreadOnly)
}

const changePage = (page: number) => void loadPage(page, inbox.unreadOnly)

const markOneRead = async (notification: InboxNotification) => {
  actionMessage.value = ''
  try {
    const updated = await inbox.markRead(notification.notificationId)
    if (!updated) actionMessage.value = '这条消息已失效，请刷新列表。'
  } catch (error) {
    actionMessage.value = error instanceof Error ? error.message : '暂时无法标记为已读，请稍后重试。'
  }
}

const markAllRead = async () => {
  const throughId = maximumId.value
  if (!throughId) return
  actionMessage.value = ''
  try {
    const updated = await inbox.markAllRead(throughId)
    if (!updated) actionMessage.value = '消息列表已变化，请刷新后重试。'
  } catch (error) {
    actionMessage.value = error instanceof Error ? error.message : '暂时无法标记消息，请稍后重试。'
  }
}

const openNotification = async (notification: InboxNotification) => {
  actionMessage.value = ''
  const target = notificationTarget(notification)
  if (!target) {
    actionMessage.value = '该消息对应的内容目前不可访问。'
    return
  }
  if (!notification.readAt) {
    try {
      const updated = await inbox.markRead(notification.notificationId)
      if (!updated) {
        actionMessage.value = '消息状态已变化，请刷新后重试。'
        return
      }
    } catch (error) {
      actionMessage.value = error instanceof Error ? error.message : '消息状态更新失败，请稍后重试。'
      return
    }
  }
  await router.push({
    ...target,
    query: {...(target.query || {}), fromInbox: '1'},
  })
}

onMounted(() => {
  void Promise.allSettled([loadPage(1, false), inbox.refreshUnreadCount()])
})
</script>

<style scoped>
.inbox-page { width: min(100%, 980px); min-width: 0; margin: 0 auto; padding: 14px 0 40px; }
.inbox-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 22px; }
.inbox-header__eyebrow { color: var(--el-color-primary); font-size: 10px; font-weight: 750; letter-spacing: .16em; }
.inbox-header h1 { margin: 5px 0 4px; color: var(--el-text-color-primary); font-size: clamp(24px, 4vw, 32px); font-weight: 700; letter-spacing: -.04em; }
.inbox-header p { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }
.inbox-header__actions { display: flex; flex: 0 0 auto; align-items: center; gap: 8px; }
.inbox-panel { overflow: hidden; border: 1px solid var(--el-border-color-lighter); border-radius: 18px; background: var(--el-bg-color); box-shadow: 0 12px 34px color-mix(in srgb, var(--el-text-color-primary) 4%, transparent); }
.inbox-toolbar { display: flex; min-height: 58px; align-items: center; justify-content: space-between; gap: 12px; padding: 0 18px; border-bottom: 1px solid var(--el-border-color-lighter); }
.inbox-filters { display: flex; align-items: center; gap: 5px; }
.inbox-filters__item { display: inline-flex; align-items: center; gap: 7px; padding: 8px 12px; border: 0; border-radius: 9px; color: var(--el-text-color-secondary); background: transparent; font: inherit; font-size: 13px; cursor: pointer; }
.inbox-filters__item:hover { color: var(--el-text-color-primary); background: var(--el-fill-color-light); }
.inbox-filters__item--active { color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-weight: 650; }
.inbox-filters__count { min-width: 18px; padding: 1px 5px; border-radius: 999px; color: #fff; background: var(--el-color-primary); font-size: 10px; line-height: 16px; text-align: center; }
.inbox-total { color: var(--el-text-color-placeholder); font-size: 12px; }
.inbox-list { display: grid; gap: 10px; padding: 14px 16px; }
.inbox-state { display: flex; align-items: center; justify-content: center; gap: 12px; padding: 26px 16px; color: var(--el-text-color-secondary); font-size: 13px; }
.inbox-state--error { color: var(--el-color-danger); }
.inbox-inline-message { margin: 14px 16px 0; padding: 10px 12px; border-radius: 9px; color: var(--el-color-warning-dark-2); background: var(--el-color-warning-light-9); font-size: 12px; line-height: 1.5; }
.inbox-pagination { justify-content: center; padding: 0 16px 18px; }
.inbox-skeleton { display: grid; gap: 10px; padding: 14px 16px; }
.inbox-skeleton__row { padding: 14px 16px; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; }
.inbox-skeleton__shape { display: flex; align-items: center; gap: 13px; }
.inbox-skeleton__shape :deep(.el-skeleton__item.is-circle) { width: 42px; height: 42px; flex: 0 0 auto; }
.inbox-skeleton__shape > div { display: grid; width: min(60%, 360px); gap: 10px; }
@media (max-width: 650px) {
  .inbox-page { padding-top: 4px; }
  .inbox-header { align-items: flex-start; flex-direction: column; gap: 14px; }
  .inbox-header__actions { width: 100%; }
  .inbox-header__actions :deep(.el-button) { flex: 1; }
  .inbox-toolbar { padding: 0 10px; }
  .inbox-list { padding: 10px; }
}
</style>
