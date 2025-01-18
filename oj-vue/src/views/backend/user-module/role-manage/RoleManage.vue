<template>
  <div class="app-container">
    <el-form :model="queryParams" class="inline-form" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="角色名称" prop="roleName">
        <el-input
            v-model="queryParams.roleName"
            placeholder="请输入菜单名称"

            @keyup.enter.native="handleQuery"
            clearable
        />
      </el-form-item>
      <el-form-item label="状态" prop="menuType">
        <el-select
            v-model="queryParams.status"
            placeholder="状态"
            clearable
            style="width: 120px"
        >
          <el-option
              v-for="item in dict.roleStatus"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="角色ID" prop="parentId">
        <el-input-number v-model="queryParams.roleId" :controls="false" placeholder="角色ID"/>
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
            v-has="'user:role:add'"
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
            v-has="'user:role:edit'"
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
            v-has="'user:role:remove'"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="refresh"
            size="small"
            @click="handleRefresh"
            v-has="'user:role:list'"
        >刷新角色缓存</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

<!--    ['角色ID', '角色名称', '状态', '标记']-->
    <el-table v-loading="isLoading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="角色ID" align="center" prop="roleId" v-if="columns[0].visible" show-overflow-tooltip />
      <el-table-column label="角色名称" align="center" prop="roleName" v-if="columns[1].visible" />
      <el-table-column label="状态" align="center" prop="status" v-if="columns[2].visible" >
        <template v-slot="scope">
          {{ roleStatusText(scope.row.status) }}
        </template>
      </el-table-column>
      <el-table-column label="标记" align="center" prop="remark" v-if="columns[3].visible" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-space>
            <el-link
                size="small"
                type="primary"
                icon="edit"
                @click="handleUpdate(scope.row)"
                v-has="'user:menu:edit'"
            >修改</el-link>
            <el-link
                size="small"
                type="primary"
                icon="delete"
                @click="handleDelete(scope.row)"
                v-has="'user:menu:remove'"
            >删除</el-link>
            <el-dropdown size="small" @command="(command: string) => handleCommand(command, scope.row)"
                         v-has-any="['user:role:grant', 'user:role:revoke', 'user:user:list'] ">
              <el-link size="small" type="primary" icon="arrow-right">更多</el-link>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="handleDataScope" icon="circle-check"
                  >数据权限</el-dropdown-item>
                  <div v-has="['user:role:grant', 'user:role:revoke', 'user:user:list']" >
                    <el-dropdown-item command="handleAuthUser" icon="user"
                    >分配用户</el-dropdown-item>
                  </div>

                </el-dropdown-menu>
              </template>

            </el-dropdown>
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

    <!-- 添加或修改测试功能对话框 -->
    <el-dialog :title="title" v-model="open" width="680px" append-to-body>
      <el-form :model="form" :rules="rules" label-width="100px">
        <el-row>

          <el-col :span="24">
            <el-form-item label="角色名称" prop="roleName">
              <el-input v-model="form.roleName" placeholder="请输入角色名称" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="角色状态" prop="status">
              <el-radio-group v-model="form.roleId">
                <el-radio v-for="item of dict.roleStatus" :label="item.value" :value="item.value">{{ item.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item prop="perms" label="标记">
              <el-input v-model="form.remark" placeholder="标记" maxlength="100" />
            </el-form-item>
          </el-col>
        </el-row>

      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" :loading="isUpdateLoading  || isAddLoading">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!--    权限分配对话框    -->
    <el-dialog :title="title" v-model="openDataScope" width="500px" append-to-body>
      <el-form :model="form" label-width="80px" v-loading="loadingRole">
        <el-form-item label="角色名称">
          <el-input v-model="form.roleName" :disabled="true" />
        </el-form-item>
        <el-form-item label="数据权限">
          <el-tree
              class="tree"
              :data="menuTree"
              ref="treeRef"
              show-checkbox
              check-strictly
              accordion
              node-key="id"
              empty-text="加载中，请稍候"
              :props="treeConfig"
          ></el-tree>
        </el-form-item>
      </el-form>
      <template #footer class="dialog-footer">
        <el-button type="primary" @click="submitMenu" :loading="isRevokeLoading || isGrantLoading">确 定</el-button>
        <el-button @click="cancelMenu">取 消</el-button>
      </template>
    </el-dialog>

  </div>

</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {ElDialog, ElMessageBox, ElTree} from "element-plus";
import __ from "lodash";
import {type QueryRole, refreshRoleCache, removeRole, type RoleForm, RoleStatus, type RoleView} from "@/api/role";
import {debouncedAddRole, debouncedGetRole, debouncedUpdateRole, dict} from "@/api/role";
import {useRoute, useRouter} from "vue-router";
import {debouncedGrant, debouncedRevoke, listRoleMenu, type MenuRoleRelation, type TreedMenu} from "@/api/auth/menu";
import {getAllTreedMenu} from "@/api/menu";
import {setTreeId} from "@/utils/menu";
import type {TreeOptionProps} from "element-plus/es/components/tree/src/tree.type";
import type {TreeNodeData} from "element-plus/lib/components/tree/src/tree.type";
import type {IdType} from "@/api/common.ts";
// 查询需要的表单数据

const route = useRoute();
const router = useRouter();

const queryParams = reactive<QueryRole>({
  asc: true,
  currentPage: 1,
  pageSize: 20,
  roleId: undefined,
  roleName: undefined,
  status: undefined,
  remark: undefined
});

const form = reactive<RoleForm>({
  roleId: undefined,
  roleName: undefined,
  status: RoleStatus.NORMAL,
  remark: "",
});


const rules = ref();

const open = ref(false);

const {columns} = useColumn(['角色ID', '角色名称', '状态', '标记']);


const roleStatusText = (status: RoleStatus) => {
  return status === RoleStatus.NORMAL ? "正常" : "停用";
}

// 重制列表
const resetQuery = () => {
  queryParams.roleId = undefined;
  queryParams.roleName = undefined;
  queryParams.status = undefined;
  queryParams.remark = undefined;
  queryParams.sortColumn = undefined;

  getList();
};

// 重置表单
const resetForm = () => {
  form.roleId = undefined;
  form.roleName = '';
  form.status = RoleStatus.NORMAL;
  form.remark = '';
}

const showSearch = ref(true);

const {loading, isLoading, get: getRole} = debouncedGetRole(queryParams, (data) => {
  tableList.length = 0;
  total.value = data.totalRecords
  tableList.push(...data.data)
});

const tableList = reactive<RoleView[]>([]);
const total = ref<number>(0);

// 获取列表
const getList = () => {
  loading();
  getRole();

}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])

const handleSelectionChange = (selection: RoleView[]) => {
  ids.value = selection.map(item => item.roleId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}

const handleRefresh = () => {
  refreshRoleCache();
}

const handleDelete = (row: RoleView | Event) => {
  if (row instanceof Event) {
    ElMessageBox.confirm(`您是否要删除ID为${ids.value}的数据项？`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeRole(ids.value).then(getList);
        })
  } else {
    ElMessageBox.confirm('是否确认删除名称为"' + row.roleName + '"的数据项？', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeRole(row.roleId).then(getList);
        })
  }


};

// 搜索按钮
const handleQuery = () => {
  getList();
  single.value = false
  multiple.value = false;
};

const finishDialog = () => {
  open.value = false;
  resetForm();
  getList();
};

const {loading: updateLoading, isLoading: isUpdateLoading, update} = debouncedUpdateRole(form, finishDialog);

const {loading: addLoading, isLoading: isAddLoading, add} = debouncedAddRole(form, finishDialog);

const {} = debouncedAddRole(form, finishDialog)
// 1: Insert 2: Update
const dialogState = ref(1);
const title = computed(() => {
  return dialogState.value === 1 ? "添加" : "修改";
});
const handleAdd = () => {
  dialogState.value = 1;
  open.value = true;
};
const handleUpdate = (data: RoleView | void) => {
  open.value = true;
  dialogState.value = 2;
  if (!data) {
    const id = ids.value[0];
    data = __.find(tableList, x => x.roleId === id)
  }
  __.assign(form, data)
};

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
};

const cancel = () => {
  open.value = false;
  resetForm()
};



// 查看更改用户角色menu
const handleAuthUser = (row: RoleView) => {
  router.push({name: "role-auth", params: {id: row.roleId}});
};

const handleCommand = (command: string, row: RoleView) => {
  switch (command) {
    case "handleDataScope":
      handleMenu(row);
      break;
    case "handleAuthUser":
      handleAuthUser(row);
      break;
    default:
      break;
  }
};


const treeConfig = reactive<TreeOptionProps>({
  children: 'children',
  // @ts-ignore
  label: (data: TreeNodeData, node: Node): string => {return <string>data.menu.menuName}
})








const menuTree = reactive<TreedMenu[]>([])
const openDataScope = ref(false);
const treeRef = ref<InstanceType<typeof ElTree>>();

const original = ref<IdType[]>([]);
const current = ref<IdType[]>([])
const delArray = ref<MenuRoleRelation[]>([])
const addArray = ref<MenuRoleRelation[]>([])

const init = () => {
  openDataScope.value = true;
  loadingRole.value = true;

  if (__.isEmpty(menuTree)) {
    getAllTreedMenu()
        .then((data) => {
          setTreeId(data);
          menuTree.push(...data)
        });
  }
  for (let key in treeRef.value?.getCheckedKeys()) {
    treeRef.value?.setChecked(key, false, true);
  }

}

const reset = () => {
  openDataScope.value = false;
  delArray.value.length = 0;
  addArray.value.length = 0;
  resetForm();
  for (let key in treeRef.value?.getCheckedKeys()) {
    treeRef.value?.setChecked(key, false, true);
  }
}

const {isLoading: isGrantLoading, loading: grantLoading, add: grant} = debouncedGrant(addArray.value, reset)
const {isLoading: isRevokeLoading, loading: revokeLoading, add: revoke} = debouncedRevoke(delArray.value, reset)



const submitMenu = () => {
  delArray.value.length = 0;
  addArray.value.length = 0;


  current.value = <IdType[]>treeRef.value?.getCheckedKeys();

  const delIdTemp = __.difference(original.value, current.value);
  const addIdTemp = __.difference(current.value, original.value);


  delIdTemp.forEach(x => delArray.value.push({
    roleId: form.roleId,
    // @ts-ignore
    menuId: x
  }))

  addIdTemp.forEach(x => addArray.value.push({
    roleId: form.roleId,
    // @ts-ignore
    menuId: x
  }))

  if (delArray.value.length > 0) {
    revokeLoading();
    revoke()
  }
  if (addArray.value.length > 0) {
    grantLoading();
    grant();
  }
}

const cancelMenu = () => {
  reset();
}

const loadingRole = ref(false);
// 查看更改角色menu
const handleMenu = (row: RoleView) => {
  init();
  form.roleId = row.roleId;
  form.roleName = row.roleName;
  listRoleMenu(row.roleId).then((data) => {
    original.value = data.map(x => x.menuId);
    loadingRole.value = false;
    original.value.forEach(x => {
      treeRef.value?.setChecked(x, true, true);
    })

  })
};
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