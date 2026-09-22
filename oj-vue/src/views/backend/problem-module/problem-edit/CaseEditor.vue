<template>
  <section class="case-editor">
    <EditorToolbar
      title="测试数据"
      description="每个测试点包含输入、预期输出和分值。"
      :meta="`${cases.length} 个测试点 · ${totalScore} 分`"
      action-text="添加测试点"
      :action-icon="Plus"
      @action="addCase"
    />
    <div v-if="cases.length" class="case-list">
      <article v-for="(item, index) in cases" :key="item.caseId || `case-${index}`" class="case-card">
        <div class="case-card__heading"><div><span>TEST {{ String(index + 1).padStart(2, '0') }}</span><strong>测试点 {{ index + 1 }}</strong></div><el-button text circle type="danger" :icon="Delete" aria-label="删除测试点" @click="removeCase(index)" /></div>
        <div class="case-card__fields">
          <label><span>标准输入</span><el-input :model-value="item.input" type="textarea" :rows="5" resize="vertical" placeholder="输入测试数据" @update:model-value="patchCase(index, { input: $event })" /></label>
          <label><span>标准输出</span><el-input :model-value="item.output" type="textarea" :rows="5" resize="vertical" placeholder="输入预期输出" @update:model-value="patchCase(index, { output: $event })" /></label>
        </div>
        <div class="case-card__footer"><span>该测试点分值</span><el-input-number :model-value="item.score" :min="0" :max="100" :precision="2" controls-position="right" @update:model-value="patchCase(index, { score: $event ?? undefined })" /></div>
      </article>
    </div>
    <EditorEmptyState v-else :icon="Files" title="还没有测试点" description="添加至少一个测试点，判题时才能验证代码。" action-text="添加第一个测试点" :action-icon="Plus" @action="addCase" />
  </section>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {Delete, Files, Plus} from '@element-plus/icons-vue';
import type {OjCase} from '@/api/problem';
import EditorEmptyState from '@/components/problem-editor/EditorEmptyState.vue';
import EditorToolbar from '@/components/problem-editor/EditorToolbar.vue';

const props = defineProps<{cases: OjCase[]}>();
const emit = defineEmits<{(event: 'update:cases', value: OjCase[]): void}>();
const totalScore = computed(() => props.cases.reduce((sum, item) => sum + Number(item.score || 0), 0).toFixed(2).replace(/\.00$/, ''));
const addCase = () => emit('update:cases', [...props.cases, {input: '', output: '', score: 1}]);
const removeCase = (index: number) => emit('update:cases', props.cases.filter((_, itemIndex) => itemIndex !== index));
const patchCase = (index: number, patch: Partial<OjCase>) => emit('update:cases', props.cases.map((item, itemIndex) => itemIndex === index ? {...item, ...patch} : item));
</script>

<style scoped>
.case-editor { min-width: 0; }.case-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }.case-card { min-width: 0; padding: 15px; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; background: var(--el-bg-color-page); }.case-card__heading, .case-card__heading > div { display: flex; align-items: center; }.case-card__heading { justify-content: space-between; gap: 10px; margin-bottom: 14px; }.case-card__heading > div { gap: 9px; }.case-card__heading span { color: var(--el-color-primary); font: 700 10px var(--code-font-family, monospace); letter-spacing: .12em; }.case-card__heading strong { font-size: 13px; }.case-card__fields { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; min-width: 0; }.case-card__fields label, .case-card__footer { display: flex; flex-direction: column; gap: 6px; }.case-card__fields label > span, .case-card__footer > span { color: var(--el-text-color-secondary); font-size: 11px; }.case-card__footer { align-items: flex-start; margin-top: 12px; }.case-card__footer :deep(.el-input-number) { width: 130px; }
@media (max-width: 760px) { .case-list { grid-template-columns: 1fr; } } @media (max-width: 480px) { .case-card__fields { grid-template-columns: 1fr; } }
</style>
