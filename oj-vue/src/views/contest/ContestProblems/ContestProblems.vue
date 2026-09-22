<template>
  <main class="contest-workspace">
    <activity-resizable-panel v-model:size="sidebarSize" v-model:collapsed="sidebarCollapsed" class="contest-layout"
                              :min-size="20" :max-size="36">
      <template #sidebar>
        <aside class="problem-sidebar">
          <div class="sidebar-scroll">
            <section class="activity-summary">
              <div class="summary-actions">
                <el-button class="back-button" text :icon="ArrowLeft" aria-label="返回活动列表" @click="router.back()">返回活动列表</el-button>
                <el-button class="hide-button" text :icon="DArrowLeft" title="隐藏题目导航" aria-label="隐藏题目导航"
                           @click="sidebarCollapsed = true">隐藏题目
                </el-button>
              </div>
              <div class="summary-heading">
                <div class="activity-identity"><span class="activity-icon"><el-icon><component
                    :is="contest?.type === ContestType.HOMEWORK ? Notebook : Trophy"/></el-icon></span>
                  <div><p>{{ contest?.type === ContestType.HOMEWORK ? '作业空间' : '比赛空间' }}</p>
                    <h2 :title="contest?.title">{{ contest?.title || '正在加载活动' }}</h2></div>
                </div>
                <span class="status-badge" :class="`status-badge--${activityStatus.tone}`"><i/>{{
                    activityStatus.label
                  }}</span></div>
              <p class="summary-hint">{{
                  statusError ? statusError : isAnswering ? '可以继续作答，答案会在提交时记录。' : '当前活动不再接受新的提交。'
                }}</p>
              <div class="summary-metrics">
                <div><strong>{{ score }}</strong><small>当前得分</small></div>
                <div><strong>{{ completedCount }}<em>/{{ problemList.length }}</em></strong><small>已完成</small></div>
              </div>
              <div class="progress-track"><span :style="{ width: `${progressPercent}%` }"/></div>
            </section>

            <section class="problem-navigation">
              <div class="sidebar-heading">
                <div><span class="eyebrow">PROBLEM NAVIGATION</span>
                  <h2>题目导航</h2></div>
                <span class="problem-count">{{ problemList.length }} 题</span></div>
              <p class="sidebar-tip">拖动分隔线调整宽度，点击题目开始作答。</p>
              <div v-loading="isLoading" class="problem-list">
                <button v-for="(item, index) in sortedProblemList" :key="item.problemId" class="problem-card"
                        :class="{ 'is-active': String(currentRow?.problemId) === String(item.problemId), 'is-completed': item.finish === true, 'is-correct': item.correct === true, 'is-wrong': item.finish === true && item.correct === false }"
                        type="button"
                        :aria-current="String(currentRow?.problemId) === String(item.problemId) ? 'page' : undefined"
                        :aria-label="`${item.title || '未命名题目'}，${item.finish ? (item.correct ? '已通过' : '已完成但未通过') : '未完成'}`"
                        @click="selectProblem(item)">
                  <span class="problem-card__number">{{ String(index + 1).padStart(2, '0') }}</span>
                  <span class="problem-card__content"><strong :title="item.title">{{
                      item.title || '未命名题目'
                    }}</strong><small>ID {{ shortId(item.problemId) }} · {{ item.score ?? 0 }} 分</small></span>
                  <span v-if="item.finish" class="problem-card__state"><el-icon v-if="item.correct"><CircleCheck/></el-icon><el-icon
                      v-else><CircleClose/></el-icon><span class="sr-only">{{ item.correct ? '已通过' : '已完成但未通过' }}</span></span>
                  <el-icon v-else class="problem-card__arrow">
                    <ArrowRight/>
                  </el-icon>
                </button>
                <el-alert v-if="problemError" type="error" :closable="false" show-icon>
                  <template #title><span>{{ problemError }}</span><el-button link type="primary" @click="refreshProblemState">重试</el-button></template>
                </el-alert>
                <el-empty v-if="!isLoading && !problemError && !problemList.length" :image-size="64" description="暂无题目"/>
              </div>
            </section>
          </div>

          <footer class="submit-panel">
            <div><small>作答进度</small><strong>{{ completedCount }} / {{ problemList.length }}</strong></div>
            <el-button v-if="isAnswering" type="warning" plain :icon="Finished" :loading="isHandingIn"
                       :disabled="!canHandIn" @click="handleHandIn">提交试卷
            </el-button>
            <el-tag v-else type="info" effect="plain">不可提交</el-tag>
          </footer>
        </aside>
      </template>

      <section class="problem-stage">
        <div v-if="currentRow" class="problem-stage__bar">
          <div class="stage-heading">
            <span class="stage-index">第 {{ currentIndex + 1 }} 题</span>
            <strong :title="currentRow.title">{{ currentRow.title }}</strong>
            <small>ID {{ shortId(currentRow.problemId) }}</small>
          </div>
          <el-button text :icon="House" aria-label="返回活动概览" @click="currentRow = undefined">活动概览</el-button>
        </div>
        <div v-if="currentRow" class="problem-detail-shell">
          <detail-problem :key="String(currentRow.problemId)" :problem-id="currentRow.problemId" :contest-id="contestId"
                          :disable-submit="!isAnswering" @submitted="refreshProblemState"/>
        </div>

        <div v-else class="activity-overview">
          <template v-if="contest">
            <section class="overview-intro"><span class="overview-intro__icon"><el-icon><component
                :is="contest.type === ContestType.HOMEWORK ? Notebook : Trophy"/></el-icon></span>
              <p class="eyebrow">{{ contest.type === ContestType.HOMEWORK ? 'ASSIGNMENT BRIEF' : 'CONTEST BRIEF' }}</p>
              <h2>准备好开始了吗？</h2>
              <p>从左侧选择一道题，进入专注的答题空间。你可以随时收起题目导航，把更多空间留给题目内容。</p>
              <div class="overview-meta"><span><el-icon><Calendar/></el-icon>{{ formatOptionalDate(contest.startTime) }}</span><span
                  class="meta-divider">至</span><span>{{ formatOptionalDate(contest.endTime) }}</span>
                <el-tag :type="authTagType(contest.auth)" effect="plain">{{ authText(contest.auth) }}</el-tag>
              </div>
            </section>
            <section v-if="contest.description" class="overview-description">
              <div class="section-heading"><span>01</span>
                <div><strong>活动说明</strong><small>开始前先了解本次活动的要求</small></div>
              </div>
              <markdown-preview :text="contest.description"/>
            </section>
            <section class="overview-guide">
              <div class="section-heading"><span>02</span>
                <div><strong>答题提示</strong><small>保持节奏，提交前检查你的答案</small></div>
              </div>
              <div class="guide-grid">
                <div><strong>选择题目</strong><span>侧栏中的题目会保留当前作答进度。</span></div>
                <div><strong>随时切换</strong><span>切换题目不会丢失已经保存的答案。</span></div>
                <div><strong>完成提交</strong><span>确认所有答案后，在侧栏底部提交试卷。</span></div>
              </div>
            </section>
          </template>
          <el-empty v-else-if="!isLoadingContest" description="活动信息加载失败，请刷新重试"/>
        </div>
      </section>
    </activity-resizable-panel>
  </main>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, reactive, ref, watch} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {
  ArrowLeft,
  ArrowRight,
  Calendar,
  CircleCheck,
  CircleClose,
  DArrowLeft,
  Finished,
  House,
  Notebook,
  Trophy
} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import type {ProblemInListView} from '@/api/list'
import {getContestProblems} from '@/api/list/problem'
import DetailProblem from '@/components/DetailProblem/DetailProblem.vue'
import MarkdownPreview from '@/components/MarkdownPreview.vue'
import ActivityResizablePanel from '@/components/ActivityResizablePanel/ActivityResizablePanel.vue'
import {ContestType, type ContestView, fetchContestById, getContestStatus, handInPaper} from '@/api/contest'
import {authTagType, authText, formatDate, getActivityTimeState} from '@/utils/contest'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const contestId = computed(() => String(route.params.id))
const contest = ref<ContestView>()
const problemList = reactive<ProblemInListView[]>([])
const currentRow = ref<ProblemInListView>()
const sidebarSize = ref(25)
const sidebarCollapsed = ref(false)
const isAnswering = ref<boolean>()
const isLoadingContest = ref(true)
const isLoading = ref(false)
const problemError = ref('')
const contestError = ref('')
const statusError = ref('')
const isHandingIn = ref(false)
const now = ref(dayjs())
let clockTimer: ReturnType<typeof setInterval> | undefined
let loadSequence = 0
const sortedProblemList = computed(() => [...problemList].sort((a, b) => (a.problemOrder ?? 0) - (b.problemOrder ?? 0)))
const score = computed(() => problemList.reduce((total, problem) => total + (problem.userScore || 0), 0))
const completedCount = computed(() => problemList.filter(problem => problem.finish === true).length)
const progressPercent = computed(() => problemList.length ? Math.round(completedCount.value / problemList.length * 100) : 0)
const currentIndex = computed(() => sortedProblemList.value.findIndex(item => String(item.problemId) === String(currentRow.value?.problemId)))
const activityTime = computed(() => getActivityTimeState(contest.value?.startTime, contest.value?.endTime, now.value))
const activityStatus = computed(() => {
  if (!contest.value) return {label: '加载中', tone: 'pending'}
  if (contestError.value) return {label: '信息异常', tone: 'ended'}
  if (!activityTime.value.valid) return {label: '时间异常', tone: 'ended'}
  if (activityTime.value.notStarted) return {label: '未开始', tone: 'pending'}
  if (activityTime.value.over) return {label: '已结束', tone: 'ended'}
  if (statusError.value) return {label: '状态未知', tone: 'pending'}
  if (isAnswering.value === false) return {label: '已提交', tone: 'ended'}
  return {label: '进行中', tone: 'running'}
})
const canHandIn = computed(() => Boolean(isAnswering.value && contest.value && activityTime.value.valid && !activityTime.value.over && !isLoadingContest.value && !isLoading.value && !problemError.value && problemList.length))

const shortId = (value: unknown) => {
  const text = String(value);
  return text.length > 16 ? `${text.slice(0, 7)}…${text.slice(-5)}` : text
}
const formatOptionalDate = (value?: string) => value ? formatDate(value) : '待定'
const loadProblems = async (id: string, sequence = loadSequence) => {
  isLoading.value = true
  problemError.value = ''
  try {
    const data = await getContestProblems(id)
    if (sequence !== loadSequence || id !== contestId.value) return
    problemList.splice(0, problemList.length, ...(data || []))
  } catch (reason) {
    if (sequence !== loadSequence || id !== contestId.value) return
    problemError.value = reason instanceof Error ? reason.message : '题目列表加载失败，请重试'
  } finally {
    if (sequence === loadSequence && id === contestId.value) isLoading.value = false
  }
}

const loadPage = async (id = contestId.value) => {
  const sequence = ++loadSequence
  isLoadingContest.value = true
  contest.value = undefined
  contestError.value = ''
  statusError.value = ''
  problemError.value = ''
  problemList.splice(0, problemList.length)
  currentRow.value = undefined
  sidebarCollapsed.value = false
  isAnswering.value = undefined
  void loadProblems(id, sequence)

  try {
    const contestData = await fetchContestById(id)
    if (sequence !== loadSequence || id !== contestId.value) return
    contest.value = contestData
  } catch (reason) {
    if (sequence === loadSequence && id === contestId.value) {
      contestError.value = reason instanceof Error ? reason.message : '活动信息加载失败，请刷新重试'
    }
  } finally {
    if (sequence === loadSequence && id === contestId.value) isLoadingContest.value = false
  }

  try {
    const status = await getContestStatus(id)
    if (sequence === loadSequence && id === contestId.value) {
      isAnswering.value = status
      statusError.value = ''
    }
  } catch (reason) {
    if (sequence === loadSequence && id === contestId.value) {
      statusError.value = reason instanceof Error ? reason.message : '活动答题状态暂时无法确认'
    }
  }
}
const selectProblem = (row: ProblemInListView) => {
  currentRow.value = row;
  sidebarCollapsed.value = false
}
const refreshProblemState = async () => {
  await loadProblems(contestId.value, loadSequence)
}
const refreshActivityState = async () => {
  await refreshProblemState()
  try {
    isAnswering.value = await getContestStatus(contestId.value)
    statusError.value = ''
  } catch {
    statusError.value = '交卷已发送，但活动状态暂时无法确认，请刷新重试'
  }
}
const handleHandIn = async () => {
  if (isHandingIn.value || !canHandIn.value) return
  try {
    await ElMessageBox.confirm('提交试卷后将不能继续作答，确认提交吗？', '提交试卷', {
      cancelButtonText: '继续作答',
      confirmButtonText: '确认提交',
      type: 'warning'
    })
  } catch (reason) {
    if (reason === 'cancel' || reason === 'close') ElMessage.info('已取消提交')
    return
  }

  isHandingIn.value = true
  try {
    const submitted = await handInPaper(contestId.value)
    if (!submitted) {
      ElMessage.error('交卷失败，请稍后重试')
      return
    }
    isAnswering.value = false
    currentRow.value = undefined
    ElMessage.success('交卷成功')
    await refreshActivityState()
  } catch (reason) {
    ElMessage.error(reason instanceof Error ? reason.message : '交卷失败，请稍后重试')
  } finally {
    isHandingIn.value = false
  }
}

watch(contestId, id => { void loadPage(id) }, {immediate: true})
onMounted(() => {
  clockTimer = setInterval(() => { now.value = dayjs() }, 30_000)
})
onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer)
  loadSequence++
})
</script>

<style scoped>
.contest-workspace {
  display: flex;
  width: 100%;
  height: var(--in-main-content-height);
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  color: var(--el-text-color-primary);
}

.activity-icon {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 10px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 17px;
}

.contest-layout {
  display: flex;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  min-height: 0;
  min-width: 0;
  flex: 1;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 15px;
  box-shadow: 0 7px 24px rgb(15 23 42 / 4%);
}

.problem-sidebar {
  display: flex;
  width: 100%;
  height: 100%;
  min-width: 0;
  max-height: 100%;
  box-sizing: border-box;
  flex-direction: column;
  background: var(--el-bg-color);
}

.sidebar-scroll {
  min-height: 0;
  flex: 1;
  overflow: auto;
  padding: 14px 13px 0;
}

.activity-summary {
  margin-bottom: 17px;
  padding: 13px 12px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 13px;
  background: var(--el-bg-color-page);
}

.summary-heading, .sidebar-heading, .summary-metrics, .overview-meta, .overview-intro, .section-heading {
  display: flex;
}

.summary-heading, .sidebar-heading {
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.eyebrow {
  margin: 0 0 3px;
  color: var(--el-color-primary);
  font-size: 9px;
  font-weight: 800;
  letter-spacing: .14em;
}

.sidebar-heading h2 {
  margin: 0;
  font-size: 15px;
}

.summary-actions, .activity-identity {
  display: flex;
  align-items: center;
}

.summary-actions {
  justify-content: space-between;
  gap: 4px;
  margin: -4px -5px 12px;
}

.back-button, .hide-button {
  flex: 0 0 auto;
  padding: 4px 5px;
  font-size: 11px;
}

.activity-identity {
  min-width: 0;
  gap: 9px;
  margin-bottom: 16px;
}

.activity-identity > div {
  min-width: 0;
}

.activity-identity p {
  margin: 0 0 2px;
  color: var(--el-text-color-secondary);
  font-size: 10px;
}

.activity-identity h2 {
  max-width: 100%;
  margin: 0;
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 7px;
  border-radius: 999px;
  background: var(--el-fill-color);
  font-size: 10px;
  font-weight: 650;
  white-space: nowrap;
}

.status-badge i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-badge--running {
  color: var(--el-color-success);
}

.status-badge--pending {
  color: var(--el-color-warning);
}

.status-badge--ended {
  color: var(--el-text-color-secondary);
}

.summary-hint {
  margin: 8px 0 13px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  line-height: 1.5;
}

.summary-metrics {
  gap: 26px;
}

.summary-metrics div {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.summary-metrics strong {
  color: var(--el-text-color-primary);
  font: 700 17px var(--code-font-family, monospace);
}

.summary-metrics em {
  color: var(--el-text-color-placeholder);
  font-size: 11px;
  font-style: normal;
}

.summary-metrics small {
  color: var(--el-text-color-secondary);
  font-size: 10px;
}

.progress-track {
  height: 4px;
  margin-top: 12px;
  overflow: hidden;
  border-radius: 99px;
  background: var(--el-fill-color-darker);
}

.progress-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--el-color-primary);
  transition: width .25s ease;
}

.sidebar-tip {
  margin: 6px 0 11px;
  color: var(--el-text-color-secondary);
  font-size: 10px;
  line-height: 1.45;
}

.problem-list {
  min-height: 40px;
}

.problem-card {
  display: flex;
  width: 100%;
  min-width: 0;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
  padding: 9px 7px;
  border: 1px solid transparent;
  border-radius: 10px;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition: border-color .18s ease, background-color .18s ease, transform .18s ease;
}

.problem-card:hover {
  border-color: var(--el-border-color-light);
  background: var(--el-fill-color-light);
  transform: translateX(2px);
}

.problem-card.is-active {
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-9);
}

.problem-card__number {
  width: 23px;
  flex: 0 0 auto;
  color: var(--el-text-color-placeholder);
  font: 700 10px var(--code-font-family, monospace);
}

.problem-card.is-active .problem-card__number {
  color: var(--el-color-primary);
}

.problem-card__content {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 3px;
}

.problem-card__content strong, .problem-card__content small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.problem-card__content strong {
  color: var(--el-text-color-primary);
  font-size: 12px;
}

.problem-card__content small {
  color: var(--el-text-color-secondary);
  font-size: 10px;
}

.problem-card__state, .problem-card__arrow {
  flex: 0 0 auto;
  color: var(--el-text-color-placeholder);
}

.problem-card__state {
  font-size: 14px;
}

.problem-card.is-correct .problem-card__state {
  color: var(--el-color-success);
}

.problem-card.is-wrong .problem-card__state {
  color: var(--el-color-danger);
}

.submit-panel {
  display: flex;
  min-height: 58px;
  box-sizing: border-box;
  flex: 0 0 auto;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 13px;
  border-top: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.submit-panel div {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.submit-panel small {
  color: var(--el-text-color-secondary);
  font-size: 10px;
}

.submit-panel strong {
  font: 700 13px var(--code-font-family, monospace);
}

.problem-stage {
  position: relative;
  display: flex;
  height: 100%;
  min-width: 0;
  min-height: 0;
  flex-direction: column;
  overflow: hidden;
  background: var(--el-bg-color-page);
}

.problem-stage__bar {
  display: flex;
  min-height: 44px;
  box-sizing: border-box;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 5px 14px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.stage-heading {
  display: flex;
  min-width: 0;
  flex: 1 1 auto;
  align-items: center;
  gap: 8px;
}

.stage-heading strong {
  min-width: 0;
  flex: 1 1 auto;
  max-width: min(50vw, 680px);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-heading small {
  color: var(--el-text-color-secondary);
  font: 10px var(--code-font-family, monospace);
  white-space: nowrap;
}

.stage-index {
  flex: 0 0 auto;
  color: var(--el-color-primary);
  font: 700 10px var(--code-font-family, monospace);
}

.problem-stage__bar > .el-button {
  flex: 0 0 auto;
  white-space: nowrap;
}

.problem-detail-shell {
  display: flex;
  min-height: 0;
  flex: 1;
  overflow: hidden;
}

.problem-detail-shell :deep(> div) {
  width: 100%;
  height: 100%;
  min-height: 0;
}

.problem-detail-shell :deep(> .oj-workbench) {
  width: 100%;
  height: 100%;
  min-height: 0;
  max-height: 100%;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.activity-overview {
  height: 100%;
  box-sizing: border-box;
  overflow: auto;
  padding: clamp(20px, 4vw, 52px) clamp(16px, 5vw, 72px);
}

.overview-intro {
  max-width: 780px;
  margin: 0 auto 27px;
  flex-direction: column;
  align-items: flex-start;
}

.overview-intro__icon {
  display: grid;
  width: 42px;
  height: 42px;
  margin-bottom: 15px;
  place-items: center;
  border-radius: 13px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-size: 21px;
}

.overview-intro h2 {
  margin: 0;
  font-size: clamp(25px, 4vw, 39px);
  letter-spacing: -.05em;
}

.overview-intro > p:not(.eyebrow) {
  max-width: 620px;
  margin: 11px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
  line-height: 1.75;
}

.overview-meta {
  flex-wrap: wrap;
  align-items: center;
  gap: 9px;
  margin-top: 19px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.overview-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.meta-divider {
  color: var(--el-text-color-placeholder);
}

.overview-description, .overview-guide {
  max-width: 780px;
  margin: 0 auto 14px;
  padding: 17px 19px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
  background: var(--el-bg-color);
}

.section-heading {
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 13px;
}

.section-heading > span {
  color: var(--el-color-primary);
  font: 700 11px/1.5 var(--code-font-family, monospace);
}

.section-heading strong, .section-heading small {
  display: block;
}

.section-heading strong {
  font-size: 14px;
}

.section-heading small {
  margin-top: 2px;
  color: var(--el-text-color-secondary);
  font-size: 10px;
}

.overview-description :deep(.reading-preview-shell) {
  border: 0;
  border-radius: 0;
  background: transparent;
}

.overview-description :deep(.md-editor-preview) {
  padding: 0;
}

.guide-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.guide-grid div {
  padding: 11px;
  border-radius: 10px;
  background: var(--el-fill-color-light);
}

.guide-grid strong, .guide-grid span {
  display: block;
}

.guide-grid strong {
  font-size: 12px;
}

.guide-grid span {
  margin-top: 5px;
  color: var(--el-text-color-secondary);
  font-size: 10px;
  line-height: 1.55;
}

@media (max-width: 760px) {
  .contest-workspace {
    height: calc(100dvh - var(--menu-height) - 24px);
  }

  .problem-stage__bar {
    padding-inline: 10px;
  }

  .stage-heading small {
    display: none;
  }

  .stage-heading strong {
    max-width: none;
  }

  .activity-overview {
    padding: 22px 14px;
  }

  .guide-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 500px) {
  .back-button {
    font-size: 0;
  }

  .back-button .el-icon {
    margin: 0;
    font-size: 16px;
  }

  .hide-button {
    font-size: 0;
  }

  .hide-button .el-icon {
    margin: 0;
    font-size: 16px;
  }

  .activity-icon {
    width: 30px;
    height: 30px;
  }

  .activity-identity h2 {
    font-size: 13px;
  }

  .overview-intro h2 {
    font-size: 27px;
  }
}
</style>
