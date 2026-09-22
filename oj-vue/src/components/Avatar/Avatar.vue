<template>
  <div class="avatar" :class="[`avatar--${props.shape || 'circle'}`, {'avatar--error': imageFailed}]" :style="avatarStyle">
    <el-avatar v-if="!path || imageFailed || !imageReady" class="avatar__image avatar__fallback" :shape="props.shape || 'circle'" icon="UserFilled" />
    <img v-if="path && !imageFailed" class="avatar__photo" :class="{'avatar__photo--loading': !imageReady}" :src="path" alt="" @load="handleImageLoad" @error="handleImageError" />
  </div>
</template>
<script setup lang="ts">

import {getAvatarPath} from "@/api/file";
import type {IdType} from "@/api/common.ts";
import {useUserStore} from "@/stores/useUserStore.ts";
import {computed, ref, watch} from "vue";

const props = defineProps<{userId?: IdType | null, shape?: 'circle' | 'square', size?: number | string}>();
const emit = defineEmits<{
  (event: 'load-error', userId: IdType | null | undefined): void;
}>();
const userStore = useUserStore();
const imageFailed = ref(false);
const imageReady = ref(false);

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
  return getAvatarPath(id, avatarVersion.value);
});

const resetImageState = () => {
  imageFailed.value = false;
  imageReady.value = false;
};

const handleImageLoad = () => {
  imageReady.value = true;
};

const handleImageError = () => {
  imageFailed.value = true;
  imageReady.value = false;
  emit('load-error', resolvedUserId.value);
};

watch([resolvedUserId, avatarVersion], resetImageState);

</script>
<style scoped>
.avatar { position: relative; display: inline-flex; width: var(--avatar-size); height: var(--avatar-size); flex: 0 0 auto; align-items: center; justify-content: center; overflow: hidden; vertical-align: middle; border-radius: 50%; }
.avatar--square { border-radius: 8px; }
.avatar__image { display: flex; width: 100%; height: 100%; align-items: center; justify-content: center; }
.avatar__fallback { position: absolute; inset: 0; }
.avatar__photo { position: absolute; inset: 0; display: block; width: 100%; height: 100%; object-fit: cover; opacity: 1; transition: opacity .12s ease; }
.avatar__photo--loading { opacity: 0; }
.avatar--error .avatar__image { color: var(--el-text-color-secondary); background: var(--el-fill-color-light); }
@media (prefers-reduced-motion: reduce) { .avatar__photo { transition: none; } }
</style>
