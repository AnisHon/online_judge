<template>
  <div class="contest-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="ArrowLeftBold"
            size="small"
            @click="router.back()"
        >返回</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'problem:contest:edit'"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="delete"
            size="small"
            :disabled="single"
            @click="handleDelete()"
            v-has="'problem:contest:remove'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <el-table :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="比赛ID" align="center" prop="nikeName" v-if="columns[0].visible" show-overflow-tooltip />
      <el-table-column label="最迟时间" align="center" prop="deadline" v-if="columns[1].visible" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'problem:contest:remove'"
          >删除</el-link>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加或修改测试功能对话框 -->
    <el-dialog :title="title" width="30%" v-model="open" append-to-body>
      <el-form :model="form" :rules="rules" label-position="top" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="用户Id" prop="userId">
              <el-input v-model="form.userId" placeholder="请选择用户Id"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="最迟时间" prop="deadline">
              <el-date-picker
                  v-model="form.deadline"
                  type="datetime"
                  placeholder="最迟时间"
                  format="YYYY-MM-DD HH:mm:ss"
                  date-format="MMM DD, YYYY"
                  time-format="HH:mm"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" :loading="isLoading">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="选择题单" v-model="openSelectList" append-to-body>

    </el-dialog>


  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import {ElDialog, ElMessageBox} from "element-plus";
import {useRoute, useRouter} from "vue-router";
import {
  addSupplement,
  getSupplements,
  removeSupplement,
  type Supplement,
  type SupplementForm
} from "@/api/contest/supplement.ts";
import useLoading from "@/hooks/useLoading.ts";
import __ from "lodash";

const router = useRouter();

const route = useRoute();

const openSelectList = ref(false);

const {loading, isLoading, finish} = useLoading();


const form = reactive<SupplementForm>({
  contestId: <string>route.params.contestId,
  userId: undefined,
  deadline: undefined
});


const rules = ref();

const open = ref(false);

const {columns} = useColumn(['昵称', '最迟时间']);

// 重置表单
const resetForm = () => {
  form.userId = undefined;
  form.deadline = undefined;
}

const showSearch = ref(true);

const tableList = reactive<Supplement[]>([]);

// 获取列表
const getList = async () => {
  const supplements = await getSupplements(form.contestId);
  tableList.length = 0;
  tableList.push(...supplements);

}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<Supplement[]>([])

const handleSelectionChange = (selection: Supplement[]) => {
  ids.value = selection;
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

const handleDelete = (row: Supplement | void) => {

  const id: Supplement = row ? row : ids.value[0];
  ElMessageBox.confirm(`您是否要取消补交？`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
      .then(() => {
        removeSupplement(id.contestId, id.userId);
      })
}

// 1: Insert 2: Update
const dialogState = ref(1);
const title = computed(() => {
  return dialogState.value === 1 ? "添加" : "修改";
})
const handleAdd = () => {
  dialogState.value = 1;
  open.value = true;
}


const debouncedAdd = __.debounce(async () => {
  await addSupplement(form);
  finish();
  await getList();
}, 1000)

const submitForm = () => {
  if (dialogState.value === 1) {
    loading();
    debouncedAdd();
  }
}

const cancel = () => {
  open.value = false;
  resetForm()
}

// created -> 获取列表
getList();




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