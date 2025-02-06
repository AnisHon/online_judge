<template>
  <div class="list-container">
    <el-form :model="queryParams" class="inline-form" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="列表名称" prop="listName">
        <el-input
            v-model="queryParams.listName"
            placeholder="请输入列表名称"

            @keyup.enter.native="handleQuery"
            clearable
        />
      </el-form-item>
      <el-form-item label="列表ID" prop="parentId">
        <el-input v-model="queryParams.listId" :controls="false"/>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="search"  @click="handleQuery">搜索</el-button>
        <el-button icon="refresh"  @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'problem:list:add'"
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
            v-has="'problem:list:edit'"
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
            v-has="'problem:list:remove'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <el-table v-loading="isLoading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="题单ID" align="center" prop="listId" v-if="columns[0].visible" show-overflow-tooltip />
      <el-table-column label="题单名称" align="center" prop="listName" v-if="columns[1].visible" />
      <el-table-column label="题单描述" align="center" prop="description" v-if="columns[2].visible" />
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
          <el-dropdown size="small" @command="(command: string) => handleCommand(command, scope.row)"
                       v-has-any="['problem:list:add-problem', 'problem:list:del-problem', 'problem:problem:list'] ">
            <el-link size="small" type="primary" icon="arrow-right">更多</el-link>
            <template #dropdown>
              <el-dropdown-menu>
                <div v-has="['problem:list:add-problem', 'problem:list:del-problem', 'problem:problem:list']" >
                  <el-dropdown-item command="handleProblem" icon="management"
                  >管理题目</el-dropdown-item>
                </div>

              </el-dropdown-menu>
            </template>

          </el-dropdown>
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
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="题单名称" prop="listName">
              <el-input v-model="form.listName" placeholder="请输入题单名称"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item prop="remark" label="描述">
              <el-input v-model="form.description" type="textarea" placeholder="请输入标记" maxlength="450" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>

        <el-button type="primary" @click="submitForm" :loading="isUpdateLoading  || isAddLoading">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>

      </template>

    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import {
  type ListForm, type ListView,
  debouncedAddList,
  debouncedGetList,
  debouncedUpdateList,
  type QueryList,
  removeList
} from "@/api/list";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {ElDialog, ElMessageBox} from "element-plus";
import __ from "lodash";
import {useRouter} from "vue-router";
import type {IdType} from "@/api/common.ts";

const router = useRouter();
// 查询需要的表单数据
const queryParams = reactive<QueryList>({
  asc: true,
  currentPage: 1,
  pageSize: 20,
  listId: undefined,
  listName: undefined,
});

const form = reactive<ListForm>({
  listId: undefined,
  listName: '',
  description: '',
});


const rules = ref();

const open = ref(false);

const {columns} = useColumn(['题单ID', '题单名称', '题单描述', '创建时间']);

// 重制题单
const resetQuery = () => {
  queryParams.listName = undefined;
  queryParams.listId = undefined;
  queryParams.sortColumn = undefined;

  getList();
};

// 重置表单
const resetForm = () => {
  form.listId = undefined;
  form.listName = '';
  form.description = '';
}

const showSearch = ref(true);

const {loading, isLoading, get} = debouncedGetList(queryParams, (data) => {
  tableList.length = 0;
  total.value = data.totalRecords
  tableList.push(...data.data)
});

const tableList = reactive<ListView[]>([]);
const total = ref<number>(0);

// 获取题单
const getList = () => {
  loading();
  get();

}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])

const handleSelectionChange = (selection: ListView[]) => {
  ids.value = selection.map(item => item.listId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}



const handleDelete = (row?: ListView) => {
  const id = row ? row.listId : ids.value
  ElMessageBox.confirm(`您是否要删除ID为${id}的数据项？`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
      .then(() => {
        removeList(id).then(getList);
      })
}

// 搜索按钮
const handleQuery = () => {
  getList();
  single.value = false
  multiple.value = false;
}

const finishDialog = () => {
  open.value = false;
  resetForm();
  getList();
}

const {loading: updateLoading, isLoading: isUpdateLoading, update} = debouncedUpdateList(form, finishDialog);

const {loading: addLoading, isLoading: isAddLoading, add} = debouncedAddList(form, finishDialog);

const {} = debouncedAddList(form, finishDialog)
// 1: Insert 2: Update
const dialogState = ref(1);
const title = computed(() => {
  return dialogState.value === 1 ? "添加" : "修改";
})
const handleAdd = () => {
  resetForm();
  dialogState.value = 1;
  open.value = true;

}
const handleUpdate = (data: ListView | void) => {
  resetForm();
  open.value = true;
  dialogState.value = 2;

  if (!data) {
    const id = ids.value[0];
    data = __.find(tableList, x => x.listId === id)
  }
  __.assign(form, data)
}

const submitForm = () => {

  if (dialogState.value === 1) {
    addLoading();
    // 添加
    add();
  } else {
    updateLoading();
    // 修改
    update();
  }
}

const cancel = () => {
  open.value = false;
  resetForm()
}


const handleCommand = (command: string, row: ListView) => {
  if (command === 'handleProblem') {

    router.push({name: "list-problem", params: {id: row.listId}})
  }
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