<template>
  <div class="app-container">
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="ArrowLeftBold"
            size="small"
            @click="router.back"
        >返回</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="userDialog = true"
            v-has="'problem:contest:edit'"
        >直接添加</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="plus"
            size="small"
            @click="classDialog = true"
            v-has="'problem:contest:edit'"
        >班级添加</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="delete"
            size="small"
            :disabled="multiple"
            @click="handleDelete()"
            v-has="'problem:contest:edit'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <!--    '用户id', '用户名称', '邮箱地址', '用户昵称', '用户状态', '创建时间', '标记'-->
    <el-table :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="用户id" align="center" prop="userId" v-if="columns[0].visible" show-overflow-tooltip />
      <el-table-column label="用户名称" align="center" prop="userName" v-if="columns[1].visible" />
      <el-table-column label="用户昵称" align="center" prop="nikeName" v-if="columns[3].visible" />
      <el-table-column label="邮箱地址" align="center" prop="email" v-if="columns[2].visible" />
<!--      <el-table-column label="用户状态" align="center" prop="icon" v-if="columns[4].visible">-->
<!--        <template v-slot="scope">-->
<!--          <el-tag type="danger" v-if="scope.row.status === 1">封禁</el-tag>-->
<!--          <el-tag type="success" v-else>正常</el-tag>-->
<!--        </template>-->
<!--      </el-table-column>-->
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[4].visible" />
      <el-table-column label="标记" align="center" prop="remark" v-if="columns[5].visible" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <div v-has="'user:role:revoke'">
            <el-link
                size="small"
                type="primary"
                icon="delete"
                @click="handleDelete(scope.row)"
            >删除</el-link>
          </div>

        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="直接添加用户" v-model="userDialog">
      <user-viewer v-model:ids="userIds" :loading="false"/>
      <template #footer>
        <el-button type="primary" @click="submit" >确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>

    <el-dialog title="从班级添加用户" v-model="classDialog">
      <class-view @select-class="handleSelectClass"> </class-view>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from "vue";
import {type UserView} from "@/api/user";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import {ElMessageBox, ElNotification} from "element-plus";
import {useRoute, useRouter} from "vue-router";
import UserViewer from "@/components/user-viewer/UserViewer.vue";
import type {IdType} from "@/api/common.ts";
import ClassView from "@/views/backend/teacher/contest-manage/component/ClassView.vue";
import {addUserByClass, addUserDirect, getUserByContest, removeUser} from "@/api/contest/user.ts";

const route = useRoute();
const router = useRouter()

const contestId: IdType = route.params.contestId as IdType;

const userDialog = ref(false);

const classDialog = ref(false);

const {columns} = useColumn(['用户id', '用户名称', '邮箱地址', '用户昵称',
  // '用户状态',
  '创建时间', '标记']);

const showSearch = ref(true);

const tableList = reactive<UserView[]>([]);

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])

// 授权选择的用户ID
const userIds = reactive<IdType[]>([])

// 获取列表
const getList = async () => {
  tableList.length = 0;
  tableList.push(...await getUserByContest(contestId));
}

const handleSelectionChange = (selection: UserView[]) => {
  ids.value = selection.map(item => item.userId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

const handleDelete = (row?: UserView) => {
  const id = row ? row.userId : ids.value;
  ElMessageBox.confirm(`你确定要取消${id}的考试？`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })

      .then(async () => {
        await removeUser(contestId, id);
        await getList();
      }).catch(()=>{})
}

const submit = async () => {
  if (userIds.length === 0) {
    ElNotification.warning("请选择用户");
    return;
  }
  await addUserDirect(contestId, userIds);
  userIds.length = 0;
  userDialog.value = false;
  await getList();
}

const handleSelectClass = async (classId: IdType) => {
  await addUserByClass(contestId, classId);
  classDialog.value = false;
  await getList();
}

const cancel = () => {
  userDialog.value = false;
  userIds.length = 0;
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