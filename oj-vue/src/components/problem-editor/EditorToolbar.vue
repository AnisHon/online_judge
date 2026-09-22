<template>
  <div class="editor-toolbar">
    <div class="editor-toolbar__copy">
      <strong>{{ title }}</strong>
      <span>{{ description }}</span>
    </div>
    <div class="editor-toolbar__actions">
      <span v-if="meta" class="editor-toolbar__meta">{{ meta }}</span>
      <el-button type="primary" plain size="small" :icon="actionIcon" @click="$emit('action')">
        {{ actionText }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type {Component, PropType} from 'vue';

defineProps({
  title: {type: String, required: true},
  description: {type: String, required: true},
  meta: {type: String, default: ''},
  actionText: {type: String, required: true},
  actionIcon: {type: Object as PropType<Component>, required: true},
});

defineEmits<{(event: 'action'): void}>();
</script>

<style scoped>
.editor-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 14px; }
.editor-toolbar__copy, .editor-toolbar__actions { display: flex; align-items: center; gap: 10px; }
.editor-toolbar__copy { min-width: 0; flex-direction: column; align-items: flex-start; gap: 3px; }
.editor-toolbar strong { font-size: 14px; }
.editor-toolbar span { color: var(--el-text-color-secondary); font-size: 11px; }
.editor-toolbar__meta { color: var(--el-color-primary) !important; font-weight: 650; white-space: nowrap; }
@media (max-width: 760px) {
  .editor-toolbar { align-items: flex-start; flex-direction: column; }
  .editor-toolbar__actions { width: 100%; justify-content: space-between; }
}
</style>
