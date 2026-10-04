<template>
  <el-button class="follow-button" :type="following ? 'default' : 'primary'" :plain="following"
             :loading="pending" :disabled="disabled || pending" :aria-label="following ? '取消关注' : '关注用户'"
             @click="emit('toggle')">
    {{ following ? '已关注' : '关注' }}
  </el-button>
</template>
<script setup lang="ts">
defineProps<{following: boolean; pending: boolean; disabled?: boolean}>()
const emit = defineEmits<{toggle: []}>()
</script>
<style scoped>
.follow-button { min-width: 108px; height: 36px; box-sizing: border-box; }
</style>
