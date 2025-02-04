<template>
  <div>
    <el-avatar v-if="!path" icon="UserFilled"/>
    <el-avatar v-else class="portrait" :shape="shape" :src="path" @error="path = undefined"/>
  </div>
</template>
<script setup lang="ts">

import {getAvatarPath} from "@/api/file";
import type {IdType} from "@/api/common.ts";
import {useUserStore} from "@/stores/useUserStore.ts";
import {ref} from "vue";

const {userId = undefined, shape = 'circle' } = defineProps<{userId?: IdType, shape?: 'circle' | 'square'}>();

const path = ref<string>();

if (userId === undefined) {
  const id = useUserStore()?.user?.userId;
  if (id !== undefined) {
    path.value = getAvatarPath(id);
  }
} else {
  path.value = getAvatarPath(userId);
}


</script>
<style scoped>

</style>