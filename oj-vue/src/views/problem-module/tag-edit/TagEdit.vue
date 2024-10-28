<template>
  <div class="tag-container">
    <el-row :gutter="10" tag="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'problem:tag:add'"
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
            v-has="'problem:tag:update'"
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
            v-has="'problem:tag:delete'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

<!--    ['标签ID', '标签名称', '标签颜色', '标签颜色预览','创建时间']-->
    <el-table v-loading="isLoading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="标签ID" align="center" prop="tagId" v-if="columns[0].visible" />
      <el-table-column label="标签名称" align="center" prop="tagName" v-if="columns[1].visible" />
      <el-table-column label="标签颜色" align="center" prop="tagColor" v-if="columns[2].visible" />
      <el-table-column label="标签颜色预览" align="center" v-if="columns[3].visible" >
        <template v-slot="scope">
          <el-tag :color="scope.row.tagColor">
            <div style="color: white">{{ scope.row.tagName }}</div>
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[4].visible" />
      <el-table-column label="操作" align="center" tag-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="edit"
              @click="handleUpdate(scope.row)"
              v-has="'problem:tag:update'"
          >修改</el-link>
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'problem:tag:delete'"
          >删除</el-link>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改测试功能对话框 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="标签名称" prop="icon">
              <el-input v-model="form.tagName" placeholder="请输入标签名称"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="标签颜色名称" prop="icon">
              <el-input v-model="form.tagColor" placeholder="请输入标签颜色名称"/>
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
  type TagForm, type TagView,
  debouncedAddTag,
  debouncedGetTag,
  debouncedUpdateTag,
  removeTag
} from "@/api/tag";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import {ElDialog, ElMessageBox} from "element-plus";
import __ from "lodash";


// 查询需要的表单数据


const form = reactive<TagForm>({
  tagId: undefined,
  tagName: '',
  tagColor: ''
});


const rules = ref();

const open = ref(false);

const {columns} = useColumn(['标签ID', '标签名称', '标签颜色', '标签颜色预览','创建时间']);



// 重置表单
const resetForm = () => {
  form.tagId = undefined;
  form.tagName = '';
}

const showSearch = ref(true);

const {loading, isLoading, get: getTag} = debouncedGetTag((data) => {
  tableList.length = 0;
  tableList.push(...data)
});

const tableList = reactive<TagView[]>([]);

// 获取列表
const getList = () => {
  loading();
  getTag();

}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<number[]>([])

const handleSelectionChange = (selection: TagView[]) => {
  ids.value = selection.map(item => item.tagId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}



const handleDelete = (row: TagView | Event) => {
  if (row instanceof Event) {
    ElMessageBox.confirm(`您是否要删除ID为${ids.value}的数据项？`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeTag(ids.value).then(getList);
        })
  } else {
    ElMessageBox.confirm('是否确认删除名称为"' + row.tagName + '"的数据项？', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeTag(row.tagId).then(getList);
        })
  }


}


const finishDialog = () => {
  open.value = false;
  resetForm();
  getList();
}

const {loading: updateLoading, isLoading: isUpdateLoading, update} = debouncedUpdateTag(form, finishDialog);

const {loading: addLoading, isLoading: isAddLoading, add} = debouncedAddTag(form, finishDialog);

const {} = debouncedAddTag(form, finishDialog)
// 1: Insert 2: Update
const dialogState = ref(1);
const title = computed(() => {
  return dialogState.value === 1 ? "添加" : "修改";
})
const handleAdd = () => {
  dialogState.value = 1;
  open.value = true;
}
const handleUpdate = (data: TagView) => {
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