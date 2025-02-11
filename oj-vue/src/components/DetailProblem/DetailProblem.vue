<template>
  <div v-loading="problemIsLoading">
    <el-row justify="center" v-if="problem !== undefined" :gutter="20">

      <el-col style="padding: 0" class="problem-content" ref="contentRef" :span="12" v-show="!isFullScreen">
        <el-scrollbar style="padding: 0 10px" height="var(--in-main-content-height)">
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
          <el-tabs
              v-model="currentTab"
              type="card"
              class="demo-tabs"
          >
            <el-tab-pane name="detail">
              <template #label>
                <el-icon><Document /></el-icon>
                <span>&nbsp;题目</span>
              </template>
              <div class="content">
                <h2>题目描述</h2>
                <p class="description">
                  <markdown-preview :text="description" />
                </p>

                <div class="detail-problem">
                  <online-judge-problem :problem="ojProblem" v-if="isOjProblem"/>
                  <fill-blank-problem v-model="judgeForm"  v-else-if="isFillProblem" />
                  <choice-choose-problem :problem-view="problem" v-model="judgeForm" v-else-if="isChoiceProblem" />
                  <el-skeleton v-else animated>
                    <el-skeleton-item variant="p"/>
                    <el-skeleton-item variant="p"/>
                    <el-skeleton-item variant="p"/>
                    <el-skeleton-item variant="p"/>
                    <el-skeleton-item variant="p"/>
                  </el-skeleton>
                </div>


                <div v-if="!isOjProblem">
                  <div class="submit">
                    <el-button type="success" :disabled="isShowResult || disableSubmit" @click="onHandleSubmit" :loading="isLoading">提交</el-button>
                  </div>
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
            </el-tab-pane>

            <el-tab-pane
                name="log"
                v-if="isOjProblem"
            >
              <template #label>
                <el-icon><ChatLineSquare /></el-icon>
                <span>&nbsp;提交记录</span>
              </template>
              <el-table :data="submitLogs">
                <el-table-column prop="submitId" label="提交ID" align="center" show-overflow-tooltip/>
                <el-table-column prop="userId" label="用户ID" align="center" show-overflow-tooltip/>
                <el-table-column prop="problemId" label="题目ID" align="center" show-overflow-tooltip/>
                <el-table-column prop="language" label="语言" align="center" />
                <el-table-column prop="status" label="结果" align="center">
                  <template v-slot="scope">
                    <el-tooltip content="AC 通过 WA 答案错误 CE 编译错误 RE 运行时错误 TLE 超时 MLE 内存过限">
                      <el-tag type="info" v-if="scope.row.status === OJResult.QUEUE">排队中</el-tag>
                      <el-tag type="primary" v-else-if="scope.row.status === OJResult.COMPILING">编译中</el-tag>
                      <el-tag type="success" v-else-if="scope.row.status === OJResult.ACCEPT">AC</el-tag>
                      <el-tag type="danger" v-else>{{ scope.row.status }}</el-tag>
                    </el-tooltip>
                  </template>
                </el-table-column >
                <el-table-column prop="time" label="时间(ms)" align="center"/>
                <el-table-column prop="memory" label="内存(MiB)" align="center"/>
              </el-table>
            </el-tab-pane>

            <el-tab-pane
                name="solution"
                v-if="!contestId"
                lazy
            >
              <template #label>
                <el-icon><Notebook /></el-icon>
                <span>&nbsp;题解</span>
              </template>

              <solutions
                  v-model:param="solutionParam"
                  :scroll-element="contentRef?.$el"
              />

            </el-tab-pane>

          </el-tabs>
        </el-scrollbar>
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
            @submit="onHandleSubmit"
            @full-screen="onHandleFullScreen"
            @on-ready="onEditorReady"
            @open-log="openLog"
            @test="submitTest"
            :loading="isLoading"
        />
        <el-row ref="testInputRowRef" :gutter="20" style="max-height: 80px">
          <el-col :span="12">
            <el-scrollbar>
              <h4 style="margin: 0">标准输入</h4>
              <el-input type="textarea" v-model="stdin" resize="none" />
            </el-scrollbar>
          </el-col>
          <el-col :span="12">
            <h4 style="margin: 0">输出</h4>
            <el-scrollbar :height="50">
              <p style="white-space: pre; font-family: monospace" v-text="stdout"></p>

            </el-scrollbar>
          </el-col>
        </el-row>

      </el-col>

    </el-row>

    <el-dialog v-model="openErrorDialog" style="min-height: 400px">
      <el-result
          icon="error"
          :title="errorTitle"
          :sub-title="errorText"
      >
        <template #extra>
          <div v-if="!!errMsg">
            <h3 style="margin: 0">标准错误流输出</h3>
            <p class="stderr"
                v-text="errMsg" ></p>
          </div>
        </template>
      </el-result>


    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import {
  debouncedGetDetailProblem,
  type OjProblemView,
  type ProblemDetailView,
  ProblemType,
  recentSubmit,
} from "@/api/problem";
import {computed, createVNode, onMounted, onUnmounted, reactive, type Ref, ref, type VNode, watch} from "vue";
import EnhancedCodeEditor from '@/components/EnhancedCodeEdior/index.vue'
import {problemTypeToString} from "@/utils/problem";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {
  type Answer,
  getDebouncedJudge,
  getUserAnswer,
  type JudgeForm,
  type JudgeMessage,
  type JudgeResponse,
  type LogSubmit,
  OJResult,
  saveUserAnswer,
  sendTest,
} from "@/api/problem/judge";
import useLoading from "@/hooks/useLoading";
import OnlineJudgeProblem from "./OnlineJudgeProblem.vue";
import FillBlankProblem from "./FillBlank.vue";
import ChoiceChooseProblem from "./ChoiceChoose.vue";
import ProblemResult from "@/components/ProblemResult/ProblemResult.vue";
import __ from "lodash";
import {letterToNumber} from "@/utils/stringUtils";
import {ElMessage, ElNotification, type MessageHandler} from "element-plus";
import CustomElMessage from "@/components/CustomElMessage.vue";
import {ChatLineSquare, Document, Notebook} from "@element-plus/icons-vue";
import Solutions from "@/views/solutions/component/SolutionsComponent/SolutionsComponent.vue";
import type {QuerySolution} from "@/api/solution";
import {getUuid, initSSE, isSseConnected, offSse, onSse, SseEvent} from "@/utils/sse";
import type {IdType} from "@/api/common.ts";

const errorTitle = ref("");

const errorText = ref("");

const openErrorDialog = ref(false);

// 当前tab
const currentTab = ref("detail")

// 题目对象
const problem = ref<ProblemDetailView>();

// 代码编辑器是否全屏
const isFullScreen = ref(false)

// 代码编辑器大小引用
const contentRef = ref<InstanceType<typeof EnhancedCodeEditor> | null>(null);

// 传入题目组件
const {problemId, contestId, disableSubmit = false} = defineProps<{problemId: IdType, contestId?: IdType, disableSubmit?: boolean}>()

const solutionParam = reactive<QuerySolution>({
  asc: true,
  pageSize: 20,
  currentPage: 1,
  problemId: problemId,
  userId: undefined,
})

const {loading, finish, isLoading} = useLoading()

// 各种信息的计算属性
const problemType = computed(() => problem.value?.problemVo.type)

//测试用例输入框
const testInputRowRef = ref(null);

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
  code: "",
  languageId: '1',
});

// 判题的表单的副本，用于比对
const judgeFormCopy = reactive<JudgeForm>({
  contestId: undefined,
  problemId: problemId,
  answers: [],
  code: ""
});

// RE CE时的消息
const errMsg = ref<string>();

// 所有提交日志
const submitLogs = reactive<LogSubmit[]>([])

// 测试的标准输入
const stdin = ref<string>("")

// 测试的标准输出
const stdout = ref("");

const openLog = () => {
  currentTab.value = "log";
}

// 发送OJ测试
const submitTest = () => {
  if (!isSseConnected()) {
    ElNotification.error({title: "测试失败", message: "连接已断开，请尝试刷新网页"})
    return;
  }
  handleTestSubmit();
  judgeForm.uuid = getUuid();
}



// 发送OJ测试
const handleTestSubmit = async () => {
  stdout.value = "";
  errMsg.value = undefined;
  loading();

  const testForm = {code: judgeForm.code, languageId: judgeForm.languageId, stdin: stdin.value, uuid: getUuid()};
  const {code} = await sendTest(testForm);
  if (code === 200) {
    getOjResult(true);
  } else {
    finish();
  }

}

// SSE的变换状态的回调
const onUpdateJudgeState = (judgeMessage: JudgeMessage, handler: any, instance: MessageHandler, vnode: VNode, isTest = false) => {

  if (judgeMessage.state == OJResult.COMPILING) {

    vnode?.component?.exposed?.update("编译中")

  } else if (judgeMessage.state == OJResult.RUNNING) {
    vnode?.component?.exposed?.update("运行中")
  }

  errMsg.value = judgeMessage.stderr;
  switch (judgeMessage.state) {
    case OJResult.ACCEPT:
      stdout.value = judgeMessage.stdout || "";
      if (!isTest) {
        getLogs()
        openLog();
      }
      break;
    case OJResult.RUNTIME_ERROR:
      errorTitle.value = "RE";
      errorText.value = "运行时错误";
      break;
    case OJResult.WRONG_ANSWER:
      errorTitle.value = "WA";
      errorText.value = "答案错误\n";
      break;
    case OJResult.TIME_LIMIT_EXCEEDED:
      errorTitle.value = "TLE";
      errorText.value = "时间超限";
      break;
    case OJResult.MEMORY_LIMIT_EXCEEDED:
      errorTitle.value = "MLE";
      errorText.value = "内存超限";
      break;
    case OJResult.COMPILE_ERROR:
      errorTitle.value = "CE";
      errorText.value = "编译错误";
      break;

  }
  const isFinish = judgeMessage.state != OJResult.COMPILING
      && judgeMessage.state != OJResult.QUEUE
      && judgeMessage.state != OJResult.RUNNING;

  if (isFinish) {
    offSse(SseEvent.UPDATE_JUDGE_STATE, handler);
    setTimeout(instance.close, 1000);
    finish();
    if (!isTest) {
      getLogs()
      openLog();
    }
    if (judgeMessage.state != OJResult.ACCEPT) {
      openErrorDialog.value = true;
    }
  }


}

//
const getOjResult = (isTest = false) => {
  const vNode = createVNode(CustomElMessage)
  const el = ElMessage(
      {
        message: vNode,
        duration: 60000
      }
  )
  vNode?.component?.exposed?.update("排队中")

  const onUpdate = (judgeMessage: JudgeMessage) => {
    onUpdateJudgeState(judgeMessage, onUpdate, el, vNode, isTest);
  }

  onSse(SseEvent.UPDATE_JUDGE_STATE, onUpdate)
}

// 发送判题
const doJudge = getDebouncedJudge(judgeForm,
    (data: JudgeResponse) => {

      if (!data) {
        return;
      }

      showAnswers.value =  Array.isArray(data.answers) && data.answers?.length > 0;

      judgeResult.value = data;
      judgeResult.value?.answers?.sort((a, b) => a.index - b.index);

      if (problemType.value === ProblemType.OJ) {
        getOjResult();
      }
    },
    undefined,
    finish
);


// 显示答案
const showAnswers = ref(false);

// 提交Oj答案
const onHandleSubmit = () => {
  if (!isSseConnected()) {
    ElNotification.error({title: "测试失败", message: "连接已断开，请尝试刷新网页"})
    return;
  }
  stdout.value = "";
  errMsg.value = undefined;

  loading();
  doJudge();
  judgeForm.uuid = getUuid();

}

// 全屏
const onHandleFullScreen = () => {
  isFullScreen.value = !isFullScreen.value;
}

const onEditorReady = () => {
  getHeight();
}

const height = ref(0)

const getHeight = () => {
  // @ts-ignore
  height.value = contentRef.value?.$el.offsetHeight - testInputRowRef.value?.$el.clientHeight || 0;
}

const reset = () => {
  judgeForm.answers = [];
  judgeForm.code = "";
  judgeForm.problemId = problemId;
  judgeForm.contestId = contestId;
  judgeForm.languageId = '1';

  submitLogs.length = 0;

  isFullScreen.value = false;
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

const {loading: loadingProblem, isLoading: problemIsLoading, get} =
    debouncedGetDetailProblem(async data => {
      problem.value = data;


      // 回写答案
      await getAnswer();


      // 初始化填空题
      if (problem.value?.problemVo.type === ProblemType.FILL) {
        initBlanks();
      }

      // 判断是否修改过
      __.assign(judgeFormCopy, JSON.parse(JSON.stringify(judgeForm)));


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
    })

const getLogs = async () => {
  const logs = await recentSubmit(problemId)
  submitLogs.length = 0;
  submitLogs.push(...logs);
}

const getProblem = async () => {
  loadingProblem()

  // 取题目
  get(problemId)

  await getLogs();

}

// 答案回写
const getAnswer = async () => {
  const answer = await getUserAnswer({contestId: contestId, problemId: problemId})
  // answer是null表示还没有写

  if (!answer) return;
  judgeForm.code = <string>answer.code;
  answer.answers?.sort((a, b) => a.index - b.index);
  judgeForm.answers = <Answer[]>answer.answers;
  judgeForm.languageId = !!answer.languageId ? answer.languageId : '1';
}

const saveAnswer = async () => {
  const data = await saveUserAnswer(judgeForm);
  if (data) {
    ElMessage.success("保存成功")
  } else {
    ElMessage.error("保存失败")
  }

}

// created
getProblem();

watch(() => problemId, async () => {
  if (!__.isEqual(judgeForm, judgeFormCopy) && !disableSubmit) {
    await saveAnswer()
  }
  reset();
  await getProblem();
})



defineExpose<{isProblemLoading: Ref<boolean>}>({isProblemLoading: problemIsLoading})

onMounted(() => {
  const debounceFunc = __.debounce(getHeight, 100);
  window.onresize = () => {
    debounceFunc()
  }


})

onUnmounted(() => {
  window.onreset = null;

})



initSSE();
</script>


<style scoped>


.submit {
  display: flex;
  justify-content: center;
  margin: 20px 0;
}

.tags {
  margin: 5px 0;
}

.stderr {
  color: var(--el-color-error);
  padding: 20px;
  font-size: 16px;
  white-space: pre-wrap;
  font-family: Inconsolata, Helvetica, sans-serif;
  font-weight: 700;
  text-align: left
}

</style>