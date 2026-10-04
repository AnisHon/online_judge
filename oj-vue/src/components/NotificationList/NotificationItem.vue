<template>
  <article class="notification-item" :class="{'notification-item--unread': !notification.readAt}">
    <button
      class="notification-item__main"
      type="button"
      :aria-label="`${actorName}：${message}`"
      @click="$emit('open', notification)"
    >
      <div class="notification-item__avatar">
        <Avatar v-if="notification.actor" :user-id="notification.actor.userId" :size="42" />
        <span v-else class="notification-item__avatar-fallback" aria-hidden="true">
          <el-icon><UserFilled /></el-icon>
        </span>
      </div>
      <div class="notification-item__content">
        <div class="notification-item__heading">
          <strong>{{ actorName }}</strong>
          <span v-if="!notification.readAt" class="notification-item__unread-dot" aria-label="未读" />
        </div>
        <p>{{ message }}</p>
        <time :datetime="notification.occurredAt">{{ occurredAt }}</time>
        <p v-if="reason" class="notification-item__reason">{{ reason }}</p>
      </div>
      <el-icon class="notification-item__arrow"><ArrowRight /></el-icon>
    </button>
    <el-button
      v-if="!notification.readAt"
      class="notification-item__read"
      text
      :disabled="pending"
      :loading="pending"
      @click="$emit('mark-read', notification)"
    >
      标为已读
    </el-button>
  </article>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {ArrowRight, UserFilled} from '@element-plus/icons-vue'
import Avatar from '@/components/Avatar/Avatar.vue'
import type {InboxNotification} from '@/api/notification'
import {
  formatNotificationTime,
  notificationActorName,
  notificationMessage,
  notificationReason,
} from '@/utils/notificationDisplay'

const props = defineProps<{notification: InboxNotification; pending?: boolean}>()
defineEmits<{
  (event: 'open', notification: InboxNotification): void
  (event: 'mark-read', notification: InboxNotification): void
}>()

const actorName = computed(() => notificationActorName(props.notification))
const message = computed(() => notificationMessage(props.notification))
const reason = computed(() => notificationReason(props.notification))
const occurredAt = computed(() => formatNotificationTime(props.notification.occurredAt))
</script>

<style scoped>
.notification-item { display: flex; min-width: 0; align-items: center; gap: 12px; padding: 14px 16px; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; background: var(--el-bg-color); transition: border-color .16s ease, background-color .16s ease, box-shadow .16s ease; }
.notification-item:hover { border-color: var(--el-border-color); box-shadow: 0 5px 18px color-mix(in srgb, var(--el-color-primary) 8%, transparent); }
.notification-item--unread { border-color: color-mix(in srgb, var(--el-color-primary) 24%, var(--el-border-color-lighter)); background: color-mix(in srgb, var(--el-color-primary) 3%, var(--el-bg-color)); }
.notification-item__main { display: flex; min-width: 0; flex: 1; align-items: center; gap: 13px; padding: 0; border: 0; color: inherit; background: transparent; text-align: left; cursor: pointer; }
.notification-item__main:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 4px; border-radius: 8px; }
.notification-item__avatar { flex: 0 0 auto; }
.notification-item__avatar-fallback { display: grid; width: 42px; height: 42px; place-items: center; border-radius: 50%; color: var(--el-color-primary); background: var(--el-fill-color-light); font-size: 19px; }
.notification-item__content { min-width: 0; flex: 1; }
.notification-item__heading { display: flex; align-items: center; gap: 8px; color: var(--el-text-color-primary); font-size: 14px; }
.notification-item__heading strong { max-width: 100%; overflow: hidden; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }
.notification-item__unread-dot { width: 7px; height: 7px; flex: 0 0 auto; border-radius: 50%; background: var(--el-color-primary); }
.notification-item__content p { margin: 4px 0 0; color: var(--el-text-color-regular); font-size: 13px; line-height: 1.55; overflow-wrap: anywhere; }
.notification-item__content time { display: block; margin-top: 5px; color: var(--el-text-color-secondary); font-size: 11px; }
.notification-item__content .notification-item__reason { padding: 9px 11px; border-left: 2px solid var(--el-color-warning-light-5); border-radius: 0 8px 8px 0; color: var(--el-text-color-secondary); background: var(--el-fill-color-lighter); white-space: pre-wrap; overflow-wrap: anywhere; }
.notification-item__arrow { flex: 0 0 auto; color: var(--el-text-color-placeholder); }
.notification-item__read { flex: 0 0 auto; }
@media (max-width: 560px) {
  .notification-item { align-items: flex-start; flex-wrap: wrap; padding: 12px; }
  .notification-item__main { align-items: flex-start; gap: 10px; }
  .notification-item__arrow { margin-top: 12px; }
  .notification-item__read { margin-left: 51px; }
}
@media (prefers-reduced-motion: reduce) { .notification-item { transition: none; } }
</style>
