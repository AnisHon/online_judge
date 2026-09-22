<template>
  <ContestSubPageShell
    title="学生管理"
    kicker="CLASS / MEMBERS"
    :description="`管理班级 ${shortId(classId)} 的成员名单，添加和移除不会影响用户本身。`"
    :icon="UserFilled"
    tone="green"
    :stats="summaryStats"
  >
    <template #actions>
      <el-button :icon="Refresh" :loading="isLoading" @click="getList">刷新名单</el-button>
      <el-button v-has="'user:class:edit'" type="primary" :icon="Plus" @click="openAddDialog">添加学生</el-button>
      <el-button v-has="'user:class:edit'" type="danger" plain :disabled="!selectedIds.length || isLoading || actionLoading" :icon="Delete" @click="handleRemove()">批量移除</el-button>
    </template>

    <section class="member-panel">
      <div class="panel-heading">
        <div>
          <span class="panel-eyebrow">MEMBER DIRECTORY</span>
          <h2>班级成员</h2>
          <p>成员来自平台用户，移出班级只解除归属关系，不会删除账号。</p>
        </div>
        <div class="panel-heading__meta"><span class="sync-dot" :class="{ 'is-loading': isLoading }"></span><span>{{ isLoading ? '正在同步' : `班级 ID ${shortId(classId)}` }}</span></div>
      </div>

      <div class="member-toolbar">
        <div class="selection-status" :class="{ 'has-selection': selectedIds.length }"><el-icon><Select /></el-icon>{{ selectedIds.length ? `已选择 ${selectedIds.length} 位学生` : '未选择学生' }}</div>
        <RightToolBar :columns="columns" @queryTable="getList" />
      </div>

      <el-table ref="tableRef" v-loading="isLoading" class="member-table" :data="tableList" row-key="userId" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" align="center" />
        <el-table-column v-if="columns[0].visible" label="学生" min-width="280">
          <template #default="{ row }"><div class="user-cell"><span class="user-avatar" :class="{ 'is-banned': row.status === UserStatus.BANNED }">{{ userInitial(row) }}</span><div class="user-cell__main"><strong :title="row.nikeName || row.userName">{{ row.nikeName || '未设置昵称' }}</strong><span><b>@{{ row.userName }}</b><i>·</i><span :title="String(row.userId)">ID {{ shortId(row.userId) }}</span></span></div></div></template>
        </el-table-column>
        <el-table-column v-if="columns[1].visible" label="邮箱" min-width="220" prop="email" show-overflow-tooltip><template #default="{ row }">{{ row.email || '未绑定邮箱' }}</template></el-table-column>
        <el-table-column v-if="columns[2].visible" label="账号状态" width="125" align="center"><template #default="{ row }"><span class="status-pill" :class="row.status === UserStatus.BANNED ? 'is-banned' : 'is-normal'"><i></i>{{ statusLabel(row.status) }}</span></template></el-table-column>
        <el-table-column v-if="columns[3].visible" label="加入信息" min-width="180" prop="createTime" show-overflow-tooltip><template #default="{ row }"><span class="date-cell">{{ formatDate(row.createTime) }}</span><small>用户创建时间</small></template></el-table-column>
        <el-table-column v-if="columns[4].visible" label="用户备注" min-width="200" prop="remark" show-overflow-tooltip><template #default="{ row }">{{ row.remark || '—' }}</template></el-table-column>
        <el-table-column label="操作" width="100" fixed="right" align="right"><template #default="{ row }"><el-button v-has="'user:class:edit'" link type="danger" :icon="Delete" :loading="removingId === row.userId" @click="handleRemove(row)">移除</el-button></template></el-table-column>
        <template #empty><el-empty description="这个班级还没有学生" :image-size="84"><el-button v-has="'user:class:edit'" type="primary" :icon="Plus" @click="openAddDialog">添加第一位学生</el-button></el-empty></template>
      </el-table>
    </section>

    <el-dialog v-model="userDialog" class="add-member-dialog" title="添加学生" width="min(900px, 94vw)" append-to-body destroy-on-close @closed="clearPicker">
      <div class="dialog-intro"><span class="dialog-intro__icon"><el-icon><UserFilled /></el-icon></span><div><strong>加入班级成员</strong><p>搜索并多选平台用户，确认后只会新增他们与当前班级的关系。</p></div></div>
      <UserViewer v-model:ids="userIds" :loading="false" />
      <template #footer><span class="dialog-selection">已选 {{ userIds.length }} 位用户</span><el-button @click="userDialog = false">取消</el-button><el-button v-has="'user:class:edit'" type="primary" :loading="submitLoading" :disabled="!userIds.length" @click="submit">确认添加</el-button></template>
    </el-dialog>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Delete, Plus, Refresh, Select, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type TableInstance } from 'element-plus'
import { useRoute } from 'vue-router'
import ContestSubPageShell from '@/views/backend/teacher/contest-manage/component/ContestSubPageShell.vue'
import RightToolBar from '@/components/right-toolbar/RightToolBar.vue'
import UserViewer from '@/components/user-viewer/UserViewer.vue'
import { addUserForClass, getUserByClass, removeUserForClass } from '@/api/class'
import type { IdType } from '@/api/common'
import type { UserView } from '@/api/user'
import { UserStatus } from '@/api/user'
import { useColumn } from '@/hooks/useColumn'

const route = useRoute()
const classId = computed<IdType>(() => String(route.params.classId || ''))
const tableList = reactive<UserView[]>([])
const selectedIds = ref<IdType[]>([])
const userIds = reactive<IdType[]>([])
const userDialog = ref(false)
const isLoading = ref(false)
const submitLoading = ref(false)
const removingId = ref<IdType>()
const actionLoading = ref(false)
const tableRef = ref<TableInstance>()
const columns = useColumn(['学生', '邮箱地址', '账号状态', '加入信息', '用户备注']).columns

const shortId = (value: IdType) => { const text = String(value); return text.length > 18 ? `${text.slice(0, 8)}…${text.slice(-6)}` : text }
const userInitial = (user: UserView) => String(user.nikeName || user.userName || '?').slice(0, 1).toUpperCase()
const statusLabel = (status: UserStatus) => status === UserStatus.BANNED ? '已封禁' : '正常'
const formatDate = (value: Date | string | undefined) => value ? new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }) : '—'
const summaryStats = computed(() => [{ label: '班级成员', value: tableList.length, tone: 'blue' }, { label: '当前正常', value: tableList.filter(user => user.status !== UserStatus.BANNED).length, tone: 'green' }, { label: '当前封禁', value: tableList.filter(user => user.status === UserStatus.BANNED).length, tone: 'amber' }, { label: '已选择', value: selectedIds.value.length, tone: 'violet' }])

const getList = async () => {
  isLoading.value = true
  try {
    if (!classId.value) return
    tableList.splice(0, tableList.length, ...(await getUserByClass(classId.value)))
    selectedIds.value = []
    tableRef.value?.clearSelection()
  } catch {
    ElMessage.error('班级成员加载失败，请稍后重试')
  } finally {
    isLoading.value = false
  }
}
const handleSelectionChange = (selection: UserView[]) => { selectedIds.value = selection.map(user => user.userId) }
const openAddDialog = () => { userIds.splice(0); userDialog.value = true }
const clearPicker = () => { userIds.splice(0) }

const handleRemove = async (row?: UserView) => {
  const ids = row ? [row.userId] : selectedIds.value
  if (!ids.length) return
  try {
    await ElMessageBox.confirm(row ? `确定将 @${row.userName} 移出当前班级吗？` : `确定将选中的 ${ids.length} 位学生移出当前班级吗？`, '移除班级成员', { confirmButtonText: '确认移除', cancelButtonText: '取消', type: 'warning' })
    actionLoading.value = true
    if (row) removingId.value = row.userId
    await removeUserForClass(classId.value, row ? ids[0] : ids)
    ElMessage.success('班级成员已移除')
    await getList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('移除失败，请稍后重试')
  } finally {
    removingId.value = undefined
    actionLoading.value = false
  }
}

const submit = async () => {
  if (!userIds.length) { ElMessage.warning('请至少选择一位用户'); return }
  submitLoading.value = true
  try {
    await addUserForClass(classId.value, [...userIds])
    userDialog.value = false
    ElMessage.success('学生已添加到班级')
    await getList()
  } catch {
    ElMessage.error('添加失败，请稍后重试')
  } finally {
    submitLoading.value = false
  }
}

watch(() => classId.value, () => { void getList() }, { immediate: true })
</script>

<style lang="scss" scoped>
.member-panel { min-width: 0; padding: 22px 24px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 16px 40px rgb(15 23 42 / 4%); }.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 20px; }.panel-eyebrow { color: var(--el-color-success); font-size: 11px; font-weight: 800; letter-spacing: .14em; }.panel-heading h2 { margin: 7px 0 5px; font-size: 21px; letter-spacing: -.03em; }.panel-heading p { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }.panel-heading__meta { display: inline-flex; align-items: center; gap: 7px; padding-top: 5px; color: var(--el-text-color-secondary); font: 12px var(--code-font-family, monospace); white-space: nowrap; }.sync-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--el-color-success); }.sync-dot.is-loading { background: var(--el-color-warning); animation: pulse 1.1s ease-in-out infinite; }.member-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 15px; min-height: 36px; margin-bottom: 10px; }.selection-status { display: inline-flex; align-items: center; gap: 6px; color: var(--el-text-color-secondary); font-size: 12px; }.selection-status .el-icon { color: var(--el-text-color-placeholder); }.selection-status.has-selection { color: var(--el-color-primary); font-weight: 650; }.selection-status.has-selection .el-icon { color: var(--el-color-primary); }
.member-table { overflow: hidden; border-radius: 14px; }.member-table :deep(.el-table__header th.el-table__cell) { color: var(--el-text-color-secondary); font-size: 12px; font-weight: 700; background: var(--el-fill-color-light); }.member-table :deep(.el-table__row td.el-table__cell) { height: 72px; }.user-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }.user-avatar { display: grid; width: 38px; height: 38px; flex: 0 0 auto; place-items: center; border: 1px solid color-mix(in srgb, var(--el-color-success) 18%, transparent); border-radius: 12px; background: color-mix(in srgb, var(--el-color-success) 11%, var(--el-bg-color)); color: var(--el-color-success); font-size: 15px; font-weight: 750; }.user-avatar.is-banned { border-color: color-mix(in srgb, var(--el-color-danger) 20%, transparent); background: color-mix(in srgb, var(--el-color-danger) 10%, var(--el-bg-color)); color: var(--el-color-danger); }.user-cell__main { display: flex; min-width: 0; flex-direction: column; gap: 5px; }.user-cell__main strong { max-width: 200px; overflow: hidden; font-size: 13px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }.user-cell__main > span { display: flex; align-items: center; gap: 6px; overflow: hidden; color: var(--el-text-color-secondary); font-size: 11px; }.user-cell__main b { max-width: 125px; overflow: hidden; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }.user-cell__main i { color: var(--el-text-color-placeholder); font-style: normal; }.user-cell__main span span { font-family: var(--code-font-family, monospace); white-space: nowrap; }.status-pill { display: inline-flex; align-items: center; gap: 6px; padding: 5px 9px; border-radius: 999px; font-size: 11px; }.status-pill i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }.status-pill.is-normal { background: color-mix(in srgb, var(--el-color-success) 11%, var(--el-bg-color)); color: var(--el-color-success); }.status-pill.is-banned { background: color-mix(in srgb, var(--el-color-danger) 10%, var(--el-bg-color)); color: var(--el-color-danger); }.date-cell, .member-table small { display: block; }.date-cell { color: var(--el-text-color-primary); font-size: 12px; }.member-table small { margin-top: 4px; color: var(--el-text-color-secondary); font-size: 10px; }.dialog-intro { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; padding: 14px 16px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-fill-color-light); }.dialog-intro__icon { display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center; border-radius: 11px; background: color-mix(in srgb, var(--el-color-success) 13%, var(--el-bg-color)); color: var(--el-color-success); font-size: 18px; }.dialog-intro strong, .dialog-intro p { display: block; }.dialog-intro p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }.dialog-selection { margin-right: auto; color: var(--el-text-color-secondary); font-size: 12px; }
@keyframes pulse { 50% { opacity: .35; } } @media (max-width: 760px) { .member-panel { padding: 18px 14px 8px; }.panel-heading { align-items: flex-start; flex-direction: column; gap: 10px; }.member-toolbar { align-items: flex-start; flex-direction: column; }.member-table :deep(.el-table__fixed-right) { display: none; } }
</style>
