<template>
  <div class="contest-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'problem:contest:add'"
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
            v-has="'problem:contest:edit'"
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
            v-has="'problem:contest:remove'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

<!--    ['比赛ID', '比赛标题', '权限', '开始时间', '结束时间', '密码', '列表ID', '描述']-->
    <el-table v-loading="isLoading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="比赛ID" align="center" prop="contestId" v-if="columns[0].visible" show-overflow-tooltip />
      <el-table-column label="比赛标题" align="center" prop="title" v-if="columns[1].visible" show-overflow-tooltip/>
      <el-table-column label="权限" align="center" prop="auth" v-if="columns[2].visible" >
        <template v-slot="scope">
          <el-tag :type="authTagType(scope.row.auth)">{{ authText(scope.row.auth) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" v-if="columns[3].visible" show-overflow-tooltip/>
      <el-table-column label="结束时间" align="center" prop="endTime" v-if="columns[4].visible" show-overflow-tooltip/>
      <el-table-column label="密码" align="center" prop="pwd" v-if="columns[5].visible" show-overflow-tooltip/>
      <el-table-column label="类型" align="center" prop="type" v-if="columns[6].visible">
        <template v-slot="scope">
          <el-tag v-if="scope.row.type === ContestType.CONTEST" type="warning">
            竞赛
          </el-tag>
          <el-tag v-else type="danger">
            作业
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="列表ID" align="center" prop="listId" v-if="columns[7].visible" show-overflow-tooltip />
      <el-table-column label="描述" align="center" prop="description" v-if="columns[8].visible" show-overflow-tooltip />
      <el-table-column label="参加人数" align="center" prop="joinedNumber" v-if="columns[9].visible" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="edit"
              @click="handleUpdate(scope.row)"
              v-has="'problem:contest:edit'"
          >修改</el-link>
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'problem:contest:remove'"
          >删除</el-link>

          <el-dropdown size="small" @command="(command: string) => handleCommand(command, scope.row)"
                       v-has-any="['problem:contest:rank', 'problem:contest:statistic', 'problem:contest:edit']">
            <el-link size="small" type="primary" icon="arrow-right">更多</el-link>
            <template #dropdown>
              <el-dropdown-menu>
                <div v-has="'problem:contest:statistic'">
                  <el-dropdown-item command="handleProblemStatistic" icon="PieChart"
                  >题目统计</el-dropdown-item>
                </div>
                <div v-has="'problem:contest:statistic'" >
                  <el-dropdown-item command="handleUserStatistic" icon="TrendCharts"
                  >用户统计</el-dropdown-item>
                </div>
                <div v-has="'problem:contest:edit'" >
                  <el-dropdown-item command="handleSupplement" icon="UserFilled"
                  >设置迟交</el-dropdown-item>
                </div>

              </el-dropdown-menu>
            </template>

          </el-dropdown>
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

    <!-- 添加或修改测试功能对话框 -->
    <el-dialog :title="title" v-model="open" append-to-body>
      <el-form :model="form" :rules="rules" label-position="top" label-width="100px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="比赛标题" prop="title">
              <el-input v-model="form.title" placeholder="请输入比赛标题"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="比赛类型" prop="auth">
              <el-radio-group v-model="form.auth">
                <el-radio v-for="item of dict.contestAuth" :label="item.label" :value="item.value" >{{ item.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="类型" prop="auth">
              <el-radio-group v-model="form.type">
                <el-radio v-for="item of dict.contestType" :label="item.label" :value="item.value" >{{ item.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="列表ID" prop="listId">
              <el-input
                  @click="openSelectList = true"
                  v-model="form.listId"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12" >
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                  v-model="form.startTime"
                  type="datetime"
                  placeholder="开始时间"
                  format="YYYY-MM-DD HH:mm:ss"
                  date-format="MMM DD, YYYY"
                  time-format="HH:mm"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                  v-model="form.endTime"
                  type="datetime"
                  placeholder="结束时间"
                  format="YYYY-MM-DD HH:mm:ss"
                  date-format="MMM DD, YYYY"
                  time-format="HH:mm"
              />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item prop="pwd" label="密码">
              <el-input v-model="form.pwd" :disabled="form.auth !== ContestAuth.PRIVATE" placeholder="私有赛密码" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item prop="perms" label="描述">
              <MarkDownEditor v-model="form.description" />
<!--              <el-input type="textarea" v-model="form.description" placeholder="请输入描述"/>-->
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" :loading="isUpdateLoading  || isAddLoading">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <el-dialog title="选择题单" v-model="openSelectList" append-to-body>
      <ListView v-model="form.listId" v-model:isOpen="openSelectList"/>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import {
  ContestAuth,
  type ContestForm, ContestType,
  type ContestView,
  debouncedAddContest,
  debouncedGetContestAdmin,
  debouncedUpdateContest,
  dict, removeContest
} from "@/api/contest";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {ElDialog, ElMessageBox} from "element-plus";
import __ from "lodash";
import type {PagedType} from "@/api/pagedType";
import ListView from "@/components/ListView/ListView.vue";
import {authTagType, authText} from "@/utils/contest";
import MarkDownEditor from "@/components/MarkDownEditor/MarkDownEditor.vue";
import {useRouter} from "vue-router";
import type {IdType} from "@/api/common.ts";

const router = useRouter();

const openSelectList = ref(false);

// 查询需要的表单数据
const queryParams = reactive<PagedType>({
  currentPage: 1,
  pageSize: 20,
});

const form = reactive<ContestForm>({
  contestId: undefined,
  title: '',
  auth: ContestAuth.PUBLIC,
  type: ContestType.CONTEST,
  startTime: undefined,
  endTime: undefined,
  pwd: undefined,
  listId: undefined,
  description: undefined
});


const rules = ref();

const open = ref(false);

const {columns} = useColumn(['比赛ID', '比赛题目', '权限', '开始时间', '结束时间', '密码', '类型', '列表ID', '描述', '参加人数']);

// 重置表单
const resetForm = () => {
  form.contestId = undefined;
  form.title = '';
  form.type = ContestType.CONTEST;
  form.auth = ContestAuth.PUBLIC;
  form.startTime = undefined;
  form.endTime = undefined;
  form.pwd = '';
  form.listId = undefined;
  form.description = '';
}

const showSearch = ref(true);

const {loading, isLoading, get: getContest} = debouncedGetContestAdmin(queryParams, (data) => {
  tableList.length = 0;
  total.value = data.totalRecords
  tableList.push(...data.data)
});

const tableList = reactive<ContestView[]>([]);
const total = ref<number>(0);

// 获取列表
const getList = () => {
  loading();
  getContest();

}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])

const handleSelectionChange = (selection: ContestView[]) => {
  ids.value = selection.map(item => item.contestId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

const handleCommand = (command: string, row: ContestView) => {

  if (command === "handleProblemStatistic") {
    //题目统计
    router.push({name: "problem-statistic", params: {contestId: row.contestId}});

  } else if (command === "handleUserStatistic") {
    // 用户统计
    router.push({name: "user-statistic", params: {contestId: row.contestId}});

  } else if (command === "handleSupplement") {
    router.push({name: "supplement", params: {contestId: row.contestId}});
  }
}

const handleDelete = (row: ContestView | Event) => {
  if (row instanceof Event) {
    ElMessageBox.confirm(`您是否要删除ID为${ids.value}的数据项？`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeContest(ids.value).then(getList);
        })
  } else {
    ElMessageBox.confirm('是否确认删除名称为"' + row.title + '"的数据项？', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeContest(row.contestId).then(getList);
        })
  }


}

const finishDialog = () => {
  open.value = false;
  resetForm();
  getList();
}

const {loading: updateLoading, isLoading: isUpdateLoading, update} = debouncedUpdateContest(form, finishDialog);

const {loading: addLoading, isLoading: isAddLoading, add} = debouncedAddContest(form, finishDialog);

const {} = debouncedAddContest(form, finishDialog)
// 1: Insert 2: Update
const dialogState = ref(1);
const title = computed(() => {
  return dialogState.value === 1 ? "添加" : "修改";
})
const handleAdd = () => {
  dialogState.value = 1;
  open.value = true;
}
const handleUpdate = (data: ContestView) => {
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