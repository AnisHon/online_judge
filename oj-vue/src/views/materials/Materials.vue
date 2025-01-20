<template>
  <div class="common-max-width-page app-container">
    <div class="search-bar">
      <el-button
          :disabled="unselect"
          type="primary"
          icon="download"
          @click="handleDownload()"
          round
      >
        下载文件
      </el-button>

      <el-button-group>
        <el-button
            type="primary"
            icon="upload"
            @click="openUpload = true"
            plain
            round
        >
          上传文件
        </el-button>
        <el-button
            type="primary"
            icon="DocumentAdd"
            @click="handleAddDir"
            plain
            round
        >
          新建文件夹
        </el-button>
      </el-button-group>

      <span class="select-text">
        <el-text v-show="!!ids.length" type="info" size="small">
          选择了{{ ids.length }}条数据,
        </el-text>
        <el-text type="info" size="small">
          当前共有：{{ list.length }}条数据
        </el-text>
      </span>
      <el-input style="max-width: 200px" placeholder="请输入文件名"/>
    </div>
    <div class="breadcrumb">
      <el-breadcrumb>
        <el-breadcrumb-item v-for="[index, item] of path.entries()" :key="item.id" @click="jumpTo(index)">
          <el-text>
            {{ item.path }}
          </el-text>

        </el-breadcrumb-item>

      </el-breadcrumb>
    </div>

    <div class="files">
      <el-table
          class="file-table"
          header-row-class-name="file-header-bar"
          header-cell-class-name="file-header-cell"
          row-class-name="file-row"
          cell-class-name="file-cell"
          :data="sortList"
          @selection-change="handleSelectionChange"
          @rowClick="handleClickRow"
      >
        <template #empty>
          <el-empty description="空空如也~"/>
        </template>
        <el-table-column type="selection" width="55" align="center" :selectable="selectable"/>

        <el-table-column label="文件名" width="400px" prop="cloudFileId" show-overflow-tooltip>
          <template v-slot="scope">
            <div class="file">
              <div class="icon">
               <file-icon :is-folder="scope.row.dir" :name="scope.row.fileName"/>
              </div>
              <el-text v-show="!scope.row.edit" size="large" type="info" class="filename">
                {{ scope.row.fileName }}
              </el-text>
              <div class="filename-input" v-show="scope.row.edit">
                <el-input v-model="scope.row.fileName" placeholder="请输入文件名"/>
                <el-button icon="check" type="success" @click.stop="submit(scope.row)" plain circle/>
                <el-button icon="close" type="primary" @click.stop="cancel(scope.row)" plain circle/>
              </div>
            </div>

          </template>
        </el-table-column>
        <el-table-column type="default" align="center" width="250">
          <template v-slot="scope">
            <div class="file-link">
              <el-link
                  size="small"
                  type="primary"
                  icon="edit"
                  @click.stop="handleRename(scope.row)"
              >重命名</el-link>
              <el-link
                  size="small"
                  type="primary"
                  icon="remove"
                  @click.stop="handleDelete(scope.row)"
              >删除</el-link>
              <el-link
                  v-if="!scope.row.dir"
                  size="small"
                  type="primary"
                  icon="edit"
                  @click.stop="handleDownload(scope.row)"
              >下载</el-link>
              <el-link
                  v-if="!scope.row.dir"
                  size="small"
                  type="primary"
                  icon="edit"
                  @click.stop="handlePreview(scope.row)"
              >查看</el-link>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="大小" prop="fileSize" align="center">
          <template v-slot="scope">
            <el-text size="small" type="info" v-if="scope.row.fileSize">
              {{ bytesToSize(scope.row.fileSize) }}
            </el-text>
            <el-text v-else/>
          </template>
        </el-table-column>
        <el-table-column label="创建者" prop="nikeName" >
          <template v-slot="scope">
            <el-text size="small" type="info">{{ scope.row.nikeName }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="最后修改时间" prop="updateTime" align="center" show-overflow-tooltip>
          <template v-slot="scope">
            <el-text size="small" type="info">{{ scope.row.updateTime }}</el-text>
          </template>
        </el-table-column>


      </el-table>
    </div>

    <upload :parent-id="query.parentId" v-model="openUpload" @finished="getList"/>
  </div>
</template>

<script setup lang="ts">
import {computed, ref} from "vue";
import {
  type CloudFile,
  debouncedAddDir,
  deleteFile,
  download,
  listFiles, preview,
  type QueryCloudFile,
  updateFile
} from "@/api/file";
import __ from "lodash";
import {ElMessageBox, ElNotification} from "element-plus";
import FileIcon from "@/components/FileIcon/FileIcon.vue";
import Upload from "@/views/materials/component/upload.vue";
import {bytesToSize} from "@/utils/byte2size.ts";


const openUpload = ref(false);

// 文件数据
const list = ref<CloudFile[]>([])


const sortList = computed(() => {
  list.value.sort((a: CloudFile, b: CloudFile) => +b.dir - +a.dir);
  return list.value;
})

// 当前面包屑文件路径
const path = ref([
    {
      path: "/根目录",
      id: "0"
    }
])

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<string[]>([])

// 查询参数
const query = ref<QueryCloudFile>({
  parentId: "0",
  fileName: ""
})

// 是否没有选择
const unselect = computed(() => {
  return single.value && multiple.value
})

// 是否可以选择
const selectable = (row: CloudFile) => {
  return !row.dir;
}

// 获取列表
const getList = async () => {
  list.value = await listFiles(query.value);
  list.value.sort((a, b) => +a.dir - +b.dir);
}

// 抖动添加
const addDir = debouncedAddDir(getList)

// 选框改变
const handleSelectionChange = (selection: CloudFile[]) => {
  ids.value = selection.filter(item => !item.dir).map(item => item.cloudFileId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

// 点击面包屑的跳转
const jumpTo = (index: number) => {
  if (index === path.value.length - 1) {
    return;
  }
  path.value.length = index + 1;
  query.value.parentId = path.value[index].id;

  getList();
}


// 提交更改文件夹名
const submit = async (row: CloudFile) => {
  if (!row.fileName) {
    ElNotification.warning("文件夹名不能为空");
    return;
  } else if (__.findIndex(list.value, item => item.fileName === row.fileName && item !== row) != -1) {
    ElNotification.warning("文件夹名不能重复");
    return;
  }
  if (row.add) {
    addDir({
      fileName: row.fileName,
      parentId: row.parentId,
    })

  } else {
    await updateFile(row);
  }
  row.edit = false;
  await getList();
}


// 取消更改文件夹
const cancel = (row: CloudFile) => {
  if (row.add) {
    __.remove(list.value, file => file === row);
  } else {
    row.fileName = row.fileNameCopy || "";
    row.edit = false;
  }
}

// 添加文件夹
const handleAddDir = () => {
  list.value.unshift({ cloudFileId: "",
    fileName: "",
    userId: "",
    nikeName: "",
    parentId: query.value.parentId,
    dir: true,
    edit: true,
    add: true
  })
}

// 删除文件夹
const handleDelete = (row: CloudFile) => {
  ElMessageBox.confirm(`您是否要删除这个文件(夹)？`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
      .then(async () => {
        await deleteFile(row.cloudFileId);
        await getList();
      }).catch(() => {})

}

// 处理点击文件时候打开或下载文件
const handleClickRow = (row: CloudFile) => {

  if (row.edit) {
    return;
  }

  if (row.dir) {
    path.value.push({
      id: row.cloudFileId,
      path: row.fileName
    })
    query.value.parentId = row.cloudFileId;
    getList();
  }
}

// 重命名
const handleRename = (row: CloudFile) => {
  row.edit = true;
  row.fileNameCopy = row.fileName;
  row.add = false;
}

// 处理下载文件
const handleDownload = (row?: CloudFile) => {
  if (row) {
    download(<string>row.filePath, row.fileName)
  } else {
    ids.value.forEach(
        item => {
          const result = list.value.find((value) => value.cloudFileId === item);
          if (result) {
            download(<string>result.filePath, result.fileName);
          }
        }
    )
  }
}

// 处理预览文件
const handlePreview = (row: CloudFile) => {
  if (row.filePath) {
    preview(row.filePath);
  }
}

getList();
</script>

<style lang="scss" scoped>
@use "@/assets/styles/color" as *;

.app-container {
  margin: auto;
}

.breadcrumb {
  margin-bottom: 10px;
}

.search-bar {
  display: flex;
  margin: 20px 0;
  align-items: end;
}

.search-bar * {
  margin-right: 5px;
}

.select-text {
  margin-left: auto;
}

.filename-input {
  display: flex;
}
.filename-input>*:first-child {
  margin-right: 5px;
}

.file {
  display: flex;
  align-items: center;
}

.file>* {
  margin-right: 5px;
}

.filename {
  cursor: pointer;
}

.filename:hover {
  color: var(--brand-color);
}

.file-link {
  display: none;
}

.file-link>* {
  margin: 0 5px;
}


// table的deep
::v-deep(.files .file-table) {
  .file-row {
    height: 66px;
    background-color: var(--el-fill-color-extra-light);
  }


  .file-cell {
    border: none;
    color: var(--regular-text);
  }

  .file-header-cell {
    background-color: $table-header-color;
    color: $table-header-text-color;
  }

  .file-row:hover .file-link {
    display: inline-block;
  }
}


</style>