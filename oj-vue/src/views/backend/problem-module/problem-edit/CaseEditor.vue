<template>
  <section class="case-editor">
    <div class="case-toolbar">
      <div><strong>测试数据</strong><span>每个测试点包含输入、预期输出和分值。</span></div>
      <div class="case-toolbar__actions"><span>{{ cases.length }} 个测试点 · {{ totalScore }} 分</span><el-button type="primary" plain size="small" :icon="Plus" @click="addCase">添加测试点</el-button></div>
    </div>
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
    <div v-else class="case-empty"><el-icon><Files /></el-icon><strong>还没有测试点</strong><span>添加至少一个测试点，判题时才能验证代码。</span><el-button type="primary" plain size="small" :icon="Plus" @click="addCase">添加第一个测试点</el-button></div>
  </section>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {Delete, Files, Plus} from '@element-plus/icons-vue';
import type {OjCase} from '@/api/problem';

const props = defineProps<{cases: OjCase[]}>();
const emit = defineEmits<{(event: 'update:cases', value: OjCase[]): void}>();
const totalScore = computed(() => props.cases.reduce((sum, item) => sum + Number(item.score || 0), 0).toFixed(2).replace(/\.00$/, ''));
const addCase = () => emit('update:cases', [...props.cases, {input: '', output: '', score: 1}]);
const removeCase = (index: number) => emit('update:cases', props.cases.filter((_, itemIndex) => itemIndex !== index));
const patchCase = (index: number, patch: Partial<OjCase>) => emit('update:cases', props.cases.map((item, itemIndex) => itemIndex === index ? {...item, ...patch} : item));
</script>

<style scoped>
.case-editor { min-width: 0; }.case-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 14px; }.case-toolbar > div:first-child, .case-toolbar__actions { display: flex; align-items: center; gap: 10px; }.case-toolbar > div:first-child { flex-direction: column; align-items: flex-start; gap: 3px; }.case-toolbar strong { font-size: 14px; }.case-toolbar span { color: var(--el-text-color-secondary); font-size: 11px; }.case-toolbar__actions span { color: var(--el-color-primary); font-weight: 650; white-space: nowrap; }.case-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }.case-card { min-width: 0; padding: 15px; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; background: var(--el-bg-color-page); }.case-card__heading, .case-card__heading > div { display: flex; align-items: center; }.case-card__heading { justify-content: space-between; gap: 10px; margin-bottom: 14px; }.case-card__heading > div { gap: 9px; }.case-card__heading span { color: var(--el-color-primary); font: 700 10px var(--code-font-family, monospace); letter-spacing: .12em; }.case-card__heading strong { font-size: 13px; }.case-card__fields { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }.case-card__fields label, .case-card__footer { display: flex; flex-direction: column; gap: 6px; }.case-card__fields label > span, .case-card__footer > span { color: var(--el-text-color-secondary); font-size: 11px; }.case-card__footer { align-items: flex-start; margin-top: 12px; }.case-card__footer :deep(.el-input-number) { width: 130px; }.case-empty { display: grid; place-items: center; gap: 7px; min-height: 190px; padding: 22px; border: 1px dashed var(--el-border-color); border-radius: 14px; color: var(--el-text-color-secondary); text-align: center; }.case-empty > .el-icon { color: var(--el-color-primary); font-size: 30px; }.case-empty strong { color: var(--el-text-color-primary); font-size: 13px; }.case-empty span { font-size: 11px; }
@media (max-width: 760px) { .case-toolbar { align-items: flex-start; flex-direction: column; }.case-toolbar__actions { width: 100%; justify-content: space-between; }.case-list { grid-template-columns: 1fr; } }.case-card__fields { min-width: 0; } @media (max-width: 480px) { .case-card__fields { grid-template-columns: 1fr; } }
</style>
