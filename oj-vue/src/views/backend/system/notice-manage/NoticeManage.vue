<template>
  <ContestSubPageShell
    title="公告管理"
    kicker="SYSTEM / ANNOUNCEMENTS"
    description="集中维护平台公告、重要通知与内容展示，让信息发布更清晰可控。"
    :icon="Bell"
    tone="amber"
    :stats="summaryStats"
  >
    <template #actions>
      <el-button :icon="Refresh" :loading="loading" @click="getList">刷新列表</el-button>
      <el-button v-has="'content:notice:add'" type="primary" :icon="Plus" @click="handleAdd">
        发布公告
      </el-button>
    </template>

    <section class="notice-panel">
      <div class="panel-heading">
        <div>
          <span class="panel-eyebrow">CONTENT WORKSPACE</span>
          <h2>公告列表</h2>
          <p>支持批量操作、重要标记与 Markdown 内容编辑。</p>
        </div>
        <div class="panel-tools">
          <el-input
            v-model="keyword"
            clearable
            class="keyword-input"
            :prefix-icon="Search"
            placeholder="搜索公告标题"
            @keyup.enter="search"
            @clear="search"
          />
          <el-select v-model="priorityFilter" class="priority-select" placeholder="公告级别" @change="search">
            <el-option label="全部公告" value="all" />
            <el-option label="重要公告" value="important" />
            <el-option label="普通公告" value="normal" />
          </el-select>
        </div>
      </div>

      <div v-if="selectedIds.length" class="selection-bar">
        <span>已选择 {{ selectedIds.length }} 条公告</span>
        <el-button v-has="'content:notice:remove'" text type="danger" @click="handleDelete()">
          批量删除
        </el-button>
      </div>

      <el-table
        v-loading="loading"
        ref="tableRef"
        class="notice-table"
        :data="tableList"
        row-key="noticeId"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="52" align="center" />
        <el-table-column label="公告" min-width="360">
          <template #default="{ row }">
            <div class="notice-title-cell">
              <span class="notice-mark" :class="{ 'is-important': row.topUp }">
                <el-icon><Bell /></el-icon>
              </span>
              <div>
                <strong>{{ row.title }}</strong>
                <span>#{{ row.noticeId }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="130">
          <template #default="{ row }">
            <span class="priority-pill" :class="row.topUp ? 'is-important' : 'is-normal'">
              {{ row.topUp ? '重要公告' : '普通公告' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="190" prop="createTime" show-overflow-tooltip />
        <el-table-column label="更新于" min-width="190" prop="updateTime" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right" align="right">
          <template #default="{ row }">
            <el-space>
              <el-button link type="primary" :icon="View" @click="viewNotice(row.noticeId)">
                查看
              </el-button>
              <el-button
                v-has="'content:notice:edit'"
                link
                type="primary"
                :icon="Edit"
                @click="handleUpdate(row)"
              >
                编辑
              </el-button>
              <el-button
                v-has="'content:notice:remove'"
                link
                type="danger"
                :icon="Delete"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </el-space>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无公告" />
        </template>
      </el-table>

      <Pagination
        v-show="total > 0"
        v-model:page="queryParams.currentPage"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="min(1080px, 94vw)" append-to-body>
      <el-form ref="formRef" v-loading="detailLoading" class="notice-form" label-position="top" :model="form" :rules="rules">
        <div class="form-intro">
          <span class="panel-eyebrow">{{ dialogState === 'add' ? 'NEW ANNOUNCEMENT' : 'EDIT ANNOUNCEMENT' }}</span>
          <p>标题负责让用户快速理解通知内容，正文支持 Markdown。</p>
        </div>
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" maxlength="120" show-word-limit placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="公告级别">
          <el-switch v-model="form.topUp" inline-prompt active-text="重要" inactive-text="普通" />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <MarkDownEditor v-model="form.content" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting || detailLoading" :disabled="detailLoading" @click="submitForm">保存公告</el-button>
      </template>
    </el-dialog>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  Bell,
  Delete,
  Edit,
  Plus,
  Refresh,
  Search,
  View,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type TableInstance } from 'element-plus'
import ContestSubPageShell from '../../teacher/contest-manage/component/ContestSubPageShell.vue'
import Pagination from '@/components/pageination/Pagination.vue'
import MarkDownEditor from '@/components/MarkDownEditor/MarkDownEditor.vue'
import type { IdType } from '@/api/common'
import { addNotice, getNotice, listNotice, type Notice, type NoticeDto, type NoticeQuery, removeNotice, updateNotice } from '@/api/notice'

type PriorityFilter = 'all' | 'important' | 'normal'
type DialogState = 'add' | 'edit'

const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const dialogState = ref<DialogState>('add')
const formRef = ref<FormInstance>()
const keyword = ref('')
const priorityFilter = ref<PriorityFilter>('all')
const selectedIds = ref<IdType[]>([])
const tableList = ref<Notice[]>([])
const total = ref(0)
const tableRef = ref<TableInstance>()
const detailLoading = ref(false)
const queryParams = reactive<NoticeQuery>({ currentPage: 1, pageSize: 20, keyword: undefined, topUp: undefined })
const form = reactive<NoticeDto>({ noticeId: '', title: '', content: '', topUp: false })

const rules: FormRules<NoticeDto> = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'change' }],
}

const dialogTitle = computed(() => (dialogState.value === 'add' ? '发布公告' : '编辑公告'))
const summaryStats = computed(() => [
  { label: '公告总数', value: total.value, tone: 'blue' },
  { label: '当前重要', value: tableList.value.filter((item) => item.topUp).length, tone: 'amber' },
  { label: '普通公告', value: tableList.value.filter((item) => !item.topUp).length, tone: 'green' },
  { label: '当前页', value: tableList.value.length, tone: 'violet' },
])

const resetForm = () => {
  Object.assign(form, { noticeId: '', title: '', content: '', topUp: false })
  formRef.value?.clearValidate()
}

let listRequestId = 0
let detailRequestId = 0
const getList = async () => {
  queryParams.keyword = keyword.value.trim() || undefined
  queryParams.topUp = priorityFilter.value === 'all' ? undefined : priorityFilter.value === 'important'
  const requestId = ++listRequestId
  const querySnapshot = {...queryParams}
  loading.value = true
  try {
    const data = await listNotice(querySnapshot)
    if (requestId !== listRequestId) return
    tableList.value = data.data || []
    total.value = data.totalRecords || 0
    selectedIds.value = []
    tableRef.value?.clearSelection()
  } catch {
    if (requestId === listRequestId) ElMessage.error('公告列表加载失败，请稍后重试')
  } finally {
    if (requestId === listRequestId) loading.value = false
  }
}

const search = () => { queryParams.currentPage = 1; void getList() }

const handleSelectionChange = (selection: Notice[]) => {
  selectedIds.value = selection.map((item) => item.noticeId)
}

const handleAdd = () => {
  resetForm()
  dialogState.value = 'add'
  dialogVisible.value = true
}

const handleUpdate = async (notice?: Notice) => {
  const id = notice?.noticeId || selectedIds.value[0]
  if (!id) return
  const requestId = ++detailRequestId
  resetForm()
  dialogState.value = 'edit'
  dialogVisible.value = true
  detailLoading.value = true
  try {
    const data = await getNotice(id)
    if (requestId === detailRequestId) Object.assign(form, data)
  } catch {
    if (requestId === detailRequestId) {
      dialogVisible.value = false
      ElMessage.error('公告详情加载失败，请稍后重试')
    }
  } finally {
    if (requestId === detailRequestId) detailLoading.value = false
  }
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (dialogState.value === 'add') await addNotice(form)
    else await updateNotice(form)
    ElMessage.success(dialogState.value === 'add' ? '公告发布成功' : '公告更新成功')
    dialogVisible.value = false
    resetForm()
    await getList()
  } finally {
    submitting.value = false
  }
}

const handleDelete = async (notice?: Notice) => {
  const ids = notice ? [notice.noticeId] : selectedIds.value
  if (!ids.length) return
  await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 条公告吗？`, '删除公告', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await removeNotice(ids)
  ElMessage.success('公告已删除')
  await getList()
}

const viewNotice = (id: IdType) => router.push({ name: 'notice', params: { id } })

onMounted(getList)
</script>

<style lang="scss" scoped>
.notice-panel { padding: 22px 24px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 16px 40px rgb(15 23 42 / 4%); }
.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 18px; }
.panel-eyebrow { color: var(--el-color-warning); font-size: 11px; font-weight: 800; letter-spacing: .14em; }
.panel-heading h2 { margin: 7px 0 5px; font-size: 21px; }
.panel-heading p { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }
.panel-tools { display: flex; gap: 10px; }
.keyword-input { width: 230px; }
.priority-select { width: 130px; }
.selection-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; padding: 8px 12px; border-radius: 10px; background: var(--el-fill-color-light); color: var(--el-text-color-secondary); font-size: 13px; }
.notice-table { border-radius: 14px; overflow: hidden; }
.notice-title-cell { display: flex; align-items: center; gap: 12px; }
.notice-mark { display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center; border-radius: 11px; background: color-mix(in srgb, var(--el-color-primary) 10%, var(--el-bg-color)); color: var(--el-color-primary); }
.notice-mark.is-important { background: color-mix(in srgb, var(--el-color-warning) 14%, var(--el-bg-color)); color: var(--el-color-warning); }
.notice-title-cell div:last-child { display: flex; min-width: 0; flex-direction: column; gap: 4px; }
.notice-title-cell strong { overflow: hidden; color: var(--el-text-color-primary); text-overflow: ellipsis; white-space: nowrap; }
.notice-title-cell span:last-child { color: var(--el-text-color-secondary); font-size: 12px; }
.priority-pill { display: inline-flex; padding: 5px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.priority-pill.is-important { background: color-mix(in srgb, var(--el-color-warning) 12%, var(--el-bg-color)); color: var(--el-color-warning); }
.priority-pill.is-normal { background: color-mix(in srgb, var(--el-color-success) 10%, var(--el-bg-color)); color: var(--el-color-success); }
.notice-form { padding: 4px 4px 0; }
.form-intro { margin-bottom: 18px; padding: 14px 16px; border-radius: 12px; background: var(--el-fill-color-light); }
.form-intro p { margin: 7px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
:deep(.el-table__header th.el-table__cell) { color: var(--el-text-color-secondary); font-size: 12px; font-weight: 700; background: var(--el-fill-color-light); }
:deep(.el-table__row td.el-table__cell) { height: 68px; }
@media (max-width: 760px) { .panel-heading { flex-direction: column; }.panel-tools { width: 100%; }.keyword-input, .priority-select { flex: 1; width: auto; } }
@media (max-width: 480px) { .notice-panel { padding: 16px 12px 8px; }.panel-tools { flex-direction: column; }.keyword-input, .priority-select { width: 100%; }.notice-table :deep(.el-table__fixed-right) { display: none; } }
</style>
