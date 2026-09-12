<template>
  <ContestSubPageShell
    title="用户角色"
    kicker="USER / ROLE MEMBERS"
    :description="`管理 ${roleName} 下的成员授权，成员变化会立即影响对应账号的访问范围。`"
    :icon="UserFilled"
    tone="violet"
    :stats="summaryStats"
  >
    <template #actions>
      <el-button :icon="ArrowLeft" @click="router.push({ name: 'role-manage' })">返回角色管理</el-button>
      <el-button :icon="Refresh" :loading="isLoading" @click="getList">刷新成员</el-button>
      <el-button v-has="'user:role:grant'" type="primary" :icon="Plus" @click="handleAdd()">分配成员</el-button>
    </template>

    <section class="member-panel">
      <div class="role-context">
        <div class="role-context__main">
          <span class="role-context__icon"><el-icon><UserFilled /></el-icon></span>
          <div>
            <span class="panel-eyebrow">CURRENT ROLE</span>
            <h2>{{ roleName }}</h2>
            <p>角色 ID <code>{{ shortId(roleId) }}</code><span class="context-divider">·</span>在此维护角色成员名单</p>
          </div>
        </div>
        <div class="role-context__tip"><el-icon><InfoFilled /></el-icon><span>撤销授权不会删除用户账号</span></div>
      </div>

      <div class="filter-panel" :class="{ 'is-collapsed': !showSearch }">
        <div class="filter-panel__bar">
          <div class="filter-title"><el-icon><Filter /></el-icon><strong>定位成员</strong><span v-if="activeFilterCount">{{ activeFilterCount }} 项已启用</span></div>
          <el-button link type="primary" @click="showSearch = !showSearch">{{ showSearch ? '收起筛选' : '展开筛选' }}</el-button>
        </div>
        <el-form v-show="showSearch" :model="queryParams" class="member-filters" label-position="top" @submit.prevent="handleQuery">
          <el-form-item label="用户名" prop="username"><el-input v-model="queryParams.username" clearable :prefix-icon="User" placeholder="搜索登录用户名" @keyup.enter="handleQuery" /></el-form-item>
          <el-form-item label="昵称" prop="nikeName"><el-input v-model="queryParams.nikeName" clearable :prefix-icon="EditPen" placeholder="搜索展示昵称" @keyup.enter="handleQuery" /></el-form-item>
          <el-form-item label="邮箱" prop="email"><el-input v-model="queryParams.email" clearable :prefix-icon="Message" placeholder="搜索邮箱地址" @keyup.enter="handleQuery" /></el-form-item>
          <el-form-item label="用户 ID" prop="userId"><el-input v-model="queryParams.userId" clearable :prefix-icon="Key" placeholder="输入完整或部分 ID" @keyup.enter="handleQuery" /></el-form-item>
          <div class="filter-actions"><el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button><el-button :icon="Refresh" @click="resetQuery">重置</el-button></div>
        </el-form>
      </div>

      <div class="list-toolbar">
        <div class="list-toolbar__left"><span class="selection-status" :class="{ 'has-selection': selectedIds.length }"><el-icon><Select /></el-icon>{{ selectedIds.length ? `已选择 ${selectedIds.length} 位成员` : '未选择成员' }}</span><el-button v-has="'user:role:revoke'" type="danger" plain :disabled="!selectedIds.length || actionLoading" :icon="Remove" @click="handleRevoke()">批量撤销</el-button></div>
        <RightToolBar v-model:showSearch="showSearch" :columns="columns" @queryTable="getList" />
      </div>

      <el-table v-loading="isLoading" class="member-table" :data="tableList" row-key="userId" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" align="center" reserve-selection />
        <el-table-column v-if="columns[0].visible" label="成员" min-width="300">
          <template #default="{ row }"><div class="user-cell"><span class="user-avatar" :class="{ 'is-banned': row.status === UserStatus.BANNED }">{{ userInitial(row) }}</span><div class="user-cell__main"><div class="user-cell__title"><strong :title="row.nikeName || row.userName">{{ row.nikeName || '未设置昵称' }}</strong><span v-if="row.status === UserStatus.BANNED" class="mini-badge">已封禁</span></div><span class="user-cell__sub"><span>@{{ row.userName }}</span><span class="separator">·</span><span :title="String(row.userId)">ID {{ shortId(row.userId) }}</span></span></div></div></template>
        </el-table-column>
        <el-table-column v-if="columns[1].visible" label="邮箱" min-width="220" prop="email" show-overflow-tooltip><template #default="{ row }">{{ row.email || '未绑定邮箱' }}</template></el-table-column>
        <el-table-column v-if="columns[2].visible" label="账号状态" width="125" align="center"><template #default="{ row }"><span class="status-pill" :class="row.status === UserStatus.BANNED ? 'is-banned' : 'is-normal'"><i></i>{{ statusLabel(row.status) }}</span></template></el-table-column>
        <el-table-column v-if="columns[3].visible" label="加入时间" min-width="170" prop="createTime" show-overflow-tooltip><template #default="{ row }">{{ formatDate(row.createTime) }}</template></el-table-column>
        <el-table-column v-if="columns[4].visible" label="备注" min-width="200" prop="remark" show-overflow-tooltip><template #default="{ row }">{{ row.remark || '暂无备注' }}</template></el-table-column>
        <el-table-column label="操作" width="125" fixed="right" align="right"><template #default="{ row }"><el-button v-has="'user:role:revoke'" link type="danger" :icon="Remove" @click="handleRevoke(row)">撤销</el-button></template></el-table-column>
        <template #empty><el-empty description="该角色暂无成员" :image-size="84"><el-button v-has="'user:role:grant'" type="primary" :icon="Plus" @click="handleAdd()">分配第一位成员</el-button></el-empty></template>
      </el-table>
      <Pagination v-show="total > 0" v-model:page="queryParams.currentPage" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </section>

    <el-dialog v-model="open" class="grant-dialog" title="分配角色成员" width="min(920px, 94vw)" append-to-body destroy-on-close>
      <div class="dialog-intro"><span class="dialog-intro__icon"><el-icon><UserFilled /></el-icon></span><div><strong>为“{{ roleName }}”选择成员</strong><p>支持搜索、跨页勾选；已选成员提交后会获得该角色的全部授权。</p></div><span class="dialog-selected">已选 {{ grantSelectedIds.length }} 人</span></div>
      <UserViewer v-model:ids="grantSelectedIds" :loading="isGranting" />
      <template #footer><el-button @click="cancel">取消</el-button><el-button v-has="'user:role:grant'" type="primary" :loading="isGranting" @click="submit">确认分配</el-button></template>
    </el-dialog>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ArrowLeft, EditPen, Filter, InfoFilled, Key, Message, Plus, Remove, Refresh, Search, Select, User, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import ContestSubPageShell from '@/views/backend/teacher/contest-manage/component/ContestSubPageShell.vue'
import RightToolBar from '@/components/right-toolbar/RightToolBar.vue'
import Pagination from '@/components/pageination/Pagination.vue'
import UserViewer from '@/components/user-viewer/UserViewer.vue'
import { debouncedGetRoleUser, type QueryRoleUser, type UserView, UserStatus } from '@/api/user'
import { debouncedGrant, revoke, type UserRoleRelation } from '@/api/role'
import type { IdType } from '@/api/common'
import { useColumn } from '@/hooks/useColumn'
import { getRole, type RoleView } from '@/api/role'

const route = useRoute()
const router = useRouter()
const roleId = String(route.params.id || '')
const roleName = ref(`角色 ${roleId}`)
const role = ref<RoleView>()
const queryParams = reactive<QueryRoleUser>({ currentPage: 1, pageSize: 20, roleId, userId: undefined, username: undefined, nikeName: undefined, email: undefined })
const tableList = reactive<UserView[]>([])
const total = ref(0)
const selectedIds = ref<IdType[]>([])
const grantSelectedIds = reactive<IdType[]>([])
const showSearch = ref(true)
const open = ref(false)
const actionLoading = ref(false)
const addForm = reactive<UserRoleRelation[]>([])
const { columns } = useColumn(['成员', '邮箱地址', '账号状态', '加入时间', '备注'])

const activeFilterCount = computed(() => [queryParams.username, queryParams.nikeName, queryParams.email, queryParams.userId].filter(value => value !== undefined && value !== '').length)
const summaryStats = computed(() => [{ label: '角色成员', value: total.value, tone: 'blue' }, { label: '当前页正常', value: tableList.filter(user => user.status === UserStatus.NORMAL).length, tone: 'green' }, { label: '当前页封禁', value: tableList.filter(user => user.status === UserStatus.BANNED).length, tone: 'amber' }, { label: '已选择', value: selectedIds.value.length, tone: 'violet' }])

const { loading, isLoading, get: getUser } = debouncedGetRoleUser(queryParams, data => { tableList.splice(0, tableList.length, ...data.data); total.value = data.totalRecords; selectedIds.value = [] })
const getList = () => { loading(); getUser() }

function userInitial(user: UserView) { return String(user.nikeName || user.userName || '?').slice(0, 1).toUpperCase() }
function shortId(value: IdType) { const text = String(value); return text.length > 18 ? `${text.slice(0, 8)}…${text.slice(-6)}` : text }
function statusLabel(status: UserStatus) { return status === UserStatus.BANNED ? '已封禁' : '正常' }
function formatDate(value: Date | string | undefined) { return value ? new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }) : '—' }

const handleSelectionChange = (selection: UserView[]) => { selectedIds.value = selection.map(user => user.userId) }
const handleQuery = () => { queryParams.currentPage = 1; getList() }
const resetQuery = () => { queryParams.currentPage = 1; queryParams.userId = undefined; queryParams.username = undefined; queryParams.nikeName = undefined; queryParams.email = undefined; getList() }

async function loadRole() {
  try {
    const data = await getRole({ asc: true, currentPage: 1, pageSize: 1, roleId })
    role.value = data.data[0]
    if (role.value?.roleName) roleName.value = role.value.roleName
  } catch { /* 页面仍可使用角色 ID 管理成员 */ }
}

const handleRevoke = async (row?: UserView) => {
  const ids = row ? [row.userId] : selectedIds.value
  if (!ids.length) { ElMessage.warning('请先选择要撤销的成员'); return }
  const message = row ? `确定撤销用户“${row.nikeName || row.userName}”的角色吗？` : `确定撤销选中的 ${ids.length} 位成员吗？`
  try {
    await ElMessageBox.confirm(message, '撤销角色授权', { confirmButtonText: '确认撤销', cancelButtonText: '取消', type: 'warning' })
    actionLoading.value = true
    const relations = ids.map(userId => ({ roleId, userId }))
    await revoke(relations.length === 1 ? relations[0] : relations)
    ElMessage.success('角色授权已撤销')
    selectedIds.value = []
    getList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('撤销失败，请稍后重试')
  } finally { actionLoading.value = false }
}

const handleAdd = () => { grantSelectedIds.splice(0, grantSelectedIds.length); open.value = true }
const cancel = () => { open.value = false; grantSelectedIds.splice(0, grantSelectedIds.length) }
const { loading: startGrantLoading, isLoading: isGranting, add } = debouncedGrant(addForm, () => { ElMessage.success('角色成员已更新'); cancel(); getList() })
const submit = () => {
  if (!grantSelectedIds.length) { ElMessage.warning('请至少选择一位用户'); return }
  addForm.splice(0, addForm.length, ...grantSelectedIds.map(userId => ({ roleId, userId })))
  startGrantLoading()
  add()
}

loadRole()
getList()
</script>

<style lang="scss" scoped>
.member-panel { min-width: 0; padding: 22px 24px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 16px 40px rgb(15 23 42 / 4%); }.role-context { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 20px; }.role-context__main { display: flex; align-items: center; min-width: 0; gap: 13px; }.role-context__icon { display: grid; width: 44px; height: 44px; flex: 0 0 auto; place-items: center; border-radius: 13px; background: rgb(139 92 246 / 13%); color: #8b5cf6; font-size: 21px; }.panel-eyebrow { color: #8b5cf6; font-size: 11px; font-weight: 800; letter-spacing: .14em; }.role-context h2 { margin: 5px 0 4px; font-size: 20px; letter-spacing: -.03em; }.role-context p { margin: 0; color: var(--el-text-color-secondary); font-size: 12px; }.role-context code { padding: 2px 5px; border-radius: 5px; background: var(--el-fill-color-light); color: var(--el-text-color-primary); font: 11px var(--code-font-family, monospace); }.context-divider { margin: 0 6px; color: var(--el-text-color-placeholder); }.role-context__tip { display: inline-flex; align-items: center; gap: 6px; color: var(--el-text-color-secondary); font-size: 11px; white-space: nowrap; }.role-context__tip .el-icon { color: #8b5cf6; }
.filter-panel { margin-bottom: 18px; padding: 14px 16px 4px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-bg-color-page); }.filter-panel__bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 11px; }.filter-title { display: flex; align-items: center; gap: 7px; font-size: 13px; }.filter-title .el-icon { color: #8b5cf6; }.filter-title span { color: var(--el-text-color-secondary); font-size: 11px; font-weight: 400; }.member-filters { display: grid; grid-template-columns: repeat(4, minmax(140px, 1fr)) auto; align-items: end; gap: 0 14px; }.member-filters :deep(.el-form-item) { min-width: 0; margin-bottom: 10px; }.member-filters :deep(.el-form-item__label) { height: auto; margin-bottom: 5px; color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.2; }.full-width { width: 100%; }.filter-actions { display: flex; align-items: flex-end; gap: 7px; height: 68px; padding-bottom: 10px; }
.list-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 15px; min-height: 36px; margin-bottom: 10px; }.list-toolbar__left { display: flex; align-items: center; gap: 12px; }.selection-status { display: inline-flex; align-items: center; gap: 6px; color: var(--el-text-color-secondary); font-size: 12px; }.selection-status .el-icon { color: var(--el-text-color-placeholder); }.selection-status.has-selection { color: #8b5cf6; font-weight: 650; }.selection-status.has-selection .el-icon { color: #8b5cf6; }
.member-table { overflow: hidden; border-radius: 14px; }.member-table :deep(.el-table__header th.el-table__cell) { color: var(--el-text-color-secondary); font-size: 12px; font-weight: 700; background: var(--el-fill-color-light); }.member-table :deep(.el-table__row td.el-table__cell) { height: 68px; }.user-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }.user-avatar { display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center; border-radius: 11px; background: rgb(139 92 246 / 13%); color: #8b5cf6; font-size: 14px; font-weight: 750; }.user-avatar.is-banned { background: rgb(217 119 6 / 13%); color: #d97706; }.user-cell__main { display: flex; min-width: 0; flex-direction: column; gap: 5px; }.user-cell__title { display: flex; align-items: center; min-width: 0; gap: 8px; }.user-cell__title strong { max-width: 220px; overflow: hidden; font-size: 13px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }.user-cell__sub { color: var(--el-text-color-secondary); font: 11px var(--code-font-family, monospace); }.separator { margin: 0 5px; color: var(--el-text-color-placeholder); }.mini-badge { padding: 2px 6px; border-radius: 999px; background: rgb(217 119 6 / 11%); color: #d97706; font-size: 10px; }.status-pill { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; }.status-pill i { width: 6px; height: 6px; border-radius: 50%; }.status-pill.is-normal { color: #059669; }.status-pill.is-normal i { background: #10b981; }.status-pill.is-banned { color: #d97706; }.status-pill.is-banned i { background: #f59e0b; }
.dialog-intro { display: flex; align-items: center; gap: 12px; margin-bottom: 17px; padding: 14px 16px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-fill-color-light); }.dialog-intro__icon { display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center; border-radius: 11px; background: rgb(139 92 246 / 13%); color: #8b5cf6; font-size: 18px; }.dialog-intro strong, .dialog-intro p { display: block; }.dialog-intro p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }.dialog-selected { margin-left: auto; color: #8b5cf6; font-size: 12px; font-weight: 650; white-space: nowrap; }
@keyframes pulse { 50% { opacity: .35; } }
@media (max-width: 900px) { .member-filters { grid-template-columns: repeat(3, minmax(140px, 1fr)); }.filter-actions { height: auto; padding-bottom: 10px; } }
@media (max-width: 680px) { .member-panel { padding: 18px 14px 8px; }.role-context { align-items: flex-start; flex-direction: column; gap: 10px; }.member-filters { grid-template-columns: repeat(2, minmax(0, 1fr)); }.filter-actions { grid-column: 1 / -1; }.list-toolbar { align-items: flex-start; flex-direction: column; }.dialog-intro { align-items: flex-start; flex-wrap: wrap; }.dialog-selected { margin-left: 48px; } }
@media (max-width: 440px) { .member-filters { grid-template-columns: 1fr; }.filter-actions { grid-column: auto; }.filter-actions .el-button { flex: 1; }.user-cell__title { align-items: flex-start; flex-direction: column; gap: 4px; }.member-table :deep(.el-table__fixed-right) { display: none; } }
</style>
