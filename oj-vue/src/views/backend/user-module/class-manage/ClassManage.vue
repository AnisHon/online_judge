<template>
  <ContestSubPageShell
    title="班级管理"
    kicker="USER / CLASSES"
    description="管理教学组织与班级成员入口，让班级资料和学生名单各自保持清晰。"
    :icon="DataBoard"
    tone="green"
    :stats="summaryStats"
  >
    <template #actions>
      <el-button :icon="Refresh" :loading="isLoading" @click="getList">刷新班级</el-button>
      <el-button v-has="'user:class:add'" type="primary" :icon="Plus" @click="handleAdd">新建班级</el-button>
    </template>

    <section class="class-panel">
      <div class="panel-heading">
        <div>
          <span class="panel-eyebrow">CLASS DIRECTORY</span>
          <h2>教学组织</h2>
          <p>班级负责组织学生，学生名单请从独立的成员管理页维护。</p>
        </div>
        <div class="panel-heading__meta">
          <span class="sync-dot" :class="{ 'is-loading': isLoading }"></span>
          <span>{{ isLoading ? '正在同步' : `共 ${total} 个结果` }}</span>
        </div>
      </div>

      <div class="filter-panel">
        <div class="filter-panel__bar">
          <div class="filter-title"><el-icon><Filter /></el-icon><strong>筛选班级</strong><span v-if="activeFilterCount">{{ activeFilterCount }} 项已启用</span></div>
          <el-button link type="primary" @click="showSearch = !showSearch">{{ showSearch ? '收起筛选' : '展开筛选' }}</el-button>
        </div>
        <el-form v-show="showSearch" :model="queryParams" class="class-filters" label-position="top" @submit.prevent="handleQuery">
          <el-form-item label="班级名称" prop="className"><el-input v-model="queryParams.className" clearable :prefix-icon="DataBoard" placeholder="搜索班级名称" @keyup.enter="handleQuery" /></el-form-item>
          <el-form-item label="班级 ID" prop="classId"><el-input v-model="queryParams.classId" clearable :prefix-icon="Key" placeholder="输入完整或部分 ID" @keyup.enter="handleQuery" /></el-form-item>
          <div class="filter-actions"><el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button><el-button :icon="Refresh" @click="resetQuery">重置</el-button></div>
        </el-form>
      </div>

      <div class="list-toolbar">
        <div class="list-toolbar__left">
          <span class="selection-status" :class="{ 'has-selection': selectedIds.length }"><el-icon><Select /></el-icon>{{ selectedIds.length ? `已选择 ${selectedIds.length} 个班级` : '未选择班级' }}</span>
          <el-button v-has="'user:class:remove'" type="danger" plain :disabled="!selectedIds.length || actionLoading" :icon="Delete" @click="handleDelete()">批量删除</el-button>
        </div>
        <RightToolBar v-model:showSearch="showSearch" :columns="columns" @queryTable="getList" />
      </div>

      <el-table v-loading="isLoading" class="class-table" :data="tableList" row-key="classId" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" align="center" />
        <el-table-column v-if="columns[0].visible" label="班级" min-width="310">
          <template #default="{ row }">
            <div class="class-cell">
              <span class="class-icon"><el-icon><DataBoard /></el-icon></span>
              <div class="class-cell__main">
                <strong :title="row.className">{{ row.className || '未命名班级' }}</strong>
                <span :title="String(row.classId)">ID {{ shortId(row.classId) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="columns[1].visible" label="创建时间" min-width="180" prop="createTime" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column v-if="columns[2].visible" label="内部备注" min-width="230" prop="remark" show-overflow-tooltip>
          <template #default="{ row }"><span :class="{ 'muted-text': !row.remark }">{{ row.remark || '暂无备注' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="270" fixed="right" align="right">
          <template #default="{ row }">
            <el-space :size="4">
              <el-button v-has="'user:class:edit'" link type="primary" :icon="EditPen" @click="handleUpdate(row)">编辑</el-button>
              <el-button v-has="'user:class:edit'" link type="success" :icon="UserFilled" @click="openStudents(row)">学生管理</el-button>
              <el-button v-has="'user:class:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
            </el-space>
          </template>
        </el-table-column>
        <template #empty><el-empty description="没有匹配的班级" :image-size="84" /></template>
      </el-table>

      <Pagination v-show="total > 0" v-model:page="queryParams.currentPage" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </section>

    <el-dialog v-model="open" class="class-dialog" :title="dialogState === 'add' ? '新建班级' : '编辑班级'" width="min(620px, 92vw)" append-to-body destroy-on-close>
      <div class="dialog-intro">
        <span class="dialog-intro__icon"><el-icon><DataBoard /></el-icon></span>
        <div><strong>{{ dialogState === 'add' ? '建立教学组织' : '更新班级资料' }}</strong><p>班级名称用于平台展示，备注仅供管理员内部识别。</p></div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="editor-form" label-position="top">
        <section class="form-section">
          <div class="section-title"><span>01</span><div><strong>班级信息</strong><small>名称是班级在后台和成员管理中的主要识别方式</small></div></div>
          <el-form-item v-if="dialogState === 'edit'" label="班级 ID"><el-input :model-value="String(form.classId || '')" disabled :prefix-icon="Key" /></el-form-item>
          <el-form-item label="班级名称" prop="className"><el-input v-model="form.className" maxlength="100" show-word-limit :prefix-icon="DataBoard" placeholder="例如：2026 级计算机科学与技术 1 班" /></el-form-item>
        </section>
        <section class="form-section">
          <div class="section-title"><span>02</span><div><strong>内部备注</strong><small>不会展示给普通学生，用于记录来源或管理说明</small></div></div>
          <el-form-item label="备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="4" maxlength="450" show-word-limit placeholder="例如：春季学期、辅导员或班级来源" /></el-form-item>
        </section>
      </el-form>
      <template #footer><el-button @click="cancel">取消</el-button><el-button type="primary" :loading="isUpdateLoading || isAddLoading" @click="submitForm">{{ dialogState === 'add' ? '创建班级' : '保存修改' }}</el-button></template>
    </el-dialog>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { DataBoard, Delete, EditPen, Filter, Key, Plus, Refresh, Search, Select, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import { useRouter } from 'vue-router'
import ContestSubPageShell from '@/views/backend/teacher/contest-manage/component/ContestSubPageShell.vue'
import RightToolBar from '@/components/right-toolbar/RightToolBar.vue'
import Pagination from '@/components/pageination/Pagination.vue'
import { type ClassForm, type ClassView, debouncedAddClass, debouncedGetClass, debouncedUpdateClass, type QueryClass, removeClass } from '@/api/class'
import type { IdType } from '@/api/common'
import { useColumn } from '@/hooks/useColumn'

type DialogState = 'add' | 'edit'
const router = useRouter()
const queryParams = reactive<QueryClass>({ asc: true, currentPage: 1, pageSize: 20, classId: undefined, className: undefined })
const form = reactive<ClassForm>({ classId: undefined, className: '', remark: '' })
const formRef = ref<FormInstance>()
const open = ref(false)
const dialogState = ref<DialogState>('add')
const showSearch = ref(true)
const actionLoading = ref(false)
const tableList = reactive<ClassView[]>([])
const selectedIds = ref<IdType[]>([])
const total = ref(0)
const { columns } = useColumn(['班级', '创建时间', '内部备注'])

const rules = {
  className: [{ required: true, message: '班级名称不能为空', trigger: 'blur' }, { max: 100, message: '班级名称不能超过 100 个字符', trigger: 'blur' }],
  remark: [{ max: 450, message: '备注不能超过 450 个字符', trigger: 'blur' }],
}
const activeFilterCount = computed(() => [queryParams.classId, queryParams.className].filter((value) => value !== undefined && value !== '').length)
const summaryStats = computed(() => [
  { label: '查询结果', value: total.value, tone: 'blue' },
  { label: '当前页班级', value: tableList.length, tone: 'green' },
  { label: '当前页有备注', value: tableList.filter((item) => Boolean(item.remark)).length, tone: 'amber' },
  { label: '已选择', value: selectedIds.value.length, tone: 'violet' },
])
const shortId = (value: IdType) => { const text = String(value); return text.length > 18 ? `${text.slice(0, 8)}…${text.slice(-6)}` : text }
const formatDate = (value: Date | string | undefined) => value ? new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }) : '—'

const { loading, isLoading, get: getClass } = debouncedGetClass(queryParams, (data) => {
  tableList.length = 0
  tableList.push(...data.data)
  total.value = data.totalRecords
  selectedIds.value = []
})
const getList = () => { loading(); getClass() }
const handleSelectionChange = (selection: ClassView[]) => { selectedIds.value = selection.map((item) => item.classId) }
const resetQuery = () => { queryParams.currentPage = 1; queryParams.classId = undefined; queryParams.className = undefined; queryParams.sortColumn = undefined; getList() }
const handleQuery = () => { queryParams.currentPage = 1; getList() }

const resetForm = () => { form.classId = undefined; form.className = ''; form.remark = ''; formRef.value?.clearValidate() }
const handleAdd = () => { resetForm(); dialogState.value = 'add'; open.value = true }
const handleUpdate = (row?: ClassView) => {
  const target = row || tableList.find((item) => String(item.classId) === String(selectedIds.value[0]))
  if (!target) { ElMessage.warning('请先选择一个班级'); return }
  resetForm()
  Object.assign(form, target)
  dialogState.value = 'edit'
  open.value = true
}
const submitForm = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (dialogState.value === 'add') { addLoading(); add() } else { updateLoading(); update() }
}
const finishDialog = () => { open.value = false; resetForm(); getList() }
const { loading: updateLoading, isLoading: isUpdateLoading, update } = debouncedUpdateClass(form, finishDialog)
const { loading: addLoading, isLoading: isAddLoading, add } = debouncedAddClass(form, finishDialog)

const handleDelete = async (row?: ClassView) => {
  const ids = row ? [row.classId] : selectedIds.value
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(row ? `确定删除班级“${row.className}”吗？` : `确定删除选中的 ${ids.length} 个班级吗？`, '删除班级', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' })
    actionLoading.value = true
    await removeClass(row ? ids[0] : ids)
    ElMessage.success('班级已删除')
    getList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败，请稍后重试')
  } finally {
    actionLoading.value = false
  }
}
const openStudents = (row: ClassView) => { router.push({ name: 'student-manage', params: { classId: row.classId } }) }
const cancel = () => { open.value = false; resetForm() }

getList()
</script>

<style lang="scss" scoped>
.class-panel { min-width: 0; padding: 22px 24px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 16px 40px rgb(15 23 42 / 4%); }
.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 20px; }.panel-eyebrow { color: var(--el-color-primary); font-size: 11px; font-weight: 800; letter-spacing: .14em; }.panel-heading h2 { margin: 7px 0 5px; font-size: 21px; letter-spacing: -.03em; }.panel-heading p { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }.panel-heading__meta { display: inline-flex; align-items: center; gap: 7px; padding-top: 5px; color: var(--el-text-color-secondary); font-size: 12px; white-space: nowrap; }.sync-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--el-color-success); }.sync-dot.is-loading { background: var(--el-color-warning); animation: pulse 1.1s ease-in-out infinite; }
.filter-panel { margin-bottom: 18px; padding: 14px 16px 4px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-bg-color-page); }.filter-panel__bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 11px; }.filter-title { display: flex; align-items: center; gap: 7px; color: var(--el-text-color-primary); font-size: 13px; }.filter-title .el-icon { color: var(--el-color-primary); }.filter-title span { color: var(--el-text-color-secondary); font-size: 11px; font-weight: 400; }.class-filters { display: grid; grid-template-columns: minmax(220px, 1fr) minmax(220px, 1fr) auto; align-items: end; gap: 0 14px; }.class-filters :deep(.el-form-item) { margin-bottom: 10px; }.class-filters :deep(.el-form-item__label) { height: auto; margin-bottom: 5px; color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.2; }.filter-actions { display: flex; align-items: flex-end; gap: 7px; height: 68px; padding-bottom: 10px; }
.list-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 15px; min-height: 36px; margin-bottom: 10px; }.list-toolbar__left { display: flex; align-items: center; gap: 12px; }.selection-status { display: inline-flex; align-items: center; gap: 6px; color: var(--el-text-color-secondary); font-size: 12px; }.selection-status .el-icon { color: var(--el-text-color-placeholder); }.selection-status.has-selection { color: var(--el-color-primary); font-weight: 650; }.selection-status.has-selection .el-icon { color: var(--el-color-primary); }
.class-table { overflow: hidden; border-radius: 14px; }.class-table :deep(.el-table__header th.el-table__cell) { color: var(--el-text-color-secondary); font-size: 12px; font-weight: 700; background: var(--el-fill-color-light); }.class-table :deep(.el-table__row td.el-table__cell) { height: 72px; }.class-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }.class-icon { display: grid; width: 38px; height: 38px; flex: 0 0 auto; place-items: center; border: 1px solid color-mix(in srgb, var(--el-color-success) 18%, transparent); border-radius: 12px; background: color-mix(in srgb, var(--el-color-success) 11%, var(--el-bg-color)); color: var(--el-color-success); font-size: 18px; }.class-cell__main { display: flex; min-width: 0; flex-direction: column; gap: 5px; }.class-cell__main strong { max-width: 300px; overflow: hidden; font-size: 13px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }.class-cell__main span { overflow: hidden; color: var(--el-text-color-secondary); font: 11px/1.2 var(--code-font-family, monospace); text-overflow: ellipsis; white-space: nowrap; }.muted-text { color: var(--el-text-color-placeholder); }
.dialog-intro { display: flex; align-items: center; gap: 12px; margin-bottom: 19px; padding: 14px 16px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-fill-color-light); }.dialog-intro__icon { display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center; border-radius: 11px; background: color-mix(in srgb, var(--el-color-success) 13%, var(--el-bg-color)); color: var(--el-color-success); font-size: 18px; }.dialog-intro strong, .dialog-intro p { display: block; }.dialog-intro p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }.editor-form { padding: 0 2px; }.form-section { padding: 18px 0 4px; border-top: 1px solid var(--el-border-color-lighter); }.form-section:first-child { padding-top: 0; border-top: 0; }.section-title { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 15px; }.section-title > span { color: var(--el-color-success); font: 700 11px/1.4 var(--code-font-family, monospace); letter-spacing: .08em; }.section-title strong, .section-title small { display: block; }.section-title strong { font-size: 14px; }.section-title small { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }.editor-form :deep(.el-form-item) { margin-bottom: 16px; }.editor-form :deep(.el-form-item__label) { height: auto; margin-bottom: 6px; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.2; }
@keyframes pulse { 50% { opacity: .35; } }
@media (max-width: 760px) { .class-panel { padding: 18px 14px 8px; }.panel-heading { align-items: flex-start; flex-direction: column; gap: 10px; }.class-filters { grid-template-columns: 1fr; }.filter-actions { height: auto; padding: 0 0 10px; }.filter-actions .el-button { flex: 1; }.list-toolbar { align-items: flex-start; flex-direction: column; }.list-toolbar__left { width: 100%; justify-content: space-between; }.class-table :deep(.el-table__fixed-right) { display: none; }.class-cell__main strong { max-width: 220px; } }
</style>
