<template>
  <ProblemModuleShell title="题单管理" kicker="PROBLEM / COLLECTIONS" description="创建可复用的题单，编排题目顺序并快速进入题目管理。" :icon="List" tone="green">
    <div class="module-toolbar">
      <div><span class="eyebrow">CURATED SETS</span><strong>题单列表</strong><small>共 {{ total }} 个题单</small></div>
      <div class="toolbar-actions"><el-button :icon="Refresh" :loading="isLoading" @click="getList">刷新题单</el-button><el-button v-has="'problem:list:add'" type="primary" :icon="Plus" @click="handleAdd">新建题单</el-button><el-button v-has="'problem:list:remove'" type="danger" plain :disabled="!ids.length" :icon="Delete" @click="handleDelete()">删除</el-button></div>
    </div>

    <div class="filter-panel">
      <div class="filter-field"><label>题单名称</label><el-input v-model="queryParams.listName" clearable placeholder="搜索题单名称" :prefix-icon="Search" @keyup.enter="handleQuery" /></div>
      <div class="filter-field"><label>题单 ID</label><el-input v-model="queryParams.listId" clearable placeholder="输入题单 ID" @keyup.enter="handleQuery" /></div>
      <div class="filter-actions"><el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button><el-button :icon="Refresh" @click="resetQuery">重置</el-button></div>
    </div>

    <el-table v-loading="isLoading" class="list-table" :data="tableList" row-key="listId" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="54" align="center" />
      <el-table-column label="题单" min-width="340">
        <template #default="{ row }"><div class="list-title-cell"><span class="list-icon"><el-icon><List /></el-icon></span><div><strong>{{ row.listName }}</strong><span>#{{ row.listId }}</span></div></div></template>
      </el-table-column>
      <el-table-column label="描述" min-width="360" prop="description" show-overflow-tooltip>
        <template #default="{ row }"><span class="description">{{ row.description || '暂无描述' }}</span></template>
      </el-table-column>
      <el-table-column label="创建时间" min-width="190" prop="createTime" show-overflow-tooltip />
      <el-table-column label="操作" width="230" fixed="right" align="right">
        <template #default="{ row }"><el-space><el-button v-has="'problem:list:edit'" link type="primary" :icon="Edit" @click="handleUpdate(row)">编辑</el-button><el-button v-has-any="['problem:list:add-problem', 'problem:list:del-problem', 'problem:problem:list']" link type="success" :icon="Management" @click="handleCommand('handleProblem', row)">管理题目</el-button><el-button v-has="'problem:list:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button></el-space></template>
      </el-table-column>
      <template #empty><el-empty description="还没有题单" /></template>
    </el-table>
    <Pagination v-show="total > 0" v-model:page="queryParams.currentPage" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />

    <el-dialog v-model="open" :title="title === '添加' ? '新建题单' : '编辑题单'" width="min(620px, 92vw)" append-to-body>
      <div class="dialog-intro"><span class="eyebrow">COLLECTION SETTINGS</span><p>题单可以被竞赛、作业和前台题库重复使用。</p></div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="题单名称" prop="listName"><el-input v-model="form.listName" maxlength="80" show-word-limit placeholder="请输入题单名称" /></el-form-item><el-form-item label="题单描述" prop="description"><el-input v-model="form.description" type="textarea" :rows="5" maxlength="450" show-word-limit placeholder="描述题单用途或适用范围" /></el-form-item></el-form>
      <template #footer><el-button @click="cancel">取消</el-button><el-button type="primary" :loading="isUpdateLoading || isAddLoading" @click="submitForm">保存题单</el-button></template>
    </el-dialog>
  </ProblemModuleShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { Delete, Edit, List, Management, Plus, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { addList, getList as fetchLists, updateList, type ListForm, type ListView, type QueryList, removeList } from '@/api/list'
import Pagination from '@/components/pageination/Pagination.vue'
import { useRouter } from 'vue-router'
import type { IdType } from '@/api/common'
import ProblemModuleShell from '@/views/backend/problem-module/component/ProblemModuleShell.vue'

const router = useRouter()
const queryParams = reactive<QueryList>({ asc: true, currentPage: 1, pageSize: 20, listId: undefined, listName: undefined })
const form = reactive<ListForm>({ listId: undefined, listName: '', description: '' })
const rules = { listName: [{ required: true, message: '请输入题单名称', trigger: 'blur' }] }
const formRef = ref<FormInstance>()
const open = ref(false)
const tableList = reactive<ListView[]>([])
const total = ref(0)
const ids = ref<IdType[]>([])
const single = ref(true)
const multiple = ref(true)
const isLoading = ref(false)
const isSubmitting = ref(false)
const isUpdateLoading = computed(() => isSubmitting.value && dialogState.value === 2)
const isAddLoading = computed(() => isSubmitting.value && dialogState.value === 1)
const dialogState = ref(1)
const title = computed(() => dialogState.value === 1 ? '添加' : '修改')

const resetForm = () => Object.assign(form, { listId: undefined, listName: '', description: '' })
const resetQuery = () => { queryParams.listName = undefined; queryParams.listId = undefined; queryParams.sortColumn = undefined; getList() }
const getList = async () => { isLoading.value = true; try { const data = await fetchLists({...queryParams}); tableList.splice(0, tableList.length, ...data.data); total.value = data.totalRecords; ids.value = [] } finally { isLoading.value = false } }
const handleQuery = () => { queryParams.currentPage = 1; void getList() }
const handleSelectionChange = (selection: ListView[]) => { ids.value = selection.map((item) => item.listId); single.value = selection.length !== 1; multiple.value = !selection.length }
const handleAdd = () => { resetForm(); dialogState.value = 1; open.value = true }
const handleUpdate = (data?: ListView) => { const item = data || tableList.find((x) => x.listId === ids.value[0]); if (!item) return; resetForm(); Object.assign(form, item); dialogState.value = 2; open.value = true }
const handleDelete = (row?: ListView) => { const target = row ? [row.listId] : ids.value; if (!target.length) return; ElMessageBox.confirm(`确定删除选中的 ${target.length} 个题单吗？`, '删除题单', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }).then(() => removeList(target).then(getList)).catch(() => {}) }
const finishDialog = () => { open.value = false; resetForm(); getList() }
const submitForm = async () => {
  if (!(await formRef.value?.validate().catch(() => false))) return
  isSubmitting.value = true
  const payload = {...form}
  try {
    if (dialogState.value === 1) await addList(payload)
    else await updateList(payload)
    ElMessage.success(dialogState.value === 1 ? '题单已创建' : '题单已更新')
    finishDialog()
  } finally {
    isSubmitting.value = false
  }
}
const cancel = () => { open.value = false; resetForm(); formRef.value?.clearValidate() }
const handleCommand = (command: string, row: ListView) => { if (command === 'handleProblem') router.push({ name: 'list-problem', params: { id: row.listId } }) }

getList()
</script>

<style lang="scss" scoped>
.module-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 18px; }.module-toolbar > div:first-child { display: flex; align-items: baseline; gap: 10px; }.eyebrow { color: #059669; font-size: 10px; font-weight: 800; letter-spacing: .14em; }.module-toolbar strong { font-size: 18px; }.module-toolbar small { color: var(--el-text-color-secondary); font-size: 12px; }.toolbar-actions { display: flex; gap: 9px; }.filter-panel { display: flex; align-items: flex-end; gap: 12px; margin-bottom: 18px; padding: 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-fill-color-light); }.filter-field { width: 240px; }.filter-field label { display: block; margin-bottom: 6px; color: var(--el-text-color-secondary); font-size: 12px; }.filter-actions { display: flex; gap: 8px; }.list-table { border-radius: 14px; overflow: hidden; }.list-title-cell { display: flex; align-items: center; gap: 12px; }.list-icon { display: grid; width: 36px; height: 36px; place-items: center; border-radius: 11px; background: rgb(5 150 105 / 11%); color: var(--el-color-success); }.list-title-cell div:last-child { display: flex; flex-direction: column; gap: 4px; }.list-title-cell strong { color: var(--el-text-color-primary); }.list-title-cell span, .description { color: var(--el-text-color-secondary); font-size: 12px; }.dialog-intro { margin-bottom: 20px; padding: 14px 16px; border-radius: 12px; background: var(--el-fill-color-light); }.dialog-intro p { margin: 7px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
@media (max-width: 720px) { .module-toolbar, .filter-panel { align-items: stretch; flex-direction: column; }.module-toolbar > div:first-child { flex-wrap: wrap; }.toolbar-actions, .filter-field, .filter-actions { width: 100%; }.filter-field { width: auto; } }
@media (max-width: 480px) { .module-content { padding: 14px 10px; }.toolbar-actions { flex-wrap: wrap; } }
</style>
