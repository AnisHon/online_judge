
<template>

  <div>
    <div class="choices">
      <problem-radio-group
          ref="problemRadioGroupRef"
          :choices="choices"
          v-model="judgeForm.answers"
          :is-multi="isMulti"
      />
    </div>
  </div>

</template>

<script setup lang="ts">
import {type ProblemDetailView, ProblemType} from "@/api/problem";
import {computed} from "vue";
import {type JudgeForm} from "@/api/problem/judge";

const {problemView} = defineProps<{
  problemView: ProblemDetailView
}>();

const judgeForm = defineModel<JudgeForm>({required: true})

const choices = computed(() => {
  return problemView?.choices || [];
})

const isMulti = computed(() => {
  return problemView.problemVo.type === ProblemType.MULTI_CHOICE;
})

</script>

<style lang="scss" scoped>

</style>
