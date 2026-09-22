<template>
  <section class="oj-workbench" :class="{ 'oj-workbench--fullscreen': fullscreen, 'oj-workbench--activity': contestId }">
    <aside v-if="!fullscreen" ref="problemPaneRef" class="oj-problem-pane">
      <div class="problem-pane__header">
        <div class="problem-kicker">延拓 Code · PROBLEM</div>
        <h1>{{ problem.problemVo.title }}</h1>
        <div class="problem-meta">
          <el-tag v-if="difficulty" :type="difficultyType" effect="plain">{{ difficulty }}</el-tag>
          <span>{{ problem.problemVo.source || '题库' }}</span>
          <span v-for="tag in problem.tagVo" :key="tag.tagId" class="problem-tag"
                :style="{ '--tag-color': tag.tagColor }">
            {{ tag.tagName }}
          </span>
        </div>
        <div v-if="isOjProblem" class="limit-grid">
          <div><span>时间限制</span><strong>{{ problem.ojProblemVo?.timeLimit ?? '-' }} ms</strong></div>
          <div><span>内存限制</span><strong>{{ memoryLimit }} MiB</strong></div>
          <div><span>栈空间</span><strong>{{ stackLimit }} MiB</strong></div>
        </div>
      </div>

      <el-tabs v-model="activePane" class="problem-pane__tabs">
        <el-tab-pane name="statement">
          <template #label>
            <el-icon>
              <Document/>
            </el-icon>
            <span>题面</span></template>
          <el-scrollbar class="problem-pane__scroll">
            <article class="statement-content">
              <section>
                <h2>题目描述</h2>
                <markdown-preview variant="compact" :text="problem.problemVo.description || ''"/>
              </section>
              <section v-if="problem.ojProblemVo?.input">
                <h2>输入格式</h2>
                <markdown-preview variant="compact" :text="problem.ojProblemVo.input"/>
              </section>
              <section v-if="problem.ojProblemVo?.output">
                <h2>输出格式</h2>
                <markdown-preview variant="compact" :text="problem.ojProblemVo.output"/>
              </section>
              <div v-if="problem.ojProblemVo?.inputExample || problem.ojProblemVo?.outputExample" class="example-grid">
                <div v-if="problem.ojProblemVo?.inputExample">
                  <h3>输入样例</h3>
                  <pre>{{ problem.ojProblemVo.inputExample }}</pre>
                </div>
                <div v-if="problem.ojProblemVo?.outputExample">
                  <h3>输出样例</h3>
                  <pre>{{ problem.ojProblemVo.outputExample }}</pre>
                </div>
              </div>
              <section v-if="problem.problemVo.hint">
                <h2>提示</h2>
                <markdown-preview variant="compact" :text="problem.problemVo.hint"/>
              </section>
            </article>
          </el-scrollbar>
        </el-tab-pane>

        <el-tab-pane name="submissions" lazy>
          <template #label>
            <el-icon>
              <TrendCharts/>
            </el-icon>
            <span>提交记录</span><em v-if="logs.length">{{ logs.length }}</em></template>
          <el-scrollbar class="problem-pane__scroll">
            <div v-if="!logs.length" class="empty-state">
              <el-icon>
                <Document/>
              </el-icon>
              <strong>还没有提交记录</strong>
              <span>写好代码后，提交结果会显示在这里</span>
            </div>
            <div v-else class="submission-list">
              <article v-for="log in logs" :key="String(log.submitId)" class="submission-item">
                <div class="submission-item__main">
                  <span class="status-dot" :class="`status-dot--${statusTone(log.status)}`"></span>
                  <div>
                    <judge-status-badge :status="log.status" />
                    <small>#{{ log.submitId }} · {{ formatDate(log.submitTime) }}</small>
                  </div>
                </div>
                <div class="submission-item__stats">
                  <span v-if="log.totalCount">{{ log.passCount ?? 0 }}/{{ log.totalCount }} 用例</span>
                  <span v-if="log.time != null">{{ log.time }} ms</span>
                  <span v-if="log.memory != null">{{ formatMemory(log.memory) }}</span>
                  <el-button v-if="log.code" link type="primary" @click="$emit('view-code', log)">代码</el-button>
                </div>
              </article>
            </div>
          </el-scrollbar>
        </el-tab-pane>

        <el-tab-pane v-if="!contestId" name="solutions" lazy>
          <template #label>
            <el-icon>
              <Notebook/>
            </el-icon>
            <span>题解</span></template>
          <el-scrollbar class="problem-pane__scroll">
            <solutions :param="solutionParam" :scroll-element="problemPaneRef"/>
          </el-scrollbar>
        </el-tab-pane>
      </el-tabs>
    </aside>

    <main ref="codePaneRef" class="oj-code-pane">
      <div class="code-pane__topline">
        <div class="code-pane__identity">
          <span class="code-pane__eyebrow">{{ fullscreen ? 'FOCUS MODE' : 'CODE WORKSPACE' }}</span>
        </div>
        <div class="code-pane__actions">
          <el-button text :aria-label="testConsoleOpen ? '收起自定义测试' : '打开自定义测试'" @click="$emit('toggle-test')">
            <el-icon>
              <Monitor/>
            </el-icon>
            {{ testConsoleOpen ? '收起测试' : '打开测试' }}
          </el-button>
          <el-button text aria-label="查看提交记录" @click="openSubmissions">
            <el-icon>
              <TrendCharts/>
            </el-icon>
            记录
          </el-button>
          <el-button text :aria-label="fullscreen ? '退出全屏' : '全屏专注'" @click="$emit('full-screen')">
            <el-icon>
              <FullScreen/>
            </el-icon>
            {{ fullscreen ? '退出全屏' : '全屏专注' }}
          </el-button>
        </div>
      </div>

      <Teleport to="body">
        <Transition name="judge-toast">
          <div v-if="activeSubmission && statusVisible" class="judge-toast"
               :class="[`judge-toast--${statusTone(activeSubmission.status)}`, { 'judge-toast--interactive': !isPending(activeSubmission.status) }]"
               :role="isPending(activeSubmission.status) ? 'status' : 'button'"
               :tabindex="isPending(activeSubmission.status) ? -1 : 0"
               :aria-label="`查看提交 ${activeSubmission.submitId} 的详情`"
               @click="!isPending(activeSubmission.status) && $emit('view-code', activeSubmission)"
               @keydown.enter.prevent="!isPending(activeSubmission.status) && $emit('view-code', activeSubmission)"
               @keydown.space.prevent="!isPending(activeSubmission.status) && $emit('view-code', activeSubmission)">
            <span class="status-orb"><el-icon v-if="isPending(activeSubmission.status)"><Loading/></el-icon><el-icon
                v-else><CircleCheck/></el-icon></span>
            <div class="judge-toast__copy">
              <judge-status-badge :status="activeSubmission.status" />
              <small>提交 #{{ activeSubmission.submitId }}</small>
            </div>
            <div class="judge-toast__metrics">
              <span v-if="activeSubmission.totalCount">{{ activeSubmission.passCount ?? 0 }}/{{ activeSubmission.totalCount }} 用例</span>
              <span v-if="activeSubmission.time != null">{{ activeSubmission.time }} ms</span>
              <span v-if="activeSubmission.memory != null">{{ formatMemory(activeSubmission.memory) }}</span>
            </div>
            <el-progress v-if="isPending(activeSubmission.status)" :percentage="progress" :show-text="false"
                         :indeterminate="true"/>
          </div>
        </Transition>
      </Teleport>

      <resizable-panel v-model:size="editorPanelSize" variant="editor" class="oj-code-split" :class="{ 'oj-code-split--test-collapsed': !testConsoleOpen }" direction="vertical" :min-size="42" :max-size="86">
        <template #sidebar>
          <div ref="editorHostRef" class="editor-host">
            <enhanced-code-editor
                :model-value="form"
                @update:model-value="$emit('update:form', $event)"
                :height-prop="editorHeight"
                :disable-submit="disableSubmit"
                :submit-loading="submitLoading"
                :test-loading="testLoading"
                @submit="$emit('submit')"
                @test="$emit('test')"
                @full-screen="$emit('full-screen')"
                @open-log="openSubmissions"
                @on-ready="measure"
            />
          </div>
        </template>

        <section ref="testConsoleRef" class="test-console" :class="{ 'test-console--collapsed': !testConsoleOpen }">
        <header class="test-console__header">
          <div>
            <el-icon>
              <Monitor/>
            </el-icon>
            <strong>自定义测试</strong><span>使用当前代码运行一组输入</span></div>
          <div class="test-console__tools">
            <judge-status-badge v-if="testResult" :status="testResult.judgeResult" />
            <el-button text circle aria-label="收起自定义测试" @click="$emit('toggle-test')">
              <el-icon>
                <ArrowDown/>
              </el-icon>
            </el-button>
          </div>
        </header>
        <div v-show="testConsoleOpen" class="test-console__body">
          <div class="test-console__field">
            <label>标准输入</label>
            <el-input :model-value="stdin" @update:model-value="$emit('update:stdin', $event)" type="textarea"
                      resize="none" placeholder="输入测试数据，每行一个值"/>
          </div>
          <div class="test-console__field">
            <label>程序输出</label>
            <div v-if="testError" class="test-console__error" role="alert">{{ testError }}</div>
            <pre v-if="stdout" class="test-console__output">{{ stdout }}</pre>
            <pre v-if="testResult?.stderr" class="test-console__error-output">{{ testResult.stderr }}</pre>
            <div v-if="!stdout && !testResult?.stderr && !testError" class="test-console__placeholder">运行结果会显示在这里</div>
          </div>
        </div>
        </section>
      </resizable-panel>
    </main>
  </section>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, onUnmounted, reactive, ref, watch} from "vue";
import {
  ArrowDown,
  CircleCheck,
  Document,
  FullScreen,
  Loading,
  Monitor,
  Notebook,
  TrendCharts
} from "@element-plus/icons-vue";
import type {IdType} from "@/api/common.ts";
import type {ProblemDetailView} from "@/api/problem";
import {Difficulty, ProblemType} from "@/api/problem";
import type {JudgeForm, LogSubmit, TestResult} from "@/api/problem/judge";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import Solutions from "@/views/solutions/component/SolutionsComponent/SolutionsComponent.vue";
import JudgeStatusBadge from "@/components/JudgeStatusBadge/JudgeStatusBadge.vue";
import type {QuerySolution} from "@/api/solution";
import EnhancedCodeEditor from "@/components/EnhancedCodeEdior/index.vue";
import ResizablePanel from "@/components/ResizablePanel/ResizablePanel.vue";
import {getJudgeStatusMeta, isPendingJudgeStatus} from "@/utils/problem/judgeStatus";
import {formatJudgeDate, formatJudgeMemory} from "@/utils/problem/judgeDisplay";

const props = defineProps<{
  problem: ProblemDetailView;
  problemId: IdType;
  contestId?: IdType;
  disableSubmit: boolean;
  form: JudgeForm;
  logs: LogSubmit[];
  activeSubmission?: LogSubmit;
  submitLoading: boolean;
  testLoading: boolean;
  fullscreen: boolean;
  testConsoleOpen: boolean;
  stdin: string;
  stdout: string;
  testResult?: TestResult;
  testError?: string;
}>();

const emit = defineEmits<{
  (event: 'update:form', value: JudgeForm): void;
  (event: 'update:stdin', value: string): void;
  (event: 'submit'): void;
  (event: 'test'): void;
  (event: 'toggle-test'): void;
  (event: 'full-screen'): void;
  (event: 'open-log'): void;
  (event: 'view-code', log: LogSubmit): void;
}>();

const activePane = ref('statement');
const statusVisible = ref(false);
let statusDismissTimer: ReturnType<typeof setTimeout> | undefined;
const problemPaneRef = ref<HTMLElement>();
const codePaneRef = ref<HTMLElement>();
const editorHostRef = ref<HTMLElement>();
const testConsoleRef = ref<HTMLElement>();
const editorHeight = ref(420);
const editorPanelSize = ref(72);
const resizeObserver = ref<ResizeObserver>();
const solutionParam = reactive<QuerySolution>({
  asc: true,
  pageSize: 20,
  currentPage: 1,
  problemId: props.problemId,
  userId: undefined,
});

const openSubmissions = () => {
  activePane.value = 'submissions';
  emit('open-log');
};

const isOjProblem = computed(() => props.problem.problemVo.type === ProblemType.OJ);
const difficulty = computed(() => {
  const value = props.problem.ojProblemVo?.difficulty;
  return value == null ? '' : ({
    [Difficulty.UNKNOWN]: '未分类',
    [Difficulty.SIMPLE]: '简单',
    [Difficulty.MEDIUM]: '中等',
    [Difficulty.DIFFICULT]: '困难'
  } as Record<number, string>)[value] || '未分类';
});
const difficultyType = computed(() => {
  const value = props.problem.ojProblemVo?.difficulty;
  return value === Difficulty.DIFFICULT ? 'danger' : value === Difficulty.MEDIUM ? 'warning' : 'success';
});
const memoryLimit = computed(() => {
  const value = props.problem.ojProblemVo?.memoryLimit;
  return value == null ? '-' : Math.ceil(value / 1024);
});
const stackLimit = computed(() => {
  const value = props.problem.ojProblemVo?.stackLimit;
  return value == null ? '-' : Math.ceil(value / 1024);
});
const progress = computed(() => {
  const current = props.activeSubmission;
  if (!current?.totalCount) return 20;
  return Math.min(95, Math.round(((current.passCount || 0) / current.totalCount) * 100));
});

const isPending = (status?: string) => isPendingJudgeStatus(status);
const statusTone = (status?: string) => getJudgeStatusMeta(status).tone;
const formatDate = formatJudgeDate;
const formatMemory = formatJudgeMemory;

const showSubmissionToast = () => {
  if (statusDismissTimer) clearTimeout(statusDismissTimer);
  if (!props.activeSubmission) {
    statusVisible.value = false;
    return;
  }
  statusVisible.value = true;
  if (!isPending(props.activeSubmission.status)) {
    statusDismissTimer = setTimeout(() => {
      statusVisible.value = false;
      statusDismissTimer = undefined;
    }, 4200);
  }
};

watch(() => [props.activeSubmission?.submitId, props.activeSubmission?.status], showSubmissionToast, {immediate: true});

const measure = () => {
  nextTick(() => {
    // 编辑器高度直接取上方面板的实际高度，拖动分隔线时不会再叠加外层高度。
    const measuredHeight = editorHostRef.value?.clientHeight || 0;
    if (!measuredHeight) return;

    const nextHeight = Math.max(260, measuredHeight);
    // ResizeObserver 会观察 editorHost，而编辑器高度又会影响 editorHost 的布局。
    // 没有变化时不再回写，避免在 auto 高度布局中形成 ResizeObserver 反馈循环。
    if (Math.abs(editorHeight.value - nextHeight) > 1) {
      editorHeight.value = nextHeight;
    }
  });
};

onMounted(() => {
  measure();
  resizeObserver.value = new ResizeObserver(measure);
  if (codePaneRef.value) resizeObserver.value.observe(codePaneRef.value);
  if (editorHostRef.value) resizeObserver.value.observe(editorHostRef.value);
});

onUnmounted(() => {
  resizeObserver.value?.disconnect();
  if (statusDismissTimer) clearTimeout(statusDismissTimer);
});
</script>

<style scoped>
.oj-workbench {
  --oj-border: color-mix(in srgb, var(--el-border-color) 68%, transparent);
  display: grid;
  grid-template-columns: minmax(360px, 0.92fr) minmax(520px, 1.08fr);
  gap: 16px;
  width: 100%;
  height: var(--in-main-content-height);
  min-height: 0;
  max-height: var(--in-main-content-height);
  color: var(--el-text-color-primary);
}

.oj-workbench--fullscreen {
  position: fixed;
  z-index: 100;
  top: var(--menu-height);
  right: 0;
  bottom: 0;
  left: 0;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  width: auto;
  height: auto;
  box-sizing: border-box;
  padding: 16px;
  max-height: none;
  background: var(--el-bg-color-page);
}

.oj-workbench--activity {
  height: 100%;
  max-height: 100%;
}

.oj-problem-pane, .oj-code-pane {
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  border: 1px solid var(--oj-border);
  border-radius: 20px;
  background: var(--el-bg-color);
  box-shadow: 0 12px 36px color-mix(in srgb, var(--el-color-primary) 6%, transparent);
}

.oj-code-pane { position: relative; height: 100%; }
.oj-workbench > * { min-width: 0; }

.oj-problem-pane {
  display: flex;
  flex-direction: column;
}

.problem-pane__header {
  padding: 25px 26px 18px;
  border-bottom: 1px solid var(--oj-border);
  background: linear-gradient(145deg, color-mix(in srgb, var(--el-color-primary) 8%, var(--el-bg-color)), var(--el-bg-color));
}

.problem-kicker, .code-pane__eyebrow {
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .14em;
  text-transform: uppercase;
}

.problem-pane__header h1 {
  margin: 8px 0 14px;
  font-size: clamp(22px, 2vw, 30px);
  line-height: 1.2;
}

.problem-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.problem-tag {
  padding: 3px 8px;
  border: 1px solid color-mix(in srgb, var(--tag-color, var(--el-color-primary)) 35%, transparent);
  border-radius: 999px;
  color: var(--tag-color, var(--el-color-primary));
}

.limit-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 18px;
}

.limit-grid div {
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--el-fill-color-light);
}

.limit-grid span, .limit-grid strong {
  display: block;
}

.limit-grid span {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.limit-grid strong {
  margin-top: 4px;
  font-size: 13px;
}

.problem-pane__tabs {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.problem-pane__tabs :deep(.el-tabs__header) {
  margin: 0;
  padding: 0 20px;
}

.problem-pane__tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.problem-pane__tabs :deep(.el-tabs__content) {
  flex: 1;
  min-height: 0;
}

.problem-pane__tabs :deep(.el-tab-pane) {
  height: 100%;
}

.problem-pane__tabs :deep(.el-tabs__item) {
  gap: 6px;
  font-size: 13px;
}

.problem-pane__tabs em {
  min-width: 18px;
  padding: 1px 5px;
  border-radius: 99px;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  font-size: 10px;
  font-style: normal;
}

.problem-pane__scroll {
  height: 100%;
  padding: 4px 26px 26px;
}

.statement-content {
  padding: 4px 0 30px;
  line-height: 1.8;
}

.statement-content section {
  margin: 0 0 26px;
}

.statement-content h2 {
  margin: 0 0 10px;
  font-size: 17px;
}

.statement-content h3 {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.statement-content pre, .test-console__output {
  overflow: auto;
  margin: 0;
  padding: 13px 15px;
  border: 1px solid var(--oj-border);
  border-radius: 12px;
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-primary);
  font: 13px/1.65 "OJCodeFont", "JetBrains Mono", Consolas, monospace;
  white-space: pre-wrap;
}

.example-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 26px;
}

.empty-state {
  display: flex;
  align-items: center;
  flex-direction: column;
  gap: 8px;
  padding: 70px 20px;
  color: var(--el-text-color-secondary);
  text-align: center;
}

.empty-state .el-icon {
  margin-bottom: 5px;
  color: var(--el-color-primary);
  font-size: 32px;
}

.empty-state strong {
  color: var(--el-text-color-primary);
}

.empty-state span {
  font-size: 12px;
}

.submission-list {
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding: 10px 0;
}

.submission-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 13px 14px;
  border: 1px solid var(--oj-border);
  border-radius: 14px;
  background: var(--el-fill-color-lighter);
}

.submission-item__main, .submission-item__stats {
  display: flex;
  align-items: center;
  gap: 9px;
}

.submission-item__main strong, .submission-item__main small {
  display: block;
}

.submission-item__main small {
  margin-top: 3px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.submission-item__stats {
  flex-wrap: wrap;
  justify-content: flex-end;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.status-dot {
  width: 9px;
  height: 9px;
  flex: 0 0 9px;
  border-radius: 50%;
  background: var(--el-color-danger);
}

.status-dot--success {
  background: var(--el-color-success);
}

.status-dot--primary {
  background: var(--el-color-primary);
}

.status-dot--warning {
  background: var(--el-color-warning);
}

.status-dot--info {
  background: var(--el-color-info);
}

.status-dot--danger {
  background: var(--el-color-danger);
}

.status-dot--pending {
  background: var(--el-color-warning);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--el-color-warning) 14%, transparent);
}

.oj-code-pane {
  display: flex;
  flex-direction: column;
  padding: 14px;
  background: color-mix(in srgb, var(--el-fill-color-lighter) 55%, var(--el-bg-color));
}

.code-pane__topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 14px;
  min-height: 34px;
  padding: 0 7px 10px;
}

.code-pane__identity strong, .code-pane__identity span {
  display: block;
}

.code-pane__identity strong {
  margin-top: 3px;
  font-size: 14px;
}

.code-pane__actions {
  display: flex;
  flex: 0 0 auto;
  flex-wrap: wrap;
  gap: 2px;
}

.code-pane__actions .el-button {
  min-width: 88px;
  min-height: 32px;
  justify-content: center;
  white-space: nowrap;
}

.editor-host {
  display: flex;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding: 9px;
  border: 1px solid var(--oj-border);
  border-radius: 16px;
  background: var(--el-bg-color);
}

.editor-host :deep(.enhanced-editor) {
  display: flex;
  flex: 1;
  min-height: 0;
  flex-direction: column;
}

.editor-host :deep(.cm-component), .editor-host :deep(.CodeMirror) {
  min-height: 0;
  max-height: 100%;
  overflow: hidden;
}

/* 判题提示是浮层，不参与编辑器布局，避免状态变化挤压代码区。 */
.judge-toast {
  position: fixed;
  z-index: 2050;
  top: max(16px, env(safe-area-inset-top));
  left: 50%;
  display: flex;
  max-width: min(520px, calc(100vw - 32px));
  align-items: center;
  gap: 9px;
  min-height: 44px;
  box-sizing: border-box;
  padding: 7px 11px;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--status-color, var(--el-color-primary)) 30%, var(--oj-border));
  border-radius: 12px;
  background: color-mix(in srgb, var(--status-color, var(--el-color-primary)) 8%, var(--el-bg-color));
  box-shadow: 0 10px 28px rgb(15 23 42 / 14%);
  transform: translate(-50%, 0);
}

.judge-toast--success { --status-color: var(--el-color-success); }
.judge-toast--error { --status-color: var(--el-color-danger); }
.judge-toast--pending { --status-color: var(--el-color-warning); }
.judge-toast--primary { --status-color: var(--el-color-primary); }
.judge-toast--warning { --status-color: var(--el-color-warning); }
.judge-toast--info { --status-color: var(--el-color-info); }
.judge-toast--danger { --status-color: var(--el-color-danger); }
.judge-toast .status-orb { display: grid; width: 27px; height: 27px; flex: 0 0 27px; place-items: center; border-radius: 9px; background: color-mix(in srgb, var(--status-color) 15%, transparent); color: var(--status-color); }
.judge-toast__copy { display: flex; min-width: 92px; flex-direction: column; gap: 1px; }
.judge-toast__copy strong, .judge-toast__copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.judge-toast__copy strong { font-size: 12px; }
.judge-toast__copy small { color: var(--el-text-color-secondary); font-size: 10px; }
.judge-toast__metrics { display: flex; flex: 0 1 auto; flex-wrap: wrap; justify-content: flex-end; gap: 9px; color: var(--el-text-color-secondary); font-size: 10px; }
.judge-toast .el-progress { position: absolute; right: 0; bottom: 0; left: 0; }
.judge-toast--interactive { cursor: pointer; }
.judge-toast--interactive:focus-visible { outline: 2px solid var(--status-color); outline-offset: 3px; }
.judge-toast--interactive:hover { box-shadow: 0 12px 32px rgb(15 23 42 / 20%); }
.judge-toast-enter-active, .judge-toast-leave-active { transition: opacity .22s ease, transform .22s ease; }
.judge-toast-enter-from, .judge-toast-leave-to { opacity: 0; transform: translate(-50%, -18px); }

.oj-code-split { width: 100%; min-height: 0; flex: 1; border: 1px solid var(--oj-border); border-radius: 16px; background: var(--el-bg-color); }
.oj-code-split :deep(.resizable-panel__sidebar), .oj-code-split :deep(.resizable-panel__main) { box-sizing: border-box; }
.oj-code-split :deep(.resizable-panel__sidebar) { padding: 9px; }
.oj-code-split :deep(.resizable-panel__main) { padding: 9px; }
.oj-code-split--test-collapsed :deep(.resizable-panel__sidebar) { height: 100%; flex-basis: 100%; }
.oj-code-split--test-collapsed :deep(.resizable-panel__handle), .oj-code-split--test-collapsed :deep(.resizable-panel__main) { display: none; }

.test-console {
  width: 100%;
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  overflow: hidden;
  border: 0;
  border-radius: 10px;
  background: var(--el-bg-color);
}

.test-console--collapsed {
  height: 100%;
}

.test-console__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 42px;
  padding: 0 10px 0 14px;
}

.test-console__header > div, .test-console__tools {
  display: flex;
  align-items: center;
  gap: 8px;
}

.test-console__header strong {
  font-size: 13px;
}

.test-console__header span {
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.test-console__header > div:first-child .el-icon {
  color: var(--el-color-primary);
}

.test-console__body {
  display: grid;
  height: calc(100% - 42px);
  min-height: 0;
  min-width: 0;
  box-sizing: border-box;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  padding: 0 12px 12px;
}

.test-console__field {
  display: flex;
  min-height: 0;
  min-width: 0;
  flex-direction: column;
}

.test-console__field :deep(.el-textarea) {
  display: flex;
  min-height: 82px;
  flex: 1;
  flex-direction: column;
}

.test-console__field label {
  display: block;
  margin: 0 0 5px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.test-console__field :deep(.el-textarea__inner) {
  height: 100% !important;
  min-height: 82px !important;
  flex: 1;
  resize: none;
}

.test-console__output, .test-console__placeholder {
  height: auto;
  min-height: 82px;
  flex: 1;
  box-sizing: border-box;
  overflow: auto;
}

.test-console__error {
  margin-bottom: 8px;
  color: var(--el-color-danger);
  font-size: 12px;
  line-height: 1.5;
}

.test-console__error-output {
  min-height: 82px;
  flex: 1;
  box-sizing: border-box;
  overflow: auto;
  margin: 0;
  padding: 10px 12px;
  border: 1px solid color-mix(in srgb, var(--el-color-danger) 35%, var(--oj-border));
  border-radius: 12px;
  background: color-mix(in srgb, var(--el-color-danger) 5%, var(--el-bg-color));
  color: var(--el-color-danger);
  font: 12px/1.6 var(--code-font-family, 'JetBrains Mono', Consolas, monospace);
  white-space: pre-wrap;
}

.test-console__placeholder {
  display: grid;
  place-items: center;
  border: 1px dashed var(--oj-border);
  border-radius: 12px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

@media (max-width: 980px) {
  .oj-workbench {
    grid-template-columns: minmax(300px, .85fr) minmax(440px, 1.15fr);
    gap: 10px;
  }

  .problem-pane__header {
    padding: 18px;
  }

  .problem-pane__scroll {
    padding-inline: 18px;
  }
}

@media (max-width: 760px) {
  .oj-workbench {
    display: block;
    height: auto;
    max-height: none;
  }

  .oj-problem-pane {
    min-height: 52vh;
    margin-bottom: 10px;
  }

  .oj-code-pane {
    /* 单列时工作台本身允许页面滚动，但代码区必须拥有独立的有限高度。
       否则内部 vertical ResizablePanel 的 72% 高度会和 auto 高度父级互相反馈。 */
    height: clamp(420px, calc(100dvh - var(--menu-height) - 24px), 820px);
    min-height: 0;
    max-height: 820px;
  }

  .limit-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .code-pane__actions .el-button {
    padding-inline: 5px;
  }

  .code-pane__actions .el-button span {
    display: none;
  }

  .test-console__body, .example-grid {
    grid-template-columns: 1fr;
  }

  .judge-toast__metrics {
    display: none;
  }

  .judge-toast { max-width: calc(100vw - 20px); }
}

@media (max-width: 520px) {
  .oj-workbench--fullscreen { padding: 8px; }
  .oj-code-pane { padding: 9px; border-radius: 15px; }
  .code-pane__topline { align-items: stretch; gap: 7px; }
  .code-pane__actions { width: 100%; }
  .code-pane__actions .el-button { flex: 1 1 0; min-width: 0; padding-inline: 5px; }
  .code-pane__actions .el-button span { display: none; }
}
</style>
