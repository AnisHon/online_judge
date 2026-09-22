<template>
  <ProblemModuleShell title="目录管理" kicker="PROBLEM / FOLDERS" description="以树形目录组织题单与题库内容，让目录层级、题单关联和维护动作保持清晰。" :icon="Folder" tone="amber">
    <template #actions>
      <el-button :icon="Refresh" :loading="isLoading" @click="getList">刷新目录</el-button>
      <el-button v-has="'problem:folder:add'" type="primary" :icon="Plus" @click="handleAdd">新增节点</el-button>
    </template>

    <div class="folder-panel">
      <div class="panel-heading">
        <div><span class="panel-eyebrow">CONTENT HIERARCHY</span><h2>目录结构</h2><p>目录、菜单和文件按照层级展示；父级关系和显示顺序会直接影响题库导航。</p></div>
        <div class="panel-heading__meta"><span class="sync-dot" :class="{ 'is-loading': isLoading }"></span><span>{{ isLoading ? '正在同步' : `共 ${allFolderCount} 个节点` }}</span></div>
      </div>

      <div class="filter-panel" :class="{ 'is-collapsed': !showSearch }">
        <div class="filter-panel__bar"><div class="filter-title"><el-icon><Filter /></el-icon><strong>定位目录</strong><span v-if="activeFilterCount">{{ activeFilterCount }} 项已启用</span></div><div class="tree-actions"><el-button link type="primary" :icon="expandedAll ? Fold : Expand" @click="toggleExpand">{{ expandedAll ? '收起全部' : '展开全部' }}</el-button><el-button link type="primary" @click="showSearch = !showSearch">{{ showSearch ? '收起筛选' : '展开筛选' }}</el-button></div></div>
        <el-form v-show="showSearch" class="folder-filters" label-position="top" @submit.prevent="handleQuery">
          <el-form-item label="目录名称"><el-input v-model="keyword" clearable :prefix-icon="Search" placeholder="搜索目录名称或 ID" @keyup.enter="handleQuery" /></el-form-item>
          <el-form-item label="节点类型"><el-select v-model="typeFilter" clearable class="full-width" placeholder="全部类型"><el-option v-for="item in dict.folderType" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <div class="filter-actions"><el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button><el-button :icon="Refresh" @click="resetQuery">重置</el-button></div>
        </el-form>
      </div>

      <div class="list-toolbar"><div class="list-toolbar__left"><span class="selection-status" :class="{ 'has-selection': selectedIds.length }"><el-icon><Select /></el-icon>{{ selectedIds.length ? `已选择 ${selectedIds.length} 个节点` : '未选择节点' }}</span><el-button v-has="'problem:folder:remove'" type="danger" plain :disabled="!selectedIds.length || actionLoading" :icon="Delete" @click="handleDelete()">批量删除</el-button></div><span class="tree-caption">{{ filteredCount }} 个匹配节点 · 结果会保留父级路径</span><RightToolBar v-model:showSearch="showSearch" :columns="columns" @queryTable="getList" /></div>

      <el-table ref="tableRef" v-loading="isLoading" class="folder-table" :data="filteredTree" row-key="id" :indent="0" :row-style="treeRowStyle" :tree-props="{ children: 'children' }" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="52" align="center" />
        <el-table-column v-if="columns[0].visible" class-name="folder-column" label="目录" min-width="350">
          <template #default="{ row }"><div class="folder-column__cell" :style="folderCellStyle(row)"><span class="folder-icon" :class="`folder-icon--${folderTypeTone(row.folder.folderType)}`"><el-icon><component :is="row.folder.folderType === FolderType.FILE ? Document : Folder" /></el-icon></span><div class="folder-cell__main"><div class="folder-cell__title"><strong :title="row.folder.folderName">{{ row.folder.folderName }}</strong><span class="type-pill" :class="`type-pill--${folderTypeTone(row.folder.folderType)}`">{{ folderTypeText(row.folder.folderType) }}</span></div><span class="folder-cell__sub" :title="String(row.folder.folderId)">ID {{ shortId(row.folder.folderId) }}</span></div></div></template>
        </el-table-column>
        <el-table-column v-if="columns[1].visible" label="节点类型" width="110" align="center"><template #default="{ row }"><span class="type-text" :class="`type-text--${folderTypeTone(row.folder.folderType)}`">{{ folderTypeText(row.folder.folderType) }}</span></template></el-table-column>
        <el-table-column v-if="columns[2].visible" label="关联题单" min-width="180" show-overflow-tooltip><template #default="{ row }"><span v-if="row.folder.listId" class="id-text" :title="String(row.folder.listId)">#{{ shortId(row.folder.listId) }}</span><span v-else class="muted-text">—</span></template></el-table-column>
        <el-table-column v-if="columns[3].visible" label="父级目录" min-width="180" show-overflow-tooltip><template #default="{ row }"><span v-if="row.folder.parentId && String(row.folder.parentId) !== '0'" class="id-text" :title="String(row.folder.parentId)">#{{ shortId(row.folder.parentId) }}</span><span v-else class="root-pill">根目录</span></template></el-table-column>
        <el-table-column v-if="columns[4].visible" label="顺序" width="82" align="center"><template #default="{ row }"><span class="order-text">{{ row.folder.order ?? 0 }}</span></template></el-table-column>
        <el-table-column label="操作" width="175" fixed="right" align="right"><template #default="{ row }"><el-space :size="5"><el-button v-has="'problem:folder:edit'" link type="primary" :icon="EditPen" @click="handleUpdate(row)">编辑</el-button><el-button v-has="'problem:folder:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button></el-space></template></el-table-column>
        <template #empty><el-empty description="没有匹配的目录节点" :image-size="84" /></template>
      </el-table>
    </div>

    <el-dialog v-model="open" class="folder-dialog" :title="dialogState === 'add' ? '新建目录节点' : '编辑目录节点'" width="min(720px, 92vw)" append-to-body destroy-on-close>
      <div class="dialog-intro"><span class="dialog-icon"><el-icon><Folder /></el-icon></span><div><strong>{{ dialogState === 'add' ? '建立一个目录节点' : '调整目录节点' }}</strong><p>先确定节点类型，再决定是否关联题单和父级目录。</p></div></div>
      <el-form ref="formRef" class="folder-form" :model="form" :rules="rules" label-position="top">
        <section class="form-section"><div class="section-title"><span>01</span><div><strong>基础信息</strong><small>目录名称用于后台维护和前台导航展示</small></div></div><div class="form-grid"><el-form-item label="节点名称" prop="folderName"><el-input v-model="form.folderName" maxlength="80" show-word-limit placeholder="例如：数据结构基础" /></el-form-item><el-form-item label="节点类型" prop="folderType"><el-select v-model="form.folderType" class="full-width" placeholder="请选择节点类型" @change="handleFolderTypeChange"><el-option v-for="item in dict.folderType" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></div></section>
        <section class="form-section"><div class="section-title"><span>02</span><div><strong>题单关联</strong><small>{{ form.folderType === FolderType.FILE ? '文件节点需要关联一个题单' : '只有文件类型节点可以关联题单' }}</small></div></div><el-form-item label="关联题单" prop="listId"><el-input v-model="selectedListText" class="selected-list" readonly :title="selectedListFullText" :disabled="form.folderType !== FolderType.FILE" :placeholder="form.folderType === FolderType.FILE ? '请选择题单' : '当前节点类型无需关联题单'"><template #append><el-button :disabled="form.folderType !== FolderType.FILE" :icon="List" @click="openSelectList = true">选择题单</el-button></template></el-input><small class="field-tip">{{ form.folderType === FolderType.FILE ? '显示题单名称和 ID，过长 ID 会中间截断，悬停可查看完整值。' : '切换为文件类型后可进行选择。' }}</small></el-form-item></section>
        <section class="form-section"><div class="section-title"><span>03</span><div><strong>层级关系</strong><small>控制节点在目录树中的父级位置和显示顺序</small></div></div><div class="form-grid"><el-form-item label="显示顺序" prop="order"><el-input-number v-model="form.order" class="full-width" :min="0" :controls="false" placeholder="数字越小越靠前" /></el-form-item><el-form-item label="父级目录"><el-switch v-model="hasParentId" inline-prompt active-text="设置" inactive-text="根目录" /></el-form-item></div><div v-show="hasParentId" class="parent-picker"><div class="parent-picker__heading"><strong>选择父目录</strong><span>文件节点不可作为父级，当前节点及其子级已排除</span></div><el-tree ref="parentTreeRef" node-key="id" :data="parentTreeData" :props="defaultProps" :default-expand-all="false" show-checkbox check-strictly empty-text="暂无可用父目录" @check="handleParentCheck" /></div></section>
      </el-form>
      <template #footer><el-button @click="cancel">取消</el-button><el-button type="primary" :loading="isUpdateLoading || isAddLoading" @click="submitForm">保存目录节点</el-button></template>
    </el-dialog>

    <el-dialog v-model="openSelectList" class="folder-list-dialog" title="选择题单" width="min(960px, 94vw)" append-to-body destroy-on-close><ListView v-model="form.listId" v-model:isOpen="openSelectList" @select="handleListSelect" /></el-dialog>
  </ProblemModuleShell>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref } from 'vue'
import { Delete, Document, EditPen, Expand, Filter, Fold, Folder, List, Plus, Refresh, Search, Select } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, ElTree, type FormInstance, type FormRules } from 'element-plus'
import ProblemModuleShell from '@/views/backend/problem-module/component/ProblemModuleShell.vue'
import RightToolBar from '@/components/right-toolbar/RightToolBar.vue'
import ListView from '@/components/ListView/ListView.vue'
import { addFolder, dict, type FolderForm, FolderType, getTreedFolderView, removeFolder, updateFolder, type TreedFolderView } from '@/api/folder'
import { getList as fetchProblemLists, type ListView as ProblemListView } from '@/api/list'
import type { IdType } from '@/api/common'
import { useColumn } from '@/hooks/useColumn'

type FolderTreeNode = Omit<TreedFolderView, 'children'> & { depth: number; children: FolderTreeNode[] }
const form = reactive<FolderForm>({ folderId: undefined, folderName: '', folderType: FolderType.DIRECTORY, listId: undefined, order: 0, parentId: '0' })
const formRef = ref<FormInstance>()
const parentTreeRef = ref<InstanceType<typeof ElTree>>()
const open = ref(false)
const openSelectList = ref(false)
const dialogState = ref<'add' | 'edit'>('add')
const hasParentId = ref(false)
const selectedListName = ref('')
const keyword = ref('')
const typeFilter = ref<FolderType>()
const showSearch = ref(true)
const expandedAll = ref(false)
const actionLoading = ref(false)
const selectedIds = ref<IdType[]>([])
const excludedParentIds = ref<Set<string>>(new Set())
const tableList = reactive<FolderTreeNode[]>([])
const { columns } = useColumn(['目录', '节点类型', '关联题单', '父级目录', '顺序'])
const rules = computed<FormRules<FolderForm>>(() => ({
  folderName: [{ required: true, message: '请输入节点名称', trigger: 'blur' }],
  folderType: [{ required: true, message: '请选择节点类型', trigger: 'change' }],
  order: [{ required: true, message: '请输入显示顺序', trigger: 'change' }],
  listId: form.folderType === FolderType.FILE ? [{ required: true, message: '文件节点必须选择题单', trigger: 'change' }] : []
}))
const defaultProps = { children: 'children', label: (data: unknown) => (data as FolderTreeNode).folder.folderName }

const shortId = (value: IdType) => { const text = String(value); return text.length > 18 ? `${text.slice(0, 8)}…${text.slice(-6)}` : text }
const selectedListText = computed(() => form.listId ? `题单 #${shortId(form.listId)}${selectedListName.value ? ` · ${selectedListName.value}` : ''}` : '')
const selectedListFullText = computed(() => form.listId ? `题单 #${String(form.listId)}${selectedListName.value ? ` · ${selectedListName.value}` : ''}` : '')
const allFolderCount = computed(() => flattenTree(tableList).length)
const filteredTree = computed(() => filterTree(tableList))
const parentTreeData = computed(() => filterParentTree(tableList))
const filteredCount = computed(() => flattenTree(filteredTree.value).length)
const activeFilterCount = computed(() => [keyword.value, typeFilter.value].filter(value => value !== undefined && value !== '').length)

function flattenTree(nodes: TreedFolderView[]): TreedFolderView[] { return nodes.reduce<TreedFolderView[]>((result, node) => { result.push(node); if (node.children?.length) result.push(...flattenTree(node.children)); return result }, []) }
function decorateTree(nodes: TreedFolderView[], depth = 0): FolderTreeNode[] { return [...nodes].sort((a, b) => (a.folder.order ?? 0) - (b.folder.order ?? 0) || String(a.folder.folderName).localeCompare(String(b.folder.folderName), 'zh-CN')).map(node => ({ ...node, depth, children: decorateTree(node.children || [], depth + 1) })) }
function nodeMatches(node: TreedFolderView) { const text = keyword.value.trim().toLowerCase(); return (!text || String(node.folder.folderName).toLowerCase().includes(text) || String(node.folder.folderId).includes(text)) && (!typeFilter.value || node.folder.folderType === typeFilter.value) }
function filterTree(nodes: FolderTreeNode[]): FolderTreeNode[] { return nodes.reduce<FolderTreeNode[]>((result, node) => { const children = node.children.length ? filterTree(node.children) : []; if (nodeMatches(node) || children.length) result.push({ ...node, children }); return result }, []) }
function filterParentTree(nodes: FolderTreeNode[]): FolderTreeNode[] { return nodes.reduce<FolderTreeNode[]>((result, node) => { if (node.folder.folderType === FolderType.FILE || excludedParentIds.value.has(String(node.folder.folderId))) return result; result.push({ ...node, children: filterParentTree(node.children) }); return result }, []) }
function folderTypeText(type: FolderType) { return dict.folderType.find(item => item.value === type)?.label || '未知' }
function folderTypeTone(type: FolderType) { return type === FolderType.FILE ? 'blue' : type === FolderType.MENU ? 'violet' : 'amber' }
function findFolderNode(nodes: TreedFolderView[], folderId: IdType): TreedFolderView | undefined { for (const node of nodes) { if (String(node.folder.folderId) === String(folderId)) return node; const found = node.children?.length ? findFolderNode(node.children, folderId) : undefined; if (found) return found } return undefined }
function collectFolderIds(node: TreedFolderView, result: IdType[] = []) { result.push(node.folder.folderId); node.children?.forEach(child => collectFolderIds(child, result)); return result }
function setNodeKey(nodes: TreedFolderView[]) { nodes.forEach(node => { node.id = node.folder.folderId; if (node.children?.length) setNodeKey(node.children) }) }
function treeRowStyle({ row }: { row: FolderTreeNode }) { return { '--tree-offset': `${row.depth * 24}px` } }
function folderCellStyle(row: FolderTreeNode) { return row.children.length ? undefined : { paddingLeft: `${row.depth * 24}px` } }

const isLoading = ref(false)
const getList = async () => {
  isLoading.value = true
  try {
    const data = await getTreedFolderView()
    setNodeKey(data)
    tableList.splice(0, tableList.length, ...decorateTree(data))
    selectedIds.value = []
    void nextTick().then(() => expandVisibleRows(expandedAll.value))
  } finally {
    isLoading.value = false
  }
}
const handleSelectionChange = (selection: FolderTreeNode[]) => { selectedIds.value = selection.map(item => item.folder.folderId) }
const handleQuery = () => { expandedAll.value = true; setTimeout(() => expandVisibleRows(true), 0) }
const resetQuery = () => { keyword.value = ''; typeFilter.value = undefined; expandedAll.value = false; getList() }
const expandVisibleRows = (expanded: boolean) => { void expandRows(filteredTree.value, expanded) }
const expandRows = async (nodes: FolderTreeNode[], expanded: boolean): Promise<void> => { nodes.forEach(node => tableRef.value?.toggleRowExpansion(node, expanded)); const children = nodes.flatMap(node => node.children); if (expanded && children.length) { await nextTick(); await expandRows(children, true) } }
const toggleExpand = () => { expandedAll.value = !expandedAll.value; expandVisibleRows(expandedAll.value) }
const tableRef = ref<{ toggleRowExpansion: (row: FolderTreeNode, expanded?: boolean) => void }>()

const handleDelete = async (row?: TreedFolderView) => { const sourceNode = row ? findFolderNode(tableList, row.folder.folderId) || row : undefined; const ids = sourceNode ? collectFolderIds(sourceNode) : selectedIds.value; if (!ids.length) { ElMessage.warning('请先选择要删除的目录节点'); return } try { await ElMessageBox.confirm(sourceNode ? `删除“${sourceNode.folder.folderName}”会同时删除其子节点，确定继续吗？` : `确定删除选中的 ${ids.length} 个目录节点吗？`, '删除目录节点', { confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning' }); actionLoading.value = true; await removeFolder(ids); ElMessage.success('目录节点已删除'); selectedIds.value = []; getList() } catch (error) { if (error !== 'cancel' && error !== 'close') ElMessage.error('删除失败，请稍后重试') } finally { actionLoading.value = false } }

const resetForm = () => { form.folderId = undefined; form.folderName = ''; form.parentId = '0'; form.listId = undefined; form.folderType = FolderType.DIRECTORY; form.order = 0; selectedListName.value = ''; parentTreeRef.value?.setCheckedKeys([]); formRef.value?.clearValidate() }
const handleAdd = () => { resetForm(); hasParentId.value = false; excludedParentIds.value = new Set(); dialogState.value = 'add'; open.value = true }
const handleUpdate = async (node: TreedFolderView) => { resetForm(); Object.assign(form, node.folder); hasParentId.value = !!form.parentId && String(form.parentId) !== '0'; const currentNode = findFolderNode(tableList, form.folderId || node.folder.folderId); excludedParentIds.value = new Set(currentNode ? collectFolderIds(currentNode).map(id => String(id)) : [String(form.folderId)]); dialogState.value = 'edit'; open.value = true; await nextTick(); parentTreeRef.value?.setCheckedKeys(form.parentId && String(form.parentId) !== '0' ? [form.parentId] : []); if (form.listId) { const editingListId = form.listId; try { const result = await fetchProblemLists({ asc: true, currentPage: 1, pageSize: 1, listId: form.listId }); if (String(form.listId) === String(editingListId)) selectedListName.value = result.data.find(item => String(item.listId) === String(editingListId))?.listName || '' } catch { /* 题单名称补全失败不阻止编辑 */ } } }
const handleFolderTypeChange = (value: FolderType) => { if (value !== FolderType.FILE) { form.listId = undefined; selectedListName.value = '' } }
const handleListSelect = (list: ProblemListView) => { form.listId = list.listId; selectedListName.value = list.listName; openSelectList.value = false }
const handleParentCheck = (data: unknown, checkedInfo: unknown) => { const node = data as FolderTreeNode; const checkedKeys = (checkedInfo as { checkedKeys?: unknown[] }).checkedKeys || []; const checked = checkedKeys.some(key => String(key) === String(node.folder.folderId)); parentTreeRef.value?.setCheckedKeys(checked ? [node.folder.folderId] : []) }
const submitForm = async () => {
  if (!(await formRef.value?.validate().catch(() => false))) return
  const keys = parentTreeRef.value?.getCheckedKeys() as IdType[] || []
  const payload = {...form, parentId: hasParentId.value && keys.length ? keys[0] : '0'}
  actionLoading.value = true
  try {
    if (dialogState.value === 'add') await addFolder(payload)
    else await updateFolder(payload)
    ElMessage.success(dialogState.value === 'add' ? '目录节点已创建' : '目录节点已更新')
    finishDialog()
  } finally {
    actionLoading.value = false
  }
}
const finishDialog = () => { open.value = false; resetForm(); getList() }
const isUpdateLoading = computed(() => actionLoading.value && dialogState.value === 'edit')
const isAddLoading = computed(() => actionLoading.value && dialogState.value === 'add')
const cancel = () => { open.value = false; resetForm() }

getList()
</script>

<style lang="scss" scoped>
.folder-panel { min-width: 0; padding: 22px 24px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 16px 40px rgb(15 23 42 / 4%); }.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 20px; }.panel-eyebrow { color: #d97706; font-size: 11px; font-weight: 800; letter-spacing: .14em; }.panel-heading h2 { margin: 7px 0 5px; font-size: 21px; letter-spacing: -.03em; }.panel-heading p { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }.panel-heading__meta { display: inline-flex; align-items: center; gap: 7px; padding-top: 5px; color: var(--el-text-color-secondary); font-size: 12px; white-space: nowrap; }.sync-dot { width: 7px; height: 7px; border-radius: 50%; background: var(--el-color-success); }.sync-dot.is-loading { background: var(--el-color-warning); animation: pulse 1.1s ease-in-out infinite; }
.filter-panel { margin-bottom: 18px; padding: 14px 16px 4px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-bg-color-page); }.filter-panel__bar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 11px; }.filter-title { display: flex; align-items: center; gap: 7px; font-size: 13px; }.filter-title .el-icon { color: #d97706; }.filter-title span, .tree-caption { color: var(--el-text-color-secondary); font-size: 11px; font-weight: 400; }.tree-actions { display: flex; align-items: center; gap: 4px; }.folder-filters { display: grid; grid-template-columns: 2fr 1fr auto; align-items: end; gap: 0 14px; }.folder-filters :deep(.el-form-item) { min-width: 0; margin-bottom: 10px; }.folder-filters :deep(.el-form-item__label) { height: auto; margin-bottom: 5px; color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.2; }.full-width { width: 100%; }.filter-actions { display: flex; align-items: flex-end; gap: 7px; height: 68px; padding-bottom: 10px; }
.list-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 15px; min-height: 36px; margin-bottom: 10px; }.list-toolbar__left { display: flex; align-items: center; gap: 12px; }.selection-status { display: inline-flex; align-items: center; gap: 6px; color: var(--el-text-color-secondary); font-size: 12px; }.selection-status .el-icon { color: var(--el-text-color-placeholder); }.selection-status.has-selection { color: #d97706; font-weight: 650; }.selection-status.has-selection .el-icon { color: #d97706; }.folder-table { overflow: hidden; border-radius: 14px; }.folder-table :deep(.el-table__header th.el-table__cell) { color: var(--el-text-color-secondary); font-size: 12px; font-weight: 700; background: var(--el-fill-color-light); }.folder-table :deep(.el-table__row td.el-table__cell) { height: 70px; }.folder-table :deep(.folder-column .cell) { display: flex; align-items: center; min-width: 0; }.folder-table :deep(.el-table__indent) { width: 0; overflow: hidden; }.folder-table :deep(.el-table__expand-icon) { display: inline-grid; width: 22px; height: 22px; flex: 0 0 auto; margin-right: 5px; margin-left: var(--tree-offset, 0px); place-items: center; border: 1px solid var(--el-border-color); border-radius: 7px; background: var(--el-bg-color-page); color: var(--el-text-color-secondary); vertical-align: middle; transition: border-color .18s ease, background-color .18s ease, color .18s ease; }.folder-table :deep(.el-table__expand-icon:hover), .folder-table :deep(.el-table__expand-icon--expanded) { border-color: rgb(217 119 6 / 45%); color: #d97706; }.folder-table :deep(.el-table__expand-icon--expanded) { background: rgb(217 119 6 / 11%); }.folder-column__cell { display: flex; min-width: 0; flex: 1; align-items: center; gap: 12px; }.folder-icon { display: grid; width: 35px; height: 35px; flex: 0 0 auto; place-items: center; border-radius: 10px; font-size: 17px; }.folder-icon--amber { color: #d97706; background: rgb(217 119 6 / 12%); }.folder-icon--violet { color: #7c3aed; background: rgb(124 58 237 / 12%); }.folder-icon--blue { color: #2563eb; background: rgb(37 99 235 / 12%); }.folder-cell__main { display: flex; min-width: 0; flex-direction: column; gap: 5px; }.folder-cell__title { display: flex; align-items: center; min-width: 0; gap: 8px; }.folder-cell__title strong { max-width: 220px; overflow: hidden; font-size: 13px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }.folder-cell__sub { color: var(--el-text-color-secondary); font: 11px var(--code-font-family, monospace); }.type-pill { flex: 0 0 auto; padding: 2px 7px; border-radius: 999px; font-size: 10px; }.type-pill--amber { color: #d97706; background: rgb(217 119 6 / 11%); }.type-pill--violet { color: #7c3aed; background: rgb(124 58 237 / 11%); }.type-pill--blue { color: #2563eb; background: rgb(37 99 235 / 11%); }.type-text { font-size: 12px; font-weight: 650; }.type-text--amber { color: #d97706; }.type-text--violet { color: #7c3aed; }.type-text--blue { color: #2563eb; }.id-text { color: var(--el-text-color-primary); font: 11px var(--code-font-family, monospace); }.root-pill { padding: 3px 7px; border-radius: 999px; background: var(--el-fill-color-light); color: var(--el-text-color-secondary); font-size: 10px; }.order-text { color: var(--el-text-color-secondary); font-variant-numeric: tabular-nums; }
.dialog-intro { display: flex; align-items: center; gap: 11px; margin-bottom: 18px; padding: 13px 15px; border-radius: 12px; background: var(--el-fill-color-light); }.dialog-icon { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border-radius: 10px; background: color-mix(in srgb, var(--el-color-warning) 12%, var(--el-bg-color)); color: var(--el-color-warning); }.dialog-intro strong, .dialog-intro p { display: block; }.dialog-intro p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }.folder-form :deep(.el-form-item) { margin-bottom: 16px; }.folder-form :deep(.el-form-item__label) { height: auto; margin-bottom: 6px; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.2; }.field-tip { display: block; margin: 7px 0 0 2px; color: var(--el-text-color-secondary); font-size: 11px; }.form-section { padding: 18px 0 4px; border-top: 1px solid var(--el-border-color-lighter); }.form-section:first-child { padding-top: 0; border-top: 0; }.section-title { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 15px; }.section-title > span { color: var(--el-color-warning); font: 700 11px/1.4 var(--code-font-family, monospace); letter-spacing: .08em; }.section-title strong, .section-title small { display: block; }.section-title strong { font-size: 14px; }.section-title small { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }.parent-picker { max-height: 250px; margin-top: 2px; padding: 12px 14px; overflow: auto; border: 1px solid var(--el-border-color-lighter); border-radius: 12px; background: var(--el-bg-color-page); }.parent-picker__heading { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 8px; }.parent-picker__heading span { color: var(--el-text-color-secondary); font-size: 11px; }.parent-picker :deep(.el-tree) { background: transparent; color: var(--el-text-color-primary); }.parent-picker :deep(.el-tree-node__content) { min-height: 34px; border-radius: 7px; }.parent-picker :deep(.el-tree-node__content:hover) { background: var(--el-fill-color-light); }.selected-list :deep(.el-input__inner) { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
@keyframes pulse { 50% { opacity: .35; } }
@media (max-width: 800px) { .folder-filters { grid-template-columns: repeat(2, minmax(0, 1fr)); }.filter-actions { height: auto; padding-bottom: 10px; } }
@media (max-width: 620px) { .folder-panel { padding: 18px 14px 8px; }.panel-heading { align-items: flex-start; flex-direction: column; gap: 10px; }.filter-panel__bar { align-items: flex-start; flex-direction: column; }.tree-actions { width: 100%; justify-content: flex-end; }.folder-filters { grid-template-columns: 1fr; }.filter-actions { grid-column: 1 / -1; }.list-toolbar { align-items: flex-start; flex-direction: column; }.tree-caption { align-self: flex-end; }.form-grid { grid-template-columns: 1fr; gap: 0; }.parent-picker__heading { align-items: flex-start; flex-direction: column; gap: 3px; } }
@media (max-width: 440px) { .folder-table :deep(.el-table__fixed-right) { display: none; } }
</style>
