<template>
  <ProblemModuleShell
    title="题目管理"
    kicker="PROBLEM / QUESTION BANK"
    description="维护题目内容、题型、权限与测试数据，集中管理题库资产。"
    :icon="Collection"
    tone="blue"
  >
    <el-form :model="queryParams" class="filter-panel inline-form" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="题目" prop="problemName">
        <el-input
            v-model="queryParams.title"
            placeholder="请输入题目"

            @keyup.enter.native="handleQuery"
            clearable
        />
      </el-form-item>
      <el-form-item label="题目类型" prop="problemType">
        <el-select
            v-model="queryParams.type"
            placeholder="菜单类型"
            clearable
            style="width: 120px"
        >
          <el-option
              v-for="item in dict.problemTypeStr"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="题目ID" prop="parentId">
        <el-input v-model="queryParams.problemId" :controls="false"/>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="search"  @click="handleQuery">搜索</el-button>
        <el-button icon="refresh"  @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="module-toolbar">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'problem:problem:add'"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="edit"
            size="small"
            :disabled="single"
            @click="handleUpdate"
            v-has="'problem:problem:edit'"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="delete"
            size="small"
            :disabled="multiple"
            @click="handleDelete()"
            v-has="'problem:problem:remove'"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="upload"
            size="small"
            @click="handleUpload"
            v-has="'problem:problem:add'"
        >上传</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

<!--    ['问题ID', '题目', '问题描述', '问题来源', '问题类型' ,'问题权限', '创建时间', '提示']-->
    <el-table ref="tableRef" v-loading="isLoading" class="problem-table" :data="tableList" row-key="problemId" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="问题ID" align="center" prop="problemId" v-if="columns[0].visible" show-overflow-tooltip/>
      <el-table-column label="题目" align="center" prop="title" v-if="columns[1].visible" show-overflow-tooltip/>
      <el-table-column label="问题描述" align="center" prop="description" v-if="columns[2].visible" show-overflow-tooltip />

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
      <el-table-column label="提示" width="60" align="center" prop="hint" v-if="columns[7].visible" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="edit"
              @click="handleUpdate(scope.row)"
              v-has="'problem:problem:edit'"
          >修改</el-link>
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'problem:problem:remove'"
          >删除</el-link>
          <el-dropdown v-if="canUseMore" size="small" @command="(command: string) => handleCommand(command, scope.row)">
            <el-link size="small" type="primary" icon="arrow-right">更多</el-link>
            <template #dropdown>
              <el-dropdown-menu>
                <div v-has="'problem:tag:add'" >
                  <el-dropdown-item command="handleCard" icon="Postcard"
                  >设置标签</el-dropdown-item>
                </div>
                <div v-has="'problem:problem:edit'" >
                  <el-dropdown-item command="handleCase" icon="UploadFilled"
                  >设置例题</el-dropdown-item>
                </div>
              </el-dropdown-menu>

            </template>

          </el-dropdown>
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


    <el-dialog title="管理题目标签" v-model="open" class="tag-dialog" width="min(680px, 92vw)" append-to-body destroy-on-close>
      <div class="dialog-intro"><span class="dialog-icon"><el-icon><CollectionTag /></el-icon></span><div><strong>为题目整理标签</strong><p>标签用于题库筛选和题目归类，当前已选择 {{ currentCards.length }} 个。</p></div></div>
      <div class="tag-picker">
        <el-space v-loading="loadingCard" wrap>
          <el-check-tag
              v-for="item of allCards"
              :key="item.tagId"
              :checked="currentCards.some(x => x.tagId === item.tagId)"
              @change="(bool: boolean) => onChange(bool, item)"
          >
            {{ item.tagName }}
          </el-check-tag>
        </el-space>
      </div>
      <template #footer><el-button @click="cancel">取消</el-button><el-button type="primary" @click="submit" :loading="tagSubmitting">保存标签</el-button></template>
    </el-dialog>

    <el-dialog title="上传题目" v-model="openUpload" class="upload-dialog" width="min(680px, 92vw)" append-to-body destroy-on-close>
      <div class="dialog-intro"><span class="dialog-icon"><el-icon><UploadFilled /></el-icon></span><div><strong>批量导入题目</strong><p>仅支持符合平台格式的 JSON 文件，单文件不超过 500KB。</p></div></div>
      <el-upload
          drag
          ref="uploadRef"
          action="/api/problem-api/problem/upload"
          :headers="uploadHeaders"
          :limit="1"
          :on-change="handleUploadChange"
          :on-error="uploadError"
          :on-success="uploadSuccess"
          accept="application/json"
          :auto-upload="false"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到这里 <em>或点击选择文件</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            json格式文件，必须符合网站文档格式，不要大于500kb
          </div>
        </template>
      </el-upload>
      <template #footer><el-button @click="onUploadCancel">取消</el-button><el-button type="primary" @click="onHandleSubmit" :loading="uploadSubmitting">开始导入</el-button></template>
    </el-dialog>
  </ProblemModuleShell>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import {
  type AdminQueryProblem,
  getProblemsAdmin,
  dict,
  ProblemAuth,
  type ProblemView,
  removeProblems
} from "@/api/problem";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {ElMessage, ElMessageBox, type TableInstance, type UploadFile, type UploadInstance} from "element-plus";
import {problemTypeToString} from "@/utils/problem";
import {
  addTagForProblem, delTagForProblem,
  fetchTagByProblemId,
  getAllTags,
  type TagView
} from "@/api/problem/label";
// 题库页面不再依赖 lodash 处理选择集合，避免对象引用比较导致标签差异判断错误。
import {useRouter} from "vue-router";
import {Collection, CollectionTag, UploadFilled} from "@element-plus/icons-vue";
import ProblemModuleShell from "@/views/backend/problem-module/component/ProblemModuleShell.vue";
import type {UploadAjaxError} from "element-plus/es/components/upload/src/ajax";
import type {AjaxResult} from "@/utils/http";
import type {IdType} from "@/api/common.ts";
import {hasAnyPerm} from "@/utils/authUtil.ts";
import {useToken} from "@/stores/useToken";

const router = useRouter();
const canUseMore = computed(() => hasAnyPerm(['problem:tag:add', 'problem:problem:edit']));

// 查询需要的表单数据
const queryParams = reactive<AdminQueryProblem>({
  currentPage: 1,
  pageSize: 20,
  problemId: undefined,
  title: undefined,
  type: undefined
});

const {columns} = useColumn(['问题ID', '题目', '问题描述', '问题来源', '问题类型' ,'问题权限', '创建时间', '提示']);

// 重制列表
const resetQuery = () => {
  queryParams.currentPage = 1;
  queryParams.problemId = undefined;
  queryParams.title = undefined;
  queryParams.type =  undefined;
  clearSelection();
  void getList();
};

const uploadRef = ref<UploadInstance>();
const showSearch = ref(true);
const tableRef = ref<TableInstance>();
const uploadSubmitting = ref(false);
const selectedUploadFile = ref<UploadFile>();
const tokenStore = useToken();
const uploadHeaders = computed(() => ({token: tokenStore.token}));

const tableList = reactive<ProblemView[]>([]);
const total = ref<number>(0);
const isLoading = ref(false);

// 获取列表
const getList = async () => {
  clearSelection();
  isLoading.value = true;
  try {
    const data = await getProblemsAdmin({...queryParams});
    tableList.splice(0, tableList.length, ...data.data);
    total.value = data.totalRecords;
  } finally {
    isLoading.value = false;
  }
}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])

const handleSelectionChange = (selection: ProblemView[]) => {
  ids.value = selection.map(item => item.problemId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

const clearSelection = () => {
  ids.value = [];
  single.value = true;
  multiple.value = true;
  tableRef.value?.clearSelection();
};

// 上传对话框
const openUpload = ref(false);

// 上传按钮
const handleUpload = () => {
  openUpload.value = true;
}

const onHandleSubmit = () => {
  const file = selectedUploadFile.value?.raw;
  if (!file) {
    ElMessage.warning('请先选择一个 JSON 文件');
    return;
  }
  if (file.size > 500 * 1024) {
    ElMessage.warning('文件不能超过 500KB');
    return;
  }
  uploadSubmitting.value = true;
  uploadRef.value?.submit();
}

const handleUploadChange = (file: UploadFile) => {
  selectedUploadFile.value = file;
};

// 取消
const onUploadCancel = () => {
  uploadRef.value?.clearFiles();
  selectedUploadFile.value = undefined;
  uploadSubmitting.value = false;
  openUpload.value = false;
}

// 上传成功
const uploadSuccess = (resp: AjaxResult<boolean>) => {
  uploadRef.value?.clearFiles();
  selectedUploadFile.value = undefined;
  uploadSubmitting.value = false;
  openUpload.value = false;
  void getList();

  if (resp.data) {
    ElMessage.success("导入成功");
  } else {
    ElMessage.warning("部分导入失败");
  }

}



const uploadError = (evt: UploadAjaxError) => {
  uploadSubmitting.value = false;
  let message = evt.message || '题目导入失败';
  try {
    message = JSON.parse(message)?.message || message;
  } catch {
    // 上传接口也可能返回纯文本错误，保留原始信息。
  }
  ElMessage.error(message);
}

const handleDelete = async (row?: ProblemView) => {
  const targetIds = row ? [row.problemId] : [...ids.value];
  if (!targetIds.length) {
    ElMessage.warning('请先选择要删除的题目');
    return;
  }
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${targetIds.length} 道题目吗？`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
    });
    await removeProblems(targetIds);
    clearSelection();
    await getList();
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') throw error;
  }
}

// 搜索按钮
const handleQuery = () => {
  queryParams.currentPage = 1;
  clearSelection();
  void getList();
}

const handleAdd = () => {
  router.push({name: "edit-problem"});
}
const handleUpdate = (data?: ProblemView) => {
  const problemId = data?.problemId || ids.value[0];
  router.push({name: "edit-problem", query: {id: problemId}});
}

const getAuthText = (auth: ProblemAuth) => {
  return auth === ProblemAuth.CONTEST ? "比赛题目" : "普通题目";
}
const getAuthCardType = (auth: ProblemAuth) => {
  return auth === ProblemAuth.CONTEST ? "danger" : "success";
}

const currentCards = ref<TagView[]>([]);
const originCards = ref<TagView[]>([]);
const allCards = ref<TagView[]>([]);
const open = ref(false)
const currentProblemId = ref<IdType>('0')
const tagSubmitting = ref(false)

const onChange = (bool: boolean, id: TagView) => {

  if (!bool) {
    currentCards.value = currentCards.value.filter(x => x.tagId !== id.tagId);
  } else if (!currentCards.value.some(x => x.tagId === id.tagId)) {
    currentCards.value.push(id);
  }
}



const cancel = () => {
  currentCards.value = [];
  originCards.value = [];
  open.value = false;
}

const loadingCard = ref(false)
const submit = async () => {
  const selected = new Set(currentCards.value.map(item => String(item.tagId)));
  const original = new Set(originCards.value.map(item => String(item.tagId)));
  const addIds = currentCards.value.filter(item => !original.has(String(item.tagId)))
      .map(item => ({tagId: item.tagId, problemId: currentProblemId.value}));
  const deleteIds = originCards.value.filter(item => !selected.has(String(item.tagId)))
      .map(item => ({tagId: item.tagId, problemId: currentProblemId.value}));
  if (!addIds.length && !deleteIds.length) {
    open.value = false;
    return;
  }
  tagSubmitting.value = true;
  try {
    await Promise.all([
      addIds.length ? addTagForProblem(addIds) : Promise.resolve(),
      deleteIds.length ? delTagForProblem(deleteIds) : Promise.resolve()
    ]);
    ElMessage.success('题目标签已更新');
    open.value = false;
  } finally {
    tagSubmitting.value = false;
  }
}

const manageTag = (id: IdType) => {
  currentProblemId.value = id;
  open.value = true;
  loadingCard.value = true;
  fetchTagByProblemId(id)
      .then((data) => {
        originCards.value = [...data];
        currentCards.value = [...data];
      })
      .finally(() => { loadingCard.value = false; });

}

const handleCommand = (command: string, row: ProblemView) => {
  const map: Record<string, () => void> = {
    "handleCase": () => {router.push({name: 'case-edit', params: {problemId: row.problemId}})},
    "handleCard": () => { manageTag(row.problemId); }
  }
  map[command]?.();
}

const getTag = async () => {
  allCards.value = await getAllTags();
}

// created -> 获取列表
void getList();
void getTag();
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

.filter-panel {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  margin-bottom: 16px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 13px;
  background: var(--el-fill-color-light);
}

.module-toolbar { margin-bottom: 16px; }
.problem-table { border-radius: 14px; overflow: hidden; }
:deep(.el-table__header th.el-table__cell) { color: var(--el-text-color-secondary); font-size: 12px; font-weight: 700; background: var(--el-fill-color-light); }
:deep(.el-table__row td.el-table__cell) { height: 68px; }
.dialog-intro { display: flex; align-items: center; gap: 11px; margin-bottom: 18px; padding: 13px 15px; border-radius: 12px; background: var(--el-fill-color-light); }.dialog-icon { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border-radius: 10px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); }.dialog-intro strong, .dialog-intro p { display: block; }.dialog-intro p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }.tag-picker { min-height: 90px; padding: 12px; border: 1px dashed var(--el-border-color); border-radius: 12px; background: var(--el-bg-color-page); }
@media (max-width: 760px) { .filter-panel { align-items: stretch; flex-direction: column; }.filter-panel :deep(.el-form-item) { margin-right: 0; } }
</style>
