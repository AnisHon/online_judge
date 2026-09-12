<template>
  <ContestSubPageShell title="用户分数" kicker="ANALYTICS / USER SCORES" description="查看用户在本场竞赛或作业中的逐题得分，并定位具体答题记录。" :icon="User" tone="blue" :stats="stats">
    <template #actions><el-button :icon="Refresh" :loading="loading" @click="getList">刷新数据</el-button><el-button :icon="ArrowLeft" @click="router.back()">返回统计</el-button></template>
    <section class="score-panel">
      <div class="panel-heading"><div><span class="eyebrow">USER PERFORMANCE</span><h2>逐题成绩</h2><p>用户 #{{ userId }} · 竞赛 / 作业 #{{ contestId }}</p></div><el-input v-model="keyword" clearable class="keyword-input" :prefix-icon="Search" placeholder="搜索题目" /></div>
      <el-table v-loading="loading" class="score-table" :data="filteredList" row-key="problemId">
        <el-table-column label="题目" min-width="350"><template #default="{ row }"><div class="problem-cell"><span class="problem-id">#{{ row.problemId }}</span><el-link type="primary" :underline="false" target="_blank" :href="`/problem/${row.problemId}`">{{ row.title || '未命名题目' }}</el-link></div></template></el-table-column>
        <el-table-column label="结果" width="150"><template #default="{ row }"><span class="result-pill" :class="row.correct ? 'is-success' : 'is-danger'"><el-icon><component :is="row.correct ? CircleCheck : Close" /></el-icon>{{ row.correct ? '通过' : '未通过' }}</span></template></el-table-column>
        <el-table-column label="得分" width="150"><template #default="{ row }"><strong v-if="hasScore(row.score)" class="score-value">{{ row.score }}</strong><span v-else class="muted">未提交</span></template></el-table-column>
        <el-table-column label="操作" width="150" fixed="right" align="right"><template #default="{ row }"><el-button link type="primary" :icon="View" :disabled="!hasScore(row.score)" @click="toUserAnswer(row)">查看答案</el-button></template></el-table-column>
        <template #empty><el-empty description="暂无成绩记录" /></template>
      </el-table>
    </section>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowLeft, CircleCheck, Close, Refresh, Search, User, View } from '@element-plus/icons-vue'
import { useRoute, useRouter } from 'vue-router'
import type { IdType } from '@/api/common'
import { getUserScores, type UserScore } from '@/api/record'
import ContestSubPageShell from '../component/ContestSubPageShell.vue'

const route = useRoute(); const router = useRouter()
const contestId = String(route.params.contestId || ''); const userId = String(route.params.userId || '')
const loading = ref(false); const list = ref<UserScore[]>([]); const keyword = ref('')
const hasScore = (score: number | null | undefined) => score !== null && score !== undefined
const answered = computed(() => list.value.filter((item) => hasScore(item.score)).length)
const correct = computed(() => list.value.filter((item) => hasScore(item.score) && item.correct).length)
const total = computed(() => list.value.reduce((sum, item) => sum + (Number(item.score) || 0), 0))
const stats = computed(() => [{ label: '题目数', value: list.value.length, tone: 'blue' }, { label: '已提交', value: answered.value, tone: 'green' }, { label: '通过', value: correct.value, tone: 'violet' }, { label: '总分', value: total.value, tone: 'amber' }])
const filteredList = computed(() => { const value = keyword.value.trim().toLowerCase(); return value ? list.value.filter((item) => String(item.title || '').toLowerCase().includes(value) || String(item.problemId).includes(value)) : list.value })
const getList = async () => { loading.value = true; try { list.value = await getUserScores({ userId, contestId }) } finally { loading.value = false } }
const toUserAnswer = (row: UserScore) => router.push({ name: 'user-answer', params: { contestId, problemId: row.problemId, userId } })
onMounted(getList)
</script>

<style scoped>
.score-panel { padding: 24px; border: 1px solid var(--el-border-color-lighter); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 14px 40px rgb(15 23 42 / 4%); }.panel-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 18px; }.eyebrow { color: var(--el-color-primary); font-size: 10px; font-weight: 800; letter-spacing: .14em; }.panel-heading h2 { margin: 7px 0 5px; font-size: 21px; }.panel-heading p { margin: 0; color: var(--el-text-color-secondary); font-size: 12px; }.keyword-input { width: 240px; }.score-table { border-radius: 14px; overflow: hidden; }.problem-cell { display: flex; align-items: center; gap: 10px; }.problem-id { color: var(--el-text-color-placeholder); font: 12px var(--code-font-family, monospace); }.score-value { color: var(--el-text-color-primary); font-size: 18px; }.muted { color: var(--el-text-color-secondary); font-size: 12px; }.result-pill { display: inline-flex; align-items: center; gap: 5px; padding: 5px 9px; border-radius: 999px; font-size: 12px; font-weight: 700; }.result-pill.is-success { color: var(--el-color-success); background: color-mix(in srgb, var(--el-color-success) 10%, var(--el-bg-color)); }.result-pill.is-danger { color: var(--el-color-danger); background: color-mix(in srgb, var(--el-color-danger) 10%, var(--el-bg-color)); }.score-table :deep(.el-table__header th) { color: var(--el-text-color-secondary); background: var(--el-fill-color-light); }
@media (max-width: 680px) { .score-panel { padding: 16px 12px; }.panel-heading { align-items: stretch; flex-direction: column; }.keyword-input { width: 100%; } }
</style>
