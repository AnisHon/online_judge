<template>
  <div class="app-container">
    <el-form :model="queryParams" class="inline-form" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="班级名称" prop="className">
        <el-input
            v-model="queryParams.className"
            placeholder="请输入班级名称"

            @keyup.enter.native="handleQuery"
            clearable
        />
      </el-form-item>
      <el-form-item label="班级ID" prop="parentId">
        <el-input-number v-model="queryParams.classId" :controls="false"/>
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
            v-has="'user:class:add'"
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
            v-has="'user:class:edit'"
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
            v-has="'user:class:remove'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <el-table v-loading="isLoading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="班级ID" align="center" prop="classId" v-if="columns[0].visible" show-overflow-tooltip />
      <el-table-column label="班级名称" align="center" prop="className" v-if="columns[1].visible" />
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[2].visible" />
      <el-table-column label="标注" align="center" prop="remark" v-if="columns[3].visible" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="edit"
              @click="handleUpdate(scope.row)"
              v-has="'user:class:edit'"
          >修改</el-link>
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'user:class:remove'"
          >删除</el-link>
          <el-dropdown size="small" @command="(command: string) => handleCommand(command, scope.row)"
                       v-has-any="['problem:contest:rank', 'problem:contest:statistic', 'problem:contest:edit']">
            <el-link size="small" type="primary" icon="arrow-right">更多</el-link>
            <template #dropdown>
              <el-dropdown-menu>
                <div>
                  <el-dropdown-item command="handleStudent" icon="UserFilled"
                  >学生管理</el-dropdown-item>
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

    <!-- 添加或修改测试功能对话框 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="班级名称" prop="icon">
              <el-input v-model="form.className" placeholder="请输入班级名称"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item prop="perms" label="标记">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入标记" maxlength="450" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" :loading="isUpdateLoading  || isAddLoading">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import {
  type ClassForm, type ClassView,
  debouncedAddClass,
  debouncedGetClass,
  debouncedUpdateClass,
  type QueryClass,
  removeClass
} from "@/api/class";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {ElDialog, ElMessageBox} from "element-plus";
import __ from "lodash";
import type {IdType} from "@/api/common.ts";

import {useRouter} from "vue-router";

const router = useRouter();

// 查询需要的表单数据
const queryParams = reactive<QueryClass>({
  asc: true,
  currentPage: 1,
  pageSize: 20,
  classId: undefined,
  className: undefined,
});

const form = reactive<ClassForm>({
  classId: undefined,
  className: '',
  remark: ''
});

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])
// 1: Insert 2: Update
const dialogState = ref(1);

const rules = ref();

const open = ref(false);

const {columns} = useColumn(['班级ID', '班级名称', '创建时间', '标注']);


// 重制列表
const resetQuery = () => {
  queryParams.className = undefined;
  queryParams.classId = undefined;
  queryParams.sortColumn = undefined;

  getList();
};

// 重置表单
const resetForm = () => {
  form.classId = undefined;
  form.className = '';
  form.remark = '';
}

const showSearch = ref(true);

const {loading, isLoading, get: getClass} = debouncedGetClass(queryParams, (data) => {
  tableList.length = 0;
  total.value = data.totalRecords
  tableList.push(...data.data)
});

const tableList = reactive<ClassView[]>([]);
const total = ref<number>(0);

// 获取列表
const getList = () => {
  loading();
  getClass();

}


const handleSelectionChange = (selection: ClassView[]) => {
  ids.value = selection.map(item => item.classId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}



const handleDelete = (row: ClassView | Event) => {
  if (row instanceof Event) {
    ElMessageBox.confirm(`您是否要删除ID为${ids.value}的数据项？`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeClass(ids.value).then(getList);
        })
  } else {
    ElMessageBox.confirm('是否确认删除名称为"' + row.className + '"的数据项？', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeClass(row.classId).then(getList);
        })
  }


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

const {loading: updateLoading, isLoading: isUpdateLoading, update} = debouncedUpdateClass(form, finishDialog);

const {loading: addLoading, isLoading: isAddLoading, add} = debouncedAddClass(form, finishDialog);

const {} = debouncedAddClass(form, finishDialog)

const title = computed(() => {
  return dialogState.value === 1 ? "添加" : "修改";
})
const handleAdd = () => {
  dialogState.value = 1;
  open.value = true;
}
const handleUpdate = (data: ClassView | void) => {
  open.value = true;
  dialogState.value = 2;
  if (!data) {
    const id = ids.value[0];
    data = __.find(tableList, x => x.classId === id)
  }

  __.assign(form, data)
}

const submitForm = async () => {

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



const handleCommand = (command: string, row: ClassView) => {
  const commandMap: Record<string, Function> = {
    "handleStudent": () => {router.push({name: "student-manage", params:{classId: row.classId}})}
  }

  commandMap[command](row);
}


// created -> 获取列表
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