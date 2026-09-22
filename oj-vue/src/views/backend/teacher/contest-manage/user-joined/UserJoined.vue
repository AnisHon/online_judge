<template>
  <ContestSubPageShell
      title="参赛管理"
      kicker="PARTICIPANTS / ACCESS"
      description="管理进入本场竞赛或作业的用户，并维护参与名单。"
      :icon="User"
      :stats="stats"
  >
    <template #actions>
      <right-tool-bar :search="false" :columns="columns" @query-table="getList" />
      <el-button v-has="['problem:contest:edit', 'user:user:list']" type="primary" :icon="Plus" @click="userDialog = true">添加用户</el-button>
      <el-button v-has="['problem:contest:edit', 'user:class:list']" plain :icon="School" @click="classDialog = true">按班级添加</el-button>
      <el-button v-has="'problem:contest:edit'" type="danger" plain :disabled="multiple" :icon="Delete" @click="handleDelete()">移除用户</el-button>
    </template>

    <div class="content-heading"><div><strong>参与名单</strong><span>支持按选择结果批量移除</span></div><el-button text :icon="Refresh" :loading="isLoading" @click="getList">刷新</el-button></div>
    <el-table v-loading="isLoading" :data="tableList" class="participants-table" row-key="userId" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="48" align="center" />
      <el-table-column label="用户" min-width="190">
        <template #default="{row}"><div class="user-cell"><span class="user-avatar">{{ String(row.nikeName || row.userName || '?').slice(0, 1) }}</span><span><strong>{{ row.nikeName || row.userName || '未命名用户' }}</strong><small>ID: {{ row.userId }}</small></span></div></template>
      </el-table-column>
      <el-table-column label="账号" prop="userName" min-width="150" show-overflow-tooltip />
      <el-table-column label="邮箱" prop="email" min-width="190" show-overflow-tooltip />
      <el-table-column label="加入时间" prop="createTime" min-width="150" show-overflow-tooltip />
      <el-table-column label="备注" prop="remark" min-width="130" show-overflow-tooltip />
      <el-table-column label="操作" width="90" fixed="right" align="right">
        <template #default="{row}"><el-button v-has="'problem:contest:edit'" link type="danger" :icon="Delete" @click="handleDelete(row)">移除</el-button></template>
      </el-table-column>
      <template #empty><el-empty description="暂无参与用户" /></template>
    </el-table>

    <el-dialog v-model="userDialog" title="添加用户" width="min(760px, 94vw)" append-to-body destroy-on-close>
      <div class="dialog-tip"><el-icon><InfoFilled /></el-icon><span>可以搜索并多选用户，提交后会自动忽略已经在名单中的用户。</span></div>
      <UserViewer v-model:ids="userIds" :loading="false" />
      <template #footer><el-button @click="cancel">取消</el-button><el-button v-has="'problem:contest:edit'" type="primary" :loading="submitLoading" @click="submit">确认添加</el-button></template>
    </el-dialog>

    <el-dialog v-model="classDialog" title="按班级添加" width="min(960px, 94vw)" append-to-body destroy-on-close>
      <div class="dialog-tip"><el-icon><InfoFilled /></el-icon><span>选择班级后会将班级成员批量加入当前{{ isHomework ? '作业' : '竞赛' }}。</span></div>
      <ClassView @select-class="handleSelectClass" />
    </el-dialog>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import {computed, reactive, ref, watch} from "vue";
import {Delete, InfoFilled, Plus, Refresh, School, User} from "@element-plus/icons-vue";
import {ElMessageBox, ElNotification} from "element-plus";
import {useRoute} from "vue-router";
import type {IdType} from "@/api/common.ts";
import type {UserView} from "@/api/user";
import {addUserByClass, addUserDirect, getUserByContest, removeUser} from "@/api/contest/user.ts";
import UserViewer from "@/components/user-viewer/UserViewer.vue";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import {useColumn} from "@/hooks/useColumn";
import ClassView from "@/views/backend/teacher/contest-manage/component/ClassView.vue";
import ContestSubPageShell from "@/views/backend/teacher/contest-manage/component/ContestSubPageShell.vue";

const route = useRoute();
const contestId = computed(() => String(route.params.contestId || '') as IdType);
const isHomework = computed(() => route.path.includes('homework'));
const tableList = reactive<UserView[]>([]);
const userIds = reactive<IdType[]>([]);
const selectedIds = ref<IdType[]>([]);
const userDialog = ref(false);
const classDialog = ref(false);
const isLoading = ref(false);
const submitLoading = ref(false);
const multiple = computed(() => selectedIds.value.length === 0);
const columns = useColumn(['用户', '账号', '邮箱', '加入时间', '备注']).columns;
const stats = computed(() => [
  {label: '参与用户', value: tableList.length, tone: 'blue'},
  {label: '已选择', value: selectedIds.value.length, tone: 'violet'},
  {label: '有邮箱', value: tableList.filter(user => !!user.email).length, tone: 'green'},
  {label: '名单状态', value: tableList.length ? '已建立' : '待添加', tone: tableList.length ? 'green' : 'amber'}
]);

const getList = async () => {isLoading.value = true; try {tableList.splice(0, tableList.length, ...(await getUserByContest(contestId.value)));} finally {isLoading.value = false;}};
const handleSelectionChange = (selection: UserView[]) => {selectedIds.value = selection.map(user => user.userId);};
const handleDelete = async (row?: UserView) => {const ids = row ? [row.userId] : selectedIds.value; try {await ElMessageBox.confirm(`确认从名单中移除 ${ids.length} 位用户吗？`, '移除确认', {type: 'warning', confirmButtonText: '确认移除', cancelButtonText: '取消'}); await removeUser(contestId.value, row ? row.userId : ids); await getList();} catch { /* 用户取消 */ }};
const submit = async () => {if (!userIds.length) {ElNotification.warning('请选择至少一位用户'); return;} submitLoading.value = true; try {await addUserDirect(contestId.value, userIds); userIds.splice(0); userDialog.value = false; await getList();} finally {submitLoading.value = false;}};
const handleSelectClass = async (classId: IdType) => {await addUserByClass(contestId.value, classId); classDialog.value = false; await getList();};
const cancel = () => {userDialog.value = false; userIds.splice(0);};
watch(contestId, () => void getList());
getList();
</script>

<style scoped>
.content-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 12px; }.content-heading strong, .content-heading span { display: block; }.content-heading strong { font-size: 15px; }.content-heading span { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }.participants-table :deep(.el-table__cell) { padding: 12px 0; }.user-cell { display: flex; align-items: center; gap: 10px; min-width: 0; }.user-avatar { display: grid; width: 32px; height: 32px; flex: 0 0 auto; place-items: center; border-radius: 9px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 13px; font-weight: 700; }.user-cell > span:last-child { display: flex; min-width: 0; flex-direction: column; gap: 2px; }.user-cell strong { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }.user-cell small { color: var(--el-text-color-secondary); font-size: 11px; }.dialog-tip { display: flex; align-items: center; gap: 7px; margin-bottom: 15px; padding: 10px 12px; border-radius: 8px; color: var(--el-text-color-secondary); background: var(--el-fill-color-light); font-size: 12px; }
</style>
