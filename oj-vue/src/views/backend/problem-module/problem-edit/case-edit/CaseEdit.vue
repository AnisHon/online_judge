<template>
  <div class="app-container">
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
    <el-table :data="tableList" @selection-change="handleSelectionChange" stripe flexible>
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
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'problem:problem:remove'"
          >删除</el-link>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog :title="title" v-model="openDialog" width="680px" append-to-body>
      <el-form :model="form" label-width="100px" label-position="top">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="题例ID" prop="listName">
              <el-input v-model="form.caseId" disabled/>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item prop="remark" label="分数">
              <el-input-number v-model="form.score" :precision="2" :max="100" :min="0" style="width: 50%" placeholder="请输入分数" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item>
              <el-switch v-model="useUpload" active-text="上传文件" inactive-text="手动输入"/>
            </el-form-item>
          </el-col>


          <el-col :span="12" v-show="!useUpload">
            <el-form-item prop="input" label="输入题例">
              <el-input type="textarea" v-model="form.input" :rows="10" />
            </el-form-item>
          </el-col>
          <el-col :span="12"  v-show="!useUpload">
            <el-form-item prop="input" label="输入题例">
              <el-input type="textarea" v-model="form.output" :rows="10" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-show="useUpload">
            <el-form-item prop="input" label="输入题例">
              <el-upload style="width: 100%" ref="inUploadRef" :limit="1" :auto-upload="false" :on-change="(uploadFile: UploadFile) => form.inputFile = uploadFile.raw">
                <template #trigger>
                  <el-button style="width: 100px" type="primary" icon="upload" plain round />
                </template>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="12"  v-show="useUpload">
            <el-form-item prop="output" label="输出题例">
              <el-upload  style="width: 100%" :limit="1" ref="outUploadRef" :auto-upload="false" :on-change="(uploadFile: UploadFile) => form.outputFile = uploadFile.raw">
                <template #trigger>
                  <el-button style="width: 100px" type="primary"  icon="upload" plain round/>
                </template>
              </el-upload>
            </el-form-item>
          </el-col>

        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submit" :loading="isLoading">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from "vue";
import {
  type OjCase,
} from "@/api/problem";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import {ElDialog, ElMessageBox, type UploadFile, type UploadInstance} from "element-plus";
import {useRoute, useRouter} from "vue-router";
import type {IdType} from "@/api/common.ts";
import {addCase, downloadCase, listCase, type OjCaseView, removeCase} from "@/api/problem/case.ts";
import {bytesToSize} from "@/utils/byte2size.ts";
import useLoading from "@/hooks/useLoading.ts";
import __ from "lodash";

const router = useRouter();

const route = useRoute();

const inUploadRef = ref<UploadInstance>();

const outUploadRef = ref<UploadInstance>();

const problemId = route.params.problemId as string;

const {columns} = useColumn(['题例ID', '分数', '输入文件', '输入大小', '输出文件', '输出大小']);

const useUpload = ref(true);

const form = ref<OjCase>({
  caseId: undefined,
  problemId: problemId,
  input: undefined,
  output: undefined,
  inputFile: undefined,
  outputFile: undefined,
  score: undefined
})

const showSearch = ref(true);

const tableList = reactive<OjCaseView[]>([]);

const openDialog = ref(false);

const title = ref("");


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
}

const handleAdd = () => {
  title.value = "添加题解";
  openDialog.value = true;
}

const {loading, isLoading, finish} = useLoading()

const debounceAdd = __.debounce(async () =>
    addCase(form.value)
        .then(finish)
        .then(reset)
        .then(getList)
        .then(() => openDialog.value = false),
    1000);

const submit = async () => {
  loading();
  if (form.value.caseId === undefined) {
    // 添加
    debounceAdd();
  }

}

const cancel = () => {
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
</style>
