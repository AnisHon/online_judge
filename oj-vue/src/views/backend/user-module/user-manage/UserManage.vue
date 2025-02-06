<template>
  <div class="app-container">
    <el-form :model="queryParams" class="inline-form" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户名称" prop="username">
        <el-input
            v-model="queryParams.userName"
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
      <el-form-item label="用户状态" prop="status">
        <el-select
            v-model="queryParams.status"
            placeholder="用户状态"
            clearable
            style="width: 120px"
        >
          <el-option
              v-for="item in dict.userStatus"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="用户ID" prop="userId">
        <el-input-number v-model="queryParams.userId" :controls="false"/>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" icon="search"  @click="handleQuery">搜索</el-button>
        <el-button icon="refresh"  @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'user:user:add'"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="edit"
            size="small"
            :disabled="single"
            @click="handleUpdate()"
            v-has="'user:user:edit'"
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
            v-has="'user:user:remove'"
        >封禁</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>


<!--    '用户id', '用户名称', '邮箱地址', '用户昵称', '用户状态', '创建时间', '标记'-->
    <el-table v-loading="isLoading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="用户id" align="center" prop="userId" v-if="columns[0].visible" show-overflow-tooltip />
      <el-table-column label="用户名称" align="center" prop="userName" v-if="columns[1].visible" show-overflow-tooltip />
      <el-table-column label="用户昵称" align="center" prop="nikeName" v-if="columns[3].visible" show-overflow-tooltip />
      <el-table-column label="邮箱地址" align="center" prop="email" v-if="columns[2].visible" show-overflow-tooltip />
      <el-table-column label="用户状态" align="center" prop="icon" v-if="columns[4].visible">
        <template v-slot="scope">
          <el-tag type="danger" v-if="scope.row.status === 1">封禁</el-tag>
          <el-tag type="success" v-else>正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[5].visible" />
      <el-table-column label="标记" align="center" prop="remark" v-if="columns[6].visible" />
      <el-table-column label="奖励分" align="center" prop="points" v-if="columns[7].visible" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="edit"
              @click="handleUpdate(scope.row)"
              v-has="'user:user:edit'"
          >修改</el-link>
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'user:auth:ban'"
          >封禁</el-link>
          <el-dropdown size="small" @command="(command: string) => handleCommand(command, scope.row)"
                       v-has-any="['user:user:edit'] ">
            <el-link size="small" type="primary" icon="arrow-right">更多</el-link>
            <template #dropdown>
              <el-dropdown-menu>
                <div v-has="['user:user:edit']" >
                  <el-dropdown-item command="handleResetPass" icon="Lock"
                  >重置密码</el-dropdown-item>
                </div>
                <div v-has="['user:auth:unban']" >
                  <el-dropdown-item command="handleUnban" icon="Unlock"
                  >解封用户</el-dropdown-item>
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
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form :model="addForm" :rules="rules" label-width="100px" ref="ruleFormRef">
        <el-row>
          <el-col :span="24">
            <el-form-item label="用户ID" prop="parentId" v-if="dialogState === 2">
              <el-input-number v-model="updateForm.userId" :controls="false" disabled placeholder="用户ID"/>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="用户名" prop="userName">
              <el-input v-model="addForm.userName" :disabled="dialogState === 2" placeholder="请输入用户名" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="昵称" prop="nikeName">
              <el-input v-model="addForm.nikeName" placeholder="请输入昵称"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色ID" prop="role" v-if="dialogState === 1">
              <el-select v-model="addForm.role" placeholder="请选择角色" :loading="roles.length === 0">
                <el-option
                    v-for="item of roles"
                    :key="item.roleId"
                    :value="item.roleId"
                    :label="item.roleName"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="addForm.email" placeholder="请输入邮箱"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="密码" prop="email" v-if="dialogState === 1">
              <el-input v-model="addForm.password" placeholder="请输入邮箱"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="用户状态" prop="status">
              <el-radio-group v-model="addForm.status">
                <el-radio v-for="item of dict.userStatus" :label="item.value" :value="item.value">{{ item.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item prop="perms" label="标记">
              <el-input v-model="addForm.remark" type="textarea" placeholder="请输入标记" maxlength="450" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button type="primary" @click="submitForm(ruleFormRef)" :loading="isUpdateLoading  || isAddLoading">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import {
  banUser,
  debouncedAddUser,
  debouncedGetUser,
  debouncedUpdateUser,
  dict,
  type QueryUser,
  resetToDefault, unbanUser,
  type UserAddForm,
  UserStatus, type UserUpdateForm
} from "@/api/user";
import {type UserView} from "@/api/user";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {ElDialog, ElMessageBox, type FormInstance} from "element-plus";
import __ from "lodash";
import {getRole, type RoleView} from "@/api/role";
import type {IdType} from "@/api/common.ts";


const ruleFormRef = ref();

// 查询需要的表单数据
const queryParams = reactive<QueryUser>({
  asc: true,
  currentPage: 1,
  pageSize: 20,
  userId: undefined,
  userName: undefined,
  nikeName: undefined,
  email: undefined,
  status: undefined,
});

const addForm = reactive<UserAddForm>({
  userName: '',
  nikeName: '',
  email: '',
  password: '',
  status: UserStatus.NORMAL,
  role: '1',
  remark: '',
});
const updateForm = reactive<UserUpdateForm>({
  userId: '1',
  nikeName: undefined,
  email: undefined,
  status: UserStatus.NORMAL,
  remark: undefined,
});


const roles = reactive<RoleView[]>([])

const rules = {
  userName: [
    {required: true, message: "用户名不能为空", trigger: "blur"},
    {pattern: /^[a-zA-Z0-9_-]{4,16}$/, message: "用户名只能由 字母 数字 _ -构成", trigger: 'blur'}
  ],
  email: [
    {required: false, message: "请输入邮箱", trigger: 'blur'},
    {type: 'email', message: '请输入正确邮箱地址', trigger: 'blur'}
  ],
};

const open = ref(false);

const {columns} = useColumn(['用户id', '用户名称', '邮箱地址', '用户昵称', '用户状态', '创建时间', '标记', '奖励分']);


// 重制列表
const resetQuery = () => {
  queryParams.userId = undefined;
  queryParams.userName = undefined;
  queryParams.nikeName = undefined;
  queryParams.email = undefined;
  queryParams.status = undefined;
  queryParams.sortColumn = undefined;

  getList();
};

// 重置表单
const resetForm = () => {
  addForm.userName = '';
  addForm.email = '';
  addForm.role = 1;
  addForm.nikeName = '';
  addForm.password = '';
  addForm.status = UserStatus.NORMAL;
  addForm.remark = '';

  updateForm.remark = undefined;
  updateForm.status = undefined;
  updateForm.email = undefined;
  updateForm.userId = undefined;
  updateForm.nikeName = undefined;
}

const showSearch = ref(true);

const {loading, isLoading, get: getUser} = debouncedGetUser(queryParams, (data) => {
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
const ids = ref<IdType[]>([])

const handleSelectionChange = (selection: UserView[]) => {
  ids.value = selection.map(item => item.userId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}



const handleDelete = (row: UserView | Event) => {
  if (row instanceof Event) {
    ElMessageBox.confirm(`您是否要封禁ID为${ids.value}的数据项？`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          banUser(ids.value).then(getList);
        })
  } else {
    ElMessageBox.confirm('是否确认封禁名称为"' + row.userName + '"的数据项？', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          banUser(row.userId).then(getList);
        })
  }


}

// 搜索按钮
const handleQuery = () => {
  getList();
  single.value = false
  multiple.value = false;
}

const finishDialog = () => {
  open.value = false;
  resetForm();
  getList();
}

const {loading: updateLoading, isLoading: isUpdateLoading, update} = debouncedUpdateUser(updateForm, finishDialog);

const {loading: addLoading, isLoading: isAddLoading, add} = debouncedAddUser(addForm, finishDialog);

const {} = debouncedAddUser(addForm, finishDialog)
// 1: Insert 2: Update
const dialogState = ref(1);
const title = computed(() => {
  return dialogState.value === 1 ? "添加" : "修改";
})
const handleAdd = () => {
  resetForm();
  dialogState.value = 1;
  open.value = true;
}
const handleUpdate = (data: UserView | void) => {
  resetForm();
  open.value = true;
  dialogState.value = 2;
  if (!data) {
    const id = ids.value[0];
    data = __.find(tableList, x => x.userId === id)
  }

  __.assign(addForm, data)
  __.assign(updateForm, data)
}

const submitForm = (formEl: FormInstance | undefined) => {
  if (!formEl) {
    return
  }
  formEl.validate((valid) => {
    if (valid) {
      if (dialogState.value === 1) {
        addLoading();
        // 添加
        add();
      } else {
        updateLoading();
        __.assign(updateForm, addForm)
        // 修改
        update();
      }
    }
  })

}

const cancel = () => {
  open.value = false;
  resetForm()
}




const id = ref('0');

const handleUnbanUser = () => {
  ElMessageBox.confirm(`您是否要解封ID为${ids.value}的用户？`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
      .then(() => {
        unbanUser(id.value);
        getList();
      })


}

const handleResetPass = () => {
  ElMessageBox.confirm(`您是否要重置ID为${ids.value}用户的密码？`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
      .then(() => {
        resetToDefault(id.value);
        getList();
      })


}


const handleCommand = (e: string, row: UserView) => {
  id.value = row.userId;
  if (e === 'handleResetPass') {
    handleResetPass();
  } else if (e === 'handleUnban') {
    handleUnbanUser();
  }
}




// created -> 获取列表
getList()

getRole({currentPage: 1, pageSize: 200, asc: true}).then((data) => {
  roles.push(...data.data)
})


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