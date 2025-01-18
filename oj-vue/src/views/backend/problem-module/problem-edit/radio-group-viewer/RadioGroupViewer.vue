

<template>
<div>
  <problem-radio
      v-for="choice in choices"
      :key="choice.answerId"
      :option="numberToLetter(<number>choice.blankIndex)"
      :content="choice.answerText"
      :answers="answers.answers"
  />
</div>
</template>

<script setup lang="ts">

import ProblemRadio from "@/components/ProblemRadio/ProblemRadio.vue";
import type {Answer} from "@/api/problem";
import {numberToLetter} from "@/utils/stringUtils";
import {reactive} from "vue";
import type {JudgeForm} from "@/api/problem/judge";


const answers = reactive<JudgeForm>({
  problemId: '0',
  answers: [],
  code: "",
})
const {choices = []} = defineProps<{
  choices?: Answer[]
}>();


</script>
<style scoped>

</style>