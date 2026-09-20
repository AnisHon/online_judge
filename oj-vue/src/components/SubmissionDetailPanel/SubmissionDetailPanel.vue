<template>
  <el-drawer
      v-model="visible"
      class="submission-detail-drawer"
      direction="rtl"
      size="min(980px, 94vw)"
      :with-header="false"
      destroy-on-close
  >
    <div v-if="log" class="submission-detail">
      <header class="submission-detail__header">
        <div class="submission-detail__heading">
          <div class="submission-detail__eyebrow">SUBMISSION DETAIL</div>
          <div class="submission-detail__title-row">
            <h2>提交详情</h2>
            <judge-status-badge :status="log.status" />
          </div>
          <p>
            <span>#{{ log.submitId }}</span>
            <span v-if="problemTitle">{{ problemTitle }}</span>
            <span>{{ log.language || '未知语言' }}</span>
            <span>{{ formatDate(log.submitTime) }}</span>
          </p>
        </div>
        <el-button class="submission-detail__close" text circle aria-label="关闭" @click="visible = false">
          <el-icon><Close /></el-icon>
        </el-button>
      </header>

      <el-scrollbar class="submission-detail__scroll">
        <main class="submission-detail__body">
          <section class="result-hero" :class="`result-hero--${statusMeta.tone}`">
            <div class="result-hero__icon">
              <el-icon><component :is="resultIcon" /></el-icon>
            </div>
            <div>
              <strong>{{ resultTitle }}</strong>
              <p>{{ resultDescription }}</p>
            </div>
          </section>

          <section class="metric-grid" aria-label="提交概览">
            <div>
              <span>测试点</span>
              <strong>{{ passCountText }}</strong>
              <small v-if="log.totalCount">通过情况</small>
            </div>
            <div>
              <span>运行时间</span>
              <strong>{{ formatTime(log.time) }}</strong>
              <small>峰值耗时</small>
            </div>
            <div>
              <span>内存使用</span>
              <strong>{{ formatMemory(log.memory) }}</strong>
              <small>峰值占用</small>
            </div>
          </section>

          <section v-if="showErrorOutput" class="detail-section">
            <div class="section-heading">
              <div>
                <span class="section-heading__kicker">OUTPUT</span>
                <h3>{{ log.status === OJResult.COMPILE_ERROR ? '编译器输出' : '运行输出' }}</h3>
              </div>
              <el-tag size="small" :type="log.status === OJResult.COMPILE_ERROR ? 'warning' : 'danger'" effect="plain">
                {{ statusMeta.code }}
              </el-tag>
            </div>
            <pre class="error-output">{{ log.stderr }}</pre>
          </section>

          <section class="detail-section">
            <div class="section-heading">
              <div>
                <span class="section-heading__kicker">TEST CASES</span>
                <h3>测试点结果</h3>
              </div>
              <span v-if="caseResults.length" class="section-count">{{ caseResults.length }} 个</span>
            </div>

            <div v-if="casesLoading" class="cases-loading">
              <el-skeleton :rows="3" animated />
            </div>
            <div v-else-if="caseResults.length" class="case-grid">
              <article v-for="(item, index) in caseResults" :key="item.caseIndex ?? index" class="case-card"
                       :class="`case-card--${caseTone(item.status)}`">
                <div class="case-card__topline">
                  <span class="case-card__index">测试点 {{ item.caseIndex ?? '-' }}</span>
                  <judge-status-badge :status="item.status" />
                </div>
                <div class="case-card__metrics">
                  <span v-if="item.time != null">{{ formatTime(item.time) }}</span>
                  <span v-if="item.memory != null">{{ formatMemory(item.memory) }}</span>
                  <span v-if="item.score != null">得分 {{ item.score }}</span>
                </div>
              </article>
            </div>
            <div v-else class="cases-empty">
              <el-icon><DocumentChecked /></el-icon>
              <strong>{{ isPending ? '结果生成中' : '暂无测试点明细' }}</strong>
              <span>{{ isPending ? '完成后会自动更新这条提交记录' : '本次提交没有可展示的测试点结果' }}</span>
            </div>
          </section>

          <section class="detail-section code-section">
            <div class="section-heading">
              <div>
                <span class="section-heading__kicker">SOURCE CODE</span>
                <h3>提交代码</h3>
              </div>
              <el-button v-if="log.code" text size="small" @click="copyCode">
                <el-icon><CopyDocument /></el-icon>
                复制代码
              </el-button>
            </div>
            <pre v-if="log.code" class="source-code"><code>{{ log.code }}</code></pre>
            <el-empty v-else description="这条记录没有代码内容" :image-size="64" />
          </section>
        </main>
      </el-scrollbar>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import {computed, ref, watch} from 'vue';
import {Close, CopyDocument, DocumentChecked, CircleCheck, WarningFilled} from '@element-plus/icons-vue';
import {ElMessage} from 'element-plus';
import {getSubmissionCases, OJResult, type LogSubmit, type SubmitCaseResult} from '@/api/problem/judge';
import JudgeStatusBadge from '@/components/JudgeStatusBadge/JudgeStatusBadge.vue';
import {getJudgeStatusMeta, isPendingJudgeStatus} from '@/utils/problem/judgeStatus';

const props = defineProps<{
  modelValue: boolean;
  log?: LogSubmit;
  problemTitle?: string;
}>();

const emit = defineEmits<{(event: 'update:modelValue', value: boolean): void}>();

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value),
});
const caseResults = ref<SubmitCaseResult[]>([]);
const casesLoading = ref(false);
const statusMeta = computed(() => getJudgeStatusMeta(props.log?.status));
const isPending = computed(() => isPendingJudgeStatus(props.log?.status));
const resultIcon = computed(() => statusMeta.value.tone === 'success' ? CircleCheck : WarningFilled);

const resultTitle = computed(() => {
  if (props.log?.status === OJResult.ACCEPT) return '全部测试点通过';
  if (props.log?.status === OJResult.WRONG_ANSWER) return '有测试点未通过';
  if (props.log?.status === OJResult.COMPILE_ERROR) return '编译没有通过';
  if (isPending.value) return '正在处理这次提交';
  return statusMeta.value.label;
});

const resultDescription = computed(() => {
  if (props.log?.status === OJResult.ACCEPT) return '这份代码已经通过了当前题目的全部测试。';
  if (props.log?.status === OJResult.WRONG_ANSWER) {
    const total = props.log?.totalCount ?? (caseResults.value.length || '-');
    return `请检查标记为答案错误的测试点，共通过 ${props.log?.passCount ?? 0} / ${total} 个。`;
  }
  if (props.log?.status === OJResult.COMPILE_ERROR) return '下面展示编译器返回的具体信息，便于定位语法或环境问题。';
  if (props.log?.status === OJResult.JUDGE_ERROR) return '判题服务暂时无法完成这次提交，请稍后重试。';
  if (isPending.value) return '提交正在处理中，状态会自动更新。';
  return '可以在下方查看这次提交的代码和结果。';
});

const passCountText = computed(() => {
  if (props.log?.totalCount != null) return `${props.log.passCount ?? 0} / ${props.log.totalCount}`;
  return caseResults.value.length ? `${caseResults.value.filter(item => item.status === OJResult.ACCEPT).length} / ${caseResults.value.length}` : '-';
});
const showErrorOutput = computed(() =>
  !!props.log?.stderr && [OJResult.COMPILE_ERROR, OJResult.RUNTIME_ERROR].includes(props.log.status)
);

const formatDate = (value?: string) => value ? new Date(value).toLocaleString('zh-CN') : '刚刚';
const formatTime = (value?: number) => value == null ? '-' : `${value} ms`;
const formatMemory = (value?: number) => value == null ? '-' : `${value > 1024 ? (value / 1024).toFixed(1) : value} ${value > 1024 ? 'MiB' : 'KiB'}`;
const caseTone = (status?: string) => getJudgeStatusMeta(status).tone;

const loadCases = async () => {
  caseResults.value = [];
  if (!props.log?.submitId || isPending.value) return;
  casesLoading.value = true;
  try {
    caseResults.value = await getSubmissionCases(props.log.submitId);
  } catch {
    // 请求层已经提示网络错误，面板保持可用并展示提交级结果。
  } finally {
    casesLoading.value = false;
  }
};

const copyCode = async () => {
  if (!props.log?.code || !navigator.clipboard) return;
  try {
    await navigator.clipboard.writeText(props.log.code);
    ElMessage.success('代码已复制');
  } catch {
    ElMessage.warning('复制失败，请手动选择代码');
  }
};

watch(() => [props.modelValue, props.log?.submitId, props.log?.status], ([opened]) => {
  if (opened) void loadCases();
}, {immediate: true});
</script>

<style scoped>
.submission-detail-drawer :deep(.el-drawer__body) { padding: 0; overflow: hidden; }
.submission-detail-drawer :deep(.el-drawer) { background: var(--el-bg-color-page); }
.submission-detail { display: flex; height: 100%; min-height: 0; flex-direction: column; background: var(--el-bg-color-page); color: var(--el-text-color-primary); }
.submission-detail__header { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; padding: 28px 32px 24px; border-bottom: 1px solid var(--el-border-color-lighter); background: var(--el-bg-color); }
.submission-detail__eyebrow, .section-heading__kicker { color: var(--el-color-primary); font-size: 10px; font-weight: 800; letter-spacing: .14em; }
.submission-detail__title-row { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; margin-top: 7px; }
.submission-detail__title-row h2 { margin: 0; font-size: 24px; line-height: 1.2; }
.submission-detail__heading p { display: flex; flex-wrap: wrap; gap: 8px 16px; margin: 11px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }
.submission-detail__heading p span + span { position: relative; }
.submission-detail__heading p span + span::before { position: absolute; top: 50%; left: -9px; width: 3px; height: 3px; border-radius: 50%; background: var(--el-text-color-placeholder); content: ''; transform: translateY(-50%); }
.submission-detail__close { color: var(--el-text-color-secondary); }
.submission-detail__scroll { min-height: 0; flex: 1; }
.submission-detail__body { max-width: 920px; margin: 0 auto; padding: 28px 32px 42px; }
.result-hero { display: flex; align-items: center; gap: 16px; padding: 19px 20px; border: 1px solid color-mix(in srgb, var(--result-color) 24%, transparent); border-radius: 18px; background: color-mix(in srgb, var(--result-color) 7%, var(--el-bg-color)); }
.result-hero--success { --result-color: var(--el-color-success); }
.result-hero--danger { --result-color: var(--el-color-danger); }
.result-hero--warning { --result-color: var(--el-color-warning); }
.result-hero--info { --result-color: var(--el-color-info); }
.result-hero--pending, .result-hero--primary { --result-color: var(--el-color-primary); }
.result-hero__icon { display: grid; width: 42px; height: 42px; flex: 0 0 42px; place-items: center; border-radius: 13px; background: color-mix(in srgb, var(--result-color) 14%, transparent); color: var(--result-color); font-size: 23px; }
.result-hero strong { display: block; color: var(--result-color); font-size: 16px; }
.result-hero p { margin: 5px 0 0; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.6; }
.metric-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 12px; margin: 14px 0 28px; }
.metric-grid > div { padding: 15px 17px; border: 1px solid var(--el-border-color-lighter); border-radius: 15px; background: var(--el-bg-color); }
.metric-grid span, .metric-grid strong, .metric-grid small { display: block; }
.metric-grid span, .metric-grid small { color: var(--el-text-color-secondary); font-size: 11px; }
.metric-grid strong { margin: 8px 0 4px; font: 700 18px/1.2 var(--code-font-family, 'JetBrains Mono', monospace); }
.detail-section { margin-top: 27px; }
.section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; margin-bottom: 11px; }
.section-heading h3 { margin: 4px 0 0; font-size: 16px; }
.section-count { color: var(--el-text-color-secondary); font-size: 12px; }
.error-output, .source-code { overflow: auto; margin: 0; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; background: var(--el-fill-color-lighter); color: var(--el-text-color-primary); font: 13px/1.7 var(--code-font-family, 'JetBrains Mono', Consolas, monospace); white-space: pre; }
.error-output { padding: 16px; border-color: color-mix(in srgb, var(--el-color-warning) 30%, var(--el-border-color-lighter)); color: var(--el-text-color-primary); }
.source-code { max-height: 480px; padding: 18px; tab-size: 4; }
.case-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.case-card { min-width: 0; padding: 13px 14px; border: 1px solid var(--el-border-color-lighter); border-left: 3px solid var(--case-color); border-radius: 13px; background: var(--el-bg-color); }
.case-card--success { --case-color: var(--el-color-success); }
.case-card--danger { --case-color: var(--el-color-danger); }
.case-card--warning { --case-color: var(--el-color-warning); }
.case-card--info { --case-color: var(--el-color-info); }
.case-card--pending, .case-card--primary { --case-color: var(--el-color-primary); }
.case-card__topline, .case-card__metrics { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.case-card__index { font-size: 13px; font-weight: 650; }
.case-card__metrics { justify-content: flex-start; margin-top: 10px; color: var(--el-text-color-secondary); font-size: 11px; }
.cases-empty { display: grid; place-items: center; gap: 7px; padding: 34px 16px; border: 1px dashed var(--el-border-color); border-radius: 14px; color: var(--el-text-color-secondary); text-align: center; }
.cases-empty .el-icon { color: var(--el-color-primary); font-size: 28px; }
.cases-empty strong { color: var(--el-text-color-primary); font-size: 13px; }
.cases-empty span { font-size: 11px; }
.code-section { padding-bottom: 8px; }

@media (max-width: 640px) {
  .submission-detail__header, .submission-detail__body { padding-right: 18px; padding-left: 18px; }
  .submission-detail__title-row h2 { font-size: 21px; }
  .metric-grid, .case-grid { grid-template-columns: 1fr; }
}
</style>
