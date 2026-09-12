<template>
  <ContestSubPageShell title="题目统计" kicker="ANALYTICS / PROBLEMS" description="按题目查看提交、得分和正确率，快速定位教学薄弱点。" :icon="DataAnalysis" tone="violet" :stats="stats">
    <template #actions><el-button v-has="'problem:contest:statistic'" :loading="loading" :icon="Refresh" @click="getList">刷新统计</el-button></template>
    <div class="table-heading"><div><strong>题目表现</strong><span>点击题目分数可查看每位用户的答题结果</span></div><el-tag effect="plain" type="info">{{ list.length }} 道题</el-tag></div>
    <el-table v-loading="loading" :data="list" class="stat-table" stripe>
      <el-table-column label="题目" min-width="250"><template #default="{row}"><div class="problem-cell"><span class="problem-id">#{{ row.problemId }}</span><el-link type="primary" :underline="false" @click="router.push({name: 'problem', params: {id: row.problemId}})">{{ row.title }}</el-link></div></template></el-table-column>
      <el-table-column label="满分" prop="score" width="90" align="center" />
      <el-table-column label="平均分" prop="average" width="100" align="center"><template #default="{row}"><strong>{{ row.average ?? 0 }}</strong></template></el-table-column>
      <el-table-column label="正确" prop="rightNum" width="90" align="center"><template #default="{row}"><span class="number number--green">{{ row.rightNum }}</span></template></el-table-column>
      <el-table-column label="错误" prop="wrongNum" width="90" align="center"><template #default="{row}"><span class="number number--red">{{ row.wrongNum }}</span></template></el-table-column>
      <el-table-column label="未提交" prop="absentNum" width="90" align="center"><template #default="{row}"><span class="number number--amber">{{ row.absentNum }}</span></template></el-table-column>
      <el-table-column label="正确率" width="145" align="center"><template #default="{row}"><div class="accuracy-cell"><el-progress :percentage="getAccuracy(row)" :stroke-width="7" :show-text="false" /><span>{{ getAccuracy(row) }}%</span></div></template></el-table-column>
      <el-table-column label="操作" width="110" fixed="right" align="right"><template #default="{row}"><el-button v-has="'problem:contest:statistic'" link type="primary" :icon="View" @click="toProblemScore(row)">查看分数</el-button></template></el-table-column>
      <template #empty><el-empty description="当前没有题目统计" /></template>
    </el-table>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import {computed, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import {DataAnalysis, Refresh, View} from "@element-plus/icons-vue";
import {getProblemStatistic, type ProblemStatistic} from "@/api/record";
import type {IdType} from "@/api/common.ts";
import ContestSubPageShell from "@/views/backend/teacher/contest-manage/component/ContestSubPageShell.vue";

const route = useRoute();
const router = useRouter();
const contestId = route.params.contestId as IdType;
const loading = ref(false);
const list = ref<ProblemStatistic[]>([]);
const getAccuracy = (row: ProblemStatistic) => {const total = row.absentNum + row.rightNum + row.wrongNum; return total ? Math.round(row.rightNum / total * 100) : 0;};
const averageAccuracy = computed(() => list.value.length ? Math.round(list.value.reduce((sum, row) => sum + getAccuracy(row), 0) / list.value.length) : 0);
const stats = computed(() => [{label: '题目数量', value: list.value.length, tone: 'violet'}, {label: '平均正确率', value: `${averageAccuracy.value}%`, tone: 'green'}, {label: '总提交数', value: list.value.reduce((sum, row) => sum + row.rightNum + row.wrongNum, 0), tone: 'blue'}, {label: '未提交数', value: list.value.reduce((sum, row) => sum + row.absentNum, 0), tone: 'amber'}]);
const getList = async () => {loading.value = true; try {list.value = await getProblemStatistic(contestId);} finally {loading.value = false;}};
const toProblemScore = (row: ProblemStatistic) => router.push({name: 'problem-scores', params: {contestId, problemId: row.problemId}});
getList();
</script>

<style scoped>
.table-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }.table-heading strong, .table-heading span { display: block; }.table-heading strong { font-size: 15px; }.table-heading span { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }.problem-cell { display: flex; align-items: center; gap: 9px; min-width: 0; }.problem-id { color: var(--el-text-color-placeholder); font: 11px var(--code-font-family, monospace); }.problem-cell :deep(.el-link) { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }.number { font-weight: 650; }.number--green { color: var(--el-color-success); }.number--red { color: var(--el-color-danger); }.number--amber { color: var(--el-color-warning); }.accuracy-cell { display: flex; align-items: center; gap: 8px; }.accuracy-cell :deep(.el-progress) { width: 72px; }.accuracy-cell span { width: 35px; color: var(--el-text-color-secondary); font-size: 11px; text-align: right; }
</style>
