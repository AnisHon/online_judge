<template>
  <ProblemModuleShell title="题解管理" kicker="PROBLEM / SOLUTIONS" description="审核与维护题解内容，连接题目、作者和公开展示。" :icon="EditPen" tone="violet">
    <div class="module-toolbar">
      <div><span class="eyebrow">KNOWLEDGE BASE</span><strong>题解列表</strong><small>共 {{ total }} 条题解</small></div>
      <div class="toolbar-actions"><el-button :icon="Refresh" :loading="loading" @click="getList">刷新题解</el-button><el-button v-has="'problem:solution:add'" type="primary" :icon="Plus" @click="handleAdd">新增题解</el-button><el-button v-has="'problem:solution:remove'" type="danger" plain :disabled="!ids.length" :icon="Delete" @click="handleDelete()">删除</el-button></div>
    </div>

    <div class="filter-panel">
      <div class="filter-field"><label>发布者 ID</label><el-input v-model="queryParams.userId" clearable placeholder="按用户筛选" @keyup.enter="handleQuery" /></div>
      <div class="filter-field"><label>题目 ID</label><el-input v-model="queryParams.problemId" clearable placeholder="按题目筛选" @keyup.enter="handleQuery" /></div>
      <div class="filter-actions"><el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button><el-button :icon="Refresh" @click="resetQuery">重置</el-button></div>
    </div>

    <el-table v-loading="loading" class="solution-table" :data="tableList" row-key="solutionId" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="54" align="center" />
      <el-table-column label="题解" min-width="330">
        <template #default="{ row }"><div class="solution-title-cell"><span class="solution-icon"><el-icon><EditPen /></el-icon></span><div><strong :title="row.title">{{ row.title }}</strong><span>#{{ row.solutionId }} · {{ row.problemTitle || `题目 #${row.problemId}` }}</span></div></div></template>
      </el-table-column>
      <el-table-column label="作者" min-width="150"><template #default="{ row }"><div class="author-cell"><strong>{{ row.nikeName || '匿名用户' }}</strong><span>ID {{ row.userId }}</span></div></template></el-table-column>
      <el-table-column label="展示状态" width="170"><template #default="{ row }"><div class="status-stack"><span :class="['status-pill', row.private_ ? 'is-private' : 'is-public']">{{ row.private_ ? '私有' : '公开' }}</span><span v-if="row.topUp" class="pin-label">已置顶</span></div></template></el-table-column>
      <el-table-column label="置顶" width="100"><template #default="{ row }"><el-switch v-has="'problem:solution:edit'" v-model="row.topUp" size="small" @change="handlePrivateChange(row)" /></template></el-table-column>
      <el-table-column label="更新时间" min-width="190" prop="updateTime" show-overflow-tooltip />
      <el-table-column label="操作" width="230" fixed="right" align="right"><template #default="{ row }"><el-space><el-button v-has="'problem:solution:list'" link type="primary" :icon="View" @click="handleCommand('openSolution', row)">查看</el-button><el-button v-has="'problem:solution:edit'" link type="primary" :icon="Edit" @click="handleUpdate(row)">编辑</el-button><el-button v-has="'problem:solution:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button></el-space></template></el-table-column>
      <template #empty><el-empty description="还没有题解" /></template>
    </el-table>
    <Pagination v-show="total > 0" v-model:page="queryParams.currentPage" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
  </ProblemModuleShell>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Delete, Edit, EditPen, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import { ElMessageBox, ElNotification } from 'element-plus'
import { useRouter } from 'vue-router'
import { deleteSolutionAdmin, listSolutionAdmin, lowDown, type QuerySolution, type Solution, topUp } from '@/api/solution'
import type { IdType } from '@/api/common'
import Pagination from '@/components/pageination/Pagination.vue'
import ProblemModuleShell from '@/views/backend/problem-module/component/ProblemModuleShell.vue'

const router = useRouter()
const queryParams = reactive<QuerySolution>({ asc: true, currentPage: 1, pageSize: 20, problemId: undefined, userId: undefined })
const tableList = reactive<Solution[]>([])
const total = ref(0)
const ids = ref<IdType[]>([])
const loading = ref(false)
const handleQuery = () => { queryParams.currentPage = 1; getList() }
const resetQuery = () => { queryParams.problemId = undefined; queryParams.userId = undefined; queryParams.sortColumn = undefined; handleQuery() }
const getList = async () => { loading.value = true; try { const data = await listSolutionAdmin(queryParams); tableList.length = 0; total.value = data.totalRecords; tableList.push(...data.data) } finally { loading.value = false } }
const handleSelectionChange = (selection: Solution[]) => { ids.value = selection.map((item) => item.solutionId) }
const handlePrivateChange = (row: Solution) => { const text = row.topUp ? '置顶' : '取消置顶'; ElMessageBox.confirm(`确认要${text}此题解吗？`, '更新展示状态', { confirmButtonText: '确认', cancelButtonText: '取消' }).then(() => row.topUp ? topUp(row.solutionId) : lowDown(row.solutionId)).then((data) => { if (data) ElNotification.success(text + '成功'); else { row.topUp = !row.topUp; ElNotification.warning(text + '失败') } }).catch(() => { row.topUp = !row.topUp }) }
const handleDelete = (row?: Solution) => { const target = row ? [row.solutionId] : ids.value; if (!target.length) return; ElMessageBox.confirm(`确定删除选中的 ${target.length} 条题解吗？`, '删除题解', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }).then(() => deleteSolutionAdmin(target)).then(getList).catch(() => {}) }
const handleAdd = () => router.push({ name: 'solution_edit' })
const handleUpdate = (data?: Solution) => { const id = data?.solutionId || ids.value[0]; if (id) router.push({ name: 'solution_edit', query: { solutionId: id } }) }
const handleCommand = (command: string, row: Solution) => { if (command === 'openSolution') router.push({ name: 'solution', params: { id: row.solutionId } }) }

getList()
</script>

<style lang="scss" scoped>
.module-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 18px; }.module-toolbar > div:first-child { display: flex; align-items: baseline; gap: 10px; }.eyebrow { color: #8b5cf6; font-size: 10px; font-weight: 800; letter-spacing: .14em; }.module-toolbar strong { font-size: 18px; }.module-toolbar small { color: var(--el-text-color-secondary); font-size: 12px; }.toolbar-actions { display: flex; gap: 9px; }.filter-panel { display: flex; align-items: flex-end; gap: 12px; margin-bottom: 18px; padding: 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-fill-color-light); }.filter-field { width: 220px; }.filter-field label { display: block; margin-bottom: 6px; color: var(--el-text-color-secondary); font-size: 12px; }.filter-actions { display: flex; gap: 8px; }.solution-table { border-radius: 14px; overflow: hidden; }.solution-title-cell { display: flex; align-items: center; gap: 12px; }.solution-icon { display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center; border-radius: 11px; background: rgb(139 92 246 / 12%); color: #8b5cf6; }.solution-title-cell div:last-child, .author-cell, .status-stack { display: flex; flex-direction: column; gap: 4px; }.solution-title-cell strong { max-width: 290px; overflow: hidden; color: var(--el-text-color-primary); text-overflow: ellipsis; white-space: nowrap; }.solution-title-cell span, .author-cell span { color: var(--el-text-color-secondary); font-size: 12px; }.author-cell strong { color: var(--el-text-color-primary); font-size: 13px; }.status-pill { width: fit-content; padding: 4px 9px; border-radius: 999px; font-size: 12px; font-weight: 700; }.status-pill.is-public { background: color-mix(in srgb, var(--el-color-success) 10%, var(--el-bg-color)); color: var(--el-color-success); }.status-pill.is-private { background: var(--el-fill-color-light); color: var(--el-text-color-secondary); }.pin-label { color: var(--el-color-warning); font-size: 11px; }.dialog-intro { margin-bottom: 20px; padding: 14px 16px; border-radius: 12px; background: var(--el-fill-color-light); }
@media (max-width: 720px) { .module-toolbar, .filter-panel { align-items: stretch; flex-direction: column; }.module-toolbar > div:first-child { flex-wrap: wrap; }.toolbar-actions, .filter-field, .filter-actions { width: 100%; }.filter-field { width: auto; } }
</style>
