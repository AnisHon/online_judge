
<template>

  <div>
    <div class="choices">
      <problem-radio-group ref="problemRadioGroupRef" :choices="choices" :is-multi="isMulti" />
    </div>
  </div>

</template>

<script setup lang="ts">
import {type ProblemDetailView, ProblemType} from "@/api/problem";
import {computed, onUnmounted, ref, toRefs} from "vue";
import {type JudgeForm} from "@/api/problem/judge";
import ProblemRadioGroup from "@/views/system/problem/ProblemRadio/ProblemRadioGroup.vue";
import {letterToNumber} from "@/utils/stringUtils";
import {useMitt} from "@/stores/useMitt";

const {problemView, judgeForm} = defineProps<{
  problemView: ProblemDetailView,
  judgeForm: JudgeForm,
}>();

const judgeForm_ = toRefs(judgeForm);

const problemRadioGroupRef = ref<InstanceType<typeof ProblemRadioGroup> | null>(null);

const emitter = useMitt().get();

const choices = computed(() => {
  return problemView?.choices || []
})

const isMulti = computed(() => {
  return problemView.problemVo.type === ProblemType.MULTI_CHOICE
})

const setValue = () => {
  judgeForm_.answers.value = []
  problemRadioGroupRef.value?.selected.forEach((value, key) => {
    if (value) {
      const i = letterToNumber(key);
      judgeForm_.answers.value.push({index: i, answer: ""})
    }
    // console.log(1)
  })
}

emitter.on('judge', setValue);

onUnmounted(() => {
  emitter.off('judge', setValue);
})


</script>

<style lang="scss" scoped>

</style>

