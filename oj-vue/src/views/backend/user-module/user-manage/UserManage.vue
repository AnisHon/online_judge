<template>
  <ContestSubPageShell
      title="用户管理"
      kicker="USER / DIRECTORY"
      description="维护平台账号、状态与基础资料，把高风险操作放在清晰的操作边界内。"
      :icon="User"
      tone="violet"
      :stats="summaryStats"
  >
    <template #actions>
      <el-button :icon="Refresh" :loading="isLoading" @click="getList">刷新用户</el-button>
      <el-button v-has="'user:user:add'" type="primary" :icon="Plus" @click="handleAdd">新增用户</el-button>
    </template>

    <section class="user-panel">
      <div class="panel-heading">
        <div>
          <span class="panel-eyebrow">ACCOUNT DIRECTORY</span>
          <h2>账号目录</h2>
          <p>先筛选，再执行批量操作；用户 ID、状态和创建时间始终保留在视线内。</p>
        </div>
        <div class="panel-heading__meta"><span class="sync-dot" :class="{ 'is-loading': isLoading }"></span><span>{{
            isLoading ? '正在同步' : `共 ${total} 个结果`
          }}</span></div>
      </div>

      <div class="filter-panel" :class="{ 'is-collapsed': !showSearch }">
        <div class="filter-panel__bar">
          <div class="filter-title">
            <el-icon>
              <Filter/>
            </el-icon>
            <strong>筛选条件</strong><span v-if="activeFilterCount">{{ activeFilterCount }} 项已启用</span></div>
          <el-button link type="primary" @click="showSearch = !showSearch">{{
              showSearch ? '收起筛选' : '展开筛选'
            }}
          </el-button>
        </div>
        <el-form v-show="showSearch" :model="queryParams" class="user-filters" label-position="top"
                 @submit.prevent="handleQuery">
          <el-form-item label="用户名" prop="userName">
            <el-input v-model="queryParams.userName" clearable :prefix-icon="User" placeholder="搜索登录用户名"
                      @keyup.enter="handleQuery"/>
          </el-form-item>
          <el-form-item label="昵称" prop="nikeName">
            <el-input v-model="queryParams.nikeName" clearable :prefix-icon="EditPen" placeholder="搜索展示昵称"
                      @keyup.enter="handleQuery"/>
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="queryParams.email" clearable :prefix-icon="Message" placeholder="搜索邮箱地址"
                      @keyup.enter="handleQuery"/>
          </el-form-item>
          <el-form-item label="用户 ID" prop="userId">
            <el-input v-model="queryParams.userId" clearable :prefix-icon="Key" placeholder="输入完整或部分 ID"
                      @keyup.enter="handleQuery"/>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="queryParams.status" clearable class="filter-select" placeholder="全部状态">
              <el-option v-for="item in dict.userStatus" :key="item.value" :label="item.label" :value="item.value"/>
            </el-select>
          </el-form-item>
          <div class="filter-actions">
            <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </div>
        </el-form>
      </div>

      <div class="list-toolbar">
        <div class="list-toolbar__left">
          <span class="selection-status" :class="{ 'has-selection': selectedIds.length }"><el-icon><Select/></el-icon>{{
              selectedIds.length ? `已选择 ${selectedIds.length} 位用户` : '未选择用户'
            }}</span>
          <el-button v-has="'user:auth:ban'" type="danger" plain :disabled="!selectedIds.length || actionLoading"
                     :icon="Lock" @click="handleBan()">批量封禁
          </el-button>
        </div>
        <RightToolBar v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
      </div>

      <el-table ref="tableRef" v-loading="isLoading" class="user-table" :data="tableList" row-key="userId"
                @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" align="center"/>
        <el-table-column v-if="columns[0].visible" label="用户" min-width="280">
          <template #default="{ row }">
            <div class="user-cell"><span class="user-avatar" :class="{ 'is-banned': row.status === UserStatus.BANNED }">{{
                userInitial(row)
              }}</span>
              <div class="user-cell__main">
                <div class="user-cell__name"><strong :title="row.nikeName || row.userName">{{
                    row.nikeName || '未设置昵称'
                  }}</strong><span v-if="row.status === UserStatus.BANNED" class="mini-badge">已封禁</span></div>
                <span class="user-cell__sub"><span class="username">@{{ row.userName }}</span><span
                    class="separator">·</span><span :title="String(row.userId)">ID {{
                    shortId(row.userId)
                  }}</span></span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="columns[1].visible" label="邮箱" min-width="220" prop="email" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '未绑定邮箱' }}</template>
        </el-table-column>
        <el-table-column v-if="columns[2].visible" label="状态" width="120" align="center">
          <template #default="{ row }"><span class="status-pill"
                                             :class="row.status === UserStatus.BANNED ? 'is-banned' : 'is-normal'"><i></i>{{
              statusLabel(row.status)
            }}</span></template>
        </el-table-column>
        <el-table-column v-if="columns[3].visible" label="创建时间" min-width="170" prop="createTime"
                         show-overflow-tooltip>
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column v-if="columns[4].visible" label="奖励分" width="110" align="right">
          <template #default="{ row }"><span class="points-value">{{ row.points ?? 0 }}</span><span class="points-unit">pts</span>
          </template>
        </el-table-column>
        <el-table-column v-if="columns[5].visible" label="标记" min-width="180" prop="remark" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="right">
          <template #default="{ row }">
            <el-space :size="4">
              <el-button v-has="'user:user:edit'" link type="primary" :icon="EditPen" @click="handleUpdate(row)">编辑
              </el-button>
              <el-button v-if="row.status !== UserStatus.BANNED" v-has="'user:auth:ban'" link type="danger" :icon="Lock"
                         @click="handleBan(row)">封禁
              </el-button>
              <el-button v-else v-has="'user:auth:unban'" link type="success" :icon="Unlock" @click="handleUnban(row)">
                解封
              </el-button>
              <el-dropdown v-if="canEditUser" trigger="click"
                           @command="(command: string) => handleCommand(command, row)">
                <el-button link type="info" :icon="MoreFilled">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="reset" :icon="Key">重置密码</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-space>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有匹配的用户" :image-size="84"/>
        </template>
      </el-table>
      <Pagination v-show="total > 0" v-model:page="queryParams.currentPage" v-model:limit="queryParams.pageSize"
                  :total="total" @pagination="getList"/>
    </section>

    <el-dialog v-model="open" class="user-dialog" :title="dialogState === 'add' ? '新增用户' : '编辑用户'"
               width="min(720px, 92vw)" append-to-body destroy-on-close>
      <div class="dialog-intro"><span class="dialog-intro__icon"><el-icon><User/></el-icon></span>
        <div><strong>{{ dialogState === 'add' ? '建立平台账号' : '更新账号资料' }}</strong>
          <p>{{
              dialogState === 'add' ? '设置登录凭据和初始状态，创建后可继续维护资料。' : `正在编辑 @${editorForm.userName || '当前用户'} 的基础资料。`
            }}</p></div>
      </div>
      <el-form ref="ruleFormRef" :model="editorForm" :rules="rules" class="editor-form" label-position="top">
        <section class="form-section">
          <div class="section-title"><span>01</span>
            <div><strong>身份信息</strong><small>用户名用于登录，昵称用于平台内展示</small></div>
          </div>
          <div class="form-grid">
            <el-form-item label="用户名" prop="userName">
              <el-input v-model="editorForm.userName" :disabled="dialogState === 'edit'" :prefix-icon="User"
                        maxlength="16" placeholder="4-16 位字母、数字、下划线或短横线"/>
            </el-form-item>
            <el-form-item label="昵称" prop="nikeName">
              <el-input v-model="editorForm.nikeName" :prefix-icon="EditPen" maxlength="40"
                        placeholder="请输入展示昵称"/>
            </el-form-item>
          </div>
          <el-form-item v-if="dialogState === 'edit'" label="用户 ID">
            <el-input :model-value="String(editorForm.userId || '')" disabled :prefix-icon="Key"/>
          </el-form-item>
        </section>
        <section class="form-section">
          <div class="section-title"><span>02</span>
            <div><strong>登录与状态</strong><small>{{
                dialogState === 'add' ? '首次创建时需要指定角色和初始密码' : '编辑资料不会修改用户密码和角色'
              }}</small></div>
          </div>
          <div class="form-grid">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="editorForm.email" :prefix-icon="Message" placeholder="可选，用于账号找回"/>
            </el-form-item>
            <el-form-item v-if="dialogState === 'add'" label="初始角色" prop="role">
              <el-select v-model="editorForm.role" class="full-width" :loading="rolesLoading"
                         :disabled="rolesLoading || !roles.length" placeholder="请选择角色">
                <el-option v-for="role in roles" :key="role.roleId" :label="role.roleName" :value="role.roleId"/>
              </el-select>
            </el-form-item>
            <el-form-item v-if="dialogState === 'add'" label="初始密码" prop="password">
              <el-input v-model="editorForm.password" type="password" show-password :prefix-icon="Lock"
                        placeholder="至少 8 位密码"/>
            </el-form-item>
            <el-form-item label="账号状态" prop="status">
              <el-radio-group v-model="editorForm.status" class="status-options">
                <el-radio v-for="item in dict.userStatus" :key="item.value" :label="item.value" :value="item.value">
                  {{ item.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </div>
        </section>
        <section class="form-section">
          <div class="section-title"><span>03</span>
            <div><strong>内部标记</strong><small>仅供管理员识别，不会展示给普通用户</small></div>
          </div>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="editorForm.remark" type="textarea" :rows="3" maxlength="450" show-word-limit
                      placeholder="记录账号来源、维护原因或其他内部信息"/>
          </el-form-item>
        </section>
      </el-form>
      <template #footer>
        <el-button @click="cancel">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="submitting" @click="submitForm">
          {{ dialogState === 'add' ? '创建用户' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from 'vue'
import {
  EditPen,
  Filter,
  Key,
  Lock,
  Message,
  MoreFilled,
  Plus,
  Refresh,
  Search,
  Select,
  Unlock,
  User
} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type TableInstance} from 'element-plus'
import ContestSubPageShell from '@/views/backend/teacher/contest-manage/component/ContestSubPageShell.vue'
import RightToolBar from '@/components/right-toolbar/RightToolBar.vue'
import Pagination from '@/components/pageination/Pagination.vue'
import {
  banUser,
  addUser,
  getUser,
  updateUser,
  dict,
  type QueryUser,
  resetToDefault,
  unbanUser,
  type UserAddForm,
  UserStatus,
  type UserView
} from '@/api/user'
import {getRole, type RoleView} from '@/api/role'
import type {IdType} from '@/api/common'
import {useColumn} from '@/hooks/useColumn'
import {hasPerm} from '@/utils/authUtil'

type DialogState = 'add' | 'edit'
const queryParams = reactive<QueryUser>({
  asc: true,
  currentPage: 1,
  pageSize: 20,
  userId: undefined,
  userName: undefined,
  nikeName: undefined,
  email: undefined,
  status: undefined
})
const editorForm = reactive<UserAddForm & { userId?: IdType }>({
  userId: undefined,
  userName: '',
  nikeName: '',
  email: '',
  password: '',
  status: UserStatus.NORMAL,
  role: '',
  remark: ''
})
const ruleFormRef = ref<FormInstance>()
const open = ref(false)
const dialogState = ref<DialogState>('add')
const showSearch = ref(true)
const roles = reactive<RoleView[]>([])
const rolesLoading = ref(false)
const actionLoading = ref(false)
const submitting = ref(false)
const tableList = reactive<UserView[]>([])
const tableRef = ref<TableInstance>()
const total = ref(0)
const selectedIds = ref<IdType[]>([])
const {columns} = useColumn(['用户', '邮箱地址', '用户状态', '创建时间', '奖励分', '标记'])

const rules = {
  userName: [{required: true, message: '用户名不能为空', trigger: 'blur'}, {
    pattern: /^[a-zA-Z0-9_-]{4,16}$/,
    message: '用户名需为 4-16 位字母、数字、_ 或 -',
    trigger: 'blur'
  }],
  nikeName: [{max: 40, message: '昵称不能超过 40 个字符', trigger: 'blur'}],
  email: [{type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur'}],
  password: [{required: true, message: '初始密码不能为空', trigger: 'blur'}, {
    min: 8,
    message: '初始密码至少 8 位',
    trigger: 'blur'
  }],
  role: [{required: true, message: '请选择初始角色', trigger: 'change'}],
}

const activeFilterCount = computed(() => [queryParams.userId, queryParams.userName, queryParams.nikeName, queryParams.email, queryParams.status].filter((value) => value !== undefined && value !== '').length)
const canEditUser = computed(() => hasPerm('user:user:edit'))
const summaryStats = computed(() => [{label: '查询结果', value: total.value, tone: 'blue'}, {
  label: '当前页正常',
  value: tableList.filter((user) => user.status !== UserStatus.BANNED).length,
  tone: 'green'
}, {
  label: '当前页封禁',
  value: tableList.filter((user) => user.status === UserStatus.BANNED).length,
  tone: 'amber'
}, {label: '已选择', value: selectedIds.value.length, tone: 'violet'}])
const shortId = (value: IdType) => {
  const text = String(value);
  return text.length > 18 ? `${text.slice(0, 8)}…${text.slice(-6)}` : text
}
const userInitial = (user: UserView) => String(user.nikeName || user.userName || '?').slice(0, 1).toUpperCase()
const statusLabel = (status: UserStatus) => status === UserStatus.BANNED ? '已封禁' : '正常'
const formatDate = (value: Date | string | undefined) => value ? new Date(value).toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: '2-digit',
  day: '2-digit'
}) : '—'

const isLoading = ref(false)
let listRequestId = 0
const getList = async () => {
  const requestId = ++listRequestId
  isLoading.value = true
  try {
    const data = await getUser({...queryParams})
    if (requestId !== listRequestId) return
    tableList.length = 0
    tableList.push(...data.data)
    total.value = data.totalRecords
    selectedIds.value = []
    tableRef.value?.clearSelection()
  } catch {
    if (requestId === listRequestId) ElMessage.error('用户列表加载失败，请稍后重试')
  } finally {
    if (requestId === listRequestId) isLoading.value = false
  }
}
const handleSelectionChange = (selection: UserView[]) => {
  selectedIds.value = selection.map((user) => user.userId)
}
const resetQuery = () => {
  queryParams.currentPage = 1;
  queryParams.userId = undefined;
  queryParams.userName = undefined;
  queryParams.nikeName = undefined;
  queryParams.email = undefined;
  queryParams.status = undefined;
  queryParams.sortColumn = undefined;
  getList()
}
const handleQuery = () => {
  queryParams.currentPage = 1;
  getList()
}

const resetEditor = () => {
  editorForm.userId = undefined;
  editorForm.userName = '';
  editorForm.nikeName = '';
  editorForm.email = '';
  editorForm.password = '';
  editorForm.status = UserStatus.NORMAL;
  editorForm.role = roles[0]?.roleId || '';
  editorForm.remark = '';
  ruleFormRef.value?.clearValidate()
}
const handleAdd = () => {
  resetEditor();
  dialogState.value = 'add';
  open.value = true
}
const handleUpdate = (row?: UserView) => {
  const target = row || tableList.find((user) => String(user.userId) === String(selectedIds.value[0]));
  if (!target) {
    ElMessage.warning('请先选择一个用户');
    return
  }
  resetEditor();
  Object.assign(editorForm, target, {password: ''});
  dialogState.value = 'edit';
  open.value = true
}

const submitForm = async () => {
  if (!ruleFormRef.value) return
  const valid = await ruleFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (dialogState.value === 'add' && !roles.length) {
    ElMessage.warning('角色列表尚未加载完成，暂时无法创建用户')
    return
  }
  submitting.value = true
  try {
    if (dialogState.value === 'add') {
      await addUser({...editorForm})
    } else {
      const payload = {
        userId: editorForm.userId,
        nikeName: editorForm.nikeName,
        email: editorForm.email,
        status: editorForm.status,
        remark: editorForm.remark
      }
      await updateUser(payload)
    }
    ElMessage.success(dialogState.value === 'add' ? '用户已创建' : '用户已更新')
    finishDialog()
  } catch {
    ElMessage.error(dialogState.value === 'add' ? '用户创建失败，请稍后重试' : '用户更新失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
const finishDialog = () => {
  open.value = false;
  resetEditor();
  getList()
}
const isCancelled = (error: unknown) => error === 'cancel' || error === 'close'
const handleBan = async (row?: UserView) => {
  const ids = row ? [row.userId] : selectedIds.value;
  if (!ids.length) return;
  try {
    await ElMessageBox.confirm(row ? `确定封禁用户 @${row.userName} 吗？` : `确定封禁选中的 ${ids.length} 位用户吗？`, '封禁用户', {
      confirmButtonText: '确认封禁',
      cancelButtonText: '取消',
      type: 'warning'
    });
    actionLoading.value = true;
    await banUser(row ? ids[0] : ids);
    getList()
  } catch (error) {
    if (!isCancelled(error)) ElMessage.error('封禁操作失败，请稍后重试')
  } finally {
    actionLoading.value = false
  }
}
const handleUnban = async (row: UserView) => {
  try {
    await ElMessageBox.confirm(`确定解封用户 @${row.userName} 吗？`, '解封用户', {
      confirmButtonText: '确认解封',
      cancelButtonText: '取消',
      type: 'info'
    });
    actionLoading.value = true;
    await unbanUser(row.userId);
    getList()
  } catch (error) {
    if (!isCancelled(error)) ElMessage.error('解封操作失败，请稍后重试')
  } finally {
    actionLoading.value = false
  }
}
const handleResetPassword = async (row: UserView) => {
  try {
    await ElMessageBox.confirm(`将用户 @${row.userName} 的密码重置为系统默认值，继续吗？`, '重置密码', {
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
      type: 'warning'
    });
    actionLoading.value = true;
    await resetToDefault(row.userId)
  } catch (error) {
    if (!isCancelled(error)) ElMessage.error('密码重置失败，请稍后重试')
  } finally {
    actionLoading.value = false
  }
}
const handleCommand = (command: string, row: UserView) => {
  if (command === 'reset') handleResetPassword(row)
}
const cancel = () => {
  open.value = false;
  resetEditor()
}

const loadRoles = async () => {
  rolesLoading.value = true;
  try {
    const result = await getRole({currentPage: 1, pageSize: 200, asc: true});
    roles.splice(0, roles.length, ...result.data)
    if (!editorForm.role && roles[0]) editorForm.role = roles[0].roleId
  } catch {
    ElMessage.warning('角色列表加载失败，暂时无法创建用户')
  } finally {
    rolesLoading.value = false
  }
}
getList()
loadRoles()
</script>

<style lang="scss" scoped>
.user-panel {
  min-width: 0;
  padding: 22px 24px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 20px;
  background: var(--el-bg-color);
  box-shadow: 0 16px 40px rgb(15 23 42 / 4%);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 20px;
}

.panel-eyebrow {
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .14em;
}

.panel-heading h2 {
  margin: 7px 0 5px;
  font-size: 21px;
  letter-spacing: -.03em;
}

.panel-heading p {
  margin: 0;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.panel-heading__meta {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding-top: 5px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  white-space: nowrap;
}

.sync-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--el-color-success);
}

.sync-dot.is-loading {
  background: var(--el-color-warning);
  animation: pulse 1.1s ease-in-out infinite;
}

.filter-panel {
  min-width: 0;
  box-sizing: border-box;
  margin-bottom: 18px;
  padding: 14px 16px 4px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 13px;
  background: var(--el-bg-color-page);
}

.filter-panel__bar {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 11px;
}

.filter-title {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 7px;
  color: var(--el-text-color-primary);
  font-size: 13px;
}

.filter-title .el-icon {
  color: var(--el-color-primary);
}

.filter-title span {
  color: var(--el-text-color-secondary);
  font-size: 11px;
  font-weight: 400;
}

.user-filters {
  display: grid;
  width: 100%;
  min-width: 0;
  box-sizing: border-box;
  grid-template-columns: repeat(5, minmax(0, 1fr)) minmax(126px, max-content);
  align-items: end;
  gap: 0 14px;
}

.user-filters :deep(.el-form-item) {
  min-width: 0;
  margin-bottom: 10px;
}

.user-filters :deep(.el-form-item__label) {
  height: auto;
  margin-bottom: 5px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  line-height: 1.2;
}

.filter-select {
  width: 100%;
}

.filter-actions {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 7px;
  height: 68px;
  padding-bottom: 10px;
}

.filter-actions .el-button {
  margin: 0;
  white-space: nowrap;
}

.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  min-height: 36px;
  margin-bottom: 10px;
}

.list-toolbar__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.selection-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.selection-status .el-icon {
  color: var(--el-text-color-placeholder);
}

.selection-status.has-selection {
  color: var(--el-color-primary);
  font-weight: 650;
}

.selection-status.has-selection .el-icon {
  color: var(--el-color-primary);
}

.user-table {
  overflow: hidden;
  border-radius: 14px;
}

.user-table :deep(.el-table__header th.el-table__cell) {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 700;
  background: var(--el-fill-color-light);
}

.user-table :deep(.el-table__row td.el-table__cell) {
  height: 72px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.user-avatar {
  display: grid;
  width: 38px;
  height: 38px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 18%, transparent);
  border-radius: 12px;
  background: color-mix(in srgb, var(--el-color-primary) 12%, var(--el-bg-color));
  color: var(--el-color-primary);
  font-size: 15px;
  font-weight: 750;
}

.user-avatar.is-banned {
  border-color: color-mix(in srgb, var(--el-color-danger) 20%, transparent);
  background: color-mix(in srgb, var(--el-color-danger) 10%, var(--el-bg-color));
  color: var(--el-color-danger);
}

.user-cell__main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.user-cell__name {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 7px;
}

.user-cell__name strong {
  max-width: 170px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-cell__sub {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 6px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.username {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.separator {
  color: var(--el-text-color-placeholder);
}

.user-cell__sub span:last-child {
  overflow: hidden;
  font-family: var(--code-font-family, monospace);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mini-badge {
  flex: 0 0 auto;
  padding: 2px 6px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--el-color-danger) 10%, var(--el-bg-color));
  color: var(--el-color-danger);
  font-size: 10px;
  font-weight: 650;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 5px 9px;
  border-radius: 999px;
  font-size: 11px;
}

.status-pill i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-pill.is-normal {
  background: color-mix(in srgb, var(--el-color-success) 11%, var(--el-bg-color));
  color: var(--el-color-success);
}

.status-pill.is-banned {
  background: color-mix(in srgb, var(--el-color-danger) 10%, var(--el-bg-color));
  color: var(--el-color-danger);
}

.points-value {
  color: var(--el-text-color-primary);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.points-unit {
  margin-left: 4px;
  color: var(--el-text-color-secondary);
  font-size: 10px;
}

.dialog-intro {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 19px;
  padding: 14px 16px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 13px;
  background: var(--el-fill-color-light);
}

.dialog-intro__icon {
  display: grid;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 11px;
  background: color-mix(in srgb, #8b5cf6 13%, var(--el-bg-color));
  color: #8b5cf6;
  font-size: 18px;
}

.dialog-intro strong, .dialog-intro p {
  display: block;
}

.dialog-intro p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.editor-form {
  padding: 0 2px;
}

.form-section {
  padding: 18px 0 4px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.form-section:first-child {
  padding-top: 0;
  border-top: 0;
}

.section-title {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 15px;
}

.section-title > span {
  color: #8b5cf6;
  font: 700 11px/1.4 var(--code-font-family, monospace);
  letter-spacing: .08em;
}

.section-title strong, .section-title small {
  display: block;
}

.section-title strong {
  font-size: 14px;
}

.section-title small {
  margin-top: 3px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.full-width {
  width: 100%;
}

.status-options {
  min-height: 32px;
  align-items: center;
}

.editor-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.editor-form :deep(.el-form-item__label) {
  height: auto;
  margin-bottom: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.2;
}

@keyframes pulse {
  50% {
    opacity: .35;
  }
}

@media (max-width: 1050px) {
  .user-filters {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
  .filter-actions {
    height: auto;
    padding: 0 0 10px;
  }
}

@media (max-width: 760px) {
  .user-panel {
    padding: 18px 14px 8px;
  }
  .panel-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }
  .user-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .filter-actions {
    grid-column: 1 / -1;
  }
  .list-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
  .list-toolbar__left {
    width: 100%;
    justify-content: space-between;
  }
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
  .user-table :deep(.el-table__fixed-right) {
    display: none;
  }
}

@media (max-width: 480px) {
  .user-filters {
    grid-template-columns: 1fr;
  }
  .filter-actions {
    grid-column: auto;
  }
  .filter-actions .el-button {
    flex: 1;
  }
  .user-cell__name strong {
    max-width: 130px;
  }
  .user-table :deep(.el-table__row td.el-table__cell) {
    height: 64px;
  }
}
</style>
