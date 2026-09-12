<template>
  <div class="avatar" :class="`avatar--${props.shape || 'circle'}`">
    <el-avatar v-if="!path || imageFailed" class="avatar__image" :shape="props.shape || 'circle'" icon="UserFilled" />
    <el-avatar v-else class="avatar__image" :shape="props.shape || 'circle'" :src="path" @error="imageFailed = true" />
  </div>
</template>
<script setup lang="ts">

import {getAvatarPath} from "@/api/file";
import type {IdType} from "@/api/common.ts";
import {useUserStore} from "@/stores/useUserStore.ts";
import {computed, ref, watch} from "vue";

const props = defineProps<{userId?: IdType | null, shape?: 'circle' | 'square'}>();
const userStore = useUserStore();
const imageFailed = ref(false);

// 未传 userId 表示当前用户；显式传 null 表示未知用户，不能误用当前用户头像。
const resolvedUserId = computed(() => props.userId === undefined ? userStore.user?.userId : props.userId);
const path = computed(() => {
  const id = resolvedUserId.value;
  if (id === undefined || id === null) return undefined;
  return getAvatarPath(id, userStore.avatarVersions[String(id)] ?? 0);
});

watch(path, () => { imageFailed.value = false; });


</script>
<style scoped>
.avatar { display: inline-flex; width: 40px; height: 40px; flex: 0 0 auto; align-items: center; justify-content: center; overflow: hidden; vertical-align: middle; }
.avatar__image { display: flex; width: 100%; height: 100%; align-items: center; justify-content: center; }
.avatar__image :deep(img) { width: 100%; height: 100%; object-fit: cover; }
</style>
