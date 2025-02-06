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
            type="danger"
            plain
            icon="delete"
            size="small"
            :disabled="multiple"
            @click="handleDelete"
            v-has="'problem:folder:remove'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <!--    ['文件夹ID', '文件夹名称', '文件夹颜色', '文件夹颜色预览','创建时间']-->
    <el-table
        v-loading="isLoading"
        :data="tableList"
        @selection-change="handleSelectionChange"
        :tree-props="treeProps"
        row-key="folder.folderId"
    >
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="文件夹ID" align="center" prop="folder.folderId" v-if="columns[0].visible" />
      <el-table-column label="文件夹名称" align="center" prop="folder.folderName" v-if="columns[1].visible" />
      <el-table-column label="文件夹类型" align="center" prop="folder.folderType" v-if="columns[1].visible" />
      <el-table-column label="题单ID" align="center" prop="folder.listId" v-if="columns[3].visible" />
      <el-table-column label="父文件ID" align="center" prop="folder.parentId" v-if="columns[4].visible" />
      <el-table-column label="顺序" align="center" prop="folder.order" v-if="columns[5].visible" />
      <el-table-column label="操作" align="center" folder-name="folder.small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="edit"
              @click="handleUpdate(scope.row)"
              v-has="'problem:folder:edit'"
          >修改</el-link>
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'problem:folder:remove'"
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
            <el-form-item label="题单ID" prop="icon" >
              <el-input
                  v-model="form.listId"
                  @click="openSelectList = true"
                  :controls="false"
                  :disabled="form.folderType !== FolderType.FILE"
                  placeholder="请输入题单ID"
              />

            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="顺序" prop="order" >
              <el-input-number
                  v-model="form.order"
                  :controls="false"
                  placeholder="请输入顺序"
              />

            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item>
              <el-checkbox v-model="hasParentId">是否有父目录</el-checkbox>
            </el-form-item>
          </el-col>
          <el-col :span="24"  v-show="hasParentId">
            <el-form-item label="父ID" prop="icon">
              <el-tree
                  ref="treeRef"
                  node-key="id"
                  :props="defaultProps"
                  :data="treeList"
                  show-checkbox
                  check-strictly
                  :filter-node-method="filterNode"
                  @check="handleCheckChange"

              />
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

    <el-dialog title="选择题单" v-model="openSelectList" append-to-body>
      <ListView v-model="form.listId" v-model:isOpen="openSelectList"/>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, nextTick, reactive, ref} from "vue";
import {
  debouncedAddFolder,
  debouncedGetTreedFolder,
  debouncedUpdateFolder,
  dict,
  type FolderForm,
  FolderType,
  removeFolder,
  type TreedFolderView
} from "@/api/folder";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import {ElDialog, ElMessageBox, type ElTree} from "element-plus";
import __ from "lodash";
import ListView from "@/components/ListView/ListView.vue";
import type {IdType} from "@/api/common.ts";


// 查询需要的表单数据

const hasParentId = ref(true);
const form = reactive<FolderForm>({
  folderId: undefined,
  folderName: '',
  folderType: undefined,
  listId: undefined,
  order: 0,
  parentId: undefined,
});

const treeProps = reactive({
  checkStrictly: false,
  indent: 100
})

const defaultProps = {
  children: (x: TreedFolderView) => x.children,
  label: (x: TreedFolderView) => x.folder.folderName,
}

// 树形图的ref
const treeRef = ref<InstanceType<typeof ElTree>>()

const rules = ref();

const open = ref(false);
const openSelectList = ref(false);

const {columns} = useColumn(['文件夹ID', '文件夹名称', '文件夹类型', "题单ID", '父文件夹ID', '顺序']);


// 重置表单
const resetForm = () => {
  form.folderId = undefined;
  form.folderName = '';
  form.parentId = undefined;
  form.listId = undefined;
  form.folderType = undefined;
  form.order = 0;
  treeRef.value?.setCheckedKeys([]);
}

const setNodeKey = (node: TreedFolderView[]) => {
  node.forEach(x => {
    x.id = x.folder.folderId
    if (!x.file) {
      setNodeKey(x.children);
    }
  })
}

const showSearch = ref(true);

const {loading, isLoading, get: getFolder} = debouncedGetTreedFolder((data) => {
  tableList.length = 0;
  tableList.push(...data)
  setNodeKey(tableList)
});

const tableList = reactive<TreedFolderView[]>([]);

const getTreeList = (list: TreedFolderView[]): TreedFolderView[] => {
  const newList: TreedFolderView[] = [];
  if (list) {
    list.forEach(x => {
      if (x.folder.folderType !== FolderType.FILE) {
        x = Object.assign({}, x);
        x.children = getTreeList(x.children);
        newList.push(x);
      }

    })
  }

  return newList;
}

const treeList = computed(() => {
  return tableList;
})

// 获取列表
const getList = () => {
  loading();
  getFolder();

}

const isParent = (value: TreedFolderView, data: TreedFolderView): boolean => {
  if (!data) {
    return false;
  }
  if (value.folder.folderId === data.folder.folderId) {
    return true;
  } else {
    if (!value.children) {
      return false;
    }

    for (let child of value.children) {
      const result = isParent(child, data);
      if (result) {
        return true;
      }
    }
    return false;
  }
}

// 过滤
const filterNode = (value: TreedFolderView | undefined, data: TreedFolderView) => {
  const notFile = data.folder?.folderType !== FolderType.FILE;
  if (value) {
    return !isParent(value, data) && notFile;
  } else {
    return notFile;
  }
}

const handleCheckChange = (data: TreedFolderView, checked: boolean, indeterminate: boolean) => {
  treeRef.value!.setCheckedKeys([], false);
  treeRef.value?.setChecked(data.folder.folderId, checked, false);
}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])

const handleSelectionChange = (selection: TreedFolderView[]) => {
  ids.value = selection.map(item => item.folder.folderId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}


const getIds = (folder: TreedFolderView, ids: IdType[] | undefined) => {
  if (!ids) {
    ids = []
  }

  ids.push(folder.folder.folderId);
  if (!__.isEmpty(folder.children)) {
    folder.children.forEach(x => getIds(x, ids));
  }
  return ids;
}

const handleDelete = (row: TreedFolderView | Event) => {
  if (row instanceof Event) {
    ElMessageBox.confirm(`您是否要删除ID为${ids.value}的数据项？`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeFolder(ids.value).then(getList);
        })
  } else {
    ElMessageBox.confirm('是否确认删除名称为"' + row.folder.folderName + '"的数据项目和其子数据项？？', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          const ids = getIds(row, undefined)
          removeFolder(ids).then(getList);
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
  resetForm();
  treeRef.value?.filter(null)
  dialogState.value = 1;
  open.value = true;
}
const handleUpdate = (data: TreedFolderView) => {
  resetForm();
  open.value = true;

  nextTick(() => {
    treeRef.value?.filter(data)
    treeRef.value?.setChecked(data.folder.parentId, true, false);
  })

  dialogState.value = 2;
  __.assign(form, data.folder)
}

const submitForm = () => {

  const keys = treeRef.value?.getCheckedKeys();
  if (!hasParentId) {
    form.parentId = '0';
  } else if (keys?.length) {
    form.parentId = <IdType>keys[0]
  } else {
    form.parentId = '0';
  }
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