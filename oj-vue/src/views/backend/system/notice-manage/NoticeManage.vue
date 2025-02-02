<template>
  <div class="list-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'content:notice:add'"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="edit"
            size="small"
            :disabled="single"
            @click="handleUpdate()"
            v-has="'content:notice:edit'"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="delete"
            size="small"
            :disabled="multiple"
            @click="handleDelete"
            v-has="'content:notice:remove'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <el-table :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="通知ID" align="center" prop="noticeId" v-if="columns[0].visible" show-overflow-tooltip />
      <el-table-column label="通知名称" align="center" prop="title" v-if="columns[1].visible" />
      <el-table-column label="通知状态" align="center" prop="topUp" v-if="columns[2].visible" >
        <template v-slot="scope">
          <el-tag type="danger" v-if="scope.row.topUp">重要</el-tag>
          <el-tag type="primary" v-else>普通</el-tag>

        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[3].visible" />
      <el-table-column label="操作" align="center" list-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="edit"
              @click="handleUpdate(scope.row)"
              v-has="'problem:list:edit'"
          >修改</el-link>
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'problem:list:remove'"
          >删除</el-link>
          <el-link
              size="small"
              type="primary"
              icon="view"
              @click="router.push({name: 'notice', params: {id: scope.row.noticeId}})"
          >查看</el-link>
        </template>
      </el-table-column>
    </el-table>

    <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.currentPage"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />

    <!-- 添加或修改测试功能对话框 -->
    <el-dialog :title="title" v-model="open" width="1080px" append-to-body>
      <el-form label-position="top" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="ID">
              <el-input v-model="form.noticeId" placeholder="通知ID" disabled/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="通知名称">
              <el-input v-model="form.title" placeholder="请输入通知标题"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标记为重要">
              <el-switch v-model="form.topUp"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="通知内容">
              <mark-down-editor v-model="form.content"/>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>

        <el-button type="primary" @click="submitForm" :loading="isLoading" >确 定</el-button>
        <el-button @click="cancel">取 消</el-button>

      </template>

    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {ElDialog, ElMessageBox} from "element-plus";
import {useRouter} from "vue-router";
import type {IdType} from "@/api/common.ts";
import {addNotice, getNotice, listNotice, type Notice, type NoticeDto, removeNotice, updateNotice} from "@/api/notice";
import type {PagedType} from "@/api/pagedType.ts";
import MarkDownEditor from "@/components/MarkDownEditor/MarkDownEditor.vue";
import useLoading from "@/hooks/useLoading.ts";
import __ from "lodash";

const router = useRouter();
// 查询需要的表单数据
const queryParams = reactive<PagedType>({
  currentPage: 1,
  pageSize: 20,
});

const form = reactive<NoticeDto>({
  noticeId: "",
  title: "",
  content: "",
  topUp: false,
});


const rules = ref();

const open = ref(false);

const {columns} = useColumn(['通知ID', '通知名称', '通知状态', '创建时间']);

// 重置表单
const resetForm = () => {
  form.noticeId = "";
  form.title = '';
  form.content = '';
  form.topUp = false;
}

const showSearch = ref(true);


const tableList = reactive<Notice[]>([]);
const total = ref<number>(0);

// 获取题单
const getList = async () => {
  const data = await listNotice(queryParams)
  total.value = data.totalRecords;
  tableList.length = 0;
  tableList.push(...data.data);
}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])

const {loading, isLoading, finish} = useLoading();

const handleSelectionChange = (selection: Notice[]) => {
  ids.value = selection.map(item => item.noticeId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}



const handleDelete = (row?: Notice) => {

  const id = row? row.noticeId : ids.value;
  ElMessageBox.confirm(`您是否要删除ID为${id}的数据项？`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
      .then(() => {
        removeNotice(id).then(getList);
      })
}


const finishDialog = () => {
  open.value = false;
  resetForm();
  getList();
}

const debouncedAdd = __.debounce(async () => {
  await addNotice(form);
  finish();
  finishDialog();
})

const debouncedUpdate = __.debounce(async () => {
  await updateNotice(form);
  finish();
  finishDialog();
})

// 1: Insert 2: Update
const dialogState = ref(1);
const title = computed(() => {
  return dialogState.value === 1 ? "添加" : "修改";
})
const handleAdd = () => {
  dialogState.value = 1;
  open.value = true;
}

const handleUpdate = async (data?: Notice) => {
  open.value = true;
  dialogState.value = 2;

  const id = data ? data.noticeId : ids.value[0];

  const notice = await getNotice(id)

  __.assign(form, notice)
}

// 1: Insert 2: Update
const submitForm = () => {
  loading();
  if (dialogState.value === 1) {
    debouncedAdd();
  } else {
    debouncedUpdate();
  }
}

const cancel = () => {
  open.value = false;
  resetForm()
}


// created -> 获取题单
getList()




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