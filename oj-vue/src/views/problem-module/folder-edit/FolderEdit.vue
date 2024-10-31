<template>
  <div class="folder-container">
    <el-row :gutter="10" folder="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'problem:folder:add'"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="edit"
            size="small"
            :disabled="single"
            @click="handleUpdate"
            v-has="'problem:folder:update'"
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
            v-has="'problem:folder:del'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <!--    ['文件夹ID', '文件夹名称', '文件夹颜色', '文件夹颜色预览','创建时间']-->
    <el-table v-loading="isLoading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="文件夹ID" align="center" prop="folderId" v-if="columns[0].visible" />
      <el-table-column label="文件夹名称" align="center" prop="folderName" v-if="columns[1].visible" />
      <el-table-column label="文件夹类型" align="center" prop="folderType" v-if="columns[1].visible" />
      <el-table-column label="题单ID" align="center" prop="listId" v-if="columns[3].visible" />
      <el-table-column label="父文件ID" align="center" prop="parentId" v-if="columns[4].visible" />
      <el-table-column label="操作" align="center" folder-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="edit"
              @click="handleUpdate(scope.row)"
              v-has="'problem:folder:update'"
          >修改</el-link>
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'problem:folder:del'"
          >删除</el-link>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改测试功能对话框 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="名称" prop="icon">
              <el-input v-model="form.folderName" placeholder="请输入名称"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="类型" prop="icon">
              <el-select v-model="form.folderType" placeholder="请选择类型">
                <el-option v-for="item of dict.folderType" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="题单ID" prop="icon">
              <el-input-number v-model="form.listId" :controls="false" :disabled="form.folderType !== FolderType.FILE"  placeholder="请输入题单ID" />

            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="父ID" prop="icon">
              <el-select v-model="form.parentId" placeholder="请选择类型" default-first-option>
                <el-option label="无" :value="0"/>
                <el-option
                    v-for="item of tableList"
                    :label="`${item.folderId}
                    ${item.folderName}`"
                    :value="item.folderId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div slot="footer">
          <el-button type="primary" @click="submitForm" :loading="isUpdateLoading  || isAddLoading">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import {
  debouncedAddFolder,
  debouncedGetFolder,
  debouncedUpdateFolder,
  dict,
  type FolderForm,
  FolderType,
  type FolderView,
  removeFolder
} from "@/api/folder";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import {ElDialog, ElMessageBox} from "element-plus";
import __ from "lodash";


// 查询需要的表单数据


const form = reactive<FolderForm>({
  folderId: undefined,
  folderName: '',
  folderType: undefined,
  listId: undefined,
  parentId: undefined,
});


const rules = ref();

const open = ref(false);

const {columns} = useColumn(['文件夹ID', '文件夹名称', '文件夹类型', "题单ID", '父文件夹ID']);




// 重置表单
const resetForm = () => {
  form.folderId = undefined;
  form.folderName = '';
  form.parentId = undefined;
  form.listId = undefined;
  form.folderType = undefined;
}

const showSearch = ref(true);

const {loading, isLoading, get: getFolder} = debouncedGetFolder((data) => {
  tableList.length = 0;
  tableList.push(...data)
});

const tableList = reactive<FolderView[]>([]);

// 获取列表
const getList = () => {
  loading();
  getFolder();

}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<number[]>([])

const handleSelectionChange = (selection: FolderView[]) => {
  ids.value = selection.map(item => item.folderId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

const filterList = (id: number) => {
  return tableList.filter(x => x.listId !== id && x.folderType !== FolderType.FILE)
}



const handleDelete = (row: FolderView | Event) => {
  if (row instanceof Event) {
    ElMessageBox.confirm(`您是否要删除ID为${ids.value}的数据项？`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeFolder(ids.value).then(getList);
        })
  } else {
    ElMessageBox.confirm('是否确认删除名称为"' + row.folderName + '"的数据项？', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeFolder(row.folderId).then(getList);
        })
  }


}


const finishDialog = () => {
  open.value = false;
  resetForm();
  getList();
}

const {loading: updateLoading, isLoading: isUpdateLoading, update} = debouncedUpdateFolder(form, finishDialog);

const {loading: addLoading, isLoading: isAddLoading, add} = debouncedAddFolder(form, finishDialog);

// 1: Insert 2: Update
const dialogState = ref(1);
const title = computed(() => {
  return dialogState.value === 1 ? "添加" : "修改";
})
const handleAdd = () => {
  dialogState.value = 1;
  open.value = true;
}
const handleUpdate = (data: FolderView) => {
  open.value = true;
  dialogState.value = 2;
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






// created -> 获取列表
getList()




</script>

<style scoped>

</style>

<style>
</style>