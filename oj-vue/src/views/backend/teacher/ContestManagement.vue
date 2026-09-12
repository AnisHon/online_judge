<template>
  <main class="management-page">
    <section class="management-heading">
      <div class="heading-copy">
        <div class="heading-title-row">
          <span class="heading-icon" :class="`heading-icon--${isHomework ? 'amber' : 'blue'}`">
            <el-icon><component :is="isHomework ? Notebook : Flag" /></el-icon>
          </span>
          <div>
            <p class="page-kicker">TEACHING / {{ isHomework ? 'HOMEWORK' : 'CONTEST' }}</p>
            <h1>{{ pageTitle }}</h1>
          </div>
        </div>
        <p class="page-description">{{ isHomework ? '布置、维护和跟踪课程作业。' : '创建、维护和分析平台竞赛。' }} 所有操作都会根据当前账号权限显示。</p>
      </div>
      <div class="heading-summary">
        <div><strong>{{ total }}</strong><span>总数</span></div>
        <div><strong>{{ runningCount }}</strong><span>进行中</span></div>
        <div><strong>{{ joinedCount }}</strong><span>参与人次</span></div>
      </div>
    </section>

    <section class="filter-panel">
      <div class="filter-main">
        <el-input v-model="searchText" class="keyword-input" clearable placeholder="搜索标题或描述" :prefix-icon="Search" @keyup.enter="getList" />
        <el-select v-model="statusFilter" class="status-select" placeholder="全部状态" clearable>
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="getList">查询</el-button>
        <el-button :icon="Refresh" @click="resetFilters">重置</el-button>
      </div>
      <div class="filter-actions">
        <el-button v-has="'problem:contest:add'" type="primary" :icon="Plus" @click="handleAdd">新建{{ isHomework ? '作业' : '竞赛' }}</el-button>
        <el-button v-has="'problem:contest:edit'" :disabled="single" :icon="EditPen" @click="handleUpdate()">编辑</el-button>
        <el-button v-has="'problem:contest:remove'" type="danger" plain :disabled="multiple" :icon="Delete" @click="handleDeleteSelected">批量删除</el-button>
        <right-tool-bar v-model:showSearch="showSearch" :columns="columns" @query-table="getList" />
      </div>
    </section>

    <section class="table-panel">
      <div class="table-panel__heading">
        <div><h2>{{ isHomework ? '作业列表' : '竞赛列表' }}</h2><span>共 {{ total }} 条记录</span></div>
        <span class="permission-note"><el-icon><Lock /></el-icon> 操作按权限字符串过滤</span>
      </div>
      <el-table v-loading="isLoading" :data="filteredList" class="management-table" row-key="contestId" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column v-if="columns[0].visible" label="ID" prop="contestId" width="88" show-overflow-tooltip />
        <el-table-column v-if="columns[1].visible" label="名称" min-width="220" show-overflow-tooltip>
          <template #default="{row}"><div class="title-cell"><strong>{{ row.title }}</strong><small>{{ truncate(row.description) }}</small></div></template>
        </el-table-column>
        <el-table-column v-if="columns[2].visible" label="访问策略" width="108" align="center">
          <template #default="{row}"><el-tag :type="authTagType(row.auth)" effect="plain" size="small">{{ authText(row.auth) }}</el-tag></template>
        </el-table-column>
        <el-table-column v-if="columns[3].visible" label="时间安排" min-width="230">
          <template #default="{row}"><div class="time-cell"><span>{{ formatDate(row.startTime) }}</span><i>→</i><span>{{ formatDate(row.endTime) }}</span></div></template>
        </el-table-column>
        <el-table-column v-if="columns[4].visible" label="状态" width="100" align="center">
          <template #default="{row}"><span class="status-pill" :class="`status-pill--${statusOf(row).tone}`"><i></i>{{ statusOf(row).label }}</span></template>
        </el-table-column>
        <el-table-column v-if="columns[5].visible" label="题单" width="90" align="center">
          <template #default="{row}"><el-link v-if="row.listId && canViewListLink" type="primary" :underline="false" @click="goToList(row.listId)">#{{ shortListId(row.listId) }}</el-link><span v-else class="muted">{{ row.listId ? '无权限' : '未关联' }}</span></template>
        </el-table-column>
        <el-table-column v-if="columns[6].visible" label="参与人数" prop="joinedNumber" width="100" align="center">
          <template #default="{row}">{{ row.joinedNumber ?? 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="right">
          <template #default="{row}">
            <el-button v-has="'problem:contest:edit'" link type="primary" :icon="EditPen" @click="handleUpdate(row)">编辑</el-button>
            <el-dropdown v-if="canUseMore" trigger="click" @command="(command: string) => handleCommand(command, row)">
              <el-button link type="primary" :icon="MoreFilled">更多</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="canManageJoined" command="joined" :icon="User">{{ isHomework ? '提交管理' : '参赛管理' }}</el-dropdown-item>
                  <el-dropdown-item v-if="canViewStatistics" command="problem-statistic" :icon="DataAnalysis">题目统计</el-dropdown-item>
                  <el-dropdown-item v-if="canViewStatistics" command="user-statistic" :icon="Histogram">用户统计</el-dropdown-item>
                  <el-dropdown-item v-if="isHomework && canEditContest" command="supplement" :icon="Clock">设置迟交</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button v-has="'problem:contest:remove'" link type="danger" :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.currentPage" v-model:limit="queryParams.pageSize" @pagination="getList" />
      <el-empty v-if="!isLoading && filteredList.length === 0" description="没有找到符合条件的记录" :image-size="84" />
    </section>

    <el-dialog v-model="open" :title="dialogTitle" width="min(820px, 94vw)" class="contest-dialog" append-to-body destroy-on-close>
      <div class="dialog-intro"><span class="dialog-intro__icon"><el-icon><component :is="isHomework ? Notebook : Flag" /></el-icon></span><div><strong>{{ dialogState === 'add' ? '创建新的' : '编辑' }}{{ isHomework ? '作业' : '竞赛' }}</strong><p>完善基本信息、访问策略和时间安排后再保存。</p></div></div>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="contest-form" @submit.prevent>
        <div class="form-section"><div class="form-section__title"><span>01</span><div><strong>基本信息</strong><small>用于列表展示和内容识别</small></div></div><div class="form-grid form-grid--basic">
          <el-form-item label="名称" prop="title"><el-input v-model="form.title" maxlength="100" show-word-limit placeholder="例如：2026 春季算法训练赛" /></el-form-item>
          <el-form-item label="题单" prop="listId"><el-input v-model="selectedListText" class="selected-list-input" readonly :title="selectedListText" placeholder="请选择题单"><template #append><el-button :disabled="!canViewLists" :icon="List" @click="openSelectList = true">选择</el-button></template></el-input><small class="field-tip">{{ canViewLists ? `题单决定本次${isHomework ? '作业' : '竞赛'}包含的题目` : '当前账号没有题单查看权限，请联系管理员' }}</small></el-form-item>
        </div></div>
        <div class="form-section"><div class="form-section__title"><span>02</span><div><strong>访问策略</strong><small>控制谁可以进入</small></div></div><el-form-item label="可见范围" prop="auth"><el-radio-group v-model="form.auth" class="auth-options"><el-radio-button v-for="item in dict.contestAuth" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button></el-radio-group><small class="field-tip">私有赛需要密码，白名单由用户组权限决定。</small></el-form-item><el-form-item v-if="form.auth === ContestAuth.PRIVATE" label="访问密码" prop="pwd"><el-input v-model="form.pwd" show-password maxlength="64" placeholder="请输入访问密码" /></el-form-item></div>
        <div class="form-section"><div class="form-section__title"><span>03</span><div><strong>时间安排</strong><small>开始时间必须早于结束时间</small></div></div><div class="form-grid"><el-form-item label="开始时间" prop="startTime"><el-date-picker v-model="form.startTime" class="full-width" type="datetime" placeholder="选择开始时间" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DDTHH:mm:ss" :disabled-date="disableStartDate" /></el-form-item><el-form-item label="结束时间" prop="endTime"><el-date-picker v-model="form.endTime" class="full-width" type="datetime" placeholder="选择结束时间" format="YYYY-MM-DD HH:mm:ss" value-format="YYYY-MM-DDTHH:mm:ss" :disabled-date="disableEndDate" /></el-form-item></div></div>
        <div class="form-section"><div class="form-section__title"><span>04</span><div><strong>说明</strong><small>可选，支持 Markdown</small></div></div><el-form-item prop="description"><MarkDownEditor v-model="form.description" /></el-form-item></div>
      </el-form>
      <template #footer><el-button @click="cancel">取消</el-button><el-button v-has="dialogState === 'add' ? 'problem:contest:add' : 'problem:contest:edit'" type="primary" :loading="isUpdateLoading || isAddLoading" @click="submitForm">保存{{ isHomework ? '作业' : '竞赛' }}</el-button></template>
    </el-dialog>

    <el-dialog v-model="openSelectList" title="选择题单" width="min(960px, 94vw)" class="list-picker-dialog" append-to-body destroy-on-close><ListView v-model="form.listId" v-model:isOpen="openSelectList" @select="handleListSelect" /></el-dialog>
  </main>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import {ElMessageBox, type FormInstance, type FormRules} from "element-plus";
import {Clock, Collection, DataAnalysis, Delete, EditPen, Flag, Histogram, List, Lock, MoreFilled, Notebook, Plus, Refresh, Search, User} from "@element-plus/icons-vue";
import {ContestAuth, ContestType, type ContestForm, type ContestView, debouncedAddContest, debouncedGetContestAdmin, debouncedUpdateContest, dict, type PageContest, removeContest} from "@/api/contest";
import {authTagType, authText, formatDate as formatContestDate} from "@/utils/contest";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import MarkDownEditor from "@/components/MarkDownEditor/MarkDownEditor.vue";
import ListView from "@/components/ListView/ListView.vue";
import type {IdType} from "@/api/common.ts";
import type {ListView as ProblemListView} from "@/api/list";
import {getList as fetchProblemLists} from "@/api/list";
import {useRouter} from "vue-router";
import {hasAnyPerm, hasPerm} from "@/utils/authUtil.ts";

const props = defineProps<{mode: ContestType}>();
const router = useRouter();
const isHomework = computed(() => props.mode === ContestType.HOMEWORK);
const pageTitle = computed(() => isHomework.value ? '作业管理' : '竞赛管理');
const queryParams = reactive<PageContest>({currentPage: 1, pageSize: 20, type: isHomework.value ? 'HOMEWORK' : 'CONTEST'});
const tableList = reactive<ContestView[]>([]);
const total = ref(0);
const showSearch = ref(true);
const searchText = ref('');
const statusFilter = ref('');
const selectedIds = ref<IdType[]>([]);
const open = ref(false);
const openSelectList = ref(false);
const dialogState = ref<'add' | 'edit'>('add');
const formRef = ref<FormInstance>();
const form = reactive<ContestForm>({type: props.mode, title: '', auth: ContestAuth.PUBLIC, listId: undefined, startTime: undefined, endTime: undefined, pwd: '', description: ''});
const columns = useColumn(['ID', '名称', '访问策略', '时间安排', '状态', '题单', '参与人数']).columns;
const rules: FormRules<ContestForm> = {title: [{required: true, message: '请输入名称', trigger: 'blur'}], listId: [{required: true, message: '请选择题单', trigger: 'change'}], startTime: [{required: true, message: '请选择开始时间', trigger: 'change'}], endTime: [{required: true, message: '请选择结束时间', trigger: 'change'}]};
const statusOptions = [{label: '未开始', value: 'pending'}, {label: '进行中', value: 'running'}, {label: '已结束', value: 'ended'}];
const dialogTitle = computed(() => `${dialogState.value === 'add' ? '新建' : '编辑'}${isHomework.value ? '作业' : '竞赛'}`);
const selectedListName = ref('');
const selectedListText = computed({get: () => form.listId ? `题单 #${shortListId(form.listId)}${selectedListName.value ? ` · ${selectedListName.value}` : ''}` : '', set: () => undefined});
const canViewLists = computed(() => hasPerm('problem:list:list'));
const canEditContest = computed(() => hasPerm('problem:contest:edit'));
const canViewStatistics = computed(() => hasPerm('problem:contest:statistic'));
const canManageJoined = computed(() => hasAnyPerm(['problem:contest:list', 'user:user:list']));
const canUseMore = computed(() => hasAnyPerm(['problem:contest:edit', 'problem:contest:statistic', 'problem:contest:rank']));
const canViewListLink = computed(() => hasAnyPerm(['problem:list:add-problem', 'problem:list:del-problem', 'problem:problem:list']));
const single = computed(() => selectedIds.value.length !== 1);
const multiple = computed(() => selectedIds.value.length === 0);
const joinedCount = computed(() => tableList.reduce((sum, item) => sum + (item.joinedNumber || 0), 0));
const runningCount = computed(() => tableList.filter(item => statusOf(item).tone === 'running').length);
const filteredList = computed(() => {const keyword = searchText.value.trim().toLowerCase(); return tableList.filter(item => {const textMatch = !keyword || `${item.title} ${item.description || ''}`.toLowerCase().includes(keyword); return textMatch && (!statusFilter.value || statusOf(item).tone === statusFilter.value);});});

const statusOf = (row: ContestView) => {const now = Date.now(); const start = row.startTime ? new Date(row.startTime).getTime() : 0; const end = row.endTime ? new Date(row.endTime).getTime() : 0; if (start > now) return {label: '未开始', tone: 'pending'}; if (end && end < now) return {label: '已结束', tone: 'ended'}; return {label: '进行中', tone: 'running'};};
const truncate = (value?: string) => value ? value.replace(/[#*`]/g, '').trim().slice(0, 42) : '暂无描述';
const formatDate = (value?: string) => value ? formatContestDate(value) : '待定';
const toDateTimeInput = (value?: string) => value ? value.replace(' ', 'T').slice(0, 19) : undefined;
const shortListId = (value: IdType) => {const text = String(value); return text.length > 16 ? `${text.slice(0, 8)}…${text.slice(-5)}` : text;};
const resetForm = () => {Object.assign(form, {contestId: undefined, type: props.mode, title: '', auth: ContestAuth.PUBLIC, listId: undefined, startTime: undefined, endTime: undefined, pwd: '', description: ''}); selectedListName.value = '';};
const resetFilters = () => {searchText.value = ''; statusFilter.value = ''; queryParams.currentPage = 1; getList();};
const handleSelectionChange = (selection: ContestView[]) => {selectedIds.value = selection.map(item => item.contestId);};
const handleListSelect = (list: ProblemListView) => {form.listId = list.listId; selectedListName.value = list.listName;};
const goToList = (id: IdType) => {if (router.hasRoute('list-problem')) router.push({name: 'list-problem', params: {id}});};
const handleCommand = (command: string, row: ContestView) => {const params = {contestId: row.contestId}; if (command === 'joined') router.push({name: 'user-joined', params}); if (command === 'problem-statistic') router.push({name: 'problem-statistic', params}); if (command === 'user-statistic') router.push({name: 'user-statistic', params}); if (command === 'supplement') router.push({name: 'supplement', params});};
const handleAdd = () => {resetForm(); dialogState.value = 'add'; open.value = true;};
const hydrateListName = async (id?: IdType) => {if (!id || selectedListName.value) return; try {const result = await fetchProblemLists({currentPage: 1, pageSize: 1, asc: true, listId: id}); selectedListName.value = result.data.find(item => String(item.listId) === String(id))?.listName || '';} catch { /* 题单名称不是编辑的阻断条件 */ }};
const handleUpdate = (row?: ContestView) => {const target = row || tableList.find(item => item.contestId === selectedIds.value[0]); if (!target) return; resetForm(); Object.assign(form, {...target, type: props.mode, startTime: toDateTimeInput(target.startTime), endTime: toDateTimeInput(target.endTime)}); dialogState.value = 'edit'; open.value = true; void hydrateListName(target.listId);};
const handleDelete = async (row: ContestView) => {try {await ElMessageBox.confirm(`确认删除“${row.title}”吗？删除后无法恢复。`, '删除确认', {type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消'}); await removeContest(row.contestId); getList();} catch { /* 用户取消 */ }};
const handleDeleteSelected = async () => {try {await ElMessageBox.confirm(`确认删除选中的 ${selectedIds.value.length} 项吗？删除后无法恢复。`, '批量删除确认', {type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消'}); await removeContest(selectedIds.value); getList();} catch { /* 用户取消 */ }};
const cancel = () => {open.value = false; resetForm();};
const disableStartDate = (date: Date) => !!form.endTime && date.getTime() > new Date(form.endTime).getTime();
const disableEndDate = (date: Date) => !!form.startTime && date.getTime() < new Date(form.startTime).getTime();
const finishDialog = () => {open.value = false; resetForm(); getList();};
const {loading: addLoading, isLoading: isAddLoading, add} = debouncedAddContest(form, finishDialog);
const {loading: updateLoading, isLoading: isUpdateLoading, update} = debouncedUpdateContest(form, finishDialog);
const submitForm = async () => {const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; if (dialogState.value === 'add') {addLoading(); add();} else {updateLoading(); update();}};
const {loading: listLoading, isLoading, get: getContest} = debouncedGetContestAdmin(queryParams, data => {tableList.splice(0, tableList.length, ...data.data); total.value = data.totalRecords;});
const getList = () => {listLoading(); getContest();};
getList();
</script>

<style scoped>
.management-page { padding: 0 10px 28px; box-sizing: border-box; }
.management-page { width: 100%; max-width: 1440px; margin: 0 auto; color: var(--el-text-color-primary); }.management-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 22px; }.heading-title-row { display: flex; align-items: center; gap: 13px; }.heading-icon { display: grid; width: 44px; height: 44px; place-items: center; border-radius: 12px; font-size: 22px; }.heading-icon--blue { color: #2563eb; background: rgb(37 99 235 / 12%); }.heading-icon--amber { color: #d97706; background: rgb(217 119 6 / 12%); }.page-kicker { margin: 0 0 4px; color: var(--el-text-color-secondary); font-size: 10px; font-weight: 800; letter-spacing: .16em; }.heading-title-row h1 { margin: 0; font-size: 28px; letter-spacing: -.04em; }.page-description { margin: 12px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }.heading-summary { display: flex; border: 1px solid var(--el-border-color-lighter); border-radius: 10px; background: var(--el-bg-color); }.heading-summary div { min-width: 90px; padding: 11px 17px; text-align: center; }.heading-summary div + div { border-left: 1px solid var(--el-border-color-lighter); }.heading-summary strong, .heading-summary span { display: block; }.heading-summary strong { font-size: 20px; }.heading-summary span { margin-top: 2px; color: var(--el-text-color-secondary); font-size: 11px; }.filter-panel, .table-panel { border: 1px solid var(--el-border-color-lighter); border-radius: 12px; background: var(--el-bg-color); box-shadow: 0 4px 18px rgb(15 23 42 / 4%); }.filter-panel { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; padding: 13px 16px; }.filter-main, .filter-actions { display: flex; align-items: center; gap: 9px; flex-wrap: wrap; }.filter-actions { justify-content: flex-end; }.keyword-input { width: min(280px, 35vw); }.status-select { width: 125px; }.table-panel { padding: 18px 20px 12px; }.table-panel__heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 13px; }.table-panel__heading div { display: flex; align-items: baseline; gap: 9px; }.table-panel__heading h2 { margin: 0; font-size: 17px; }.table-panel__heading span { color: var(--el-text-color-secondary); font-size: 12px; }.permission-note { display: inline-flex; align-items: center; gap: 5px; }.management-table :deep(.el-table__cell) { padding: 12px 0; }.management-table :deep(.el-table__header-wrapper th) { color: var(--el-text-color-secondary); font-size: 12px; font-weight: 650; background: var(--el-fill-color-light); }.title-cell, .time-cell { display: flex; flex-direction: column; gap: 4px; min-width: 0; }.title-cell strong { overflow: hidden; font-size: 13px; white-space: nowrap; text-overflow: ellipsis; }.title-cell small { overflow: hidden; color: var(--el-text-color-secondary); font-size: 11px; white-space: nowrap; text-overflow: ellipsis; }.time-cell { color: var(--el-text-color-secondary); font-family: var(--el-font-family); font-size: 12px; }.time-cell i { display: none; }.status-pill { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; }.status-pill i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }.status-pill--running { color: var(--el-color-success); }.status-pill--pending { color: var(--el-color-warning); }.status-pill--ended { color: var(--el-text-color-placeholder); }.muted { color: var(--el-text-color-placeholder); font-size: 12px; }.contest-dialog :deep(.el-dialog__body), .list-picker-dialog :deep(.el-dialog__body) { padding-top: 8px; }.dialog-intro { display: flex; align-items: center; gap: 11px; margin-bottom: 20px; padding: 13px 15px; border: 1px solid var(--el-border-color-lighter); border-radius: 9px; background: var(--el-fill-color-light); }.dialog-intro__icon { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 9px; color: var(--el-color-primary); background: var(--el-color-primary-light-8); font-size: 17px; }.dialog-intro strong, .dialog-intro p { display: block; }.dialog-intro strong { font-size: 13px; }.dialog-intro p { margin: 2px 0 0; color: var(--el-text-color-secondary); font-size: 11px; }.form-section { padding: 18px 0 5px; border-top: 1px solid var(--el-border-color-lighter); }.form-section:first-child { padding-top: 0; border-top: 0; }.form-section__title { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 14px; }.form-section__title > span { color: var(--el-color-primary); font: 700 11px/1.4 var(--code-font-family, monospace); }.form-section__title strong, .form-section__title small { display: block; }.form-section__title strong { font-size: 13px; }.form-section__title small, .field-tip { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }.form-grid--basic { grid-template-columns: 1.15fr .85fr; }.full-width { width: 100%; }.contest-form :deep(.el-form-item) { margin-bottom: 15px; }.contest-form :deep(.el-form-item__label) { height: auto; margin-bottom: 5px; color: var(--el-text-color-primary); font-size: 12px; line-height: 1.4; }.auth-options { display: flex; }.auth-options :deep(.el-radio-button__inner) { min-width: 88px; }.tool-list { display: flex; }
@media (max-width: 900px) { .management-heading { align-items: flex-start; flex-direction: column; }.heading-summary { width: 100%; }.heading-summary div { flex: 1; }.filter-panel { align-items: stretch; flex-direction: column; }.filter-main, .filter-actions { width: 100%; }.filter-actions { justify-content: flex-start; }.keyword-input { width: min(100%, 320px); } }
@media (max-width: 650px) { .management-heading { gap: 16px; }.heading-title-row h1 { font-size: 24px; }.page-description { line-height: 1.7; }.filter-panel, .table-panel { padding: 14px; }.filter-main > .el-input, .filter-main > .el-select { width: 100%; }.table-panel__heading { align-items: flex-start; flex-direction: column; gap: 7px; }.permission-note { display: none; }.management-table :deep(.el-table__cell) { padding: 10px 0; }.management-table :deep(.el-table__cell:nth-child(4)) { min-width: 190px; }.form-grid, .form-grid--basic { grid-template-columns: 1fr; }.heading-summary div { min-width: 0; padding: 10px 8px; }.heading-summary strong { font-size: 17px; }.auth-options :deep(.el-radio-button__inner) { min-width: 0; padding: 9px 11px; } }
</style>
