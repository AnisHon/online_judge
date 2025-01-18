<template>
  <div class="app-container">
    <el-form :model="queryParams" class="inline-form" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="菜单名称" prop="menuName">
        <el-input
            v-model="queryParams.menuName"
            placeholder="请输入菜单名称"

            @keyup.enter.native="handleQuery"
            clearable
        />
      </el-form-item>
      <el-form-item label="权限标识" prop="perms">
        <el-input
            v-model="queryParams.perms"
            placeholder="请输入权限标识"
            clearable
            @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="菜单类型" prop="menuType">
        <el-select
            v-model="queryParams.menuType"
            placeholder="菜单类型"
            clearable
            style="width: 120px"
        >
          <el-option
              v-for="item in dict.menuType"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="父菜单ID" prop="parentId">
        <el-input-number v-model="queryParams.parentId" :controls="false"/>
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
            v-has="'user:menu:add'"
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
            v-has="'user:menu:edit'"
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
            v-has="'user:menu:remove'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

<!--    ['菜单ID', '菜单名称', '菜单类型', '父菜单ID', '菜单图标', '权限标识', '路由路径', '顺序', '创建时间', '标注']-->
    <el-table v-loading="isLoading" :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="菜单ID" align="center" prop="menuId" v-if="columns[0].visible" show-overflow-tooltip />
      <el-table-column label="菜单名称" align="center" prop="menuName" v-if="columns[1].visible" />
      <el-table-column label="菜单类型" align="center" prop="menuType" v-if="columns[2].visible" />
      <el-table-column label="父菜单ID" align="center" prop="parentId" v-if="columns[3].visible" />
      <el-table-column label="菜单图标" align="center" prop="icon" v-if="columns[4].visible">
        <template v-slot="scope">
          <div v-if="scope.row.icon !== '#'">
            <icon-loader :icon="scope.row.icon" />
          </div>
          <div v-else>
            {{ scope.row.icon }}
          </div>

        </template>
      </el-table-column>
      <el-table-column label="权限标识" align="center" prop="perms" v-if="columns[5].visible" />
      <el-table-column label="路由路径" align="center" prop="router" v-if="columns[6].visible" />
      <el-table-column label="顺序" width="60" align="center" prop="orderNum" v-if="columns[7].visible" />
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[8].visible" />
      <el-table-column label="标注" align="center" prop="remark" v-if="columns[9].visible" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
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
            <el-form-item label="上级菜单ID" prop="parentId">
              <el-input-number v-model="form.parentId" :controls="false" placeholder="请输入上级菜单ID"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="菜单类型" prop="menuType">
              <el-radio-group v-model="form.menuType">
                <el-radio :label="MenuType.MENU" :value="MenuType.MENU">菜单</el-radio>
                <el-radio :label="MenuType.MENU_ITEM" :value="MenuType.MENU_ITEM">菜单项</el-radio>
                <el-radio :label="MenuType.BUTTON" :value="MenuType.BUTTON">按钮</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24" v-if="form.icon != MenuType.BUTTON">
            <el-form-item label="菜单图标" prop="icon">
              <el-input v-model="form.icon" placeholder="请输入icon"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="菜单名称" prop="menuName">
              <el-input v-model="form.menuName" placeholder="请输入菜单名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="orderNum">
              <el-input-number v-model="form.orderNum" controls-position="right" />
            </el-form-item>
          </el-col>

          <el-col :span="12" v-if="form.menuType != MenuType.BUTTON">
            <el-form-item prop="router" label="路由地址">
              <el-input v-model="form.router" placeholder="请输入路由地址" />
            </el-form-item>
          </el-col>
          <el-col :span="12" v-if="form.menuType != MenuType.MENU">
            <el-form-item prop="perms" label="权限标识">
              <el-input v-model="form.perms" placeholder="请输入权限标识" maxlength="32" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item prop="perms" label="标记">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入标记" maxlength="450" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm" :loading="isUpdateLoading  || isAddLoading">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import {debouncedAddMenu, debouncedGetMenu, debouncedUpdateMenu, dict, type QueryMenu, removeMenu} from "@/api/menu";
import {type MenuForm, MenuType, type MenuView} from "@/api/auth/menu";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import IconLoader from "@/components/IconLoader/IconLoader.vue"
import {ElDialog, ElMessageBox} from "element-plus";
import __ from "lodash";
import type {IdType} from "@/api/common.ts";


// 查询需要的表单数据
const queryParams = reactive<QueryMenu>({
  asc: true,
  currentPage: 1,
  pageSize: 20,
  menuId: undefined,
  menuName: undefined,
  menuType: undefined,
  parentId: undefined,
  icon: undefined,
  perms: undefined,
  router: undefined,
  remark: undefined
});

const form = reactive<MenuForm>({
  menuId: undefined,
  menuName: '',
  menuType: MenuType.MENU,
  parentId: undefined,
  icon: '#',
  perms: '',
  router: '',
  orderNum: 0,
  remark: ''
});


const rules = ref();

const open = ref(false);

const {columns} = useColumn(['菜单ID', '菜单名称', '菜单类型', '父菜单ID', '菜单图标', '权限标识', '路由路径', '顺序', '创建时间', '标注']);


// 重制列表
const resetQuery = () => {
  queryParams.menuType = undefined;
  queryParams.menuName = undefined;
  queryParams.parentId = undefined;
  queryParams.icon = undefined;
  queryParams.perms = undefined;
  queryParams.router = undefined;
  queryParams.remark = undefined;
  queryParams.sortColumn = undefined;

  getList();
};

// 重置表单
const resetForm = () => {
  form.menuId = undefined;
  form.menuName = '';
  form.menuType = MenuType.MENU;
  form.parentId = undefined;
  form.icon = '#';
  form.perms = '';
  form.router = '';
  form.orderNum = 0;
  form.remark = '';
}

const showSearch = ref(true);

const {loading, isLoading, get: getMenu} = debouncedGetMenu(queryParams, (data) => {
  tableList.length = 0;
  total.value = data.totalRecords
  tableList.push(...data.data)
});

const tableList = reactive<MenuView[]>([]);
const total = ref<number>(0);

// 获取列表
const getList = () => {
  loading();
  getMenu();

}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])

const handleSelectionChange = (selection: MenuView[]) => {
  ids.value = selection.map(item => item.menuId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}



const handleDelete = (row: MenuView | Event) => {
  if (row instanceof Event) {
    ElMessageBox.confirm(`您是否要删除ID为${ids.value}的数据项？`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeMenu(ids.value).then(getList);
        })
  } else {
    ElMessageBox.confirm('是否确认删除名称为"' + row.menuName + '"的数据项？', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeMenu(row.menuId).then(getList);
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

const {loading: updateLoading, isLoading: isUpdateLoading, update} = debouncedUpdateMenu(form, finishDialog);

const {loading: addLoading, isLoading: isAddLoading, add} = debouncedAddMenu(form, finishDialog);

const {} = debouncedAddMenu(form, finishDialog)
// 1: Insert 2: Update
const dialogState = ref(1);
const title = computed(() => {
  return dialogState.value === 1 ? "添加" : "修改";
})
const handleAdd = () => {
  dialogState.value = 1;
  open.value = true;
}
const handleUpdate = (data: MenuView | void) => {
  open.value = true;
  dialogState.value = 2;
  if (!data) {
    const id = ids.value[0];
    data = __.find(tableList, x => x.menuId === id)
  }
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