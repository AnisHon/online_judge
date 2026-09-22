<script setup lang="ts">
import {onBeforeUnmount, ref} from 'vue';

const props = defineProps<{value: string}>();
const text = ref('复制');
let restoreTimer: ReturnType<typeof setTimeout> | undefined;

const handleClick = async () => {
  try {
    await navigator.clipboard.writeText(props.value || '');
    text.value = '已复制';
  } catch {
    text.value = '复制失败';
  }
  if (restoreTimer) clearTimeout(restoreTimer);
  restoreTimer = setTimeout(() => { text.value = '复制'; }, 1800);
};
onBeforeUnmount(() => { if (restoreTimer) clearTimeout(restoreTimer); });
</script>

<template>
  <el-link class="copy-link" type="info" @click.stop="handleClick">{{ text }}</el-link>
</template>

<style scoped>
.copy-link { user-select: none; }
</style>
