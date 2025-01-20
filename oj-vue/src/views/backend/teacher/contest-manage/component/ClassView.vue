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
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <el-table v-loading="isLoading" :data="tableList">
      <el-table-column label="班级ID" align="center" prop="classId" v-if="columns[0].visible" show-overflow-tooltip />
      <el-table-column label="班级名称" align="center" prop="className" v-if="columns[1].visible" />
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[2].visible" />
      <el-table-column label="标注" align="center" prop="remark" v-if="columns[3].visible" />
      <el-table-column label="操作" align="center" type="default" >
        <template v-slot="scope">
          <el-link type="primary" @click="emit('selectClass', scope.row.classId)">选择</el-link>
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
  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from "vue";
import {type ClassView, debouncedGetClass, type QueryClass,} from "@/api/class";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import type {IdType} from "@/api/common.ts";

const emit = defineEmits<{(e: 'selectClass', classId: IdType): void}>();

// 查询需要的表单数据
const queryParams = reactive<QueryClass>({
  asc: true,
  currentPage: 1,
  pageSize: 20,
  classId: undefined,
  className: undefined,
});



const {columns} = useColumn(['班级ID', '班级名称', '创建时间', '标注']);


// 重制列表
const resetQuery = () => {
  queryParams.className = undefined;
  queryParams.classId = undefined;
  queryParams.sortColumn = undefined;
  getList();
};


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

// 搜索按钮
const handleQuery = () => {
  getList();
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