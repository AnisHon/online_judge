<template>
  <div class="role-container">
    <el-form :model="queryParams" class="inline-form" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户名称" prop="username">
        <el-input
            v-model="queryParams.username"
            placeholder="请输入用户名称"

            @keyup.enter.native="handleQuery"
            clearable
        />
      </el-form-item>
      <el-form-item label="用户昵称" prop="nikeName">
        <el-input
            v-model="queryParams.nikeName"
            placeholder="请输入用户昵称"
            clearable
            @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="用户邮箱" prop="email">
        <el-input
            v-model="queryParams.email"
            placeholder="请输入用户昵称"
            clearable
            @keyup.enter.native="handleQuery"
        />
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="search"  @click="handleQuery">搜索</el-button>
        <el-button icon="refresh"  @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="ArrowLeftBold"
            size="small"
            @click="router.push({name: 'role-manage'})"
        >返回角色管理</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'user:role:grant'"
        >添加授权</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="delete"
            size="small"
            :disabled="multiple"
            @click="handleDelete"
            v-has="'user:role:revoke'"
        >撤销授权</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>


    <!--    '用户id', '用户名称', '邮箱地址', '用户昵称', '用户状态', '创建时间', '标记'-->
    <el-table v-loading="isLoading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="用户id" align="center" prop="userId" v-if="columns[0].visible" />
      <el-table-column label="用户名称" align="center" prop="userName" v-if="columns[1].visible" />
      <el-table-column label="用户昵称" align="center" prop="nikeName" v-if="columns[3].visible" />
      <el-table-column label="邮箱地址" align="center" prop="email" v-if="columns[2].visible" />
      <el-table-column label="用户状态" align="center" prop="icon" v-if="columns[4].visible">
        <template v-slot="scope">
          <el-tag type="danger" v-if="scope.row.status === 1">封禁</el-tag>
          <el-tag type="success" v-else>正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[5].visible" />
      <el-table-column label="标记" align="center" prop="remark" v-if="columns[6].visible" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <div v-has="'user:role:revoke'">
            <el-link
                size="small"
                type="primary"
                icon="delete"
                @click="handleDelete(scope.row)"
            >撤销授权</el-link>
          </div>

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



    <el-dialog title="授权用户角色" v-model="open">
      <user-viwer v-model:ids="grantSelectedIds" :loading="addLoading"/>
      <template #footer>
        <el-button type="primary" @click="submit" :loading="addLoading">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from "vue";
import {
  debouncedGetRoleUser,
  type QueryRoleUser,
  removeUser
} from "@/api/user";
import {type UserView} from "@/api/user";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {ElMessageBox} from "element-plus";
import {useRoute, useRouter} from "vue-router";
import UserViwer from "@/views/user-module/role-manage/user-viewer/UserViwer.vue";
import {debouncedGrant, revoke, type UserRoleRelation} from "@/api/role";

const grantSelectedIds = reactive<number[]>([])

const route = useRoute();
const router = useRouter()

// 查询需要的表单数据
const queryParams = reactive<QueryRoleUser>({
  currentPage: 1,
  pageSize: 20,
  username: undefined,
  nikeName: undefined,
  email: undefined,
  roleId: undefined,
  userId: undefined,
});

const form = reactive<UserRoleRelation>({
  userId: 0,
  roleId: 0
});

const addForm = reactive<UserRoleRelation[]>([]);


const open = ref(false);

const {columns} = useColumn(['用户id', '用户名称', '邮箱地址', '用户昵称', '用户状态', '创建时间', '标记']);


// 重制列表
const resetQuery = () => {
  queryParams.userId = undefined;
  queryParams.username = undefined;
  queryParams.nikeName = undefined;
  queryParams.email = undefined;

  getList();
};



const showSearch = ref(true);

const {loading, isLoading, get: getUser} = debouncedGetRoleUser(queryParams, (data) => {
  tableList.length = 0;
  total.value = data.totalRecords
  tableList.push(...data.data)
});

const tableList = reactive<UserView[]>([]);
const total = ref<number>(0);

// 获取列表
const getList = () => {
  loading();
  getUser();

}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<number[]>([])

const handleSelectionChange = (selection: UserView[]) => {
  ids.value = selection.map(item => item.userId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}



const handleDelete = (row: UserView | Event) => {

  if (row instanceof Event) {
    ElMessageBox.confirm(`您是否要撤销ID为${ids.value}的权限？`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })

        .then(() => {
          addForm.length = 0;
          ids.value.forEach(v => {addForm.push({roleId: form.roleId, userId: v})})
          revoke(addForm).then(getList);
        })
  } else {
    ElMessageBox.confirm('是否确认撤销用户名称为"' + row.userName + '"的权限？', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          revoke({userId: row.userId, roleId: form.roleId}).then(getList);
        })
  }


}

// 搜索按钮
const handleQuery = () => {
  getList();
  single.value = false
  multiple.value = false;
}


const handleAdd = () => {
  open.value = true;

}

const submit = () => {
  startAddLoading();
  addForm.length = 0;
  grantSelectedIds.forEach((v) => {
    addForm.push({userId: v, roleId: form.roleId});
  })
  add();

}


const {loading: startAddLoading, isLoading: addLoading, add} = debouncedGrant(addForm, () => {
  open.value = false;
  getList();
})

const cancel = (id: number) => {
  open.value = false;
}






// created -> 获取列表
getList()

const {id} = route.params;
queryParams.roleId = parseInt(<string>id);
form.roleId = queryParams.roleId;
</script>

<style scoped>

</style>

<style>
.role-container {
  .inline-form {
    .el-input {
      --el-input-width: 220px;
    }

    .el-select {
      --el-select-width: 220px;
    }
  }

}
</style>