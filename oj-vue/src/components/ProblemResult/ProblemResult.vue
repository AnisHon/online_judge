<template>
  <section class="problem-result" aria-live="polite">
    <div class="result-heading" :class="clazz">
      <span class="result-icon">{{ correct ? '✓' : '!' }}</span>
      <div><h3>{{ resultText }}</h3><span>本次提交得分 {{ score }}</span></div>
    </div>
    <div v-if="answers.length" class="answer-section">
      <h3>正确答案</h3>
      <div class="answer-groups">
        <div v-for="(items, key) in groupResult" :key="key" class="answer-group">
          <markdown-preview v-if="isFill" variant="embedded" :text="itemIndexText(Number(key))" />
          <markdown-preview v-for="item in items" :key="`${key}-${item.index}-${item.answer}`" variant="embedded" :text="itemText(item)" />
        </div>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import MarkdownPreview from '@/components/MarkdownPreview.vue';
import {computed} from 'vue';
import {OJResult, type Answer, type JudgeResponse} from '@/api/problem/judge';
import {ProblemType} from '@/api/problem';
import {numberToLetter} from '@/utils/stringUtils';

const props = withDefaults(defineProps<{result?: JudgeResponse; type?: ProblemType}>(), {
  result: () => ({answers: [], correct: false, totalScore: '0', fullMark: '0', judgeResult: OJResult.QUEUE, errorMessage: ''}),
  type: ProblemType.FILL,
});
const correct = computed(() => props.result.correct === true);
const answers = computed(() => Array.isArray(props.result.answers) ? props.result.answers : []);
const groupResult = computed(() => {
  const groups: Record<string, Answer[]> = {};
  answers.value.forEach(answer => {
    const key = String(answer.index);
    (groups[key] ||= []).push(answer);
  });
  return groups;
});
const clazz = computed(() => correct.value ? 'right' : 'wrong');
const isFill = computed(() => props.type === ProblemType.FILL);
const score = computed(() => props.result.totalScore || '0');
const resultText = computed(() => correct.value ? '回答正确' : '回答错误');
const itemIndexText = (idx: number) => `__第${idx}空__`;
const itemText = (item: Answer) => isFill.value ? ` ${item.answer || ''} ` : `__${numberToLetter(item.index)}__`;
</script>

<style scoped>
.problem-result { display: grid; gap: 18px; padding: 18px; border: 1px solid var(--el-border-color-light); border-radius: 14px; background: var(--el-fill-color-extra-light); }
.result-heading { display: flex; align-items: center; gap: 12px; }
.result-heading h3 { margin: 0 0 3px; font-size: 18px; }
.result-heading span:last-child { color: var(--el-text-color-secondary); font-size: 13px; }
.result-icon { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 12px; color: #fff; font-weight: 800; }
.right .result-icon { background: var(--el-color-success); }.wrong .result-icon { background: var(--el-color-danger); }
.right h3 { color: var(--el-color-success); }.wrong h3 { color: var(--el-color-danger); }
.answer-section h3 { margin: 0 0 8px; font-size: 14px; }
.answer-groups { display: grid; gap: 8px; }
.answer-group { display: flex; flex-wrap: wrap; align-items: center; gap: 4px 10px; padding: 8px 10px; border-radius: 9px; background: var(--el-bg-color); }
</style>
