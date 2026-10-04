<template>
  <el-popover trigger="click" placement="top" :width="224" :show-arrow="true">
    <template #reference>
      <el-button class="emoji-trigger" text aria-label="选择表情" title="选择表情">
        <span aria-hidden="true">🙂</span>
      </el-button>
    </template>
    <div class="emoji-grid" role="group" aria-label="基础表情">
      <button v-for="emoji in COMMENT_EMOJIS" :key="emoji" type="button" :aria-label="`插入${emoji}`" @click="$emit('select', emoji)">
        {{ emoji }}
      </button>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import {COMMENT_EMOJIS} from '@/utils/commentText'
defineEmits<{(event: 'select', emoji: string): void}>()
</script>

<style scoped>
.emoji-trigger { min-width: 36px; min-height: 36px; padding: 0; font-size: 19px; }
.emoji-grid { display: grid; grid-template-columns: repeat(4, minmax(36px, 1fr)); gap: 6px; }
.emoji-grid button { display: grid; min-width: 38px; min-height: 38px; place-items: center; border: 1px solid var(--el-border-color-lighter); border-radius: 9px; background: var(--el-bg-color); font-size: 21px; cursor: pointer; }
.emoji-grid button:hover, .emoji-grid button:focus-visible { border-color: var(--el-color-primary-light-5); background: var(--el-color-primary-light-9); outline: none; }
</style>
