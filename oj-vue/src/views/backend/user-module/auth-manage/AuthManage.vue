<template>
  <ContestSubPageShell
      title="权限管理"
      kicker="USER / AUTHORIZATION"
      description="以菜单资源树维护后台导航、权限标识和路由入口，父子关系一目了然。"
      :icon="Key"
      tone="violet"
      :stats="summaryStats"
  >
    <template #actions>
      <el-button v-has="'user:menu:list'" :icon="Refresh" :loading="isLoading" @click="getList">刷新资源树</el-button>
      <el-button v-has="'user:menu:add'" type="primary" :icon="Plus" @click="handleAdd()">新增资源</el-button>
    </template>

    <section v-if="canReadResource" class="auth-panel">
      <div class="panel-heading">
        <div>
          <span class="panel-eyebrow">RESOURCE HIERARCHY</span>
          <h2>菜单资源树</h2>
          <p>菜单栏、菜单项和按钮按父子关系组织；权限标识是角色授权的实际依据。</p>
        </div>
        <div class="panel-heading__meta"><span class="sync-dot" :class="{ 'is-loading': isLoading }"></span><span>{{
            isLoading ? '正在同步' : `共 ${allMenus.length} 个资源`
          }}</span></div>
      </div>

      <div class="filter-panel">
        <div class="filter-panel__bar">
          <div class="filter-title">
            <el-icon>
              <Filter/>
            </el-icon>
            <strong>定位资源</strong><span v-if="activeFilterCount">{{ activeFilterCount }} 项已启用</span></div>
          <div class="tree-actions">
            <el-button link type="primary" :icon="expandedAll ? Fold : Expand" @click="toggleExpand">
              {{ expandedAll ? '收起全部' : '展开全部' }}
            </el-button>
            <el-button link @click="clearFilters">清除筛选</el-button>
          </div>
        </div>
        <el-form :model="queryParams" class="auth-filters" label-position="top" @submit.prevent="handleQuery">
          <el-form-item label="资源名称" prop="menuName">
            <el-input v-model="queryParams.menuName" clearable :prefix-icon="Search" placeholder="搜索菜单名称"
                      @keyup.enter="handleQuery"/>
          </el-form-item>
          <el-form-item label="权限标识" prop="perms">
            <el-input v-model="queryParams.perms" clearable :prefix-icon="Lock" placeholder="例如 user:user:list"
                      @keyup.enter="handleQuery"/>
          </el-form-item>
          <el-form-item label="资源类型" prop="menuType">
            <el-select v-model="queryParams.menuType" clearable class="full-width" placeholder="全部类型">
              <el-option v-for="item in dict.menuType" :key="item.value" :label="item.label" :value="item.value"/>
            </el-select>
          </el-form-item>
          <el-form-item label="资源 ID" prop="menuId">
            <el-input v-model="queryParams.menuId" clearable :prefix-icon="Key" placeholder="输入 ID"
                      @keyup.enter="handleQuery"/>
          </el-form-item>
          <div class="filter-actions">
            <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
            <el-button :icon="Refresh" @click="clearFilters">重置</el-button>
          </div>
        </el-form>
      </div>

      <div class="list-toolbar">
        <div class="list-toolbar__left"><span class="selection-status" :class="{ 'has-selection': selectedIds.length }"><el-icon><Select/></el-icon>{{
            selectedIds.length ? `已选择 ${selectedIds.length} 个资源` : '未选择资源'
          }}</span>
          <el-button v-has="'user:menu:remove'" type="danger" plain :disabled="!selectedIds.length || actionLoading"
                     :icon="Delete" @click="handleDelete()">批量删除
          </el-button>
        </div>
        <span class="tree-caption">{{ filteredCount }} 个匹配资源 · 树形结果会保留父级路径</span>
      </div>

      <el-table ref="tableRef" v-loading="isLoading" class="auth-table" :data="filteredTree" row-key="id" :indent="0"
                :tree-props="{ children: 'children' }"
                @selection-change="handleSelectionChange" @expand-change="handleExpandChange">
        <el-table-column type="selection" width="52" align="center"/>
        <el-table-column v-if="columns[0].visible" class-name="resource-column" label="资源" min-width="320">
          <template #default="{ row }">
            <div class="resource-cell" :style="resourceCellStyle(row)"><span class="resource-icon"
                                                                             :class="`resource-icon--${typeTone(row.menu.menuType)}`"><el-icon
                v-if="resolveIcon(row.menu.icon)"><component :is="resolveIcon(row.menu.icon)"/></el-icon><span
                v-else>—</span></span>
              <div class="resource-cell__main">
                <div class="resource-cell__title"><strong :title="row.menu.menuName">{{
                    row.menu.menuName
                  }}</strong><span class="type-pill" :class="`type-pill--${typeTone(row.menu.menuType)}`">{{
                    typeLabel(row.menu.menuType)
                  }}</span></div>
                <span class="resource-cell__sub" :title="String(row.menu.menuId)">ID {{
                    shortId(row.menu.menuId)
                  }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="columns[1].visible" label="权限标识" min-width="235" prop="menu.perms"
                         show-overflow-tooltip>
          <template #default="{ row }"><code v-if="row.menu.perms">{{ row.menu.perms }}</code><span v-else
                                                                                                    class="muted-text">菜单容器</span>
          </template>
        </el-table-column>
        <el-table-column v-if="columns[2].visible" label="路由 / 组件" min-width="220">
          <template #default="{ row }">
            <div class="route-cell"><span v-if="row.menu.router" :title="row.menu.router">{{
                row.menu.router
              }}</span><small v-if="row.menu.component" :title="row.menu.component">{{
                row.menu.component
              }}</small><span v-if="!row.menu.router && !row.menu.component" class="muted-text">—</span></div>
          </template>
        </el-table-column>
        <el-table-column v-if="columns[3].visible" label="顺序" width="82" align="center" prop="menu.orderNum"/>
        <el-table-column v-if="columns[4].visible" label="创建时间" width="145" prop="menu.createTime"
                         show-overflow-tooltip>
          <template #default="{ row }">{{ formatDate(row.menu.createTime) }}</template>
        </el-table-column>
        <el-table-column v-if="columns[5].visible" label="备注" min-width="170" prop="menu.remark"
                         show-overflow-tooltip>
          <template #default="{ row }">{{ row.menu.remark || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="185" fixed="right" align="right">
          <template #default="{ row }">
            <el-space :size="5">
              <el-button v-has="'user:menu:add'" link type="success" :icon="Plus" @click="handleAdd(row)">下级
              </el-button>
              <el-button v-has="'user:menu:edit'" link type="primary" :icon="EditPen" @click="handleUpdate(row)">编辑
              </el-button>
              <el-button v-has="'user:menu:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">删除
              </el-button>
            </el-space>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="没有匹配的资源" :image-size="84"/>
        </template>
      </el-table>
    </section>

    <el-empty v-else description="你没有查看权限资源的权限" :image-size="96" />

    <el-dialog v-model="open" class="auth-dialog" :title="dialogState === 'add' ? '新增权限资源' : '编辑权限资源'"
               width="min(760px, 92vw)" append-to-body destroy-on-close>
      <div class="dialog-intro"><span class="dialog-intro__icon"><el-icon><Key/></el-icon></span>
        <div><strong>{{ dialogState === 'add' ? '建立菜单资源' : '调整资源配置' }}</strong>
          <p>菜单类型决定它在导航和授权中的角色，按钮资源需要配置权限标识。</p></div>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" class="editor-form" label-position="top">
        <section class="form-section">
          <div class="section-title"><span>01</span>
            <div><strong>层级与类型</strong><small>先确定资源挂载位置，再配置展示和授权信息</small></div>
          </div>
          <div class="form-grid">
            <el-form-item v-if="dialogState === 'edit'" label="资源 ID">
              <el-input :model-value="String(form.menuId || '')" disabled :prefix-icon="Key"/>
            </el-form-item>
            <el-form-item label="父级资源" prop="parentId">
              <el-tree-select v-model="form.parentId" class="full-width" :data="parentOptions" node-key="id"
                              check-strictly filterable :render-after-expand="false"
                              placeholder="选择父级资源，留空为根节点"/>
            </el-form-item>
            <el-form-item label="资源类型" prop="menuType">
              <el-radio-group v-model="form.menuType" class="type-options">
                <el-radio v-for="item in dict.menuType" :key="item.value" :label="item.value" :value="item.value">
                  {{ item.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="显示顺序" prop="orderNum">
              <el-input-number v-model="form.orderNum" class="full-width" :min="0" :controls="false"
                               placeholder="数字越小越靠前"/>
            </el-form-item>
          </div>
        </section>
        <section class="form-section">
          <div class="section-title"><span>02</span>
            <div><strong>展示信息</strong><small>名称和图标会出现在后台导航中，按钮资源不需要图标</small></div>
          </div>
          <div class="form-grid">
            <el-form-item label="资源名称" prop="menuName">
              <el-input v-model="form.menuName" maxlength="80" show-word-limit placeholder="例如：用户管理"/>
            </el-form-item>
          </div>
          <el-form-item v-if="form.menuType !== MenuType.BUTTON" class="icon-form-item" label="菜单图标" prop="icon">
            <IconPicker v-model="form.icon"/>
          </el-form-item>
        </section>
        <section class="form-section">
          <div class="section-title"><span>03</span>
            <div><strong>路由与授权</strong><small>路由供菜单跳转使用，组件决定页面实现，权限标识供角色授权使用</small>
            </div>
          </div>
          <div class="form-grid">
            <el-form-item v-if="form.menuType !== MenuType.BUTTON" label="路由地址" prop="router">
              <el-input v-model="form.router" maxlength="180" placeholder="例如：user-module/user-manage"/>
            </el-form-item>
            <el-form-item v-if="form.menuType !== MenuType.BUTTON" label="组件路径" prop="component">
              <el-input v-model="form.component" maxlength="220"
                        placeholder="例如：backend/user-module/user-manage/UserManage"/>
            </el-form-item>
            <el-form-item v-if="form.menuType !== MenuType.MENU" label="权限标识" prop="perms">
              <el-input v-model="form.perms" maxlength="80" placeholder="例如：user:user:list"/>
            </el-form-item>
          </div>
          <el-form-item label="内部备注" prop="remark">
            <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="450" show-word-limit
                      placeholder="记录资源用途、授权范围或维护说明"/>
          </el-form-item>
        </section>
      </el-form>
      <template #footer>
        <el-button @click="cancel">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="submitting" @click="submitForm">
          {{ dialogState === 'add' ? '创建资源' : '保存修改' }}
        </el-button>
      </template>
    </el-dialog>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import {computed, nextTick, onBeforeUnmount, reactive, ref, watch} from 'vue'
import {Delete, EditPen, Expand, Filter, Fold, Key, Lock, Plus, Refresh, Search, Select} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox, type FormInstance, type TableInstance} from 'element-plus'
import ContestSubPageShell from '@/views/backend/teacher/contest-manage/component/ContestSubPageShell.vue'
import IconPicker from '@/components/IconPicker/IconPicker.vue'
import {addMenu, dict, getAllTreedMenu, type QueryMenu, removeMenu, updateMenu} from '@/api/menu'
import {MenuType, type MenuForm, type TreedMenu} from '@/api/auth/menu'
import type {IdType} from '@/api/common'
import {useColumn} from '@/hooks/useColumn'
import {setTreeId} from '@/utils/menu'
import {hasPerm} from '@/utils/authUtil'
import {useMenuStore} from '@/stores/useMenuStore'

type DialogState = 'add' | 'edit'
type TreeOption = { id: IdType | string; label: string; children?: TreeOption[] }
type AuthTreeNode = Omit<TreedMenu, 'children'> & { depth: number; children: AuthTreeNode[] }
const queryParams = reactive<QueryMenu>({
  asc: true,
  currentPage: 1,
  pageSize: 20,
  menuId: undefined,
  menuName: undefined,
  menuType: undefined,
  perms: undefined
})
const form = reactive<MenuForm>({
  menuId: undefined,
  menuName: '',
  menuType: MenuType.MENU,
  parentId: '0',
  icon: '#',
  perms: '',
  router: '',
  component: '',
  orderNum: 0,
  remark: ''
})
const formRef = ref<FormInstance>()
const tableRef = ref<TableInstance>()
const menuTree = reactive<AuthTreeNode[]>([])
const selectedIds = ref<IdType[]>([])
const open = ref(false)
const dialogState = ref<DialogState>('add')
const isLoading = ref(false)
const actionLoading = ref(false)
const submitting = ref(false)
const expandedAll = ref(false)
const userExpandedIds = ref<Set<string>>(new Set())
let expandOperationId = 0
let listRequestId = 0
const menuStore = useMenuStore()
const columns = useColumn(['资源', '权限标识', '路由 / 组件', '顺序', '创建时间', '备注']).columns

const canReadResource = computed(() => hasPerm('user:menu:list'))
const rules = computed(() => ({
  menuName: [{required: true, message: '资源名称不能为空', trigger: 'blur'}],
  parentId: [{required: true, message: '请选择父级资源或根节点', trigger: 'change'}],
  menuType: [{required: true, message: '请选择资源类型', trigger: 'change'}],
  orderNum: [{required: true, message: '请输入显示顺序', trigger: 'change'}],
  router: form.menuType === MenuType.BUTTON ? [] : [{required: true, message: '请输入路由地址', trigger: 'blur'}],
  component: form.menuType === MenuType.MENU_ITEM ? [{required: true, message: '请输入组件路径', trigger: 'blur'}] : [],
  perms: form.menuType === MenuType.MENU ? [] : [{required: true, message: '请输入权限标识', trigger: 'blur'}],
}))

const allMenus = computed(() => flattenTree(menuTree))
const activeFilterCount = computed(() => [queryParams.menuId, queryParams.menuName, queryParams.perms, queryParams.menuType].filter(value => value !== undefined && value !== '').length)
const summaryStats = computed(() => [{
  label: '全部资源',
  value: allMenus.value.length,
  tone: 'blue'
}, {
  label: '菜单资源',
  value: allMenus.value.filter(item => item.menu.menuType !== MenuType.BUTTON).length,
  tone: 'green'
}, {
  label: '按钮权限',
  value: allMenus.value.filter(item => item.menu.menuType === MenuType.BUTTON).length,
  tone: 'amber'
}, {label: '当前匹配', value: filteredCount.value, tone: 'violet'}])
const filteredTree = computed(() => filterTree(menuTree))
const filteredCount = computed(() => flattenTree(filteredTree.value).length)
const parentOptions = computed<TreeOption[]>(() => [{
  id: '0',
  label: '根节点'
}, ...toParentOptions(menuTree, form.menuId)])

function flattenTree(nodes: TreedMenu[]): TreedMenu[] {
  return nodes.reduce<TreedMenu[]>((result, node) => {
    result.push(node);
    if (node.children?.length) result.push(...flattenTree(node.children));
    return result
  }, [])
}

function toParentOptions(nodes: TreedMenu[], currentId?: IdType): TreeOption[] {
  const blockedIds = new Set<string>()
  if (currentId !== undefined) {
    const current = findNode(nodes, currentId)
    if (current) collectNodeIds(current, blockedIds)
    blockedIds.add(String(currentId))
  }
  return nodes.filter(node => node.menu.menuType !== MenuType.BUTTON && !blockedIds.has(String(node.menu.menuId))).map(node => ({
    id: node.menu.menuId,
    label: `${node.menu.menuName} · ${typeLabel(node.menu.menuType)}`,
    children: node.children?.length ? toParentOptions(node.children, currentId).filter(child => !blockedIds.has(String(child.id))) : undefined
  }))
}

function findNode(nodes: TreedMenu[], id: IdType): TreedMenu | undefined {
  for (const node of nodes) {
    if (String(node.menu.menuId) === String(id)) return node
    const found = node.children?.length ? findNode(node.children, id) : undefined
    if (found) return found
  }
  return undefined
}

function collectNodeIds(node: TreedMenu, result: Set<string>) {
  result.add(String(node.menu.menuId))
  node.children?.forEach(child => collectNodeIds(child, result))
}

function nodeMatches(node: TreedMenu): boolean {
  const menu = node.menu;
  return (!queryParams.menuId || String(menu.menuId).includes(String(queryParams.menuId))) && (!queryParams.menuName || menu.menuName?.toLowerCase().includes(String(queryParams.menuName).toLowerCase())) && (!queryParams.perms || menu.perms?.toLowerCase().includes(String(queryParams.perms).toLowerCase())) && (!queryParams.menuType || menu.menuType === queryParams.menuType)
}

function menuTypeRank(type: MenuType) {
  return type === MenuType.MENU ? 0 : type === MenuType.MENU_ITEM ? 1 : 2
}

function sortTree(nodes: TreedMenu[]) {
  return [...nodes].sort((a, b) => menuTypeRank(a.menu.menuType) - menuTypeRank(b.menu.menuType) || (a.menu.orderNum ?? 0) - (b.menu.orderNum ?? 0) || String(a.menu.menuName).localeCompare(String(b.menu.menuName), 'zh-CN'))
}

function decorateTree(nodes: TreedMenu[], depth = 0): AuthTreeNode[] {
  return sortTree(nodes).map(node => ({...node, depth, children: decorateTree(node.children || [], depth + 1)}))
}

function filterTree(nodes: AuthTreeNode[]): AuthTreeNode[] {
  return nodes.reduce<AuthTreeNode[]>((result, node) => {
    const children = node.children?.length ? filterTree(node.children) : [];
    if (nodeMatches(node) || children.length) result.push({...node, children});
    return result
  }, [])
}

function typeLabel(type: MenuType) {
  return dict.menuType.find(item => item.value === type)?.label || '未知'
}

function typeTone(type: MenuType) {
  return type === MenuType.BUTTON ? 'amber' : type === MenuType.MENU_ITEM ? 'blue' : 'violet'
}

function shortId(value: IdType) {
  const text = String(value);
  return text.length > 18 ? `${text.slice(0, 8)}…${text.slice(-6)}` : text
}

function formatDate(value: Date | string | undefined) {
  return value ? new Date(value).toLocaleDateString('zh-CN', {year: 'numeric', month: '2-digit', day: '2-digit'}) : '—'
}

function resolveIcon(name?: string) {
  return name && name !== '#' ? name : undefined
}

function resourceCellStyle(row: AuthTreeNode) {
  return {paddingLeft: `${row.depth * 24}px`}
}

const getList = async () => {
  if (!canReadResource.value) return
  const requestId = ++listRequestId
  isLoading.value = true;
  try {
    const data = await getAllTreedMenu();
    if (requestId !== listRequestId) return
    setTreeId(data);
    menuTree.splice(0, menuTree.length, ...decorateTree(data));
    await nextTick()
    syncExpandedRows()
  } catch {
    if (requestId === listRequestId) ElMessage.error('权限资源加载失败，请稍后重试')
  } finally {
    if (requestId === listRequestId) isLoading.value = false
  }
}
const handleQuery = () => {
  expandedAll.value = false
  void nextTick(syncExpandedRows)
}
const clearFilters = () => {
  queryParams.menuId = undefined;
  queryParams.menuName = undefined;
  queryParams.perms = undefined;
  queryParams.menuType = undefined;
  expandedAll.value = false
  void nextTick(syncExpandedRows)
}

function nodeKey(node: TreedMenu) {
  return String(node.menu.menuId)
}

function collectAllIds(nodes: TreedMenu[], result = new Set<string>()) {
  nodes.forEach(node => {
    result.add(nodeKey(node))
    collectAllIds(node.children || [], result)
  })
  return result
}

function collectMatchPathIds(nodes: AuthTreeNode[], result = new Set<string>()): boolean {
  let hasMatch = false
  nodes.forEach(node => {
    const childMatch = collectMatchPathIds(node.children || [], result)
    if (nodeMatches(node) || childMatch) {
      result.add(nodeKey(node))
      hasMatch = true
    }
  })
  return hasMatch
}

const syncExpandedRows = () => {
  const operationId = ++expandOperationId
  const matchPathIds = new Set<string>()
  if (activeFilterCount.value) collectMatchPathIds(filteredTree.value, matchPathIds)
  const targetIds = expandedAll.value
    ? collectAllIds(filteredTree.value)
    : activeFilterCount.value
      ? matchPathIds
      : new Set(userExpandedIds.value)
  void (async () => {
    await nextTick()
    if (operationId !== expandOperationId) return
    filteredTree.value.forEach(node => tableRef.value?.toggleRowExpansion(node, false))
    await nextTick()
    await expandRowsForKeys(filteredTree.value, targetIds, operationId)
  })()
}

const expandRowsForKeys = async (nodes: AuthTreeNode[], targetIds: Set<string>, operationId: number): Promise<void> => {
  if (operationId !== expandOperationId) return
  const targets = nodes.filter(node => targetIds.has(nodeKey(node)))
  targets.forEach(node => tableRef.value?.toggleRowExpansion(node, true))
  if (!targets.length) return
  await nextTick()
  await expandRowsForKeys(targets.flatMap(node => node.children || []), targetIds, operationId)
}

const handleExpandChange = (_row: AuthTreeNode, expandedRows: AuthTreeNode[]) => {
  if (expandedAll.value || activeFilterCount.value) return
  userExpandedIds.value = new Set(expandedRows.map(nodeKey))
}

const toggleExpand = () => {
  expandedAll.value = !expandedAll.value
  if (!expandedAll.value) userExpandedIds.value = new Set()
  syncExpandedRows()
}

const buildMenuPayload = (): MenuForm => {
  const type = form.menuType || MenuType.MENU
  const payload: MenuForm = {
    menuId: form.menuId,
    menuName: form.menuName?.trim(),
    menuType: type,
    parentId: form.parentId || '0',
    orderNum: form.orderNum,
    remark: form.remark?.trim() || '',
  }
  if (type !== MenuType.BUTTON) {
    payload.icon = form.icon || '#'
    payload.router = form.router?.trim() || ''
    payload.component = form.component?.trim() || ''
  }
  if (type !== MenuType.MENU) payload.perms = form.perms?.trim() || ''
  return payload
}

const handleSelectionChange = (selection: TreedMenu[]) => {
  selectedIds.value = selection.map(item => item.menu.menuId)
}

const submitForm = async () => {
  if (!formRef.value) return;
  const valid = await formRef.value.validate().catch(() => false);
  if (!valid) return;
  const payload = buildMenuPayload()
  submitting.value = true
  try {
    if (dialogState.value === 'add') await addMenu(payload)
    else await updateMenu(payload)
    ElMessage.success(dialogState.value === 'add' ? '资源已创建' : '资源已更新')
    finishDialog()
  } catch {
    ElMessage.error(dialogState.value === 'add' ? '资源创建失败，请稍后重试' : '资源更新失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  form.menuId = undefined;
  form.menuName = '';
  form.menuType = MenuType.MENU;
  form.parentId = '0';
  form.icon = '#';
  form.perms = '';
  form.router = '';
  form.component = '';
  form.orderNum = 0;
  form.remark = '';
  formRef.value?.clearValidate()
}
const handleAdd = (parent?: TreedMenu) => {
  resetForm();
  form.parentId = parent?.menu.menuId || '0';
  dialogState.value = 'add';
  open.value = true
}
const handleUpdate = (node?: TreedMenu) => {
  const target = node || allMenus.value.find(item => String(item.menu.menuId) === String(selectedIds.value[0]));
  if (!target) {
    ElMessage.warning('请先选择一个资源');
    return
  }
  resetForm();
  Object.assign(form, target.menu);
  form.parentId = form.parentId || '0';
  dialogState.value = 'edit';
  open.value = true
}
const finishDialog = () => {
  open.value = false;
  resetForm();
  menuStore.invalidate()
  void getList()
}
const handleDelete = async (node?: TreedMenu) => {
  const ids = node ? [node.menu.menuId] : selectedIds.value;
  if (!ids.length) return;
  try {
    await ElMessageBox.confirm(node ? `删除“${node.menu.menuName}”可能同时影响它的子资源，确定继续吗？` : `确定删除选中的 ${ids.length} 个资源吗？`, '删除权限资源', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning'
    });
    actionLoading.value = true;
    await removeMenu(node ? ids[0] : ids);
    ElMessage.success('资源已删除');
    menuStore.invalidate()
    await getList()
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败，请稍后重试')
  } finally {
    actionLoading.value = false
  }
}
const cancel = () => {
  open.value = false;
  resetForm()
}

watch(() => form.menuType, (type) => {
  if (type === MenuType.BUTTON) {
    form.icon = '#'
    form.router = ''
    form.component = ''
  } else if (type === MenuType.MENU) {
    form.perms = ''
  }
})

watch(canReadResource, (allowed) => {
  if (allowed) void getList()
}, {immediate: true})
onBeforeUnmount(() => {
  listRequestId++
  expandOperationId++
})
</script>

<style lang="scss" scoped>
.auth-panel {
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
  color: #8b5cf6;
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
  margin-bottom: 18px;
  padding: 14px 16px 4px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 13px;
  background: var(--el-bg-color-page);
}

.filter-panel__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 11px;
}

.filter-title {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 13px;
}

.filter-title .el-icon {
  color: #8b5cf6;
}

.filter-title span, .tree-caption {
  color: var(--el-text-color-secondary);
  font-size: 11px;
  font-weight: 400;
}

.tree-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}

.auth-filters {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr)) auto;
  align-items: end;
  gap: 0 14px;
}

.auth-filters :deep(.el-form-item) {
  min-width: 0;
  margin-bottom: 10px;
}

.auth-filters :deep(.el-form-item__label) {
  height: auto;
  margin-bottom: 5px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
  line-height: 1.2;
}

.full-width {
  width: 100%;
}

.filter-actions {
  display: flex;
  align-items: flex-end;
  gap: 7px;
  height: 68px;
  padding-bottom: 10px;
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
  color: #8b5cf6;
  font-weight: 650;
}

.selection-status.has-selection .el-icon {
  color: #8b5cf6;
}

.auth-table {
  overflow: hidden;
  border-radius: 14px;
}

.auth-table :deep(.el-table__header th.el-table__cell) {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 700;
  background: var(--el-fill-color-light);
}

.auth-table :deep(.el-table__row td.el-table__cell) {
  height: 68px;
}

.auth-table :deep(.resource-column .cell) {
  display: flex;
  align-items: center;
  min-width: 0;
}

.auth-table :deep(.el-table__indent) {
  width: 0;
  overflow: hidden;
}

.auth-table :deep(.el-table__expand-icon) {
  display: inline-grid;
  width: 22px;
  height: 22px;
  flex: 0 0 auto;
  margin-right: 5px;
  margin-left: 0;
  place-items: center;
  border: 1px solid var(--el-border-color);
  border-radius: 7px;
  background: var(--el-bg-color-page);
  color: var(--el-text-color-secondary);
  vertical-align: middle;
  transition: border-color .18s ease, background-color .18s ease, color .18s ease;
}

.auth-table :deep(.el-table__expand-icon:hover) {
  border-color: rgb(139 92 246 / 55%);
  color: #8b5cf6;
}

.auth-table :deep(.el-table__expand-icon--expanded) {
  border-color: rgb(139 92 246 / 40%);
  background: rgb(139 92 246 / 11%);
  color: #8b5cf6;
}

.auth-table :deep(.el-table__expand-icon .el-icon) {
  font-size: 12px;
}

.resource-cell {
  display: flex;
  min-width: 0;
  flex: 1;
  align-items: center;
  gap: 12px;
}

.resource-icon {
  display: grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 10px;
  font-size: 17px;
}

.resource-icon--violet {
  color: #8b5cf6;
  background: rgb(139 92 246 / 12%);
}

.resource-icon--blue {
  color: #2563eb;
  background: rgb(37 99 235 / 12%);
}

.resource-icon--amber {
  color: #d97706;
  background: rgb(217 119 6 / 12%);
}

.resource-cell__main {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.resource-cell__title {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 8px;
}

.resource-cell__title strong {
  max-width: 220px;
  overflow: hidden;
  font-size: 13px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.resource-cell__sub {
  color: var(--el-text-color-secondary);
  font: 11px var(--code-font-family, monospace);
}

.type-pill {
  flex: 0 0 auto;
  padding: 2px 6px;
  border-radius: 999px;
  font-size: 10px;
}

.type-pill--violet {
  color: #8b5cf6;
  background: rgb(139 92 246 / 11%);
}

.type-pill--blue {
  color: #2563eb;
  background: rgb(37 99 235 / 11%);
}

.type-pill--amber {
  color: #d97706;
  background: rgb(217 119 6 / 11%);
}

.auth-table code {
  padding: 3px 6px;
  border-radius: 5px;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-primary);
  font: 11px var(--code-font-family, monospace);
}

.muted-text {
  color: var(--el-text-color-placeholder);
}

.route-cell {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.route-cell span, .route-cell small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.route-cell span {
  color: var(--el-text-color-primary);
  font-size: 12px;
}

.route-cell small {
  color: var(--el-text-color-secondary);
  font: 10px var(--code-font-family, monospace);
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
  background: rgb(139 92 246 / 13%);
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

.type-options {
  min-height: 32px;
  align-items: center;
}

.field-tip {
  display: block;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 11px;
}

.icon-form-item {
  margin-top: 1px;
}

@keyframes pulse {
  50% {
    opacity: .35;
  }
}

@media (max-width: 900px) {
  .auth-filters {
    grid-template-columns: repeat(3, minmax(140px, 1fr));
  }
  .filter-actions {
    height: auto;
    padding-bottom: 10px;
  }
}

@media (max-width: 680px) {
  .auth-panel {
    padding: 18px 14px 8px;
  }
  .panel-heading {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }
  .auth-filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .filter-actions {
    grid-column: 1 / -1;
  }
  .list-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
  .tree-caption {
    align-self: flex-end;
  }
  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }
  .auth-table :deep(.el-table__fixed-right) {
    display: none;
  }
}

@media (max-width: 440px) {
  .auth-filters {
    grid-template-columns: 1fr;
  }
  .filter-actions {
    grid-column: auto;
  }
  .filter-actions .el-button {
    flex: 1;
  }
  .tree-actions {
    flex-wrap: wrap;
    justify-content: flex-end;
  }
}
</style>
