<template>
  <section v-loading="loading" class="user-picker">
    <div class="picker-heading">
      <div class="picker-heading__title"><span class="picker-icon"><el-icon><User /></el-icon></span><div><strong>选择平台用户</strong><small>支持按用户名、昵称、邮箱或 ID 搜索，可跨页保留已选结果。</small></div></div>
      <span class="picker-count">已选 {{ selectedIds.length }} 人</span>
    </div>

    <el-form v-show="showSearch" :model="queryParams" class="picker-filters" label-position="top" @submit.prevent="handleQuery">
      <el-form-item label="用户名" prop="userName"><el-input v-model="queryParams.userName" clearable placeholder="搜索用户名" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="昵称" prop="nikeName"><el-input v-model="queryParams.nikeName" clearable placeholder="搜索昵称" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="邮箱" prop="email"><el-input v-model="queryParams.email" clearable placeholder="搜索邮箱" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="用户 ID" prop="userId"><el-input v-model="queryParams.userId" clearable placeholder="输入 ID" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" clearable class="full-width" placeholder="全部状态"><el-option v-for="item in dict.userStatus" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <div class="picker-actions"><el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button><el-button :icon="Refresh" @click="resetQuery">重置</el-button></div>
    </el-form>

    <div class="picker-toolbar"><span class="result-caption">{{ total ? `找到 ${total} 位用户` : '暂无搜索结果' }}</span><el-button link type="primary" @click="showSearch = !showSearch">{{ showSearch ? '收起筛选' : '展开筛选' }}</el-button></div>
    <el-table ref="tableRef" v-loading="isLoading" class="picker-table" :data="tableList" row-key="userId" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="52" align="center" reserve-selection />
      <el-table-column label="用户" min-width="250">
        <template #default="{ row }"><div class="user-cell"><span class="user-avatar">{{ userInitial(row) }}</span><div><strong :title="row.nikeName || row.userName">{{ row.nikeName || '未设置昵称' }}</strong><small>@{{ row.userName }} · ID {{ shortId(row.userId) }}</small></div></div></template>
      </el-table-column>
      <el-table-column label="邮箱" min-width="190" prop="email" show-overflow-tooltip><template #default="{ row }">{{ row.email || '未绑定邮箱' }}</template></el-table-column>
      <el-table-column label="状态" width="105" align="center"><template #default="{ row }"><el-tag size="small" :type="row.status === UserStatus.BANNED ? 'danger' : 'success'" effect="plain">{{ statusLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="创建时间" width="125" prop="createTime" show-overflow-tooltip><template #default="{ row }">{{ formatDate(row.createTime) }}</template></el-table-column>
      <template #empty><el-empty description="没有找到用户" :image-size="70" /></template>
    </el-table>
    <Pagination v-show="total > 0" v-model:page="queryParams.currentPage" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
  </section>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { Refresh, Search, User } from '@element-plus/icons-vue'
import { debouncedGetUser, dict, type QueryUser, UserStatus, type UserView } from '@/api/user'
import type { IdType } from '@/api/common'
import Pagination from '@/components/pageination/Pagination.vue'

const props = defineProps<{ ids: IdType[]; loading: boolean; selectionLimit?: number }>()
const queryParams = reactive<QueryUser>({ asc: true, currentPage: 1, pageSize: 10, userId: undefined, userName: undefined, nikeName: undefined, email: undefined, status: undefined })
const tableList = reactive<UserView[]>([])
const total = ref(0)
const showSearch = ref(true)
const selectedIds = props.ids

const shortId = (value: IdType) => { const text = String(value); return text.length > 18 ? `${text.slice(0, 8)}…${text.slice(-6)}` : text }
const userInitial = (user: UserView) => String(user.nikeName || user.userName || '?').slice(0, 1).toUpperCase()
const statusLabel = (status: UserStatus) => status === UserStatus.BANNED ? '已封禁' : '正常'
const formatDate = (value: Date | string | undefined) => value ? new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }) : '—'

const { loading: load, isLoading, get: getUser } = debouncedGetUser(queryParams, (data) => { tableList.splice(0, tableList.length, ...data.data); total.value = data.totalRecords })
const getList = () => { load(); getUser() }
const handleSelectionChange = (selection: UserView[]) => {
  const currentPageIds = new Set(tableList.map(user => String(user.userId)))
  const visibleSelection = props.selectionLimit ? selection.slice(-props.selectionLimit) : selection
  const selectedOnPage = new Set(visibleSelection.map(user => String(user.userId)))
  const nextIds = selectedIds.filter(id => !currentPageIds.has(String(id)) || selectedOnPage.has(String(id)))
  visibleSelection.forEach(user => { if (!nextIds.some(id => String(id) === String(user.userId))) nextIds.push(user.userId) })
  if (props.selectionLimit && nextIds.length > props.selectionLimit) {
    nextIds.splice(0, nextIds.length - props.selectionLimit)
  }
  selectedIds.splice(0, selectedIds.length, ...nextIds)
}
const resetQuery = () => { queryParams.currentPage = 1; queryParams.userId = undefined; queryParams.userName = undefined; queryParams.nikeName = undefined; queryParams.email = undefined; queryParams.status = undefined; queryParams.sortColumn = undefined; getList() }
const handleQuery = () => { queryParams.currentPage = 1; getList() }

getList()
</script>

<style lang="scss" scoped>
.user-picker { color: var(--el-text-color-primary); }.picker-heading { display: flex; align-items: center; justify-content: space-between; gap: 15px; margin-bottom: 16px; }.picker-heading__title { display: flex; align-items: center; gap: 10px; min-width: 0; }.picker-icon { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border-radius: 10px; background: color-mix(in srgb, var(--el-color-primary) 12%, var(--el-bg-color)); color: var(--el-color-primary); }.picker-heading strong, .picker-heading small { display: block; }.picker-heading strong { font-size: 14px; }.picker-heading small { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }.picker-count { flex: 0 0 auto; color: var(--el-color-primary); font-size: 12px; font-weight: 650; }.picker-filters { display: grid; grid-template-columns: repeat(5, minmax(120px, 1fr)) auto; align-items: end; gap: 0 12px; padding: 13px 14px 2px; border: 1px solid var(--el-border-color-lighter); border-radius: 12px; background: var(--el-bg-color-page); }.picker-filters :deep(.el-form-item) { margin-bottom: 10px; }.picker-filters :deep(.el-form-item__label) { height: auto; margin-bottom: 5px; color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.2; }.full-width { width: 100%; }.picker-actions { display: flex; gap: 6px; height: 68px; padding-bottom: 10px; align-items: flex-end; }.picker-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 10px; min-height: 40px; }.result-caption { color: var(--el-text-color-secondary); font-size: 12px; }.picker-table { border-radius: 12px; }.picker-table :deep(.el-table__header th.el-table__cell) { color: var(--el-text-color-secondary); font-size: 12px; background: var(--el-fill-color-light); }.picker-table :deep(.el-table__row td.el-table__cell) { height: 60px; }.user-cell { display: flex; align-items: center; gap: 10px; min-width: 0; }.user-avatar { display: grid; width: 32px; height: 32px; flex: 0 0 auto; place-items: center; border-radius: 10px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); font-size: 13px; font-weight: 700; }.user-cell > div { display: flex; min-width: 0; flex-direction: column; gap: 4px; }.user-cell strong { max-width: 180px; overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }.user-cell small { color: var(--el-text-color-secondary); font: 11px var(--code-font-family, monospace); white-space: nowrap; }.user-picker :deep(.pagination-container) { padding: 22px 8px 8px; }
@media (max-width: 900px) { .picker-filters { grid-template-columns: repeat(3, minmax(140px, 1fr)); }.picker-actions { height: auto; padding-bottom: 10px; } } @media (max-width: 620px) { .picker-heading { align-items: flex-start; flex-direction: column; }.picker-filters { grid-template-columns: repeat(2, minmax(0, 1fr)); }.picker-actions { grid-column: 1 / -1; }.picker-actions .el-button { flex: 1; }.picker-table :deep(.el-table__fixed-right) { display: none; } } @media (max-width: 420px) { .picker-filters { grid-template-columns: 1fr; }.picker-actions { grid-column: auto; } }
</style>
