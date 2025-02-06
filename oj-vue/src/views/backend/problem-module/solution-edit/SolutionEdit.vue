<template>
  <div class="app-container">
    <el-form :model="queryParams" class="inline-form" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="发送者ID" prop="className">
        <el-input
            v-model="queryParams.userId"
            placeholder="请输入发送者ID"

            @keyup.enter.native="handleQuery"
            :controls="false"
        />
      </el-form-item>
      <el-form-item label="题目ID" prop="parentId">
        <el-input
            v-model="queryParams.problemId"
            placeholder="请输入题目ID"
            :controls="false"
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
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'problem:solution:add'"
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
            v-has="'problem:solution:edit'"
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
            v-has="'problem:solution:remove'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <el-table :data="tableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="题解ID" align="center" prop="solutionId" v-if="columns[0].visible" show-overflow-tooltip/>
      <el-table-column label="标题" align="center" prop="title" v-if="columns[1].visible" show-overflow-tooltip/>
      <el-table-column label="置顶" align="center" prop="topUp" v-if="columns[2].visible" >
        <template v-slot="scope">
          <el-switch
              :active-value="true"
              :inactive-value="false"
              v-model="scope.row.topUp"
              @change="handlePrivateChange(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="发布者昵称" align="center" prop="nikeName" v-if="columns[3].visible" />
      <el-table-column label="是否公开" align="center" prop="private_" v-if="columns[4].visible" >
        <template v-slot="scope">
          <el-tag v-if="scope.row.private_" type="success">公开</el-tag>
          <el-tag v-else type="danger">私有</el-tag>
        </template>

      </el-table-column>
      <el-table-column label="题目" align="center" prop="problemTitle" v-if="columns[5].visible" show-overflow-tooltip/>
      <el-table-column label="内容" align="center" prop="content" v-if="columns[6].visible" show-overflow-tooltip/>
      <el-table-column label="创建日期" align="center" prop="createTime" v-if="columns[7].visible" show-overflow-tooltip />
      <el-table-column label="更新日期" align="center" prop="updateTime" v-if="columns[8].visible" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="edit"
              @click="handleUpdate(scope.row)"
              v-has="'problem:solution:edit'"
          >修改</el-link>
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'problem:solution:remove'"
          >删除</el-link>
          <el-dropdown size="small" @command="(command: string) => handleCommand(command, scope.row)"
                       v-has-any="['problem:solution:list'] ">
            <el-link size="small" type="primary" icon="arrow-right">更多</el-link>
            <template #dropdown>
              <el-dropdown-menu>
                <div v-has="'problem:solution:list'" >
                  <el-dropdown-item command="openSolution" icon="Monitor">
                    查看题解
                  </el-dropdown-item>
                </div>
              </el-dropdown-menu>
            </template>

          </el-dropdown>
        </template>
      </el-table-column>

      <template>
        <el-empty description="没有任何数据项"/>
      </template>
    </el-table>

    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="queryParams.currentPage"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
    />
  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from "vue";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";

import {ElMessageBox, ElNotification} from "element-plus";

import {useRouter} from "vue-router";
import {
  deleteSolutionAdmin,
  listSolutionAdmin, lowDown,
  type QuerySolution,
  type Solution, topUp
} from "@/api/solution";
import type {IdType} from "@/api/common.ts";

const router = useRouter();

// 查询需要的表单数据
const queryParams = reactive<QuerySolution>({
  asc: true,
  currentPage: 1,
  pageSize: 20,
  problemId: undefined,
  userId: undefined,
});

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<IdType[]>([])

const columns = ref([
  {
    key: 0,
    label: "题解ID",
    visible: true,
  },
  {
    key: 1,
    label: "标题",
    visible: true,
  },
  {
    key: 2,
    label: "置顶",
    visible: true,
  },
  {
    key: 3,
    label: "发布者昵称",
    visible: true,
  },
  {
    key: 4,
    label: "是否公开",
    visible: true,
  },
  {
    key: 5,
    label: "题目",
    visible: true,
  },
  {
    key: 6,
    label: "内容",
    visible: true,
  },
  {
    key: 7,
    label: "创建日期",
    visible: true,
  },
  {
    key: 8,
    label: "更新日期",
    visible: true,
  }
]);


// 重制列表
const resetQuery = () => {
  queryParams.problemId = undefined;
  queryParams.userId = undefined;
  queryParams.sortColumn = undefined;

  getList();
};

const showSearch = ref(true);


const tableList = reactive<Solution[]>([]);
const total = ref<number>(0);

// 获取列表
const getList = async () => {
  const data = await listSolutionAdmin(queryParams)
  tableList.length = 0;
  total.value = data.totalRecords
  tableList.push(...data.data)

}

// 用户状态修改
const handlePrivateChange = (row: Solution) => {
  const text = row.topUp ? "置顶" : "取消置顶";
  ElMessageBox.confirm('确认要' + text + "此题解吗?").then(function() {
    if (row.topUp) {
      return topUp(row.solutionId);
    } else {
      return lowDown(row.solutionId);
    }
  }).then((data) => {
    if (data) {
      ElNotification.success(text + "成功");
    } else {
      ElNotification.warning(text + "失败");
    }
  }).catch(function() {
    row.topUp = !row.topUp;
  });
};

const handleSelectionChange = (selection: Solution[]) => {
  ids.value = selection.map(item => item.solutionId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}



const handleDelete = (row?: Solution) => {
  const id = row ? row.solutionId : ids.value[0];
  ElMessageBox.confirm(`您是否要删除ID为${id}的数据项？` , {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  })
      .then(() => {
        deleteSolutionAdmin(id).then(getList);
      })

}

// 搜索按钮
const handleQuery = () => {
  getList();
  single.value = false
  multiple.value = false;
}


const handleAdd = () => {
  router.push({name: 'solution_edit'})
}

const handleUpdate = (data: Solution | void) => {
  const id = data ? data.solutionId : ids.value[0];
  router.push({name: 'solution_edit', query: {solutionId: id}});
}


const handleCommand = (command: string, row: Solution) => {
  if (command === "openSolution") {
    router.push({name: 'solution', params: {id: row.solutionId}});
  }
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