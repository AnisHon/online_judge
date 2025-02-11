<template>
  <div>
    <el-row justify="center" v-if="problem !== undefined" :gutter="20">
      <el-col class="problem-content" ref="contentRef" :span="12" v-show="!isFullScreen">
        <div class="header">
          <h1>{{ problem?.problemVo.title }}</h1>
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
          <div class="tags">
            <el-space>
              <el-tag>
                {{ problem?.problemVo.source }}
              </el-tag>
              <el-tag>
                {{ stringProblemType }}
              </el-tag>
              <el-tag v-for="item of problem?.tagVo" :key="item.tagId" :color="item.tagColor">
                <span class="common-tag-text-color">
                  {{ item.tagName }}
                </span>
              </el-tag>
            </el-space>
          </div>

        </div>
        <div class="content">
          <h2>题目描述</h2>
          <p class="description">
            <markdown-preview :text="description" />
          </p>

          <div class="detail-problem">
            <online-judge-problem :problem="ojProblem" v-if="isOjProblem"/>
            <fill-blank v-model="judgeForm"  v-else-if="isFillProblem" />
            <choice-choose :problem-view="problem" v-model="judgeForm" v-else-if="isChoiceProblem" />
          </div>



          <div class="hint" v-if="hint">
            <h2>提示</h2>
            <div>
              <markdown-preview :text="hint" />
            </div>
          </div>

          <div class="result" v-if="isShowResult && !isOjProblem">
            <ProblemResult :type="problemType" :result="judgeResult"/>
          </div>

        </div>

      </el-col>


      <el-col
          :span="codeSpan"
          v-if="isShowCodeEditor"
          style="padding: 0 20px"
      >
        <enhanced-code-editor
            :disable-submit="disableSubmit"
            v-model="judgeForm"
            :heightProp="height"
            @full-screen="onHandleFullScreen"
            @on-ready="onEditorReady"
            :loading="false"
        />

      </el-col>

    </el-row>


  </div>
</template>

<script setup lang="ts">
import {getDetailProblem, type OjProblemView, type ProblemDetailView, ProblemType,} from "@/api/problem";
import {computed, onMounted, onUnmounted, reactive, ref,} from "vue";
import EnhancedCodeEditor from '@/components/EnhancedCodeEdior/index.vue'
import {problemTypeToString} from "@/utils/problem";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {type Answer, type JudgeForm, type JudgeResponse,} from "@/api/problem/judge";

import ProblemResult from "@/components/ProblemResult/ProblemResult.vue";
import {letterToNumber} from "@/utils/stringUtils";
import type {IdType} from "@/api/common.ts";
import OnlineJudgeProblem from "@/components/DetailProblem/OnlineJudgeProblem.vue";
import ChoiceChoose from "@/components/DetailProblem/ChoiceChoose.vue";
import FillBlank from "@/components/DetailProblem/FillBlank.vue";
import {getUserAnswer} from "@/api/record";
import {useRoute} from "vue-router";
import __ from "lodash";


const route = useRoute();

// 题目对象
const problem = ref<ProblemDetailView>();

// 代码编辑器是否全屏
const isFullScreen = ref(false)

// 代码编辑器大小引用
const contentRef = ref<InstanceType<typeof EnhancedCodeEditor> | null>(null);

const disableSubmit = ref(true);

const problemId = <IdType>route.params.problemId;

const userId= <IdType>route.params.userId;

const contestId = <IdType>route.params.contestId;


// 各种信息的计算属性
const problemType = computed(() => problem.value?.problemVo.type)


const isChoiceProblem = computed(() => {
  return problemType.value === ProblemType.CHOICE || problemType.value === ProblemType.MULTI_CHOICE;
})

const isOjProblem = computed(() => problemType.value === ProblemType.OJ)

const isFillProblem = computed(() => problemType.value === ProblemType.FILL)

const hint = computed(() => problem.value?.problemVo?.hint)

const ojProblem = computed(():OjProblemView => <OjProblemView>problem!.value!.ojProblemVo);

const isShowCodeEditor = computed((): boolean => {
  if (problem.value === undefined) {
    return false;
  }
  const type = problem.value?.problemVo.type;
  return type === ProblemType.OJ
});

const difficulty = computed(() => {
  const array = ['不确定', '简单', '中等', '困难']
  if (problem.value === undefined || problem.value.ojProblemVo === undefined) {
    return "未知";
  }
  return array[problem.value.ojProblemVo.difficulty]
});

// mb
const memoryLimit = computed(() => Math.ceil(ojProblem.value.memoryLimit / 1024));

// ms
const timeLimit = computed(() => ojProblem.value.timeLimit);

// mb
const stackLimit = computed(() => ojProblem.value.stackLimit)

// 问题类型，但是转换成字符串
const stringProblemType = computed(() => problemTypeToString(<ProblemType>problemType.value));

// 题目描述
const description = computed(() => problem.value?.problemVo.description || "")

// 判题结果
const judgeResult = ref<JudgeResponse>();

// 是否显示判题结果
const isShowResult = computed(() => {
  if (contestId) {
    return false;
  } else {
    return !!judgeResult.value;
  }

})

// 全屏大小 / 非全屏大小
const codeSpan = computed(() => {
  let span: number;
  if (isFullScreen.value) {
    return contestId ? 24 : 16;
  } else {
    span = 12;
  }
  return span;

})

// 判题的表单
const judgeForm = reactive<JudgeForm>({
  contestId: contestId,
  problemId: problemId,
  answers: [],
  code: ""
});


// 全屏
const onHandleFullScreen = () => {
  isFullScreen.value = !isFullScreen.value;
}

const onEditorReady = () => {
  getHeight();
}

const height = ref(0)

const getHeight = () => {
  height.value = contentRef.value?.$el.offsetHeight || 0;
}


const initBlanks = () => {

  // 有内容就不动
  if (judgeForm.answers.length > 0) {
    return;
  }

  // 没内容再添加
  const choices = problem.value?.choices || [];
  for (let choice of choices) {
    judgeForm.answers.push(reactive({
      index: <number>choice!.blankIndex,
      answer: ""
    }))
  }
}


const getProblem = async () => {

  problem.value = await getDetailProblem(problemId);
  // 回写答案
  await getAnswer();
  // 初始化填空题
  if (problem.value?.problemVo.type === ProblemType.FILL) {
    initBlanks();
  }

  judgeForm.answers?.forEach(x => {

    if (problem.value?.problemVo.type === ProblemType.FILL) {
      if (problem.value.choices?.length !== judgeForm.answers.length) {
        judgeForm.answers.length = <number>problem.value.choices?.length;
      }
    } else {
      const find = problem.value?.choices?.find(item => letterToNumber(<string>item.order) === x.index);
      if (!find) {
        judgeForm.answers = [];
        return;
      }
    }

  })
}

// 答案回写
const getAnswer = async () => {
  const answer = await getUserAnswer(problemId, contestId, userId);
  // answer是null表示还没有写
  if (!answer) return;
  judgeForm.code = <string>answer.code;
  answer.answers?.sort((a, b) => a.index - b.index);
  judgeForm.answers = <Answer[]>answer.answers;
  judgeForm.languageId = !!answer.languageId ? answer.languageId : '1';
}


// created
getProblem();


onMounted(() => {
  const debounceFunc = __.debounce(getHeight, 100);
  window.onresize = () => {
    debounceFunc()
  }


})

onUnmounted(() => {
  window.onreset = null;

})


</script>


<style lang="scss" scoped>
@use "@/assets/styles/color" as *;

::v-deep(.table) {
  .header-cell {
    height: 64px;
    background-color: $table-header-color;
  }

  .el-progress__text {
    font-size: 14px !important;
  }

  .row {
    height: 64px;
  }

}

</style>