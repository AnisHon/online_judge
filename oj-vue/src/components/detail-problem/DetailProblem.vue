<template>
  <div v-loading="problemIsLoading">
    <transition name="el-zoom-in-top">
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

          <el-divider/>

          <div class="content">
            <h2>题目描述</h2>
            <p class="description">
              <markdown-preview :text="description" />
            </p>

            <div class="detail-problem">
              <online-judge-problem :problem="ojProblem" v-if="isOjProblem"/>
              <fill-blank-problem :count="count" v-model="judgeForm"  v-else-if="isFillProblem" />
              <choice-choose-problem :problem-view="problem" v-model="judgeForm" v-else-if="isChoiceProblem" />
            </div>

            <div v-if="!isOjProblem">
              <div class="submit">
                <el-button type="success" @click="onHandleSubmit" :loading="isLoading">提交</el-button>
              </div>

              <div class="result" v-if="isShowResult">
                <ProblemResult :type="problemType" :result="judgeResult"/>
              </div>

            </div>


            <div class="hint" v-if="hint">
              <h2>提示</h2>
              <div>
                <markdown-preview :text="hint" />
              </div>


            </div>

          </div>

        </el-col>

        <el-col
            :span="codeSpan"
            v-if="isShowCodeEditor"
            style="padding: 0 20px"
        >
          <enhanced-code-editor
              v-model="judgeForm"
              :heightProp="height"
              @submit="onHandleSubmit"
              @full-screen="onHandleFullScreen"
              @on-ready="onEditorReady"
          />
        </el-col>

      </el-row>


    </transition>

  </div>
</template>

<script setup lang="ts">
import {
  debouncedGetDetailProblem,
  type OjProblemView,
  type ProblemDetailView,
  ProblemType,
} from "@/api/problem";
import {computed, onMounted, onUnmounted, reactive, type Ref, ref, watch} from "vue";
import EnhancedCodeEditor from '@/components/EnhancedCodeEdior/index.vue'
import {problemTypeToString} from "@/utils/problem";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {
  type Answer, debouncedSave,
  getDebouncedJudge,
  getUserAnswer,
  type JudgeForm,
  type JudgeResponse, saveUserAnswer,
} from "@/api/problem/judge";
import {debounce} from "@/utils/debounce";
import useLoading from "@/hooks/useLoading";
import OnlineJudgeProblem from "./OnlineJudgeProblem.vue";
import FillBlankProblem from "./FillBlank.vue";
import ChoiceChooseProblem from "./ChoiceChoose.vue";
import ProblemResult from "@/components/ProblemResult/ProblemResult.vue";
import __ from "lodash";

const problem = ref<ProblemDetailView>();

// 代码编辑器是否全屏
const isFullScreen = ref(false)

// 代码编辑器大小引用
const contentRef = ref<InstanceType<typeof EnhancedCodeEditor> | null>(null);

// 传入题目组件
const {problemId, contestId} = defineProps<{problemId: number, contestId?: number}>()

const {loading, finish, isLoading} = useLoading()
// 填空题有多少空
const count = computed(() => problem.value?.count || 0)

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

const stringProblemType = computed(() => problemTypeToString(<ProblemType>problemType.value));

const description = computed(() => problem.value?.problemVo.description || "")

const judgeResult = ref<JudgeResponse>();

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

const judgeForm = reactive<JudgeForm>({
  contestId: contestId,
  problemId: problemId,
  answers: [],
  code: ""
});
const judgeFormCopy = reactive<JudgeForm>({
  contestId: undefined,
  problemId: problemId,
  answers: [],
  code: ""
});


const doJudge = getDebouncedJudge(judgeForm,
    (data: JudgeResponse) => {
      showAnswers.value =  Array.isArray(data.answers) && data.answers.length > 0;
      // judgeResult.value = data;
      // judgeResult.value?.answers?.sort((a, b) => a.index - b.index);
    },
    undefined,
    finish
);



const showAnswers = ref(false);


// 三个事件
const onHandleSubmit = () => {
  loading()
  doJudge();
}

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

const reset = () => {
  judgeForm.answers = [];
  judgeForm.code = "";
  judgeForm.problemId = problemId;
  judgeForm.contestId = contestId;
}

const initBlanks = (count: number) => {

  // 有内容就不动
  if (judgeForm.answers.length > 0) {
    return;
  }

  // 没内容再添加
  for (let i = 0; i < count; i++) {
    judgeForm.answers.push(reactive({
      index: i,
      answer: ""
    }))
  }
}

const {loading: loadingProblem, isLoading: problemIsLoading, get} =
    debouncedGetDetailProblem(async data => {
      problem.value = data;


      // 回写答案
      await getAnswer();


      // 初始化填空题
      if (problem.value?.problemVo.type === ProblemType.FILL) {
        initBlanks(problem.value.count || 0);
      }

      // 判断是否修改过
      __.assign(judgeFormCopy, JSON.parse(JSON.stringify(judgeForm)));

    })

const getProblem = async () => {
  loadingProblem()

  // 取题目
  get(problemId)


}

// 答案回写
const getAnswer = async () => {
  const answer = await getUserAnswer({contestId: contestId, problemId: problemId})
  // answer是null表示还没有写
  if (!answer) return;
  judgeForm.code = <string>answer.code;
  answer.answers?.sort((a, b) => a.index - b.index);
  judgeForm.answers = <Answer[]>answer.answers;
  judgeForm.languageId = answer.languageId;
}

const saveAnswer = async () => {
  const data = await saveUserAnswer(judgeForm);
  if (data) {
    ElMessage.success("保存成功")
  } else {
    ElMessage.error("保存失败")
  }

}

watch(() => problemId, async () => {

  if (!__.isEqual(judgeForm, judgeFormCopy)) {
    await saveAnswer()
  }
  reset();
  await getProblem();

}, {immediate: true})

defineExpose<{isProblemLoading: Ref<boolean>}>({isProblemLoading: problemIsLoading})

onMounted(() => {

  const debounceFunc = debounce(getHeight, 100, false);
  window.onresize = () => {
    debounceFunc()
  }
})

onUnmounted(() => {
  window.onreset = null;
})
</script>


<style scoped>

.problem-content {
  padding: 0 20px;
  height: var(--in-main-content-height);
  overflow: auto;
}

.submit {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}

.tags {
  margin: 5px 0;
}

</style>