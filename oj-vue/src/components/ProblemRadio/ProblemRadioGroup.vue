<template>
  <div class="radio-group" :role="props.isMulti ? 'group' : 'radiogroup'">
    <problem-radio
      v-for="choice in sortedChoices"
      :key="String(choice.order)"
      :option="String(choice.order)"
      :content="choice.content || ''"
      :answers="answers"
      :is-multi="props.isMulti"
      @select="handleSelect"
    />
  </div>
</template>

<script setup lang="ts">
import type {ChoiceProblemView} from '@/api/problem';
import ProblemRadio from '@/components/ProblemRadio/ProblemRadio.vue';
import type {Answer} from '@/api/problem/judge';
import {computed} from 'vue';
import {letterToNumber} from '@/utils/stringUtils';

const props = withDefaults(defineProps<{isMulti?: boolean; choices?: ChoiceProblemView[]}>(), {
  isMulti: false,
  choices: () => [],
});
const answers = defineModel<Answer[]>({required: true});
const sortedChoices = computed(() => [...props.choices].sort((a, b) => letterToNumber(String(a.order || '')) - letterToNumber(String(b.order || ''))));

const handleSelect = (index: number, selected: boolean) => {
  const current = answers.value || [];
  if (props.isMulti) {
    answers.value = selected
      ? current.filter(answer => answer.index !== index)
      : [...current, {index, answer: ''}].sort((a, b) => a.index - b.index);
  } else {
    answers.value = [{index, answer: ''}];
  }
};
</script>

<style scoped>
.radio-group { display: grid; gap: 9px; }
</style>
