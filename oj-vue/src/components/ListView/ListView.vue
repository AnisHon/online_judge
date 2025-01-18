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
        <el-input-number v-model="queryParams.listId" :controls="false"/>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="search"  @click="handleQuery">搜索</el-button>
        <el-button icon="refresh"  @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <el-table v-loading="isLoading" :data="tableList" >
      <el-table-column label="列表ID" align="center" prop="listId" v-if="columns[0].visible" />
      <el-table-column label="列表名称" align="center" prop="listName" v-if="columns[1].visible" />
      <el-table-column label="列表描述" align="center" prop="description" v-if="columns[2].visible" />
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[2].visible" />
      <el-table-column label="操作" align="center" list-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="edit"
              @click="handleChoose(scope.row)"
              v-has="'problem:list:update'"
          >选择</el-link>
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

  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from "vue";
import {
  type ListView,
  debouncedGetList,
  type QueryList,
} from "@/api/list";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import type {IdType} from "@/api/common.ts";

// 查询需要的表单数据
const queryParams = reactive<QueryList>({
  asc: true,
  currentPage: 1,
  pageSize: 20,
  listId: undefined,
  listName: undefined,
});

const listId = defineModel<IdType>()
const isOpen = defineModel("isOpen", {type: Boolean})

const {columns} = useColumn(['列表ID', '列表名称', '列表描述']);

// 重制列表
const resetQuery = () => {
  queryParams.listName = undefined;
  queryParams.listId = undefined;
  queryParams.sortColumn = undefined;

  getList();
};



const showSearch = ref(true);

const {loading, isLoading, get} = debouncedGetList(queryParams, (data) => {
  tableList.length = 0;
  total.value = data.totalRecords
  tableList.push(...data.data)
});

const tableList = reactive<ListView[]>([]);
const total = ref<number>(0);

// 获取列表
const getList = () => {
  loading();
  get();

}



// 搜索按钮
const handleQuery = () => {
  getList();
}

const handleChoose = (data: ListView) => {
  listId.value = data.listId;
  isOpen.value = false;
}


// created -> 获取列表
getList()

</script>

<style scoped>

</style>

<style lang="scss" scoped>
::v-deep(.inline-form ) {

  .el-input {
    --el-input-width: 220px;
  }

  .el-select {
    --el-select-width: 220px;
  }
}

::v-deep(.inline-form) {
  .el-table__row .el-dropdown {
    height: 23px;
  }
}
</style>