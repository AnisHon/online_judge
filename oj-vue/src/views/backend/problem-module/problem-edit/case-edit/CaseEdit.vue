<template>
  <ProblemModuleShell
    title="测试用例"
    kicker="PROBLEM / TEST CASES"
    :description="`管理题目 #${problemId} 的输入、输出与分值。`"
    :icon="Files"
    tone="blue"
  >
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="ArrowLeft"
            size="small"
            @click="router.back()"
            v-has="'problem:problem:add'"
        >返回</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'problem:problem:add'"
        >添加</el-button>
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
        <el-tooltip content="下载输入测试用例">
          <el-button
              plain
              @click="download(false)"
              type="success"
              icon="download"
              size="small"
              :disabled="multiple"
              v-has="'problem:problem:list'"
          >输入样例</el-button>

        </el-tooltip>
      </el-col>
      <el-col :span="1.5">
        <el-tooltip content="下载输出测试用例">
          <el-button
              plain
              @click="download(true)"
              type="info"
              icon="download"
              size="small"
              :disabled="multiple"
              v-has="'problem:problem:list'"
          >输出样例</el-button>
        </el-tooltip>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <!--    ['问题ID', '题目', '问题描述', '问题来源', '问题类型' ,'问题权限', '创建时间', '提示']-->
    <el-table class="case-table" :data="tableList" @selection-change="handleSelectionChange" stripe flexible>
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="题例ID" align="center" prop="caseId" v-if="columns[0].visible" show-overflow-tooltip/>
      <el-table-column label="分数" align="center" prop="score" v-if="columns[1].visible" show-overflow-tooltip/>
      <el-table-column label="输入文件" align="center" prop="input" v-if="columns[2].visible" show-overflow-tooltip />
      <el-table-column label="输入大小" align="center" prop="inputSize" v-if="columns[3].visible" show-overflow-tooltip >
        <template v-slot="scope">
          <el-text type="info">{{ bytesToSize(scope.row.inputSize) }}</el-text>
        </template>
      </el-table-column>
      <el-table-column label="输出文件" align="center" prop="output" v-if="columns[4].visible" show-overflow-tooltip/>
      <el-table-column label="输出大小" align="center"  prop="outputSize" v-if="columns[5].visible" show-overflow-tooltip>
        <template v-slot="scope">
          <el-text type="info">{{ bytesToSize(scope.row.outputSize) }}</el-text>
        </template>
      </el-table-column >
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-space>
            <el-link size="small" type="primary" @click="handleEdit(scope.row)" v-has="'problem:problem:list'">
              {{ hasPerm('problem:problem:edit') ? '查看/编辑' : '查看' }}
            </el-link>
            <el-link size="small" type="danger" @click="handleDelete(scope.row)" v-has="'problem:problem:remove'">删除</el-link>
          </el-space>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog :title="title" v-model="openDialog" class="case-dialog" width="min(760px, 92vw)" append-to-body destroy-on-close>
      <div class="dialog-intro"><span class="dialog-icon"><el-icon><Files /></el-icon></span><div><strong>{{ editorMode === 'add' ? '添加测试用例' : '测试用例内容' }}</strong><p>单个文件不超过1MiB时支持在线查看和编辑；大文件使用下载、上传方式。</p></div></div>
      <el-alert v-if="inlineBlocked" class="large-case-warning" type="warning" :closable="false" show-icon>
        <template #title>该测试用例超过 1MiB，已禁止在浏览器中加载正文</template>
        <div class="large-case-actions">
          <span>请下载到本地编辑后上传替换，避免浏览器卡顿或意外占用内存。</span>
          <el-button link type="primary" @click="downloadCurrentCase(false)">下载输入文件</el-button>
          <el-button link type="primary" @click="downloadCurrentCase(true)">下载输出文件</el-button>
        </div>
      </el-alert>
      <el-form :model="form" label-width="100px" label-position="top" @submit.prevent="submit">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="题例ID" prop="listName">
              <el-input v-model="form.caseId" disabled/>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item prop="remark" label="分数">
              <el-input-number v-model="form.score" :precision="2" :max="100" :min="0" style="width: 50%" placeholder="请输入分数" :disabled="!canEditCurrent" />
            </el-form-item>
          </el-col>
          <el-col v-if="canEditCurrent && !inlineBlocked" :span="24">
            <el-form-item>
              <el-switch v-model="useUpload" active-text="上传文件" inactive-text="手动输入"/>
            </el-form-item>
          </el-col>

          <el-col :span="12" v-if="!useUpload && !inlineBlocked">
            <el-form-item prop="input" label="输入题例">
              <el-input type="textarea" v-model="form.input" :rows="10" :readonly="!canEditCurrent" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="!useUpload && !inlineBlocked">
            <el-form-item prop="input" label="输出题例">
              <el-input type="textarea" v-model="form.output" :rows="10" :readonly="!canEditCurrent" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="useUpload && canEditCurrent">
            <el-form-item prop="input" label="输入题例">
              <el-upload style="width: 100%" ref="inUploadRef" :limit="1" :auto-upload="false" :on-change="setInputFile" :on-remove="() => form.inputFile = undefined">
                <template #trigger>
                  <el-button type="primary" icon="upload" plain>选择输入文件</el-button>
                </template>
                <template #tip><div class="el-upload__tip">留空表示不替换当前文件，可上传大文件。</div></template>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="useUpload && canEditCurrent">
            <el-form-item prop="output" label="输出题例">
              <el-upload style="width: 100%" :limit="1" ref="outUploadRef" :auto-upload="false" :on-change="setOutputFile" :on-remove="() => form.outputFile = undefined">
                <template #trigger>
                  <el-button type="primary" icon="upload" plain>选择输出文件</el-button>
                </template>
                <template #tip><div class="el-upload__tip">留空表示不替换当前文件，可上传大文件。</div></template>
              </el-upload>
            </el-form-item>
          </el-col>

        </el-row>
      </el-form>
      <template #footer>
        <el-button v-if="canEditCurrent" type="primary" @click="submit" :loading="isLoading">{{ editorMode === 'add' ? '添加用例' : '保存修改' }}</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </ProblemModuleShell>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import type {OjCase} from "@/api/problem";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import {ElMessage, ElMessageBox, type UploadFile, type UploadInstance} from "element-plus";
import {useRoute, useRouter} from "vue-router";
import type {IdType} from "@/api/common.ts";
import {addCase, downloadCase, getCaseContent, listCase, type OjCaseView, removeCase, updateCase} from "@/api/problem/case.ts";
import {bytesToSize} from "@/utils/byte2size.ts";
import useLoading from "@/hooks/useLoading.ts";
import {Files} from "@element-plus/icons-vue";
import ProblemModuleShell from "@/views/backend/problem-module/component/ProblemModuleShell.vue";
import {hasPerm} from "@/utils/authUtil";

const router = useRouter();

const route = useRoute();

const inUploadRef = ref<UploadInstance>();

const outUploadRef = ref<UploadInstance>();

const problemId = route.params.problemId as string;
const INLINE_CASE_LIMIT = 1024 * 1024;
type EditorMode = 'add' | 'edit' | 'view';

const {columns} = useColumn(['题例ID', '分数', '输入文件', '输入大小', '输出文件', '输出大小']);

const useUpload = ref(true);

const form = ref<OjCase>({
  caseId: undefined,
  problemId: problemId,
  input: undefined,
  output: undefined,
  inputFile: undefined,
  outputFile: undefined,
  score: 1
})

const showSearch = ref(true);

const tableList = reactive<OjCaseView[]>([]);

const openDialog = ref(false);

const title = ref("");
const editorMode = ref<EditorMode>('add');
const inlineBlocked = ref(false);
const selectedCase = ref<OjCaseView | null>(null);
const canEditCurrent = computed(() => editorMode.value === 'add'
  ? hasPerm('problem:problem:add') || hasPerm('problem:problem:edit')
  : hasPerm('problem:problem:edit'));


const reset = () => {
  inUploadRef.value?.clearFiles();
  outUploadRef.value?.clearFiles();
  form.value = {
    caseId: undefined,
    problemId: problemId,
    input: undefined,
    output: undefined,
    inputFile: undefined,
    outputFile: undefined,
    score: undefined
  }
  inlineBlocked.value = false;
  selectedCase.value = null;
  useUpload.value = true;
}

// 获取列表
const getList = async () => {
  const data = await listCase(problemId)
  tableList.length = 0;
  tableList.push(...data)
}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])

const handleSelectionChange = (selection: OjCaseView[]) => {
  ids.value = selection.map(item => item.caseId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

const handleDelete = (row?: OjCaseView) => {
  const id = row ? row.caseId : ids.value;
  ElMessageBox.confirm(`您是否要删除ID为${id}的数据项？`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
      .then(() => {
        removeCase(id).then(getList);
      })
      .catch(() => undefined)
}

const handleAdd = () => {
  reset();
  editorMode.value = 'add';
  title.value = "添加测试用例";
  openDialog.value = true;
}

const {loading, isLoading, finish} = useLoading()

let contentRequestId = 0;

const handleEdit = async (row: OjCaseView) => {
  reset();
  const requestId = ++contentRequestId;
  selectedCase.value = row;
  editorMode.value = hasPerm('problem:problem:edit') ? 'edit' : 'view';
  title.value = editorMode.value === 'edit' ? '查看/编辑测试用例' : '查看测试用例';
  form.value = {
    caseId: row.caseId,
    problemId,
    score: row.score,
    input: undefined,
    output: undefined,
    inputFile: undefined,
    outputFile: undefined,
  };
  inlineBlocked.value = row.inputSize > INLINE_CASE_LIMIT || row.outputSize > INLINE_CASE_LIMIT;
  useUpload.value = inlineBlocked.value;
  openDialog.value = true;

  if (inlineBlocked.value) return;
  try {
    const [input, output] = await Promise.all([
      getCaseContent(row.caseId, 'input'),
      getCaseContent(row.caseId, 'output'),
    ]);
    if (requestId !== contentRequestId) return;
    form.value.input = input;
    form.value.output = output;
  } catch {
    if (requestId === contentRequestId) ElMessage.error('测试用例内容加载失败');
  }
};

const setInputFile = (uploadFile: UploadFile) => { form.value.inputFile = uploadFile.raw; };
const setOutputFile = (uploadFile: UploadFile) => { form.value.outputFile = uploadFile.raw; };

const downloadCurrentCase = (output: boolean) => {
  const path = output ? selectedCase.value?.output : selectedCase.value?.input;
  if (path) void downloadCase(path);
};

const submit = async () => {
  if (!canEditCurrent.value || isLoading.value) return;
  if (!useUpload.value && !inlineBlocked.value) {
    const inputBytes = new TextEncoder().encode(form.value.input || '').length;
    const outputBytes = new TextEncoder().encode(form.value.output || '').length;
    if (inputBytes > INLINE_CASE_LIMIT || outputBytes > INLINE_CASE_LIMIT) {
      ElMessage.warning('单个测试用例在线编辑不能超过1MiB，请切换为文件上传');
      useUpload.value = true;
      return;
    }
  }

  loading();
  try {
    const saved = editorMode.value === 'add'
      ? await addCase(form.value)
      : await updateCase(form.value.caseId!, form.value);
    if (!saved) return;
    openDialog.value = false;
    reset();
    await getList();
  } catch {
    // HTTP 层已显示可公开的业务错误；此处只保证按钮状态恢复。
  } finally {
    finish();
  }
}

const cancel = () => {
  contentRequestId++;
  openDialog.value = false;
  reset();
}

const download = (out: boolean) => {
  const selected = tableList
      .filter(item => ids.value.includes(item.caseId))
      .map(item => out ? item.output : item.input);
  selected.forEach(item => {
    downloadCase(item);
  })

}


// created -> 获取列表
getList();
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

.case-table { border-radius: 14px; overflow: hidden; }.dialog-intro { display: flex; align-items: center; gap: 11px; margin-bottom: 18px; padding: 13px 15px; border-radius: 12px; background: var(--el-fill-color-light); }.dialog-icon { display: grid; width: 34px; height: 34px; place-items: center; border-radius: 10px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); }.dialog-intro strong, .dialog-intro p { display: block; }.dialog-intro p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }
</style>
