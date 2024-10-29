<template>
  <div>
    <div class="header">
      <h1>{{ problem?.problem.title }}</h1>
      <div class="tags">
        <el-space v-if="problemType === ProblemType.OJ">
          <el-tag type="danger">
            难度: {{ difficulty }}
          </el-tag>
          <el-tag type="info">
            空间限制: {{ memoryLimit }}MiB
          </el-tag>
          <el-tag type="info">
            时间限制: {{ timeLimit }}ms
          </el-tag>
          <el-tag type="info">
            栈空间限制: {{ stackLimit }}MiB
          </el-tag>
        </el-space>
      </div>
    </div>
    <div class="tags">
      <el-space>
        <el-tag>
          {{ problem?.problem.source }}
        </el-tag>
      </el-space>
    </div>

    <el-divider/>
    <div class="content">
      <h2>题目描述</h2>
      <p class="description">
        <markdown-preview :text="description"></markdown-preview>
      </p>

      <div class="detail-problem">
        <online-judge-problem-reviewer :problem="ojProblem" v-if="isOjProblem"/>
        <fill-blank-reviewer v-if="isFillProblem" :count="count" :judgeForm="judgeForm"/>
        <choice-choose-reviewer :problem-view="problem" v-if="isChoiceProblem" />
      </div>
      <div class="hint">
        <h2>提示</h2>
        <div>
          <markdown-preview :text="hint" />
        </div>


      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import {
  type ProblemForm,
  ProblemType,
} from "@/api/problem";
import {useRoute} from "vue-router";
import {computed, reactive} from "vue";
import {problemTypeToString} from "@/utils/problem";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {type JudgeForm} from "@/api/problem/judge";
import OnlineJudgeProblemReviewer
  from "@/views/problem-module/problem-edit/problem-reviewer/OnlineJudgeProblemReviewer.vue";
import FillBlankReviewer from "@/views/problem-module/problem-edit/problem-reviewer/FillBlankReviewer.vue";
import ChoiceChooseReviewer from "@/views/problem-module/problem-edit/problem-reviewer/ChoiceChooseReviewer.vue";

const route = useRoute();

const {problem} = defineProps<{
  problem: ProblemForm
}>();
const problemId = computed(() => {
  return  parseInt(<string>route.params.id)
})

const count = computed(() => {
  return problem.choices.length;
})

const judgeForm = reactive<JudgeForm>({
  contestId: undefined,
  problemId: problemId.value,
  answers: [],
});

const problemType = computed(() => {
  return problem.problem?.type;
})

const isChoiceProblem = computed(() => {
  return problemType.value === ProblemType.CHOICE || problemType.value === ProblemType.MULTI_CHOICE;
})

const isOjProblem = computed(() => {
  return problemType.value === ProblemType.OJ;
})

const isFillProblem = computed(() => {
  return problemType.value === ProblemType.FILL;
})

const hint = computed((): string => {
  if (problem?.problem.hint === null || problem?.problem.hint === undefined || problem?.problem.hint === '') {
    return "无";
  } else {
    return problem?.problem.hint;
  }
})

const ojProblem = computed(() => {
  return problem?.ojProblem;
});

const difficulty = computed(() => {
  const array = ['不确定', '简单', '中等', '困难']
  if (problem === undefined || problem.ojProblem === undefined) {
    return "未知";
  }
  return array[problem.ojProblem.difficulty || 0]
});

// mb
const memoryLimit = computed(() => {
  return Math.ceil((ojProblem.value?.memoryLimit || 0) / 1024);
});

// ms
const timeLimit = computed(() => {
  return ojProblem.value?.timeLimit || 0
});

// mb
const stackLimit = computed(() => {
  return ojProblem.value?.stackLimit || 0;
})

const stringProblemType = computed(() => {
  return problemTypeToString(<ProblemType>problemType.value);
});

const description = computed(() => {
  return problem?.problem.description || "";
})


</script>
