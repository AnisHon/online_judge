<template>
  <ContestSubPageShell
    title="角色管理"
    kicker="USER / ROLES"
    description="把角色作为权限边界管理：先确认角色状态，再进入资源授权和成员分配。"
    :icon="UserFilled"
    tone="violet"
    :stats="summaryStats"
  >
    <template #actions>
      <el-button :icon="Refresh" :loading="isLoading" @click="getList">刷新角色</el-button>
      <el-button v-has="'user:role:list'" :icon="RefreshRight" :loading="cacheRefreshing" :disabled="cacheRefreshing" @click="handleRefresh">刷新缓存</el-button>
      <el-button v-has="'user:role:add'" type="primary" :icon="Plus" @click="handleAdd()">新增角色</el-button>
    </template>

    <section class="role-panel admin-role-surface">
      <div class="panel-heading">
        <div>
          <span class="panel-eyebrow">ACCESS DIRECTORY</span>
          <h2>角色目录</h2>
          <p>角色决定可进入的功能范围；具体资源授权和成员管理从每一行的快捷入口进入。</p>
        </div>
        <div class="panel-heading__meta">
          <span class="sync-dot" :class="{ 'is-loading': isLoading }"></span>
          <span>{{ isLoading ? '正在同步' : `共 ${total} 个角色` }}</span>
        </div>
      </div>

      <div class="filter-panel" :class="{ 'is-collapsed': !showSearch }">
        <div class="filter-panel__bar">
          <div class="filter-title">
            <el-icon><Filter /></el-icon>
            <strong>定位角色</strong>
            <span v-if="activeFilterCount">{{ activeFilterCount }} 项已启用</span>
          </div>
          <el-button link type="primary" @click="showSearch = !showSearch">{{ showSearch ? '收起筛选' : '展开筛选' }}</el-button>
        </div>
        <el-form v-show="showSearch" :model="queryParams" class="role-filters" label-position="top" @submit.prevent="handleQuery">
          <el-form-item label="角色名称" prop="roleName">
            <el-input v-model="queryParams.roleName" clearable :prefix-icon="UserFilled" placeholder="搜索角色名称" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="角色 ID" prop="roleId">
            <el-input v-model="queryParams.roleId" clearable :prefix-icon="Key" placeholder="输入完整或部分 ID" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="角色状态" prop="status">
            <el-select v-model="queryParams.status" clearable class="full-width" placeholder="全部状态">
              <el-option v-for="item in dict.roleStatus" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="queryParams.remark" clearable :prefix-icon="Memo" placeholder="搜索内部备注" @keyup.enter="handleQuery" />
          </el-form-item>
          <div class="filter-actions">
            <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
            <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          </div>
        </el-form>
      </div>

      <div class="list-toolbar">
        <div class="list-toolbar__left">
          <span class="selection-status" :class="{ 'has-selection': selectedIds.length }">
            <el-icon><Select /></el-icon>
            {{ selectedIds.length ? `已选择 ${selectedIds.length} 个角色` : '未选择角色' }}
          </span>
          <el-button v-has="'user:role:remove'" type="danger" plain :disabled="!selectedIds.length || actionLoading" :icon="Delete" @click="handleDelete()">批量删除</el-button>
        </div>
        <RightToolBar v-model:showSearch="showSearch" :columns="columns" @queryTable="getList" />
      </div>

      <el-table ref="tableRef" v-loading="isLoading" class="role-table" :data="tableList" row-key="roleId" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" align="center" />
        <el-table-column v-if="columns[0].visible" label="角色" min-width="290">
          <template #default="{ row }">
            <div class="role-cell">
              <span class="role-avatar" :class="`role-avatar--${statusTone(row.status)}`">{{ roleInitial(row.roleName) }}</span>
              <div class="role-cell__main">
                <div class="role-cell__title">
                  <strong :title="row.roleName">{{ row.roleName }}</strong>
                  <span class="status-pill" :class="`status-pill--${statusTone(row.status)}`"><i></i>{{ roleStatusText(row.status) }}</span>
                </div>
                <span class="role-cell__sub" :title="String(row.roleId)">ID {{ shortId(row.roleId) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="columns[1].visible" label="状态" width="120" align="center">
          <template #default="{ row }"><span class="status-text" :class="`status-text--${statusTone(row.status)}`">{{ roleStatusText(row.status) }}</span></template>
        </el-table-column>
        <el-table-column v-if="columns[2].visible" label="创建时间" min-width="170" prop="createTime" show-overflow-tooltip>
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column v-if="columns[3].visible" label="备注" min-width="220" prop="remark" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '暂无备注' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="290" fixed="right" align="right">
          <template #default="{ row }">
            <el-space :size="4">
              <el-button v-has="'user:role:edit'" link type="primary" :icon="EditPen" @click="handleUpdate(row)">编辑</el-button>
              <el-button v-has="'user:role:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
              <el-dropdown v-if="moreActions.length" trigger="click" @command="(command: MoreActionCommand) => handleCommand(command, row)">
                <el-button link type="info" :icon="MoreFilled">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="item in moreActions" :key="item.command" :command="item.command">
                      <el-icon><component :is="item.icon" /></el-icon>{{ item.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-space>
          </template>
        </el-table-column>
        <template #empty><el-empty description="没有匹配的角色" :image-size="84" /></template>
      </el-table>

      <Pagination v-show="total > 0" v-model:page="queryParams.currentPage" v-model:limit="queryParams.pageSize" :total="total" @pagination="getList" />
    </section>

    <el-dialog v-model="open" class="role-dialog" :title="dialogState === 'add' ? '新增角色' : '编辑角色'" width="min(620px, 92vw)" append-to-body destroy-on-close>
      <div class="dialog-intro">
        <span class="dialog-intro__icon"><el-icon><UserFilled /></el-icon></span>
        <div>
          <strong>{{ dialogState === 'add' ? '建立新的权限边界' : '调整角色基础信息' }}</strong>
          <p>角色保存后，再从“更多 · 资源权限”配置菜单和按钮权限。</p>
        </div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="editor-form" label-position="top">
        <section class="form-section">
          <div class="section-title"><span>01</span><div><strong>角色身份</strong><small>名称用于识别角色，角色 ID 由系统生成</small></div></div>
          <el-form-item label="角色名称" prop="roleName"><el-input v-model="form.roleName" :prefix-icon="UserFilled" maxlength="60" show-word-limit placeholder="例如：教师、内容审核员" /></el-form-item>
          <el-form-item v-if="dialogState === 'edit'" label="角色 ID"><el-input :model-value="String(form.roleId || '')" disabled :prefix-icon="Key" /></el-form-item>
        </section>
        <section class="form-section">
          <div class="section-title"><span>02</span><div><strong>状态与说明</strong><small>停用角色后应同步检查已有成员的访问影响</small></div></div>
          <el-form-item label="角色状态" prop="status"><el-radio-group v-model="form.status" class="status-options"><el-radio v-for="item in dict.roleStatus" :key="item.value" :label="item.value" :value="item.value">{{ item.label }}</el-radio></el-radio-group></el-form-item>
          <el-form-item label="内部备注" prop="remark"><el-input v-model="form.remark" type="textarea" :rows="4" maxlength="450" show-word-limit placeholder="记录角色用途、适用范围或维护说明" /></el-form-item>
        </section>
      </el-form>
      <template #footer><el-button @click="cancel">取消</el-button><el-button type="primary" :loading="submitting" :disabled="submitting" @click="submitForm">{{ dialogState === 'add' ? '创建角色' : '保存修改' }}</el-button></template>
    </el-dialog>

    <el-dialog v-model="openDataScope" class="permission-dialog" title="配置资源权限" width="min(680px, 92vw)" append-to-body destroy-on-close>
      <div class="permission-dialog__head">
        <div><span class="panel-eyebrow">ROLE / ACCESS</span><h3>{{ permissionRoleName || '当前角色' }}</h3><p>勾选角色可访问的菜单、菜单项和按钮权限。当前采用独立勾选，不会自动替换父子节点。</p></div>
        <span class="permission-count">已选 {{ checkedPermissionCount }} 项</span>
      </div>
      <div v-loading="loadingRole" class="permission-tree-wrap">
        <el-tree ref="treeRef" :data="menuTree" node-key="id" show-checkbox check-strictly :expand-on-click-node="false" :default-expand-all="false" :props="treeProps" empty-text="暂无资源权限" @check="handlePermissionCheck" />
      </div>
      <template #footer><el-button :disabled="permissionSaving" @click="cancelMenu">取消</el-button><el-button type="primary" :loading="permissionSaving" :disabled="loadingRole || !canManageScope" @click="submitMenu">保存权限</el-button></template>
    </el-dialog>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, reactive, ref, type Component } from 'vue'
import { Delete, EditPen, Filter, Key, Lock, Memo, MoreFilled, Plus, Refresh, RefreshRight, Search, Select, User, UserFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElTree, type FormInstance, type TableInstance } from 'element-plus'
import ContestSubPageShell from '@/views/backend/teacher/contest-manage/component/ContestSubPageShell.vue'
import RightToolBar from '@/components/right-toolbar/RightToolBar.vue'
import Pagination from '@/components/pageination/Pagination.vue'
import { addRole, dict, getRole as fetchRoles, refreshRoleCache, removeRole, RoleStatus, type QueryRole, type RoleForm, type RoleView, updateRole } from '@/api/role'
import { grant as grantMenu, listRoleMenu, revoke as revokeMenu, type MenuRoleRelation, type TreedMenu } from '@/api/auth/menu'
import { getAllTreedMenu } from '@/api/menu'
import { setTreeId } from '@/utils/menu'
import { useColumn } from '@/hooks/useColumn'
import type { IdType } from '@/api/common'
import { useRouter } from 'vue-router'
import { hasPerm } from '@/utils/authUtil'
import type { TreeOptionProps } from 'element-plus/es/components/tree/src/tree.type'
import type { TreeNodeData } from 'element-plus/lib/components/tree/src/tree.type'

type DialogState = 'add' | 'edit'

const queryParams = reactive<QueryRole>({ asc: true, currentPage: 1, pageSize: 20, roleId: undefined, roleName: undefined, status: undefined, remark: undefined })
const form = reactive<RoleForm>({ roleId: undefined, roleName: '', status: RoleStatus.NORMAL, remark: '' })
const formRef = ref<FormInstance>()
const open = ref(false)
const openDataScope = ref(false)
const dialogState = ref<DialogState>('add')
const showSearch = ref(true)
const actionLoading = ref(false)
const cacheRefreshing = ref(false)
const submitting = ref(false)
const tableList = reactive<RoleView[]>([])
const tableRef = ref<TableInstance>()
const total = ref(0)
const selectedIds = ref<IdType[]>([])
const { columns } = useColumn(['角色', '状态', '创建时间', '备注'])
const router = useRouter()

const rules = {
  roleName: [{ required: true, message: '角色名称不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '请选择角色状态', trigger: 'change' }],
}

const activeFilterCount = computed(() => [queryParams.roleId, queryParams.roleName, queryParams.status, queryParams.remark].filter(value => value !== undefined && value !== '').length)
const canReadMenu = computed(() => hasPerm('user:menu:list'))
const canGrantMenu = computed(() => hasPerm('user:menu:grant'))
const canRevokeMenu = computed(() => hasPerm('user:menu:revoke'))
const canManageScope = computed(() => canReadMenu.value && (canGrantMenu.value || canRevokeMenu.value))
const canListUsers = computed(() => hasPerm('user:user:list'))
type MoreActionCommand = 'handleDataScope' | 'handleAuthUser'
const moreActions = computed<Array<{ command: MoreActionCommand; label: string; icon: Component }>>(() => {
  const actions: Array<{ command: MoreActionCommand; label: string; icon: Component }> = []
  if (canManageScope.value) actions.push({ command: 'handleDataScope', label: '资源权限', icon: Lock })
  if (canListUsers.value) actions.push({ command: 'handleAuthUser', label: '分配成员', icon: User })
  return actions
})
const summaryStats = computed(() => [
  { label: '角色总数', value: total.value, tone: 'blue' },
  { label: '当前页正常', value: tableList.filter(role => role.status === RoleStatus.NORMAL).length, tone: 'green' },
  { label: '当前页停用', value: tableList.filter(role => role.status === RoleStatus.SUSPEND).length, tone: 'amber' },
  { label: '已选择', value: selectedIds.value.length, tone: 'violet' },
])

const isLoading = ref(false)
let listRequestId = 0
const getList = async (): Promise<boolean> => {
  const requestId = ++listRequestId
  isLoading.value = true
  try {
    const data = await fetchRoles({...queryParams})
    if (requestId !== listRequestId) return false
    tableList.splice(0, tableList.length, ...data.data)
    total.value = data.totalRecords
    selectedIds.value = []
    tableRef.value?.clearSelection()
    return true
  } catch {
    if (requestId === listRequestId) ElMessage.error('角色列表加载失败，请稍后重试')
    return false
  } finally {
    if (requestId === listRequestId) isLoading.value = false
  }
}

function roleStatusText(status: RoleStatus) { return status === RoleStatus.NORMAL ? '正常' : '停用' }
function statusTone(status: RoleStatus) { return status === RoleStatus.NORMAL ? 'green' : 'amber' }
function roleInitial(name?: string) { return String(name || '角').slice(0, 1) }
function shortId(value: IdType) { const text = String(value); return text.length > 18 ? `${text.slice(0, 8)}…${text.slice(-6)}` : text }
function formatDate(value: Date | string | undefined) { return value ? new Date(value).toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit' }) : '—' }

const handleSelectionChange = (selection: RoleView[]) => { selectedIds.value = selection.map(role => role.roleId) }
const handleQuery = () => { queryParams.currentPage = 1; getList() }
const resetQuery = () => { queryParams.currentPage = 1; queryParams.roleId = undefined; queryParams.roleName = undefined; queryParams.status = undefined; queryParams.remark = undefined; queryParams.sortColumn = undefined; getList() }
const handleRefresh = async () => {
  if (cacheRefreshing.value) return
  cacheRefreshing.value = true
  try {
    await refreshRoleCache()
    const listLoaded = await getList()
    if (listLoaded) ElMessage.success('角色缓存和列表已刷新')
    else ElMessage.warning('角色缓存已刷新，但角色列表加载失败')
  } catch {
    ElMessage.error('角色缓存刷新失败，角色列表未刷新')
  } finally {
    cacheRefreshing.value = false
  }
}

const resetForm = () => { form.roleId = undefined; form.roleName = ''; form.status = RoleStatus.NORMAL; form.remark = ''; formRef.value?.clearValidate() }
const handleAdd = () => { resetForm(); dialogState.value = 'add'; open.value = true }
const handleUpdate = (row?: RoleView) => {
  const target = row || tableList.find(role => String(role.roleId) === String(selectedIds.value[0]))
  if (!target) { ElMessage.warning('请先选择一个角色'); return }
  resetForm()
  Object.assign(form, target)
  dialogState.value = 'edit'
  open.value = true
}

const submitForm = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (dialogState.value === 'add') await addRole({...form})
    else await updateRole({...form})
    ElMessage.success(dialogState.value === 'add' ? '角色已创建' : '角色已更新')
    finishDialog()
  } catch {
    ElMessage.error(dialogState.value === 'add' ? '角色创建失败，请稍后重试' : '角色更新失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
function finishDialog() { open.value = false; resetForm(); getList() }
const cancel = () => { open.value = false; resetForm() }

const handleDelete = async (row?: RoleView) => {
  const ids = row ? [row.roleId] : selectedIds.value
  if (!ids.length) { ElMessage.warning('请先选择要删除的角色'); return }
  const message = row ? `确定删除角色“${row.roleName}”吗？删除后角色授权关系也将失效。` : `确定删除选中的 ${ids.length} 个角色吗？删除后角色授权关系也将失效。`
  try {
    await ElMessageBox.confirm(message, '删除角色', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' })
    actionLoading.value = true
    await removeRole(ids.length === 1 ? ids[0] : ids)
    ElMessage.success('角色已删除')
    selectedIds.value = []
    getList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败，请稍后重试')
  } finally { actionLoading.value = false }
}

const handleCommand = (command: MoreActionCommand, row: RoleView) => {
  if (command === 'handleDataScope') handleMenu(row)
  if (command === 'handleAuthUser') handleAuthUser(row)
}
const handleAuthUser = (row: RoleView) => { router.push({ name: 'role-auth', params: { id: row.roleId } }) }

const menuTree = reactive<TreedMenu[]>([])
const treeRef = ref<InstanceType<typeof ElTree>>()
const loadingRole = ref(false)
const permissionRoleId = ref<IdType>()
const permissionRoleName = ref('')
const originalPermissionIds = ref<IdType[]>([])
const checkedPermissionIds = ref<IdType[]>([])
const permissionSaving = ref(false)
let permissionSession = 0

const treeProps = reactive<TreeOptionProps>({
  children: 'children',
  // @ts-ignore Element Plus 的 TreeNodeData 未暴露业务树节点的 menu 字段
  label: (data: TreeNodeData): string => String((data as TreedMenu).menu.menuName),
})
const checkedPermissionCount = computed(() => checkedPermissionIds.value.length)

function menuTypeRank(type: string) { return type === 'M' ? 0 : type === 'I' ? 1 : 2 }
function sortMenuTree(nodes: TreedMenu[]): TreedMenu[] {
  return [...nodes].sort((a, b) => menuTypeRank(a.menu.menuType) - menuTypeRank(b.menu.menuType) || (a.menu.orderNum ?? 0) - (b.menu.orderNum ?? 0) || String(a.menu.menuName).localeCompare(String(b.menu.menuName), 'zh-CN')).map(node => ({ ...node, children: sortMenuTree(node.children || []) }))
}
const sameId = (left: IdType, right: IdType) => String(left) === String(right)

const isPermissionSessionActive = (session: number, roleId?: IdType) => {
  if (session !== permissionSession || !openDataScope.value) return false
  return roleId === undefined || (permissionRoleId.value !== undefined && sameId(permissionRoleId.value, roleId))
}

const restorePermissionSnapshot = async (ids: IdType[], session: number, roleId?: IdType) => {
  await nextTick()
  if (!isPermissionSessionActive(session, roleId)) return
  treeRef.value?.setCheckedKeys(ids, false)
  checkedPermissionIds.value = [...ids]
}

async function loadMenuTree(session: number, roleId: IdType) {
  const data = await getAllTreedMenu()
  if (!isPermissionSessionActive(session, roleId)) return false
  setTreeId(data)
  menuTree.splice(0, menuTree.length, ...sortMenuTree(data))
  return true
}

const handlePermissionCheck = (_data: unknown, state: { checkedKeys: Array<IdType | number | string> }) => {
  const current = (state.checkedKeys || []).map(String)
  const removed = originalPermissionIds.value.some(id => !current.some(item => sameId(item, id)))
  const added = current.some(id => !originalPermissionIds.value.some(item => sameId(item, id)))
  if ((removed && !canRevokeMenu.value) || (added && !canGrantMenu.value)) {
    void restorePermissionSnapshot(originalPermissionIds.value, permissionSession)
    ElMessage.warning(removed && !canRevokeMenu.value ? '当前账号没有撤销权限' : '当前账号没有授予权限')
    return
  }
  checkedPermissionIds.value = [...current]
}

const loadRolePermissions = async (roleId: IdType, session: number) => {
  const data = await listRoleMenu(roleId)
  if (!isPermissionSessionActive(session, roleId)) return
  const ids = data.map(item => item.menuId)
  originalPermissionIds.value = [...ids]
  await restorePermissionSnapshot(ids, session, roleId)
}

const handleMenu = async (row: RoleView) => {
  if (!canManageScope.value) return
  const session = ++permissionSession
  permissionRoleId.value = row.roleId
  permissionRoleName.value = row.roleName
  openDataScope.value = true
  loadingRole.value = true
  checkedPermissionIds.value = []
  try {
    if (!await loadMenuTree(session, row.roleId)) return
    await loadRolePermissions(row.roleId, session)
  } catch { if (session === permissionSession) ElMessage.error('角色权限加载失败，请稍后重试') } finally { if (session === permissionSession) loadingRole.value = false }
}

const submitMenu = async () => {
  const roleId = permissionRoleId.value
  if (!roleId || !canManageScope.value || loadingRole.value || permissionSaving.value) return
  const current = (treeRef.value?.getCheckedKeys() || []) as IdType[]
  const original = [...originalPermissionIds.value]
  const removed = original.filter(id => !current.some(item => sameId(item, id)))
  const added = current.filter(id => !original.some(item => sameId(item, id)))
  if (removed.length && !canRevokeMenu.value) { ElMessage.warning('当前账号没有撤销权限'); await restorePermissionSnapshot(original, permissionSession); return }
  if (added.length && !canGrantMenu.value) { ElMessage.warning('当前账号没有授予权限'); await restorePermissionSnapshot(original, permissionSession); return }
  if (!removed.length && !added.length) { ElMessage.info('权限没有变化'); cancelMenu(); return }
  const requests: Promise<void>[] = []
  if (removed.length) requests.push(revokeMenu(removed.map(menuId => ({ roleId, menuId }))))
  if (added.length) requests.push(grantMenu(added.map(menuId => ({ roleId, menuId }))))
  const session = permissionSession
  permissionSaving.value = true
  try {
    const results = await Promise.allSettled(requests)
    if (!isPermissionSessionActive(session, roleId)) return
    if (results.some(result => result.status === 'rejected')) {
      ElMessage.error('权限保存未完成，请刷新后确认当前授权状态')
      await loadRolePermissions(roleId, session)
      return
    }
    ElMessage.success('角色权限已更新')
    cancelMenu()
  } finally {
    permissionSaving.value = false
  }
}
const cancelMenu = () => {
  permissionSession += 1
  openDataScope.value = false
  permissionRoleId.value = undefined
  permissionRoleName.value = ''
  originalPermissionIds.value = []
  checkedPermissionIds.value = []
}

onBeforeUnmount(() => {
  permissionSession += 1
})

getList()
</script>

<style lang="scss" scoped>
.role-panel { min-width: 0; padding: 22px 24px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 16px 40px rgb(15 23 42 / 4%); }
.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 20px; }.panel-eyebrow { color: #8b5cf6; font-size: 11px; font-weight: 800; letter-spacing: .14em; }.panel-heading h2 { margin: 7px 0 5px; font-size: 21px; letter-spacing: -.03em; }.panel-heading p { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }.panel-heading__meta { display: inline-flex; align-items: center; gap: 7px; padding-top: 5px; color: var(--el-text-color-secondary); font-size: 12px; white-space: nowrap; }.sync-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--el-color-success); }.sync-dot.is-loading { background: var(--el-color-warning); animation: pulse 1.1s ease-in-out infinite; }
.filter-panel { margin-bottom: 18px; padding: 14px 16px 4px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-bg-color-page); }.filter-panel__bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 11px; }.filter-title { display: flex; align-items: center; gap: 7px; font-size: 13px; }.filter-title .el-icon { color: #8b5cf6; }.filter-title span { color: var(--el-text-color-secondary); font-size: 11px; font-weight: 400; }.role-filters { display: grid; grid-template-columns: repeat(4, minmax(140px, 1fr)) auto; align-items: end; gap: 0 14px; }.role-filters :deep(.el-form-item) { min-width: 0; margin-bottom: 10px; }.role-filters :deep(.el-form-item__label) { height: auto; margin-bottom: 5px; color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.2; }.full-width { width: 100%; }.filter-actions { display: flex; align-items: flex-end; gap: 7px; height: 68px; padding-bottom: 10px; }
.list-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 15px; min-height: 36px; margin-bottom: 10px; }.list-toolbar__left { display: flex; align-items: center; gap: 12px; }.selection-status { display: inline-flex; align-items: center; gap: 6px; color: var(--el-text-color-secondary); font-size: 12px; }.selection-status .el-icon { color: var(--el-text-color-placeholder); }.selection-status.has-selection { color: #8b5cf6; font-weight: 650; }.selection-status.has-selection .el-icon { color: #8b5cf6; }
.role-table { overflow: hidden; border-radius: 14px; }.role-table :deep(.el-table__header th.el-table__cell) { color: var(--el-text-color-secondary); font-size: 12px; font-weight: 700; background: var(--el-fill-color-light); }.role-table :deep(.el-table__row td.el-table__cell) { height: 70px; }.role-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }.role-avatar { display: grid; width: 38px; height: 38px; flex: 0 0 auto; place-items: center; border-radius: 12px; font-size: 15px; font-weight: 800; }.role-avatar--green { color: #059669; background: rgb(5 150 105 / 13%); }.role-avatar--amber { color: #d97706; background: rgb(217 119 6 / 13%); }.role-cell__main { display: flex; min-width: 0; flex-direction: column; gap: 5px; }.role-cell__title { display: flex; align-items: center; min-width: 0; gap: 8px; }.role-cell__title strong { max-width: 210px; overflow: hidden; font-size: 14px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }.role-cell__sub { color: var(--el-text-color-secondary); font: 11px var(--code-font-family, monospace); }.status-pill { display: inline-flex; flex: 0 0 auto; align-items: center; gap: 4px; padding: 2px 7px; border-radius: 999px; font-size: 10px; }.status-pill i { width: 5px; height: 5px; border-radius: 50%; }.status-pill--green { color: #059669; background: rgb(5 150 105 / 11%); }.status-pill--green i { background: #10b981; }.status-pill--amber { color: #d97706; background: rgb(217 119 6 / 11%); }.status-pill--amber i { background: #f59e0b; }.status-text { font-size: 12px; font-weight: 650; }.status-text--green { color: #059669; }.status-text--amber { color: #d97706; }
.dialog-intro { display: flex; align-items: center; gap: 12px; margin-bottom: 19px; padding: 14px 16px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-fill-color-light); }.dialog-intro__icon { display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center; border-radius: 11px; background: rgb(139 92 246 / 13%); color: #8b5cf6; font-size: 18px; }.dialog-intro strong, .dialog-intro p { display: block; }.dialog-intro p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }.editor-form :deep(.el-form-item) { margin-bottom: 16px; }.editor-form :deep(.el-form-item__label) { height: auto; margin-bottom: 6px; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.2; }.form-section { padding: 18px 0 4px; border-top: 1px solid var(--el-border-color-lighter); }.form-section:first-child { padding-top: 0; border-top: 0; }.section-title { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 15px; }.section-title > span { color: #8b5cf6; font: 700 11px/1.4 var(--code-font-family, monospace); letter-spacing: .08em; }.section-title strong, .section-title small { display: block; }.section-title small { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }.status-options { min-height: 32px; align-items: center; }
.permission-dialog__head { display: flex; align-items: flex-start; justify-content: space-between; gap: 18px; margin-bottom: 16px; }.permission-dialog__head h3 { margin: 6px 0 4px; font-size: 18px; }.permission-dialog__head p { max-width: 500px; margin: 0; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.6; }.permission-count { flex: 0 0 auto; padding: 6px 9px; border-radius: 8px; background: rgb(139 92 246 / 11%); color: #8b5cf6; font: 11px var(--code-font-family, monospace); }.permission-tree-wrap { min-height: 260px; max-height: 52vh; overflow: auto; padding: 12px 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-bg-color-page); }.permission-tree-wrap :deep(.el-tree) { background: transparent; color: var(--el-text-color-primary); }.permission-tree-wrap :deep(.el-tree-node__content) { min-height: 36px; border-radius: 8px; }.permission-tree-wrap :deep(.el-tree-node__content:hover) { background: var(--el-fill-color-light); }.permission-tree-wrap :deep(.el-tree-node__expand-icon) { color: var(--el-text-color-secondary); }.permission-tree-wrap :deep(.el-tree-node__expand-icon.expanded) { color: #8b5cf6; }
@keyframes pulse { 50% { opacity: .35; } }
@media (max-width: 900px) { .role-filters { grid-template-columns: repeat(3, minmax(140px, 1fr)); }.filter-actions { height: auto; padding-bottom: 10px; } }
@media (max-width: 680px) { .role-panel { padding: 18px 14px 8px; }.panel-heading { align-items: flex-start; flex-direction: column; gap: 10px; }.role-filters { grid-template-columns: repeat(2, minmax(0, 1fr)); }.filter-actions { grid-column: 1 / -1; }.list-toolbar { align-items: flex-start; flex-direction: column; }.permission-dialog__head { flex-direction: column; }.permission-count { align-self: flex-start; } }
@media (max-width: 440px) { .role-filters { grid-template-columns: 1fr; }.filter-actions { grid-column: auto; }.filter-actions .el-button { flex: 1; }.role-cell__title { align-items: flex-start; flex-direction: column; gap: 4px; }.role-table :deep(.el-table__fixed-right) { display: none; } }
</style>
