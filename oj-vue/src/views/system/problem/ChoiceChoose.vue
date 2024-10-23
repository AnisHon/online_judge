
<template>

  <div>

    <div class="choices">
      <problem-radio-group ref="problemRadioGroupRef" :choices="choices" :is-multi="isMulti" />
    </div>

    <div class="submit">
      <el-button type="success" @click="handleSubmit">提交</el-button>
    </div>


    <div class="result">
      <ProblemResult />
    </div>
  </div>

</template>

<script setup lang="ts">
import {type ChoiceProblemView, type ProblemDetailView, ProblemType} from "@/api/problem";
import {computed, reactive, ref} from "vue";
import {getDebouncedJudge, type JudgeForm, type JudgeResponse} from "@/api/problem/judge";
import ProblemRadioGroup from "@/views/system/problem/ProblemRadio/ProblemRadioGroup.vue";
import {letterToNumber} from "@/utils/stringUtils";
import ProblemResult from "@/views/system/problem/ProblemResult/ProblemResult.vue";

const {problemView} = defineProps<{problemView: ProblemDetailView}>();

const judgeForm = reactive<JudgeForm>({
  contestId: undefined,
  problemId: problemView.problemVo.problemId,
  answers: [],
})

const problemRadioGroupRef = ref<InstanceType<typeof ProblemRadioGroup> | null>(null);


const doJudge = getDebouncedJudge(judgeForm,
    (data: JudgeResponse) => {
      console.log(data)
    }
)

const choices = computed(() => {
  return problemView?.choices || []
})

const isMulti = computed(() => {
  return problemView.problemVo.type === ProblemType.MULTI_CHOICE
})

const handleSubmit = () => {
  judgeForm.answers = []
  problemRadioGroupRef.value?.selected.forEach((value: boolean, key: string) => {
    if (value) {
      const i = letterToNumber(key);
      judgeForm.answers.push({index: i, answer: ""})
    }
  })
  doJudge()
}


const getOption = (item: ChoiceProblemView) => {
  return `${item.order} ${item.content}`
}





</script>

<style scoped>
.submit {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}
</style>

