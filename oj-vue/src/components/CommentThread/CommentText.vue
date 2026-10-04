<template><p class="comment-text">{{ text }}</p></template>
<script setup lang="ts">
defineProps<{text: string}>()
</script>
<style scoped>
.comment-text { margin: 7px 0 8px; color: var(--el-text-color-regular); font-size: 14px; line-height: 1.75; white-space: pre-wrap; overflow-wrap: anywhere; word-break: normal; }
</style>
