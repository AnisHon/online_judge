<template>
  <ContestSubPageShell title="用户统计" kicker="ANALYTICS / USERS" description="查看参与用户的提交状态和得分，跟进未完成或迟交情况。" :icon="User" tone="green" :stats="stats">
    <template #actions><el-button v-has="'problem:contest:statistic'" :loading="loading" :icon="Refresh" @click="getList">刷新统计</el-button></template>
    <div class="table-heading"><div><strong>用户表现</strong><span>可按提交状态筛选，退回提交需要竞赛编辑权限</span></div><div class="filters"><el-input v-model="keyword" clearable placeholder="搜索昵称或用户 ID" :prefix-icon="Search" /><el-select v-model="statusFilter" clearable placeholder="全部状态"><el-option label="已提交" value="submitted" /><el-option label="未提交" value="pending" /><el-option label="迟交" value="late" /></el-select></div></div>
    <el-table v-loading="loading" :data="filteredList" class="stat-table" stripe>
      <el-table-column label="用户" min-width="190"><template #default="{row}"><div class="user-cell"><span class="user-avatar">{{ String(row.nikeName || '?').slice(0, 1) }}</span><span><strong>{{ row.nikeName || '未命名用户' }}</strong><small>ID: {{ row.userId }}</small></span></div></template></el-table-column>
      <el-table-column label="提交状态" width="105" align="center"><template #default="{row}"><span class="state-tag" :class="row.submitted ? 'state-tag--success' : 'state-tag--muted'"><i></i>{{ row.submitted ? '已提交' : '未提交' }}</span></template></el-table-column>
      <el-table-column label="迟交状态" width="105" align="center"><template #default="{row}"><span v-if="!row.submitted" class="muted">—</span><span v-else class="state-tag" :class="row.late ? 'state-tag--warning' : 'state-tag--success'"><i></i>{{ row.late ? '迟交' : '按时' }}</span></template></el-table-column>
      <el-table-column label="得分" prop="score" width="95" align="center"><template #default="{row}"><strong class="score">{{ row.score ?? 0 }}</strong></template></el-table-column>
      <el-table-column label="题目完成" min-width="165"><template #default="{row}"><div class="completion"><span>{{ row.correctNum + row.wrongNum }} / {{ row.correctNum + row.wrongNum + row.absentNum }}</span><el-progress :percentage="completion(row)" :show-text="false" :stroke-width="6" /></div></template></el-table-column>
      <el-table-column label="操作" width="175" fixed="right" align="right"><template #default="{row}"><el-button v-has="'problem:contest:statistic'" link type="primary" :icon="View" @click="toUserScore(row)">用户分数</el-button><el-button v-has="'problem:contest:edit'" link type="warning" :disabled="!row.submitted" :icon="RefreshLeft" @click="handleReturn(row)">退回提交</el-button></template></el-table-column>
      <template #empty><el-empty description="没有找到符合条件的用户" /></template>
    </el-table>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import {computed, ref, watch} from "vue";
import {useRoute, useRouter} from "vue-router";
import {ElMessageBox} from "element-plus";
import {Refresh, RefreshLeft, Search, User, View} from "@element-plus/icons-vue";
import {getUserStatistic, returnUserSubmit, type UserStatistic} from "@/api/record";
import type {IdType} from "@/api/common.ts";
import ContestSubPageShell from "@/views/backend/teacher/contest-manage/component/ContestSubPageShell.vue";

const route = useRoute();
const router = useRouter();
const contestId = computed(() => String(route.params.contestId || '') as IdType);
const loading = ref(false);
const list = ref<UserStatistic[]>([]);
const keyword = ref('');
const statusFilter = ref('');
const completion = (row: UserStatistic) => {const total = row.correctNum + row.wrongNum + row.absentNum; return total ? Math.round((row.correctNum + row.wrongNum) / total * 100) : 0;};
const filteredList = computed(() => {const query = keyword.value.trim().toLowerCase(); return list.value.filter(row => {const textMatch = !query || `${row.nikeName} ${row.userId}`.toLowerCase().includes(query); const statusMatch = !statusFilter.value || (statusFilter.value === 'submitted' && row.submitted) || (statusFilter.value === 'pending' && !row.submitted) || (statusFilter.value === 'late' && row.late); return textMatch && statusMatch;});});
const stats = computed(() => {const submitted = list.value.filter(row => row.submitted).length; return [{label: '参与用户', value: list.value.length, tone: 'green'}, {label: '已提交', value: submitted, tone: 'blue'}, {label: '提交率', value: `${list.value.length ? Math.round(submitted / list.value.length * 100) : 0}%`, tone: 'violet'}, {label: '迟交用户', value: list.value.filter(row => row.late).length, tone: 'amber'}];});
const getList = async () => {loading.value = true; try {list.value = await getUserStatistic(contestId.value);} finally {loading.value = false;}};
const toUserScore = (row: UserStatistic) => router.push({name: 'user-scores', params: {contestId: contestId.value, userId: row.userId}});
const handleReturn = async (row: UserStatistic) => {try {await ElMessageBox.confirm(`确认退回用户“${row.nikeName}”的提交吗？`, '退回提交', {type: 'warning', confirmButtonText: '确认退回', cancelButtonText: '取消'}); await returnUserSubmit(contestId.value, row.userId); await getList();} catch { /* 用户取消 */ }};
watch(contestId, () => void getList());
getList();
</script>

<style scoped>
.table-heading { display: flex; align-items: center; justify-content: space-between; gap: 15px; margin-bottom: 12px; }.table-heading strong, .table-heading span { display: block; }.table-heading strong { font-size: 15px; }.table-heading span { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }.filters { display: flex; gap: 8px; }.filters :deep(.el-input) { width: 190px; }.filters :deep(.el-select) { width: 110px; }.user-cell { display: flex; align-items: center; gap: 10px; }.user-avatar { display: grid; width: 32px; height: 32px; place-items: center; border-radius: 9px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 13px; font-weight: 700; }.user-cell > span:last-child { display: flex; min-width: 0; flex-direction: column; gap: 2px; }.user-cell strong { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }.user-cell small { color: var(--el-text-color-secondary); font-size: 11px; }.state-tag { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; }.state-tag i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }.state-tag--success { color: var(--el-color-success); }.state-tag--muted { color: var(--el-text-color-placeholder); }.state-tag--warning { color: var(--el-color-warning); }.muted { color: var(--el-text-color-placeholder); }.score { color: var(--el-color-primary); }.completion { display: flex; align-items: center; gap: 10px; color: var(--el-text-color-secondary); font-size: 11px; }.completion :deep(.el-progress) { width: 72px; }
@media (max-width: 760px) { .table-heading { align-items: flex-start; flex-direction: column; }.filters { width: 100%; }.filters :deep(.el-input), .filters :deep(.el-select) { width: 100%; }.filters :deep(.el-input) { flex: 1; } }
</style>
