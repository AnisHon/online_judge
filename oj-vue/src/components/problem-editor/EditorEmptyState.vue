<template>
  <div class="editor-empty">
    <div class="editor-empty__icon"><el-icon><component :is="icon" /></el-icon></div>
    <strong>{{ title }}</strong>
    <span>{{ description }}</span>
    <el-button type="primary" plain size="small" :icon="actionIcon" @click="$emit('action')">
      {{ actionText }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import type {Component, PropType} from 'vue';

defineProps({
  icon: {type: Object as PropType<Component>, required: true},
  title: {type: String, required: true},
  description: {type: String, required: true},
  actionText: {type: String, required: true},
  actionIcon: {type: Object as PropType<Component>, required: true},
});

defineEmits<{(event: 'action'): void}>();
</script>

<style scoped>
.editor-empty { display: grid; place-items: center; gap: 7px; min-height: 190px; padding: 22px; border: 1px dashed var(--el-border-color); border-radius: 14px; color: var(--el-text-color-secondary); text-align: center; }
.editor-empty__icon { display: grid; width: 42px; height: 42px; place-items: center; border-radius: 13px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); font-size: 21px; }
.editor-empty strong { color: var(--el-text-color-primary); font-size: 13px; }
.editor-empty span { font-size: 11px; }
</style>
