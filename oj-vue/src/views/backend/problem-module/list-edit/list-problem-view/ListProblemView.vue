<template>
  <div class="app-container">
    <el-row  class="mb8">
      <el-col v-show="showSearch">
        <ListProblemViewForm :list-id="listId" v-model="queryParams" @query="getList"/>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>
    <!--    ['问题ID', '题目', '问题描述', '问题来源', '问题类型' ,'问题权限', '创建时间', '提示']-->
    <el-table ref="tableRef" v-loading="isLoading" :data="tableList" row-key="problemId" :reserve-selection="true" @select="handleSelect" @select-all="handleSelectAll">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="问题ID" align="center" prop="problemId" v-if="columns[0].visible" />
      <el-table-column label="题目" align="center" prop="title" v-if="columns[1].visible" />
      <el-table-column label="问题描述" align="center" prop="description" v-if="columns[2].visible">
        <template v-slot="scope">
          <markdown-preview :text="scope.row.description"/>
        </template>
      </el-table-column>
      <el-table-column label="问题来源" align="center" prop="source" v-if="columns[3].visible" />
      <el-table-column label="问题类型" align="center" prop="type" v-if="columns[4].visible">
        <template v-slot="scope">
          <el-tag type="primary">{{ problemTypeToString(scope.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="问题权限" align="center" prop="auth" v-if="columns[5].visible" >
        <template v-slot="scope">
          <el-tag :type="getAuthCardType(scope.row.auth)">{{ getAuthText(scope.row.auth) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[6].visible" />
      <el-table-column label="提示" width="60" align="center" prop="hint" v-if="columns[7].visible" />
    </el-table>

    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.currentPage"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />
    <el-alert v-if="errorMessage" class="picker-error" :title="errorMessage" type="error" show-icon closable @close="errorMessage = ''" />
  </div>
</template>

<script setup lang="ts">
import {nextTick, reactive, ref, watch} from "vue";
import {
  ProblemAuth,
  type ProblemView,
} from "@/api/problem";
import {useStatuesColumn} from "@/hooks/useColumn";
import {problemTypeToString} from "@/utils/problem";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {fetchProblemsNotInList, type ListProblemQuery} from "@/api/list";
import ListProblemViewForm from "@/views/backend/problem-module/list-edit/list-problem-view/ListProblemViewForm.vue";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import type {TableInstance} from "element-plus";
import type {IdType} from "@/api/common.ts";

const {listId} = defineProps<{
  listId: IdType
}>()
// 显示搜索栏
const showSearch = ref(true);

const total = ref(0)

// 查询需要的表单数据
const queryParams = reactive<ListProblemQuery>({
  currentPage: 1,
  pageSize: 20,
  listId: listId,
  problemId: undefined,
  title: undefined,
  type: undefined,
  tagIds: [],
});

const {columns} = useStatuesColumn(
    ['问题ID', '题目', '问题描述', '问题来源', '问题类型' ,'问题权限', '创建时间', '提示'],
    [false, true, false, true, true, true, false, false]
);
const tableList = reactive<ProblemView[]>([]);
const tableRef = ref<TableInstance>();
const isLoading = ref(false);
const errorMessage = ref('');
let requestVersion = 0;

// 获取列表
const getList = async () => {
  const version = ++requestVersion;
  isLoading.value = true;
  errorMessage.value = '';
  try {
    const data = await fetchProblemsNotInList({...queryParams});
    if (version !== requestVersion) return;
    tableList.splice(0, tableList.length, ...data.data);
    total.value = data.totalRecords;
    syncSelection();
  } catch (error) {
    if (version !== requestVersion) return;
    errorMessage.value = error instanceof Error ? error.message : '题目加载失败，请稍后重试';
  } finally {
    if (version === requestVersion) isLoading.value = false;
  }
}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = defineModel<IdType[]>({default: () => []})

const selectedIds = () => new Set((ids.value || []).map(id => String(id)));

const syncSelection = () => {
  const selected = selectedIds();
  void nextTick(() => tableList.forEach(row => {
    tableRef.value?.toggleRowSelection(row, selected.has(String(row.problemId)), true);
  }));
};

const updateSelectionState = () => {
  single.value = (ids.value || []).length !== 1;
  multiple.value = !(ids.value || []).length;
};

const handleSelect = (_selection: ProblemView[], row: ProblemView) => {
  const selected = selectedIds();
  const id = String(row.problemId);
  if (selected.has(id)) selected.delete(id); else selected.add(id);
  ids.value = [...selected];
  updateSelectionState();
};

const handleSelectAll = (selection: ProblemView[]) => {
  const selected = selectedIds();
  const pageIds = tableList.map(row => String(row.problemId));
  if (selection.length === tableList.length) pageIds.forEach(id => selected.add(id));
  else pageIds.forEach(id => selected.delete(id));
  ids.value = [...selected];
  updateSelectionState();
}

const getAuthText = (auth: ProblemAuth) => {
  return auth === ProblemAuth.CONTEST ? "比赛题目" : "普通题目";
}

const getAuthCardType = (auth: ProblemAuth) => {
  return auth === ProblemAuth.CONTEST ? "danger" : "success";
}

watch(() => ids.value, () => {
  updateSelectionState();
  syncSelection();
}, {deep: true});

void getList();



</script>

<style scoped>
.picker-error { margin-top: 12px; }

</style>

<style scoped>
.app-container {
  :deep(.inline-form) {
    .el-input {
      --el-input-width: 220px;
    }

    .el-select {
      --el-select-width: 220px;
    }
  }
  :deep(.el-table__row .el-dropdown) {
    height: 23px;
  }

}
</style>
