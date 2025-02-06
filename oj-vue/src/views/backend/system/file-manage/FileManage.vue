<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="delete"
            size="small"
            :disabled="multiple"
            @click="handleDelete()"
            v-has="'content:file:remove'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <el-table :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="文件ID" align="center" prop="fileId" v-if="columns[0].visible" show-overflow-tooltip />
      <el-table-column label="文件MD5" align="center" prop="fileMd5" v-if="columns[1].visible" show-overflow-tooltip />
      <el-table-column label="文件名称" align="center" prop="fileName" v-if="columns[2].visible" show-overflow-tooltip />
      <el-table-column label="文件路径" align="center" prop="filePath" v-if="columns[3].visible" show-overflow-tooltip />
      <el-table-column label="文件大小" align="center" prop="fileSize" v-if="columns[4].visible" >
        <template v-slot="scope">
          {{ bytesToSize(scope.row.fileSize) }}
        </template>
      </el-table-column>
      <el-table-column label="文件类型" align="center" prop="fileType" v-if="columns[5].visible" />
      <el-table-column label="文件引用" align="center" prop="reference" v-if="columns[6].visible" />
      <el-table-column label="上传时间" align="center" prop="uploadTime" v-if="columns[7].visible" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-space>
            <el-link
                size="small"
                type="primary"
                icon="delete"
                @click="handleDelete(scope.row)"
                v-has="'user:class:remove'"
            >删除</el-link>
            <el-link
                size="small"
                type="primary"
                icon="download"
                @click="handleDownload(scope.row)"
                v-has="'user:class:remove'"
            >下载</el-link>
          </el-space>

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
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {ElMessageBox} from "element-plus";
import type {IdType} from "@/api/common.ts";
import type {PagedType} from "@/api/pagedType.ts";
import {type FileInfo, listFileInfo, removeFileInfo} from "@/api/file/fileInfo.ts";
import {bytesToSize} from "@/utils/byte2size.ts";
import {downloadFile} from "@/api/file";


// 查询需要的表单数据
const queryParams = reactive<PagedType>({
  currentPage: 1,
  pageSize: 20,
});


const {columns} = useColumn(['文件ID', '文件MD5', '文件名', '文件路径', "文件大小", "文件类型", "文件引用数", "文件上传时间"]);


const showSearch = ref(true);

const tableList = reactive<FileInfo[]>([]);
const total = ref<number>(0);

// 获取列表
const getList = async () => {
  const {data, totalRecords} = await listFileInfo(queryParams)
  tableList.length = 0;
  tableList.push(...data);
  total.value = totalRecords;
}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])

const handleSelectionChange = (selection: FileInfo[]) => {
  ids.value = selection.map(item => item.fileId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

const handleDownload = (row: FileInfo) => {
  let filename = row.fileName;
  if (row.fileType) {
    filename += "." + row.fileType;
  }
  downloadFile(row.filePath, filename);
}

const handleDelete = (row?: FileInfo) => {
  const id = row ? row.fileId : ids.value
  ElMessageBox.confirm(`您是否要删除ID为${id}的数据项？`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
      .then(() => {
        removeFileInfo(id)
            .then(getList)
      })
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