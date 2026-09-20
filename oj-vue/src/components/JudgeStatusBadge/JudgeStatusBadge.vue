<template>
  <span class="judge-status-badge" :class="`judge-status-badge--${meta.tone}`">
    <span>{{ meta.label }}</span>
    <code>{{ meta.code }}</code>
  </span>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {getJudgeStatusMeta} from '@/utils/problem/judgeStatus';

const props = defineProps<{ status?: string }>();
const meta = computed(() => getJudgeStatusMeta(props.status));
</script>

<style scoped>
.judge-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  padding: 4px 8px;
  border: 1px solid color-mix(in srgb, var(--judge-status-color) 24%, transparent);
  border-radius: 8px;
  background: color-mix(in srgb, var(--judge-status-color) 9%, var(--el-bg-color));
  color: var(--judge-status-color);
  font-size: 12px;
  font-weight: 650;
  line-height: 1.2;
  white-space: nowrap;
}

.judge-status-badge code {
  padding: 1px 4px;
  border-radius: 4px;
  background: color-mix(in srgb, var(--judge-status-color) 13%, transparent);
  color: inherit;
  font: 750 10px/1.2 var(--code-font-family, "JetBrains Mono", monospace);
  letter-spacing: .04em;
}

.judge-status-badge--pending { --judge-status-color: var(--el-color-primary); }
.judge-status-badge--primary { --judge-status-color: var(--el-color-primary); }
.judge-status-badge--success { --judge-status-color: var(--el-color-success); }
.judge-status-badge--warning { --judge-status-color: var(--el-color-warning); }
.judge-status-badge--danger { --judge-status-color: var(--el-color-danger); }
.judge-status-badge--info { --judge-status-color: var(--el-color-info); }
</style>
