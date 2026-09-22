<template>
  <ProblemModuleShell
    title="题单题目"
    kicker="COLLECTION / PROBLEMS"
    :description="`管理题单 #${listId} 中的题目、顺序与分数。`"
    :icon="List"
    tone="green"
  >

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="ArrowLeft"
            size="small"
            @click="back"
            v-has="'problem:list:add-problem'"
        >返回</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'problem:list:add-problem'"
        >添加题目</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="Edit"
            size="small"
            @click="isEdit = !isEdit"
            v-has="'problem:list:add-problem'"
        >{{ isEdit ? '完成编辑' : '编辑分数' }}</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="primary"
            :loading="isOrderSaving"
            :disabled="!hasPendingOrderChanges"
            icon="Upload"
            size="small"
            @click="saveProblemOrder"
            v-has="'problem:list:add-problem'"
        >保存排序<span v-if="pendingOrderCount">（{{ pendingOrderCount }}）</span></el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="delete"
            size="small"
            :disabled="multiple || isDeleting"
            :loading="isDeleting"
            @click="handleDelete"
            v-has="'problem:list:del-problem'"
        >删除题目</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <!--    ['问题ID', '题目', '问题描述', '问题来源', '问题类型' ,'问题权限', '创建时间', '提示']-->
    <div v-if="hasPendingOrderChanges" class="order-notice">
      <el-icon><InfoFilled /></el-icon>
      <span>拖动题目调整顺序后，点击“保存排序”一次性提交。</span>
      <el-button link type="primary" :disabled="isOrderSaving" @click="resetProblemOrder">撤销排序</el-button>
    </div>
    <el-table ref="tableRef" v-loading="isLoading" class="list-problem-table" :data="sortedTableList" row-key="problemId" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="问题ID" align="center" prop="problemId" v-if="columns[0].visible" show-overflow-tooltip />
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
      <el-table-column label="问题顺序" align="center" prop="problemOrder" v-if="columns[8].visible">
        <template v-slot="scope">
          <div
              class="order-cell"
              :class="{ 'is-drag-over': dragOverProblemId === String(scope.row.problemId) }"
              @dragover.prevent="handleDragOver(scope.row)"
              @drop.prevent="handleDrop(scope.row)"
          >
            <button
                type="button"
                class="drag-handle"
                :class="{ 'is-disabled': !isEdit }"
                :draggable="isEdit"
                :aria-disabled="!isEdit"
                :aria-grabbed="draggingProblemId === String(scope.row.problemId)"
                :aria-label="`调整 ${scope.row.title || '题目'} 的顺序，可使用上下方向键移动`"
                title="拖动调整顺序；编辑状态下也可使用上下方向键"
                @dragstart="handleDragStart(scope.row, $event)"
                @dragend="handleDragEnd"
                @keydown.up.prevent="moveRow(scope.$index, -1)"
                @keydown.down.prevent="moveRow(scope.$index, 1)"
            ><el-icon><Rank /></el-icon></button>
            <span class="order-number">{{ scope.row.problemOrder }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="问题分数" align="center" prop="score" v-if="columns[9].visible" >
        <template v-slot="scope">
          <el-input-number v-model="scope.row.tempScore" :disabled="!isEdit || savingScoreIds.has(String(scope.row.problemId))" @keyup.enter="$event.target.blur()"  :controls="false" :precision="2" @blur="handleScoreUpdate(scope.row)"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'problem:list:del-problem'"
          >删除</el-link>
        </template>
      </el-table-column>
    </el-table>

    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.currentPage"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />


    <el-dialog title="添加题目到题单" v-model="open" class="add-problem-dialog" width="min(980px, 94vw)" append-to-body destroy-on-close>
      <div class="dialog-intro"><span class="dialog-icon"><el-icon><List /></el-icon></span><div><strong>选择要加入的题目</strong><p>可以按题目、题型或标签筛选，选中后一次性加入当前题单。</p></div></div>
      <div class="picker-body">
        <ListProblemView :list-id="listId" v-model="addProblemIds"/>
      </div>
      <template #footer>
        <span class="selection-summary">已选择 {{ addProblemIds.length }} 道题目</span>
        <el-button @click="cancel">取消</el-button>
        <el-button type="primary" :disabled="!addProblemIds.length" @click="submit" :loading="isAddLoading">加入题单</el-button>
      </template>
    </el-dialog>


  </ProblemModuleShell>
</template>

<script setup lang="ts">
import {computed, reactive, ref, watch} from "vue";
import {
  ProblemAuth,
} from "@/api/problem";
import {useStatuesColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {ElInputNumber, ElMessage, ElMessageBox, type TableInstance} from "element-plus";
import {problemTypeToString} from "@/utils/problem";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {useRoute, useRouter} from "vue-router";
import {
  addProblemToList,
  getProblemsAdmin, delProblemFromList,
  type ListProblemQuery, type ProblemInListView,
  batchUpdateProblemOrder, updateProblemRelation
} from "@/api/list";
import ListProblemView from "@/views/backend/problem-module/list-edit/list-problem-view/ListProblemView.vue";
import type {IdType} from "@/api/common.ts";
import {InfoFilled, List, Rank} from "@element-plus/icons-vue";
import ProblemModuleShell from "@/views/backend/problem-module/component/ProblemModuleShell.vue";

const route = useRoute();
const router = useRouter();
const listId = computed((): IdType => {
  return <string>route.params.id;
})
// 查询需要的表单数据
const queryParams = reactive<ListProblemQuery>({
  currentPage: 1,
  pageSize: 20,
  listId: listId.value,
  problemId: undefined,
  title: undefined,
  type: undefined
});

const {columns} = useStatuesColumn(
    ['问题ID', '题目', '问题描述', '问题来源', '问题类型' ,'问题权限', '创建时间', '提示', '问题顺序', '分数'],
    [true, true, false, false, true, true, false, false, true, true]
);

// 弹窗是否打开
const open = ref(false)

const showSearch = ref(true);

const tableList = reactive<ProblemInListView[]>([]);
const sortedTableList = computed(() => {
  return [...tableList].sort((a, b) => (a.problemOrder ?? 0) - (b.problemOrder ?? 0));
})
const total = ref<number>(0);
const orderSnapshot = new Map<string, number>();
const isOrderSaving = ref(false);
const draggingProblemId = ref<string | null>(null);
const dragOverProblemId = ref<string | null>(null);
const tableRef = ref<TableInstance>();
const isLoading = ref(false);
const isAddLoading = ref(false);
const isDeleting = ref(false);
const savingScoreIds = ref<Set<string>>(new Set());
let listRequestId = 0;

const hasPendingOrderChanges = computed(() => {
  return sortedTableList.value.some(row => orderSnapshot.get(String(row.problemId)) !== row.problemOrder);
});

const pendingOrderCount = computed(() => {
  return sortedTableList.value.filter(row => orderSnapshot.get(String(row.problemId)) !== row.problemOrder).length;
});

// 获取列表
const getList = async () => {
  const requestId = ++listRequestId;
  const currentListId = listId.value;
  isLoading.value = true;
  try {
    const data = await getProblemsAdmin(currentListId);
    if (requestId !== listRequestId || String(currentListId) !== String(listId.value)) return;
    tableList.splice(0, tableList.length, ...data);
    tableList.sort((a, b) => (a.problemOrder ?? 0) - (b.problemOrder ?? 0));
    total.value = tableList.length;
    orderSnapshot.clear();
    tableList.forEach(row => {
      row.tempOrder = row.problemOrder;
      row.tempScore = row.score;
      orderSnapshot.set(String(row.problemId), row.problemOrder ?? 0);
    });
  } finally {
    if (requestId === listRequestId) isLoading.value = false;
  }
}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])

const addProblemIds = ref<IdType[]>([])

const isEdit = ref(false)



const handleSelectionChange = (selection: ProblemInListView[]) => {
  ids.value = selection.map(item => item.problemId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}



const handleDelete = async (row?: ProblemInListView) => {
  const targetIds = row ? [row.problemId] : [...ids.value];
  if (!targetIds.length) {
    ElMessage.warning('请先选择要移除的题目');
    return;
  }
  const relations = targetIds.map(problemId => ({listId: listId.value, problemId}));
  try {
    await ElMessageBox.confirm(`确定从题单移除选中的 ${targetIds.length} 道题目吗？`, '移除题目', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });
    isDeleting.value = true;
    await delProblemFromList(relations);
    ids.value = [];
    tableRef.value?.clearSelection();
    await getList();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') ElMessage.error('移除题目失败，请稍后重试');
  } finally {
    isDeleting.value = false;
  }
}


const handleAdd = () => {
  open.value = true;
}
const handleScoreUpdate = async (data: ProblemInListView) => {
  if (data.tempScore === undefined) {
    data.tempScore = data.score;
  }

  if (data.tempScore === data.score) {
    return;
  }
  const previousScore = data.score;
  const problemKey = String(data.problemId);
  savingScoreIds.value = new Set(savingScoreIds.value).add(problemKey);
  data.score = data.tempScore;
  try {
    await updateProblemRelation({
      listId: listId.value,
      problemId: data.problemId,
      score: data.score
    });
  } catch {
    data.score = previousScore;
    data.tempScore = previousScore;
    ElMessage.error('分数保存失败，请重试');
  } finally {
    const next = new Set(savingScoreIds.value);
    next.delete(problemKey);
    savingScoreIds.value = next;
  }
}

const handleDragStart = (row: ProblemInListView, event: DragEvent) => {
  if (!isEdit.value) {
    event.preventDefault();
    return;
  }
  draggingProblemId.value = String(row.problemId);
  event.dataTransfer?.setData('text/plain', String(row.problemId));
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move';
  }
}

const handleDragOver = (row: ProblemInListView) => {
  if (draggingProblemId.value && draggingProblemId.value !== String(row.problemId)) {
    dragOverProblemId.value = String(row.problemId);
  }
}

const handleDrop = (target: ProblemInListView) => {
  const sourceId = draggingProblemId.value;
  if (!sourceId || sourceId === String(target.problemId)) {
    handleDragEnd();
    return;
  }

  const current = sortedTableList.value;
  const sourceIndex = current.findIndex(row => String(row.problemId) === sourceId);
  const targetIndex = current.findIndex(row => String(row.problemId) === String(target.problemId));
  if (sourceIndex < 0 || targetIndex < 0) {
    handleDragEnd();
    return;
  }

  const reordered = [...current];
  const [moved] = reordered.splice(sourceIndex, 1);
  const targetIndexAfterMove = reordered.findIndex(row => String(row.problemId) === String(target.problemId));
  reordered.splice(targetIndexAfterMove + 1, 0, moved);
  reordered.forEach((row, index) => {
    row.problemOrder = index + 1;
    row.tempOrder = index + 1;
  });
  tableList.splice(0, tableList.length, ...reordered);
  handleDragEnd();
}

const handleDragEnd = () => {
  draggingProblemId.value = null;
  dragOverProblemId.value = null;
}

const moveRow = (index: number, direction: -1 | 1) => {
  if (!isEdit.value) return;
  const current = [...sortedTableList.value];
  const targetIndex = index + direction;
  if (index < 0 || targetIndex < 0 || targetIndex >= current.length) return;
  const [moved] = current.splice(index, 1);
  current.splice(targetIndex, 0, moved);
  current.forEach((row, order) => {
    row.problemOrder = order + 1;
    row.tempOrder = order + 1;
  });
  tableList.splice(0, tableList.length, ...current);
};

const saveProblemOrder = async () => {
  if (!hasPendingOrderChanges.value || isOrderSaving.value) {
    return;
  }
  isOrderSaving.value = true;
  try {
    await batchUpdateProblemOrder(listId.value, sortedTableList.value.map((row, index) => ({
      problemId: row.problemId,
      problemOrder: index + 1
    })));
    sortedTableList.value.forEach((row, index) => {
      row.problemOrder = index + 1;
      row.tempOrder = index + 1;
      orderSnapshot.set(String(row.problemId), index + 1);
    });
    ElMessage.success('题单顺序已保存');
  } catch {
    ElMessage.error('题单顺序保存失败，请刷新后重试');
  } finally {
    isOrderSaving.value = false;
  }
}

const resetProblemOrder = () => {
  tableList.forEach(row => {
    const savedOrder = orderSnapshot.get(String(row.problemId));
    if (savedOrder !== undefined) {
      row.problemOrder = savedOrder;
      row.tempOrder = savedOrder;
    }
  });
}

const getAuthText = (auth: ProblemAuth) => {
  return auth === ProblemAuth.CONTEST ? "比赛题目" : "普通题目";
}

const getAuthCardType = (auth: ProblemAuth) => {
  return auth === ProblemAuth.CONTEST ? "danger" : "success";
}





const reset = () => {
  open.value = false;
  addProblemIds.value.length = 0;
}
const cancel = () => {
  reset();
}

const submit = async () => {
  const problemIds = [...addProblemIds.value];
  if (!problemIds.length) return;
  const payload = problemIds.map(problemId => ({
    listId: listId.value,
    problemId,
    problemOrder: 0,
    score: 0
  }));
  isAddLoading.value = true;
  try {
    await addProblemToList(payload);
    ElMessage.success('题目已加入题单');
    reset();
    await getList();
  } finally {
    isAddLoading.value = false;
  }
}


// 返回
const back = () => {
  void router.push({name: "list-edit"})
}

watch(listId, (value) => {
  queryParams.listId = value;
  ids.value = [];
  void getList();
}, {immediate: true});




</script>
<style lang="scss" scoped>

::v-deep(.inline-form) {
  .el-input {
    --el-input-width: 220px;
  }

  .el-select {
    --el-select-width: 220px;
  }

}

::v-deep(.el-table__row) .el-dropdown {
  height: 23px;
}

.list-problem-table { border-radius: 14px; overflow: hidden; }
.order-notice { display: flex; align-items: center; gap: 7px; margin: 0 0 10px; padding: 8px 12px; border: 1px solid var(--el-color-primary-light-7); border-radius: 10px; color: var(--el-text-color-secondary); background: var(--el-color-primary-light-9); font-size: 12px; }.order-notice .el-icon { color: var(--el-color-primary); }.order-notice .el-button { margin-left: auto; }
.order-cell { display: inline-flex; align-items: center; gap: 9px; min-width: 76px; min-height: 30px; padding: 2px 7px; border: 1px dashed transparent; border-radius: 8px; transition: border-color .2s, background-color .2s; }.order-cell.is-drag-over { border-color: var(--el-color-primary); background: var(--el-color-primary-light-9); }.drag-handle { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; padding: 0; border: 0; border-radius: 6px; color: var(--el-text-color-secondary); background: transparent; cursor: grab; }.drag-handle:hover, .drag-handle:focus-visible { outline: none; color: var(--el-color-primary); background: var(--el-fill-color-light); }.drag-handle:active { cursor: grabbing; }.drag-handle.is-disabled { cursor: not-allowed; opacity: .45; }.order-number { min-width: 20px; font-variant-numeric: tabular-nums; }
.dialog-intro { display: flex; align-items: center; gap: 11px; margin-bottom: 14px; padding: 13px 15px; border-radius: 12px; background: var(--el-fill-color-light); }.dialog-icon { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 10px; background: rgb(5 150 105 / 12%); color: var(--el-color-success); }.dialog-intro strong, .dialog-intro p { display: block; }.dialog-intro p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }.picker-body { min-height: 0; max-height: min(62vh, 640px); overflow: auto; }.selection-summary { margin-right: auto; color: var(--el-text-color-secondary); font-size: 12px; }
</style>
