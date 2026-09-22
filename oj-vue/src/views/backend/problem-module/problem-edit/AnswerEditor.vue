<template>
  <section class="answer-editor">
    <EditorToolbar
      :title="isFill ? '填空答案' : '选项与正确答案'"
      :description="isFill ? '每个空可以配置多个可接受答案。' : isMulti ? '可设置多个正确选项。' : '设置一个正确选项。'"
      :meta="`${answers.length} ${isFill ? '个空' : '个选项'}`"
      :action-text="isFill ? '添加填空' : '添加选项'"
      :action-icon="Plus"
      @action="addAnswer"
    />

    <div v-if="answers.length" class="answer-list">
      <article v-for="(answer, index) in answers" :key="answer.answerId || `${isFill ? 'blank' : 'option'}-${index}`" class="answer-card" :class="{ 'answer-card--correct': !isFill && answer.isCorrect }">
        <div class="answer-card__heading">
          <div class="answer-card__label"><span>{{ isFill ? `空 ${index + 1}` : numberToLetter(index + 1) }}</span><strong>{{ isFill ? '填空答案' : `选项 ${numberToLetter(index + 1)}` }}</strong></div>
          <div class="answer-card__actions">
            <el-checkbox v-if="!isFill" :model-value="!!answer.isCorrect" @update:model-value="toggleCorrect(index, $event)">正确答案</el-checkbox>
            <el-button text circle size="small" :disabled="index === 0" :icon="ArrowUp" aria-label="上移" @click="moveAnswer(index, -1)" />
            <el-button text circle size="small" :disabled="index === answers.length - 1" :icon="ArrowDown" aria-label="下移" @click="moveAnswer(index, 1)" />
            <el-button text circle size="small" type="danger" :icon="Delete" aria-label="删除" @click="removeAnswer(index)" />
          </div>
        </div>
        <div class="answer-card__body">
          <el-input :model-value="answer.answerText" type="textarea" :rows="isFill ? 2 : 3" resize="vertical" :placeholder="isFill ? '输入一个可接受的答案；多个答案可用逗号分隔' : '输入选项内容'" @update:model-value="patchAnswer(index, { answerText: $event })" />
          <el-input-number :model-value="answer.score" :min="0" :precision="2" controls-position="right" placeholder="分值" @update:model-value="patchAnswer(index, { score: $event ?? undefined })" />
        </div>
        <div v-if="isFill" class="answer-card__hint">填空序号会在保存时按当前顺序整理。</div>
      </article>
    </div>
    <EditorEmptyState
      v-else
      :icon="DocumentAdd"
      :title="isFill ? '还没有填空' : '还没有选项'"
      description="点击右上角按钮开始设计答案。"
      action-text="立即添加"
      :action-icon="Plus"
      @action="addAnswer"
    />
  </section>
</template>

<script setup lang="ts">
import {computed} from 'vue';
import {ArrowDown, ArrowUp, Delete, DocumentAdd, Plus} from '@element-plus/icons-vue';
import {ProblemType, type Answer} from '@/api/problem';
import {numberToLetter} from '@/utils/stringUtils';
import EditorEmptyState from '@/components/problem-editor/EditorEmptyState.vue';
import EditorToolbar from '@/components/problem-editor/EditorToolbar.vue';

const props = defineProps<{answers: Answer[]; type?: ProblemType}>();
const emit = defineEmits<{(event: 'update:answers', value: Answer[]): void}>();
const isFill = computed(() => props.type === ProblemType.FILL);
const isMulti = computed(() => props.type === ProblemType.MULTI_CHOICE);

const normalize = (items: Answer[]) => items.map((item, index) => ({...item, blankIndex: index + 1}));
const patchAnswer = (index: number, patch: Partial<Answer>) => {
  emit('update:answers', props.answers.map((item, itemIndex) => itemIndex === index ? {...item, ...patch} : item));
};
const addAnswer = () => {
  const answer: Answer = isFill.value
      ? {answerText: '', blankIndex: props.answers.length + 1, score: 1}
      : {answerText: '', blankIndex: props.answers.length + 1, isCorrect: false, score: 1};
  emit('update:answers', [...props.answers, answer]);
};
const removeAnswer = (index: number) => emit('update:answers', normalize(props.answers.filter((_, itemIndex) => itemIndex !== index)));
const moveAnswer = (index: number, offset: number) => {
  const next = [...props.answers];
  const target = index + offset;
  if (target < 0 || target >= next.length) return;
  [next[index], next[target]] = [next[target], next[index]];
  emit('update:answers', normalize(next));
};
const toggleCorrect = (index: number, checked: boolean) => {
  emit('update:answers', props.answers.map((item, itemIndex) => ({
    ...item,
    isCorrect: itemIndex === index ? checked : (!isMulti.value && checked ? false : item.isCorrect),
  })));
};
</script>

<style scoped>
.answer-editor { min-width: 0; }
.answer-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
.answer-card { min-width: 0; padding: 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; background: var(--el-bg-color-page); transition: border-color .18s ease, background-color .18s ease; }.answer-card--correct { border-color: color-mix(in srgb, var(--el-color-success) 45%, var(--el-border-color-lighter)); background: color-mix(in srgb, var(--el-color-success) 5%, var(--el-bg-color-page)); }
.answer-card__heading, .answer-card__label, .answer-card__actions { display: flex; align-items: center; }.answer-card__heading { justify-content: space-between; gap: 10px; margin-bottom: 12px; }.answer-card__label { min-width: 0; gap: 9px; }.answer-card__label > span { display: grid; width: 28px; height: 28px; flex: 0 0 auto; place-items: center; border-radius: 9px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); font: 700 13px var(--code-font-family, monospace); }.answer-card__label strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: 13px; }.answer-card__actions { gap: 2px; }.answer-card__actions :deep(.el-checkbox) { margin-right: 5px; }.answer-card__body { display: grid; grid-template-columns: minmax(0, 1fr) 100px; gap: 10px; align-items: start; }.answer-card__body :deep(.el-input-number) { width: 100%; }.answer-card__hint { margin-top: 8px; color: var(--el-text-color-secondary); font-size: 11px; }
@media (max-width: 760px) { .answer-list { grid-template-columns: 1fr; } }
@media (max-width: 480px) { .answer-card__heading { align-items: flex-start; flex-direction: column; }.answer-card__actions { width: 100%; justify-content: flex-end; }.answer-card__body { grid-template-columns: 1fr; } }
</style>
