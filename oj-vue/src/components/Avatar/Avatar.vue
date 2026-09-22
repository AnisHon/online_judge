<template>
  <div class="avatar" :class="[`avatar--${props.shape || 'circle'}`, {'avatar--error': imageFailed}]" :style="avatarStyle">
    <el-avatar v-if="!path || imageFailed" class="avatar__image" :shape="props.shape || 'circle'" icon="UserFilled" />
    <el-avatar v-else class="avatar__image" :shape="props.shape || 'circle'" :src="path" @error="handleImageError" />
  </div>
</template>
<script setup lang="ts">

import {getAvatarPath} from "@/api/file";
import type {IdType} from "@/api/common.ts";
import {useUserStore} from "@/stores/useUserStore.ts";
import {computed, onUnmounted, ref, watch} from "vue";

const props = defineProps<{userId?: IdType | null, shape?: 'circle' | 'square', size?: number | string}>();
const emit = defineEmits<{
  (event: 'load-error', userId: IdType | null | undefined): void;
}>();
const userStore = useUserStore();
const imageFailed = ref(false);
const retryAttempt = ref(0);
let retryTimer: ReturnType<typeof setTimeout> | undefined;

// 未传 userId 表示当前用户；显式传 null 表示未知用户，不能误用当前用户头像。
const resolvedUserId = computed(() => props.userId === undefined ? userStore.user?.userId : props.userId);
const avatarVersion = computed(() => {
  const id = resolvedUserId.value;
  return id === undefined || id === null ? 0 : userStore.avatarVersions[String(id)] ?? 0;
});
const avatarSize = computed(() => typeof props.size === 'number' ? `${props.size}px` : props.size || '40px');
const avatarStyle = computed(() => ({'--avatar-size': avatarSize.value}));
const path = computed(() => {
  const id = resolvedUserId.value;
  if (id === undefined || id === null) return undefined;
  return getAvatarPath(id, avatarVersion.value, retryAttempt.value);
});

const clearRetryTimer = () => {
  if (retryTimer) clearTimeout(retryTimer);
  retryTimer = undefined;
};

const resetImageState = () => {
  clearRetryTimer();
  retryAttempt.value = 0;
  imageFailed.value = false;
};

const handleImageError = () => {
  imageFailed.value = true;
  emit('load-error', resolvedUserId.value);
  if (retryAttempt.value >= 1) return;
  clearRetryTimer();
  retryTimer = setTimeout(() => {
    retryAttempt.value += 1;
    imageFailed.value = false;
  }, 1200);
};

watch([resolvedUserId, avatarVersion], resetImageState);
onUnmounted(clearRetryTimer);

</script>
<style scoped>
.avatar { display: inline-flex; width: var(--avatar-size); height: var(--avatar-size); flex: 0 0 auto; align-items: center; justify-content: center; overflow: hidden; vertical-align: middle; border-radius: 50%; }
.avatar--square { border-radius: 8px; }
.avatar__image { display: flex; width: 100%; height: 100%; align-items: center; justify-content: center; }
.avatar__image :deep(img) { width: 100%; height: 100%; object-fit: cover; }
.avatar--error .avatar__image { color: var(--el-text-color-secondary); background: var(--el-fill-color-light); }
</style>
