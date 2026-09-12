<template>

  <div class="list-picker">
    <div class="picker-hint"><span class="picker-hint__icon"><el-icon><List /></el-icon></span><span><strong>题单列表</strong><small>点击整行即可选中，名称和 ID 会回填到当前目录节点。</small></span></div>
    <el-form :model="queryParams" class="inline-form picker-form" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="列表名称" prop="listName">
        <el-input
            v-model="queryParams.listName"
            placeholder="请输入列表名称"

            @keyup.enter.native="handleQuery"
            clearable
        />
      </el-form-item>
      <el-form-item label="列表ID" prop="parentId">
        <el-input v-model="queryParams.listId" clearable placeholder="输入 ID"/>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="search"  @click="handleQuery">搜索</el-button>
        <el-button icon="refresh"  @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <el-table v-loading="isLoading" :data="tableList" class="picker-table" highlight-current-row @row-click="handleChoose">
      <el-table-column label="题单" min-width="250" v-if="columns[0].visible || columns[1].visible">
        <template #default="{ row }">
          <div class="picker-list-cell">
            <strong :title="row.listName">{{ row.listName || '未命名题单' }}</strong>
            <span :title="String(row.listId)">#{{ shortListId(row.listId) }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="列表描述" align="center" prop="description" v-if="columns[2].visible" />
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[3].visible" />
      <el-table-column label="操作" align="right" width="90" fixed="right">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="Select"
              @click="handleChoose(scope.row)"
          >选用</el-link>
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
import {List} from "@element-plus/icons-vue";

const shortListId = (value: IdType) => {
  const text = String(value);
  return text.length > 16 ? `${text.slice(0, 8)}…${text.slice(-5)}` : text;
};

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
const emit = defineEmits<{(e: 'select', list: ListView): void}>();

const {columns} = useColumn(['列表ID', '列表名称', '列表描述', '创建时间']);

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
  emit('select', data);
  isOpen.value = false;
}


// created -> 获取列表
getList()

</script>

<style lang="scss" scoped>
.list-picker { color: var(--el-text-color-primary); }
.picker-hint { display: flex; align-items: center; gap: 10px; margin-bottom: 17px; padding: 12px 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 9px; background: var(--el-fill-color-light); }.picker-hint__icon { display: grid; width: 30px; height: 30px; place-items: center; border-radius: 8px; color: var(--el-color-primary); background: var(--el-color-primary-light-8); }.picker-hint strong, .picker-hint small { display: block; }.picker-hint strong { font-size: 13px; }.picker-hint small { margin-top: 2px; color: var(--el-text-color-secondary); font-size: 11px; }
.picker-form { display: flex; align-items: center; flex-wrap: wrap; gap: 4px 0; margin-bottom: 12px; }.picker-form :deep(.el-form-item) { margin-bottom: 8px; }.picker-form :deep(.el-input), .picker-form :deep(.el-input-number) { width: 190px; }.picker-table { cursor: pointer; }.picker-table :deep(.el-table__row:hover > td) { background: var(--el-color-primary-light-9); }.picker-table :deep(.el-table__cell) { padding: 11px 0; }.picker-table :deep(.el-table__header-wrapper th) { color: var(--el-text-color-secondary); font-size: 12px; background: var(--el-fill-color-light); }
.picker-list-cell { display: flex; min-width: 0; flex-direction: column; align-items: flex-start; gap: 4px; padding: 1px 0; }.picker-list-cell strong { max-width: 100%; overflow: hidden; color: var(--el-text-color-primary); font-size: 13px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }.picker-list-cell span { max-width: 100%; overflow: hidden; color: var(--el-text-color-secondary); font: 12px/1.2 var(--code-font-family, monospace); text-overflow: ellipsis; white-space: nowrap; }
@media (max-width: 640px) { .picker-form :deep(.el-form-item), .picker-form :deep(.el-input), .picker-form :deep(.el-input-number) { width: 100%; }.picker-form :deep(.el-form-item__content) { width: 100%; }.picker-form :deep(.el-button) { flex: 1; }.picker-hint { align-items: flex-start; } }
</style>
