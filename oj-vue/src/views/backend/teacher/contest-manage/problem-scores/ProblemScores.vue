<template>
  <ContestSubPageShell
    title="题目分数"
    kicker="ANALYTICS / PROBLEM SCORES"
    description="查看本题每位用户的得分与判题结果，并继续下钻到用户答题详情。"
    :icon="DataAnalysis"
    tone="violet"
    :stats="summaryStats"
  >
    <template #actions>
      <el-button
        v-has="'problem:contest:statistic'"
        :icon="Refresh"
        :loading="loading"
        @click="loadScores"
      >
        刷新数据
      </el-button>
      <el-button :icon="ArrowLeft" @click="goBack">返回统计</el-button>
    </template>

    <section class="score-panel">
      <div class="panel-heading">
        <div>
          <span class="panel-eyebrow">PROBLEM PERFORMANCE</span>
          <h2>用户得分明细</h2>
          <p>题目 #{{ problemId }} · 共 {{ filteredList.length }} 条匹配记录</p>
        </div>
        <div class="panel-context">
          <span>竞赛 / 作业</span>
          <strong>#{{ contestId }}</strong>
        </div>
      </div>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          class="keyword-input"
          clearable
          :prefix-icon="Search"
          placeholder="搜索用户 ID 或昵称"
        />
        <el-select v-model="resultFilter" class="result-select" placeholder="筛选结果">
          <el-option label="全部结果" value="all" />
          <el-option label="已通过" value="correct" />
          <el-option label="未通过" value="wrong" />
          <el-option label="未提交" value="unsubmitted" />
        </el-select>
      </div>

      <el-table
        v-loading="loading"
        class="score-table"
        :data="filteredList"
        row-key="userId"
        empty-text="暂无符合条件的分数记录"
      >
        <el-table-column label="用户" min-width="280">
          <template #default="{ row }">
            <div class="user-cell">
              <div class="user-avatar">{{ getInitial(row.nikeName, row.userId) }}</div>
              <div class="user-meta">
                <strong>{{ row.nikeName || '未设置昵称' }}</strong>
                <span>ID {{ row.userId }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="得分" width="160" sortable prop="score">
          <template #default="{ row }">
            <span v-if="hasScore(row.score)" class="score-value">{{ row.score }}</span>
            <span v-else class="muted-value">未提交</span>
          </template>
        </el-table-column>

        <el-table-column label="判题结果" width="170">
          <template #default="{ row }">
            <span class="result-pill" :class="getResultClass(row)">
              <el-icon><component :is="getResultIcon(row)" /></el-icon>
              {{ getResultText(row) }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="right">
          <template #default="{ row }">
            <el-button
              v-has="'problem:contest:statistic'"
              class="detail-button"
              link
              type="primary"
              :icon="View"
              @click="toUserScore(row.userId)"
            >
              查看答题详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  CircleCheck,
  Close,
  DataAnalysis,
  Refresh,
  Search,
  View,
} from '@element-plus/icons-vue'
import ContestSubPageShell from '../component/ContestSubPageShell.vue'
import { getProblemScore, type ProblemScore } from '@/api/record'
import type { IdType } from '@/api/common'

type ResultFilter = 'all' | 'correct' | 'wrong' | 'unsubmitted'

const route = useRoute()
const router = useRouter()
const contestId = computed(() => String(route.params.contestId || ''))
const problemId = computed(() => String(route.params.problemId || ''))

const loading = ref(false)
const list = ref<ProblemScore[]>([])
const keyword = ref('')
const resultFilter = ref<ResultFilter>('all')

const hasScore = (score: number | null | undefined) => score !== null && score !== undefined

const submittedCount = computed(() => list.value.filter((item) => hasScore(item.score)).length)
const correctCount = computed(
  () => list.value.filter((item) => hasScore(item.score) && item.correct).length,
)
const averageScore = computed(() => {
  const scores = list.value
    .filter((item) => hasScore(item.score))
    .map((item) => Number(item.score))
  if (!scores.length) return '0'
  return (scores.reduce((total, score) => total + score, 0) / scores.length).toFixed(1)
})

const summaryStats = computed(() => [
  { label: '参与用户', value: list.value.length, tone: 'blue' },
  { label: '已提交', value: submittedCount.value, tone: 'green' },
  { label: '通过人数', value: correctCount.value, tone: 'violet' },
  { label: '平均得分', value: averageScore.value, tone: 'orange' },
])

const filteredList = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  return list.value.filter((item) => {
    const matchesKeyword =
      !normalizedKeyword ||
      String(item.userId).toLowerCase().includes(normalizedKeyword) ||
      String(item.nikeName || '').toLowerCase().includes(normalizedKeyword)

    let matchesResult = true
    if (resultFilter.value === 'correct') {
      matchesResult = hasScore(item.score) && item.correct
    } else if (resultFilter.value === 'wrong') {
      matchesResult = hasScore(item.score) && !item.correct
    } else if (resultFilter.value === 'unsubmitted') {
      matchesResult = !hasScore(item.score)
    }

    return matchesKeyword && matchesResult
  })
})

const getInitial = (nickname: string | undefined, userId: IdType) => {
  const source = String(nickname || userId || '?').trim()
  return source.slice(0, 1).toUpperCase()
}

const getResultText = (item: ProblemScore) => {
  if (!hasScore(item.score)) return '未提交'
  return item.correct ? '通过' : '未通过'
}

const getResultClass = (item: ProblemScore) => {
  if (!hasScore(item.score)) return 'is-muted'
  return item.correct ? 'is-success' : 'is-danger'
}

const getResultIcon = (item: ProblemScore) => {
  if (!hasScore(item.score)) return View
  return item.correct ? CircleCheck : Close
}

const loadScores = async () => {
  loading.value = true
  try {
    list.value = (await getProblemScore({ problemId: problemId.value, contestId: contestId.value })) || []
  } finally {
    loading.value = false
  }
}

const goBack = () => {
  router.replace({ name: 'problem-statistic', params: { contestId: contestId.value } })
}

const toUserScore = (userId: IdType) => {
  router.push({
    name: 'user-scores',
    params: { contestId: contestId.value, problemId: problemId.value, userId },
  })
}

onMounted(loadScores)
watch([contestId, problemId], () => void loadScores())
</script>

<style lang="scss" scoped>
.score-panel {
  padding: 28px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 24px;
  background: var(--el-bg-color);
  box-shadow: 0 18px 50px rgb(15 23 42 / 5%);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.panel-eyebrow {
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.panel-heading h2 {
  margin: 8px 0 6px;
  color: var(--el-text-color-primary);
  font-size: 21px;
}

.panel-heading p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.panel-context {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.panel-context strong {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 18px;
}

.keyword-input {
  max-width: 360px;
}

.result-select {
  width: 150px;
}

.score-table {
  --el-table-border-color: var(--el-border-color-lighter);
  --el-table-header-bg-color: var(--el-fill-color-light);
  border-radius: 16px;
  overflow: hidden;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  display: grid;
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 20%, transparent);
  border-radius: 13px;
  background: color-mix(in srgb, var(--el-color-primary) 11%, var(--el-bg-color));
  color: var(--el-color-primary);
  font-size: 15px;
  font-weight: 800;
}

.user-meta {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.user-meta strong {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-meta span,
.muted-value {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.score-value {
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 750;
  font-variant-numeric: tabular-nums;
}

.result-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 650;
}

.result-pill.is-success {
  border-color: color-mix(in srgb, var(--el-color-success) 22%, transparent);
  background: color-mix(in srgb, var(--el-color-success) 10%, var(--el-bg-color));
  color: var(--el-color-success);
}

.result-pill.is-danger {
  border-color: color-mix(in srgb, var(--el-color-danger) 22%, transparent);
  background: color-mix(in srgb, var(--el-color-danger) 9%, var(--el-bg-color));
  color: var(--el-color-danger);
}

.result-pill.is-muted {
  border-color: var(--el-border-color-lighter);
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
}

.detail-button {
  font-weight: 650;
}

:deep(.el-table__header th.el-table__cell) {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 700;
}

:deep(.el-table__row td.el-table__cell) {
  height: 72px;
}

@media (max-width: 720px) {
  .score-panel {
    padding: 18px 14px;
    border-radius: 18px;
  }

  .panel-heading {
    flex-direction: column;
    gap: 12px;
  }

  .panel-context {
    align-items: flex-start;
  }

  .toolbar {
    flex-direction: column;
  }

  .keyword-input,
  .result-select {
    width: 100%;
    max-width: none;
  }
}
</style>
