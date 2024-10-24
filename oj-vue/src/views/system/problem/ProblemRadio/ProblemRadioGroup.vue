

<template>
<div>
  <problem-radio
      v-for="choice in choices"
      :key="choice.order"
      :option="choice.order"
      :content="choice.content"
      @click="handleClick"
      :choose="selected.get(choice.order)"
  />
</div>
</template>

<script setup lang="ts">

import type {ChoiceProblemView} from "@/api/problem";
import ProblemRadio from "@/views/system/problem/ProblemRadio/ProblemRadio.vue";
import {reactive} from "vue";

const selected = reactive<Map<string, boolean>>(new Map<string, boolean>())
const {isMulti = false, choices = []} = defineProps<{
  isMulti?: boolean,
  choices?: ChoiceProblemView[]
}>();

choices.forEach(choice => {selected.set(choice.order, false)})

const handleClick = (v: string) => {
  if (isMulti) {
    selected.set(v, !selected.get(v));
  } else {
    selected.forEach((value, key: string, map: Map<string, boolean>) => {map.set(key, false)});
    selected.set(v, true);

  }
}

defineExpose({
  selected
})



</script>
<style scoped>

</style>