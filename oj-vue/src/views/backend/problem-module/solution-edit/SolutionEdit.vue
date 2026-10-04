<template>
  <ProblemModuleShell title="题解管理" kicker="PROBLEM / SOLUTIONS" description="审核与维护题解内容，连接题目、作者和公开展示。" :icon="EditPen" tone="violet">
    <el-empty v-if="!permissions.list" description="当前账号没有题解管理查看权限" />
    <template v-else>
    <div class="module-toolbar">
      <div><span class="eyebrow">KNOWLEDGE BASE</span><strong>题解列表</strong><small>共 {{ total }} 条题解</small></div>
      <div class="toolbar-actions"><el-button :icon="Refresh" :loading="loading" @click="getList">刷新题解</el-button><el-button v-if="permissions.add" type="primary" :icon="Plus" @click="handleAdd">新增题解</el-button></div>
    </div>

    <div class="filter-panel">
      <div class="filter-field"><label>发布者 ID</label><el-input v-model="queryParams.userId" clearable placeholder="按用户筛选" @keyup.enter="handleQuery" /></div>
      <div class="filter-field"><label>题目 ID</label><el-input v-model="queryParams.problemId" clearable placeholder="按题目筛选" @keyup.enter="handleQuery" /></div>
      <div class="filter-actions"><el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button><el-button :icon="Refresh" @click="resetQuery">重置</el-button></div>
    </div>

    <p v-if="error" class="load-error" role="status">{{ error }}</p>
    <el-table v-loading="loading" class="solution-table" :data="tableList" row-key="solutionId">
      <el-table-column label="题解" min-width="330">
        <template #default="{ row }"><div class="solution-title-cell"><span class="solution-icon"><el-icon><EditPen /></el-icon></span><div><strong :title="row.title">{{ row.title }}</strong><span>#{{ row.solutionId }} · {{ row.problemTitle || `题目 #${row.problemId}` }}</span></div></div></template>
      </el-table-column>
      <el-table-column label="作者" min-width="150"><template #default="{ row }"><div class="author-cell"><strong>{{ row.nikeName || '匿名用户' }}</strong><span>ID {{ row.userId }}</span></div></template></el-table-column>
      <el-table-column label="展示状态" width="170"><template #default="{ row }"><div class="status-stack"><span :class="['status-pill', row.effectiveVisibility === 'PUBLIC' ? 'is-public' : 'is-private']">{{ solutionVisibilityLabel(row) }}</span><span v-if="row.topUp" class="pin-label">已置顶</span></div></template></el-table-column>
      <el-table-column v-if="permissions.edit" label="置顶" width="100"><template #default="{ row }"><el-switch :model-value="row.topUp" :disabled="!!pinPending[row.solutionId]" size="small" @change="handleTopUpChange(row)" /></template></el-table-column>
      <el-table-column label="更新时间" min-width="190" prop="updateTime" show-overflow-tooltip />
      <el-table-column label="操作" width="270" fixed="right" align="right"><template #default="{ row }"><div class="row-actions"><el-button link type="primary" :icon="View" @click="handleView(row)">查看</el-button><el-button v-if="permissions.edit" link type="primary" :icon="Edit" @click="handleUpdate(row)">编辑</el-button><el-button v-if="permissions.comments" link type="primary" @click="handleComments(row)">评论</el-button><el-button v-if="permissions.remove" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button></div></template></el-table-column>
      <template #empty><el-empty description="还没有题解" /></template>
    </el-table>
    <Pagination v-show="total > 0" v-model:page="queryParams.currentPage" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </template>
    <SolutionManagementDrawer v-model="detailOpen" :solution-id="detailId" @edit="handleUpdate" @changed="getList" />
    <CommentManagementDrawer v-model="commentsOpen" :solution-id="commentSolutionId" @changed="getList" />
    <SolutionModerationDialog v-model="deleteOpen" :target-id="deleteId" :target-title="deleteTitle" mode="SOLUTION_DELETE" @success="getList" />
  </ProblemModuleShell>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { Delete, Edit, EditPen, Plus, Refresh, Search, View } from '@element-plus/icons-vue'
import { ElMessageBox, ElNotification } from 'element-plus'
import { useRouter } from 'vue-router'
import { listSolutionAdmin, lowDown, type QuerySolution, type Solution, topUp } from '@/api/solution'
import Pagination from '@/components/pageination/Pagination.vue'
import ProblemModuleShell from '@/views/backend/problem-module/component/ProblemModuleShell.vue'
import {solutionVisibilityLabel} from '@/utils/solutionVisibility'
import SolutionManagementDrawer from '@/components/SolutionModeration/SolutionManagementDrawer.vue'
import SolutionModerationDialog from '@/components/SolutionModeration/SolutionModerationDialog.vue'
import CommentManagementDrawer from '@/components/CommentManagement/CommentManagementDrawer.vue'
import {moderationError, moderationPermissions} from '@/components/SolutionModeration/moderationForm'
import {useUserStore} from '@/stores/useUserStore'

const router = useRouter()
const queryParams = reactive<QuerySolution>({ asc: true, currentPage: 1, pageSize: 20, problemId: undefined, userId: undefined })
const tableList = ref<Solution[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')
const store = useUserStore()
const permissions = computed(() => moderationPermissions(store.getAuths()))
const detailOpen = ref(false)
const detailId = ref('')
const commentsOpen = ref(false)
const commentSolutionId = ref('')
const deleteOpen = ref(false)
const deleteId = ref('')
const deleteTitle = ref('')
const pinPending = reactive<Record<string, boolean>>({})
let generation = 0
let controller: AbortController | undefined
let disposed = false
const handleQuery = () => { queryParams.currentPage = 1; getList() }
const resetQuery = () => { queryParams.problemId = undefined; queryParams.userId = undefined; queryParams.sortColumn = undefined; handleQuery() }
const getList = async () => {
  const version = ++generation
  controller?.abort()
  if (!permissions.value.list || disposed) { loading.value = false; return }
  const current = new AbortController()
  controller = current
  loading.value = true; error.value = ''
  try {
    const data = await listSolutionAdmin({...queryParams}, current.signal)
    if (disposed || version !== generation) return
    total.value = data.totalRecords; tableList.value = data.data
    if (queryParams.currentPage > 1 && !data.data.length) {
      queryParams.currentPage = Math.max(1, Math.ceil(data.totalRecords / queryParams.pageSize)); void getList()
    }
  } catch (failure) {
    if (!disposed && version === generation && !current.signal.aborted) error.value = moderationError(failure)
  } finally { if (!disposed && version === generation) loading.value = false }
}
const handleTopUpChange = async (row: Solution) => {
  if (!permissions.value.edit || pinPending[row.solutionId]) return
  pinPending[row.solutionId] = true
  const text = row.topUp ? '取消置顶' : '置顶'
  try {
    await ElMessageBox.confirm(`确认要${text}此题解吗？`, '更新展示状态')
    if (disposed || !permissions.value.edit) return
    const data = await (row.topUp ? lowDown(row.solutionId) : topUp(row.solutionId))
    if (disposed) return
    if (!data) { error.value = '展示状态未更新，请刷新后重试'; return }
    row.topUp = !row.topUp; ElNotification.success(text + '成功')
  } catch (failure) {
    if (!disposed && failure !== 'cancel' && failure !== 'close') error.value = moderationError(failure)
  } finally { delete pinPending[row.solutionId] }
}
const handleDelete = (row: Solution) => {
  if (!permissions.value.remove) return
  deleteId.value = row.solutionId; deleteTitle.value = row.title; deleteOpen.value = true
}
const handleAdd = () => { if (permissions.value.add) router.push({name: 'solution_edit', query: {management: '1'}}) }
const handleUpdate = (row: Solution) => {
  if (!permissions.value.edit) return
  detailOpen.value = false
  router.push({name: 'solution_edit', query: {solutionId: row.solutionId, management: '1'}})
}
const handleView = (row: Solution) => { if (permissions.value.list) { detailId.value = row.solutionId; detailOpen.value = true } }
const handleComments = (row: Solution) => { if (permissions.value.comments) { commentSolutionId.value = row.solutionId; commentsOpen.value = true } }
watch(() => [permissions.value.list, store.user?.userId], () => {
  detailOpen.value = false; commentsOpen.value = false; deleteOpen.value = false
  tableList.value = []; total.value = 0; void getList()
}, {immediate: true})
onBeforeUnmount(() => { disposed = true; generation++; controller?.abort() })
</script>

<style lang="scss" scoped>
.module-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 18px; }.module-toolbar > div:first-child { display: flex; align-items: baseline; gap: 10px; }.eyebrow { color: #8b5cf6; font-size: 10px; font-weight: 800; letter-spacing: .14em; }.module-toolbar strong { font-size: 18px; }.module-toolbar small { color: var(--el-text-color-secondary); font-size: 12px; }.toolbar-actions { display: flex; gap: 9px; }.filter-panel { display: flex; align-items: flex-end; gap: 12px; margin-bottom: 18px; padding: 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-fill-color-light); }.filter-field { width: 220px; }.filter-field label { display: block; margin-bottom: 6px; color: var(--el-text-color-secondary); font-size: 12px; }.filter-actions { display: flex; gap: 8px; }.solution-table { border-radius: 14px; overflow: hidden; }.solution-title-cell { display: flex; align-items: center; gap: 12px; }.solution-icon { display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center; border-radius: 11px; background: rgb(139 92 246 / 12%); color: #8b5cf6; }.solution-title-cell div:last-child, .author-cell, .status-stack { display: flex; flex-direction: column; gap: 4px; }.solution-title-cell strong { max-width: 290px; overflow: hidden; color: var(--el-text-color-primary); text-overflow: ellipsis; white-space: nowrap; }.solution-title-cell span, .author-cell span { color: var(--el-text-color-secondary); font-size: 12px; }.author-cell strong { color: var(--el-text-color-primary); font-size: 13px; }.status-pill { width: fit-content; padding: 4px 9px; border-radius: 999px; font-size: 12px; font-weight: 700; }.status-pill.is-public { background: color-mix(in srgb, var(--el-color-success) 10%, var(--el-bg-color)); color: var(--el-color-success); }.status-pill.is-private { background: var(--el-fill-color-light); color: var(--el-text-color-secondary); }.pin-label { color: var(--el-color-warning); font-size: 11px; }.dialog-intro { margin-bottom: 20px; padding: 14px 16px; border-radius: 12px; background: var(--el-fill-color-light); }
@media (max-width: 720px) { .module-toolbar, .filter-panel { align-items: stretch; flex-direction: column; }.module-toolbar > div:first-child { flex-wrap: wrap; }.toolbar-actions, .filter-field, .filter-actions { width: 100%; }.filter-field { width: auto; } }
.row-actions { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: 8px; }.row-actions .el-button { margin: 0; }.load-error { color: var(--el-color-danger); }
</style>
