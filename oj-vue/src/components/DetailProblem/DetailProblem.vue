<template>
  <oj-workbench
      v-if="problem && isOjProblem"
      v-loading="problemIsLoading"
      :problem="problem"
      :problem-id="problemId"
      :contest-id="contestId"
      :disable-submit="isSubmitDisabled"
      :form="judgeForm"
      :logs="submitLogs"
      :active-submission="activeSubmission"
      :submit-loading="isSubmitLoading"
      :test-loading="testLoading"
      :fullscreen="isFullScreen"
      :test-console-open="testConsoleOpen"
      :stdin="stdin"
      :stdout="stdout"
      :test-result="testResult"
      :test-error="testError"
      @update:form="updateJudgeForm"
      @update:stdin="stdin = $event"
      @submit="onHandleSubmit"
      @test="submitTest"
      @toggle-test="toggleTestConsole"
      @full-screen="onHandleFullScreen"
      @open-log="openLog"
      @view-code="openSubmissionDetail"
  />
  <div v-else v-loading="problemIsLoading" class="detail-problem-root" :class="{ 'detail-problem-root--contest': contestId }">
    <el-result v-if="problemError" icon="error" title="题目加载失败" :sub-title="problemError">
      <template #extra><el-button type="primary" @click="loadProblem">重新加载</el-button></template>
    </el-result>
    <el-row v-if="problem !== undefined" class="detail-problem-row" justify="center" :gutter="20">

      <el-col style="padding: 0" class="problem-content" ref="contentRef" :span="12" v-show="!isFullScreen">
        <el-scrollbar class="problem-scrollbar" style="padding: 0 10px" :height="contestId ? '100%' : 'var(--in-main-content-height)'">
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
                <div class="description">
                  <markdown-preview variant="compact" :text="description" />
                </div>

                <div class="detail-problem">
                  <online-judge-problem :problem="ojProblem" v-if="isOjProblem && ojProblem"/>
                  <fill-blank-problem
                      :model-value="judgeForm"
                      @update:model-value="updateJudgeForm"
                      v-else-if="isFillProblem"
                  />
                  <choice-choose-problem
                      :problem-view="problem"
                      :model-value="judgeForm"
                      @update:model-value="updateJudgeForm"
                      v-else-if="isChoiceProblem"
                  />
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
                    <el-button type="success" :disabled="isShowResult || isSubmitDisabled" @click="onHandleSubmit" :loading="isSubmitLoading">提交</el-button>
                  </div>
                </div>


                <div class="hint" v-if="hint">
                  <h2>提示</h2>
                  <div>
                    <markdown-preview variant="compact" :text="hint" />
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
                    <el-tooltip content="AC 通过 WA 答案错误 CE 编译错误 RE 运行时错误 TLE 超时 MLE 内存过限 JUDGE_ERROR 判题服务异常">
                      <judge-status-badge :status="scope.row.status" />
                    </el-tooltip>
                  </template>
                </el-table-column >
                <el-table-column label="源代码" align="center" width="100">
                  <template #default="scope">
                    <el-button link type="primary" @click="openSubmissionDetail(scope.row)">
                      查看
                    </el-button>
                  </template>
                </el-table-column>
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
                  :param="solutionParam"
                  @update:param="updateSolutionParam"
                  :scroll-element="contentRef?.$el"
              />

            </el-tab-pane>

          </el-tabs>
        </el-scrollbar>
      </el-col>


      <el-col
          :span="codeSpan"
          v-if="isShowCodeEditor"
          class="code-editor-column"
          style="padding: 0 20px"
      >
        <div class="editor-panel">
          <enhanced-code-editor
              :disable-submit="isSubmitDisabled"
              :model-value="judgeForm"
              @update:model-value="updateJudgeForm"
              :heightProp="height"
              @submit="onHandleSubmit"
              @full-screen="onHandleFullScreen"
              @on-ready="onEditorReady"
              @open-log="openLog"
              @test="submitTest"
              :submit-loading="isSubmitLoading"
              :test-loading="testLoading"
          />
        </div>
        <el-row ref="testInputRowRef" class="test-input-panel" :gutter="20" style="max-height: 80px">
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
  <submission-detail-panel
      v-model="submissionDetailVisible"
      :log="selectedSubmission"
      :problem-title="problem?.problemVo.title"
  />
</template>

<script setup lang="ts">
import {
  getDetailProblem,
  type OjProblemView,
  type ProblemDetailView,
  ProblemType,
  recentSubmit,
} from "@/api/problem";
import {computed, onBeforeUnmount, onMounted, onUnmounted, reactive, type Ref, ref, watch} from "vue";
import EnhancedCodeEditor from '@/components/EnhancedCodeEdior/index.vue'
import {problemTypeToString} from "@/utils/problem";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {
  judge,
  getUserAnswer,
  type JudgeForm,
  type JudgeResponse,
  type LogSubmit,
  OJResult,
  pollSubmission,
  pollTestResult,
  saveUserAnswer,
  sendTest,
} from "@/api/problem/judge";
import useLoading from "@/hooks/useLoading";
import OnlineJudgeProblem from "./OnlineJudgeProblem.vue";
import FillBlankProblem from "./FillBlank.vue";
import ChoiceChooseProblem from "./ChoiceChoose.vue";
import OjWorkbench from "@/components/OjWorkbench/OjWorkbench.vue";
import ProblemResult from "@/components/ProblemResult/ProblemResult.vue";
import JudgeStatusBadge from "@/components/JudgeStatusBadge/JudgeStatusBadge.vue";
import SubmissionDetailPanel from "@/components/SubmissionDetailPanel/SubmissionDetailPanel.vue";
import __ from "lodash";
import {ElMessage, ElNotification} from "element-plus";
import {ChatLineSquare, Document, Notebook} from "@element-plus/icons-vue";
import Solutions from "@/views/solutions/component/SolutionsComponent/SolutionsComponent.vue";
import type {QuerySolution} from "@/api/solution";
import type {IdType} from "@/api/common.ts";
import {isPendingJudgeStatus, isTerminalJudgeStatus} from "@/utils/problem/judgeStatus";

const errorTitle = ref("");

const errorText = ref("");

const openErrorDialog = ref(false);
const submissionDetailVisible = ref(false);
const selectedSubmission = ref<LogSubmit>();

// 当前tab
const currentTab = ref("detail")

// 题目对象
const problem = ref<ProblemDetailView>();
const problemError = ref('');

// 代码编辑器是否全屏
const isFullScreen = ref(false)

// 代码编辑器大小引用
const contentRef = ref<InstanceType<typeof EnhancedCodeEditor> | null>(null);

// 传入题目组件
const {problemId, contestId, disableSubmit = false, showResult} = defineProps<{problemId: IdType, contestId?: IdType, disableSubmit?: boolean, showResult?: boolean}>()

const emit = defineEmits<{
  (event: 'submitted'): void;
}>()

const solutionParam = reactive<QuerySolution>({
  asc: true,
  pageSize: 20,
  currentPage: 1,
  problemId: problemId,
  userId: undefined,
})

const updateSolutionParam = (value: QuerySolution) => {
  Object.assign(solutionParam, value)
}

const {loading: startSubmitLoading, finish: finishSubmitLoading, isLoading: isSubmitLoading} = useLoading()
const testLoading = ref(false);
const testError = ref('');
const testBusy = ref(false);
let testRunSequence = 0;

// 各种信息的计算属性
const problemType = computed(() => problem.value?.problemVo.type)

//测试用例输入框
const testInputRowRef = ref(null);

const isChoiceProblem = computed(() => {
  return problemType.value === ProblemType.CHOICE || problemType.value === ProblemType.MULTI_CHOICE;
})

const isOjProblem = computed(() => problemType.value === ProblemType.OJ)

// OJ 提交在首次点击后立即锁定，避免鼠标连点或键盘重复触发创建多条提交记录。
// 后端 Redis 还有 12 秒硬 TTL，这里的计时器使用同样的兜底时间。
const ojSubmitLocked = ref(false);
let ojSubmitUnlockTimer: ReturnType<typeof setTimeout> | undefined;
const isSubmitDisabled = computed(() => disableSubmit || (isOjProblem.value && (ojSubmitLocked.value || isPendingJudgeStatus(activeSubmission.value?.status))));

const unlockOjSubmit = () => {
  ojSubmitLocked.value = false;
  if (ojSubmitUnlockTimer) {
    clearTimeout(ojSubmitUnlockTimer);
    ojSubmitUnlockTimer = undefined;
  }
};

const lockOjSubmit = () => {
  if (ojSubmitLocked.value) return false;
  ojSubmitLocked.value = true;
  if (ojSubmitUnlockTimer) clearTimeout(ojSubmitUnlockTimer);
  // 网络异常、轮询中断或服务重启时，前端也不能把按钮锁死。
  ojSubmitUnlockTimer = setTimeout(unlockOjSubmit, 90_000);
  return true;
};

const isFillProblem = computed(() => problemType.value === ProblemType.FILL)

const hint = computed(() => problem.value?.problemVo?.hint)

const ojProblem = computed((): OjProblemView | undefined => problem.value?.ojProblemVo);

const isShowCodeEditor = computed((): boolean => {
  if (problem.value === undefined) {
    return false;
  }
  const type = problem.value?.problemVo.type;
  return type === ProblemType.OJ
});

const difficulty = computed(() => {
  const array = ['未分类', '简单', '中等', '困难']
  if (problem.value === undefined || problem.value.ojProblemVo === undefined) {
    return "未知";
  }
  return array[problem.value.ojProblemVo.difficulty] || '未分类'
});

// mb
const memoryLimit = computed(() => ojProblem.value ? Math.ceil(ojProblem.value.memoryLimit / 1024) : '-');

// ms
const timeLimit = computed(() => ojProblem.value?.timeLimit ?? '-');

// mb
const stackLimit = computed(() => ojProblem.value?.stackLimit ?? '-')

// 问题类型，但是转换成字符串
const stringProblemType = computed(() => problemTypeToString(<ProblemType>problemType.value));

// 题目描述
const description = computed(() => problem.value?.problemVo.description || "")

// 判题结果
const judgeResult = ref<JudgeResponse>();

// 是否显示判题结果
const isShowResult = computed(() => (showResult ?? !contestId) && !!judgeResult.value)

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

const updateJudgeForm = (value: JudgeForm) => {
  Object.assign(judgeForm, value)
}

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

// OJ 提交状态只通过 HTTP 短轮询获取。
const activeSubmissionId = ref<IdType>();
const activeSubmission = ref<LogSubmit>();
const submissionPolls = new Map<string, () => void>();
const testConsoleOpen = ref(false);
const testResult = ref<import("@/api/problem/judge").TestResult>();
let testPollStop: (() => void) | undefined;

const requestUuid = () => globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random().toString(16).slice(2)}`;

const openSubmissionDetail = (log: LogSubmit) => {
  selectedSubmission.value = {...log};
  submissionDetailVisible.value = true;
}

// 测试的标准输入
const stdin = ref<string>("")

// 测试的标准输出
const stdout = ref("");

const openLog = () => {
  currentTab.value = "log";
}

const toggleTestConsole = () => {
  testConsoleOpen.value = !testConsoleOpen.value;
}

// 显示答案（非 OJ 题仍复用原有即时判题逻辑）
const showAnswers = ref(false);

const isTerminalJudgeState = (status?: string) => isTerminalJudgeStatus(status);

const upsertSubmitLog = (log: LogSubmit): LogSubmit => {
  const index = submitLogs.findIndex(item => String(item.submitId) === String(log.submitId));
  let current: LogSubmit;
  if (index === -1) {
    submitLogs.unshift(log);
    current = log;
  } else {
    submitLogs[index] = {...submitLogs[index], ...log};
    current = submitLogs[index];
  }
  if (String(activeSubmissionId.value) === String(log.submitId)) {
    activeSubmission.value = current;
  }
  if (String(selectedSubmission.value?.submitId) === String(log.submitId)) {
    selectedSubmission.value = {...selectedSubmission.value, ...current};
  }
  return current;
};

const startSubmissionPolling = async (submitId: IdType) => {
  const key = String(submitId);
  submissionPolls.get(key)?.();
  const stop = await pollSubmission(submitId, (log) => {
    const current = upsertSubmitLog(log);
    if (isTerminalJudgeState(log.status)) {
      submissionPolls.get(key)?.();
      submissionPolls.delete(key);
      unlockOjSubmit();
      openSubmissionDetail(current);
      finishSubmitLoading();
      emit('submitted');
    }
  }, 1000);
  submissionPolls.set(key, stop);
};

// 发送 OJ 测试：提交后只轮询这次 uuid 对应的结果。
const submitTest = () => {
  if (testBusy.value || !judgeForm.languageId) return;
  testConsoleOpen.value = true;
  void handleTestSubmit();
};

const handleTestSubmit = async () => {
  if (testBusy.value) return;
  const sequence = ++testRunSequence;
  testBusy.value = true;
  const uuid = requestUuid();
  stdout.value = "";
  errMsg.value = undefined;
  testError.value = '';
  testResult.value = {uuid, judgeResult: OJResult.QUEUE};
  testPollStop?.();
  testLoading.value = true;
  const payload = {code: judgeForm.code, languageId: judgeForm.languageId, stdin: stdin.value, uuid};

  try {
    await sendTest(payload);
    if (sequence !== testRunSequence) return;
    testPollStop = await pollTestResult(uuid, result => {
      if (sequence !== testRunSequence) return;
      testResult.value = result;
      errMsg.value = result.stderr;
      stdout.value = result.stdout || "";
      if (isTerminalJudgeState(result.judgeResult)) {
        testLoading.value = false;
        testBusy.value = false;
        testPollStop = undefined;
      }
    }, 1000);
  } catch (error) {
    if (sequence !== testRunSequence) return;
    testError.value = error instanceof Error ? error.message : '测试运行失败，请稍后重试';
    testLoading.value = false;
    testBusy.value = false;
  }
};

const submitOj = async () => {
  const payload = JSON.parse(JSON.stringify(judgeForm)) as JudgeForm;
  try {
    const data = await judge(payload, (message) => ElNotification.error({title: "提交失败", message}));
    if (!data?.submitId) {
      ElNotification.error({title: "提交失败", message: "服务没有返回提交编号"});
      unlockOjSubmit();
      finishSubmitLoading();
      return;
    }
    activeSubmissionId.value = data.submitId;
    const queuedLog: LogSubmit = {
      submitId: data.submitId,
      userId: "0",
      problemId,
      language: judgeForm.languageId || "未知语言",
      status: OJResult.QUEUE,
      code: judgeForm.code,
    };
    activeSubmission.value = queuedLog;
    upsertSubmitLog(queuedLog);
    await startSubmissionPolling(data.submitId);
    // 提交请求已进入判题队列，按钮保持禁用，直到结果完成或 12 秒前端兜底计时结束。
    finishSubmitLoading();
  } catch {
    unlockOjSubmit();
    finishSubmitLoading();
  }
};

// OJ 提交由前端即时锁定，后端 Redis 锁负责跨标签页、跨实例的最终兜底。
const onHandleSubmit = async () => {
  if (isSubmitDisabled.value || isSubmitLoading.value || !problem.value) return;
  if (problemType.value === ProblemType.OJ && !lockOjSubmit()) return;
  stdout.value = "";
  errMsg.value = undefined;
  startSubmitLoading();
  if (problemType.value === ProblemType.OJ) {
    await submitOj();
    return;
  }

  try {
    const data = await judge(judgeForm, (message) => ElNotification.error({title: "提交失败", message}));
    showAnswers.value = Array.isArray(data?.answers) && data.answers.length > 0;
    judgeResult.value = data;
    judgeResult.value?.answers?.sort((a, b) => a.index - b.index);
    emit('submitted');
  } catch {
    // HTTP 层已经统一展示错误消息。
  } finally {
    finishSubmitLoading();
  }
};

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
  problem.value = undefined;
  problemError.value = '';
  judgeForm.answers = [];
  judgeForm.code = "";
  judgeForm.problemId = problemId;
  judgeForm.contestId = contestId;
  judgeForm.languageId = '1';

  submitLogs.length = 0;
  activeSubmission.value = undefined;
  activeSubmissionId.value = undefined;
  judgeResult.value = undefined;
  showAnswers.value = false;

  isFullScreen.value = false;
}

const initBlanks = () => {
  const choices = [...(problem.value?.choices || [])]
    .filter(choice => choice.blankIndex != null)
    .sort((a, b) => Number(a.blankIndex) - Number(b.blankIndex));
  const existing = new Map(judgeForm.answers.map(answer => [Number(answer.index), answer.answer || '']));
  judgeForm.answers = choices.map(choice => ({
    index: Number(choice.blankIndex),
    answer: existing.get(Number(choice.blankIndex)) || '',
  }));
}

const problemIsLoading = ref(false);
let loadSequence = 0;

const getLogs = async (id: IdType, sequence = loadSequence) => {
  const logs = await recentSubmit(id)
  if (sequence !== loadSequence) return;
  submitLogs.length = 0;
  submitLogs.push(...logs);
}

const loadProblem = async () => {
  const sequence = ++loadSequence;
  problemIsLoading.value = true;
  reset();
  const id = problemId;
  try {
    const detail = await getDetailProblem(id);
    if (sequence !== loadSequence) return;
    problem.value = detail;
    if (detail.problemVo.type === ProblemType.FILL) initBlanks();
    await Promise.allSettled([getAnswer(id, sequence), getLogs(id, sequence)]);
    if (sequence !== loadSequence) return;
    if (detail.problemVo.type === ProblemType.FILL) initBlanks();
    __.assign(judgeFormCopy, JSON.parse(JSON.stringify(judgeForm)));
  } catch (error) {
    if (sequence !== loadSequence) return;
    problemError.value = error instanceof Error ? error.message : '题目加载失败，请稍后重试';
  } finally {
    if (sequence === loadSequence) problemIsLoading.value = false;
  }
}

// 答案回写
const getAnswer = async (id: IdType = problemId, sequence = loadSequence) => {
  const answer = await getUserAnswer({contestId: contestId, problemId: id})
  // answer是null表示还没有写

  if (!answer) return;
  if (sequence !== loadSequence) return;
  judgeForm.code = <string>answer.code;
  judgeForm.answers = [...(answer.answers || [])].sort((a, b) => a.index - b.index);
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
loadProblem();

watch(() => problemId, async () => {
  if (!__.isEqual(judgeForm, judgeFormCopy) && !disableSubmit && problem.value) {
    await saveAnswer()
  }
  await loadProblem();
})



defineExpose<{isProblemLoading: Ref<boolean>}>({isProblemLoading: problemIsLoading})

const debouncedGetHeight = __.debounce(getHeight, 100);
const handleResize = () => debouncedGetHeight();
onMounted(() => window.addEventListener('resize', handleResize));
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  debouncedGetHeight.cancel();
});

onUnmounted(() => {
  loadSequence++;
  testRunSequence++;
  testLoading.value = false;
  testBusy.value = false;
  unlockOjSubmit();
  submissionPolls.forEach(stop => stop());
  testPollStop?.();
})
</script>


<style scoped>

.detail-problem-root--contest { height: 100%; min-height: 0; overflow: hidden; }
.detail-problem-root--contest :deep(.detail-problem-row) { height: 100%; min-height: 0; margin: 0 !important; }
.detail-problem-root--contest :deep(.problem-content), .detail-problem-root--contest :deep(.code-editor-column) { height: 100%; min-height: 0; }
.detail-problem-root--contest :deep(.problem-scrollbar) { height: 100% !important; }
.detail-problem-root--contest :deep(.code-editor-column) { display: flex; flex-direction: column; overflow: hidden; }
.detail-problem-root--contest .editor-panel { min-height: 0; flex: 1; overflow: hidden; }
.detail-problem-root--contest .test-input-panel { flex: 0 0 auto; }

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
  font-family: "OJCodeFont", "JetBrains Mono", Consolas, monospace;
  font-weight: 700;
  text-align: left
}

.submission-code {
  max-height: 60vh;
  overflow: auto;
  margin: 0;
  padding: 18px;
  border-radius: 12px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  font: 14px/1.7 "OJCodeFont", "JetBrains Mono", Consolas, monospace;
  white-space: pre;
}

</style>
