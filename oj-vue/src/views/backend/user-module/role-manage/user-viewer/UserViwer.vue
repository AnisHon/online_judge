<template>
  <div class="app-container" v-loading="addLoading">
    <el-form :model="queryParams" class="inline-form" :inline="true" v-show="showSearch" label-width="68px">

      <el-form-item label="用户名称" prop="username">
        <el-input
            v-model="queryParams.userName"
            placeholder="请输入用户名称"

            @keyup.enter.native="handleQuery"
            clearable
        />
      </el-form-item>
      <el-form-item label="用户昵称" prop="nikeName">
        <el-input
            v-model="queryParams.nikeName"
            placeholder="请输入用户昵称"
            clearable
            @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户邮箱" prop="email">
        <el-input
            v-model="queryParams.email"
            placeholder="请输入用户昵称"
            clearable
            @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户状态" prop="status">
        <el-select
            v-model="queryParams.status"
            placeholder="用户状态"
            clearable
            style="width: 120px"
        >
          <el-option
              v-for="item in dict.userStatus"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="search"  @click="handleQuery">搜索</el-button>
        <el-button icon="refresh"  @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>


<!--    '用户id', '用户名称', '邮箱地址', '用户昵称', '用户状态', '创建时间', '标记'-->
    <el-table v-loading="isLoading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="用户id" align="center" prop="userId" v-if="columns[0].visible" />
      <el-table-column label="用户名称" align="center" prop="userName" v-if="columns[1].visible" />
      <el-table-column label="用户昵称" align="center" prop="nikeName" v-if="columns[3].visible" />
      <el-table-column label="邮箱地址" align="center" prop="email" v-if="columns[2].visible" />
      <el-table-column label="用户状态" align="center" prop="icon" v-if="columns[4].visible">
        <template v-slot="scope">
          <el-tag type="danger" v-if="scope.row.status === 1">封禁</el-tag>
          <el-tag type="success" v-else>正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[5].visible" />
      <el-table-column label="标记" align="center" prop="remark" v-if="columns[6].visible" />
    </el-table>

    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.currentPage"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />

  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from "vue";
import {
  debouncedGetUser,

  dict,
  type QueryUser,
} from "@/api/user";
import {type UserView} from "@/api/user";
import {useStatuesColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";

import {getRole, type RoleView} from "@/api/role";
import type {IdType} from "@/api/common.ts";


// 查询需要的表单数据
const queryParams = reactive<QueryUser>({
  asc: true,
  currentPage: 1,
  pageSize: 20,
  userId: undefined,
  userName: undefined,
  nikeName: undefined,
  email: undefined,
  status: undefined,
});

const emit = defineEmits<{
  (e: 'update:ids', id: IdType[]): void
}>()

const {ids, loading: addLoading = false} = defineProps<{
  ids: IdType[]
  loading: boolean
}>()


const roles = reactive<RoleView[]>([])


const {columns} = useStatuesColumn(
    ['用户id', '用户名称', '邮箱地址', '用户昵称', '用户状态', '创建时间', '标记'],
    [true, true, false, true, true, false, false]
);


// 重制列表
const resetQuery = () => {
  queryParams.userId = undefined;
  queryParams.userName = undefined;
  queryParams.nikeName = undefined;
  queryParams.email = undefined;
  queryParams.status = undefined;
  queryParams.sortColumn = undefined;

  getList();
};


const showSearch = ref(true);

const {loading, isLoading, get: getUser} = debouncedGetUser(queryParams, (data) => {
  tableList.length = 0;
  total.value = data.totalRecords
  tableList.push(...data.data)
});

const tableList = reactive<UserView[]>([]);
const total = ref<number>(0);

// 获取列表
const getList = () => {
  loading();
  getUser();

}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组

const handleSelectionChange = (selection: UserView[]) => {
  ids.length = 0;
  ids.push(...selection.map(item => item.userId));
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}


// 搜索按钮
const handleQuery = () => {
  getList();
  single.value = false
  multiple.value = false;
}






// created -> 获取列表
getList()

getRole({currentPage: 1, pageSize: 200, asc: true}).then((data) => {
  roles.push(...data.data)
})


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
</style>