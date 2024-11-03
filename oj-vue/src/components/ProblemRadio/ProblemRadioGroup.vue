

<template>
<div>
  <problem-radio
      v-for="choice in choices"
      :key="choice.order"
      :option="<string>choice.order"
      :content="choice.content"
      @click="handleClick"
      :answers="answers"
  />
</div>
</template>

<script setup lang="ts">

import type {ChoiceProblemView} from "@/api/problem";
import ProblemRadio from "@/components/ProblemRadio/ProblemRadio.vue";
import type {Answer} from "@/api/problem/judge";
import __ from "lodash";

const {isMulti = false, choices = []} = defineProps<{
  isMulti?: boolean,
  choices?: ChoiceProblemView[]
}>();

const answers = defineModel<Answer[]>({required: true})

const handleClick = (index: number, b: boolean) => {
  // 单选题
  if (!isMulti) {
    answers.value!.length = 0;
    answers.value!.push({index: index, answer: ''});
  } else {
    // 多选题

    if (b) {
      // 选中就删掉
      __.remove(<Answer[]>answers.value, x => x.index === index)
    } else {
      // 没选中就加入
      answers.value!.push({index: index, answer: ''});
    }
  }
}


</script>
<style scoped>

</style>