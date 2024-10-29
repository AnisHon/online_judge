
<template>

  <div>
    <div class="choices">
      <ProblemRadioGroupViewer ref="problemRadioGroupRef" :choices="choices" :is-multi="isMulti" />
    </div>
  </div>

</template>

<script setup lang="ts">
import {type ProblemForm, ProblemType} from "@/api/problem";
import {computed, ref} from "vue";
import ProblemRadioGroupViewer from "@/views/problem-module/problem-edit/radio-group-viewer/RadioGroupViewer.vue";

const {problemView} = defineProps<{
  problemView: ProblemForm,
}>();
const problemRadioGroupRef = ref<InstanceType<typeof ProblemRadioGroupViewer> | null>(null);

const choices = computed(() => {
  return problemView?.choices || []
})

const isMulti = computed(() => {
  return problemView.problem.type === ProblemType.MULTI_CHOICE
})

</script>

<style lang="scss" scoped>

</style>

