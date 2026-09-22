<template>
  <div class="fill-blank">
    <el-form label-position="top" class="fill-form">
      <el-form-item v-for="item in judgeForm.answers" :key="item.index" :label="itemLabel(item.index)">
        <el-input
          :model-value="item.answer || ''"
          type="textarea"
          :aria-label="itemLabel(item.index)"
          :autosize="{minRows: 2, maxRows: 6}"
          placeholder="填写你的答案"
          @update:model-value="updateAnswer(item.index, $event)"
        />
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import type {JudgeForm} from '@/api/problem/judge';

const judgeForm = defineModel<JudgeForm>({required: true});
const itemLabel = (index: number) => `第 ${index} 空`;
const updateAnswer = (index: number, value: string) => {
  judgeForm.value.answers = judgeForm.value.answers.map(answer =>
    answer.index === index ? {...answer, answer: value} : answer,
  );
};
</script>

<style scoped>
.fill-form { display: grid; gap: 8px; }
.fill-form :deep(.el-form-item) { margin-bottom: 8px; }
</style>
