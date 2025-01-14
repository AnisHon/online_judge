<template>
  <div class="app-container">

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
            type="warning"
            plain
            icon="ArrowLeft"
            size="small"
            @click="back"
            v-has="'problem:list:add-problem'"
        >返回</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="primary"
            plain
            icon="plus"
            size="small"
            @click="handleAdd"
            v-has="'problem:list:add-problem'"
        >添加题目</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="success"
            plain
            icon="plus"
            size="small"
            @click="isEdit = !isEdit"
            v-has="'problem:list:add-problem'"
        >开启/关闭修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
            type="danger"
            plain
            icon="delete"
            size="small"
            :disabled="multiple"
            @click="handleDelete"
            v-has="'problem:list:del-problem'"
        >删除题目</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

    <!--    ['问题ID', '题目', '问题描述', '问题来源', '问题类型' ,'问题权限', '创建时间', '提示']-->
    <el-table v-loading="isLoading" :data="sortedTableList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="问题ID" align="center" prop="problemId" v-if="columns[0].visible" />
      <el-table-column label="题目" align="center" prop="title" v-if="columns[1].visible" />
      <el-table-column label="问题描述" align="center" prop="description" v-if="columns[2].visible">
        <template v-slot="scope">
          <markdown-preview :text="scope.row.description"/>
        </template>
      </el-table-column>
      <el-table-column label="问题来源" align="center" prop="source" v-if="columns[3].visible" />
      <el-table-column label="问题类型" align="center" prop="type" v-if="columns[4].visible">
        <template v-slot="scope">
          <el-tag type="primary">{{ problemTypeToString(scope.row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="问题权限" align="center" prop="auth" v-if="columns[5].visible" >
        <template v-slot="scope">
          <el-tag :type="getAuthCardType(scope.row.auth)">{{ getAuthText(scope.row.auth) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns[6].visible" />
      <el-table-column label="提示" width="60" align="center" prop="hint" v-if="columns[7].visible" />
      <el-table-column label="问题顺序" align="center" prop="problemOrder" v-if="columns[8].visible">
        <template v-slot="scope">
          <el-input-number v-model="scope.row.tempOrder" :disabled="!isEdit" :controls="false" @keyup.enter="$event.target.blur()" @blur="handleUpdate(scope.row)"/>
        </template>
      </el-table-column>
      <el-table-column label="问题分数" align="center" prop="score" v-if="columns[9].visible" >
        <template v-slot="scope">
          <el-input-number v-model="scope.row.tempScore" :disabled="!isEdit" @keyup.enter="$event.target.blur()"  :controls="false" :precision="2" @blur="handleUpdate(scope.row)"/>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'problem:list:del-problem'"
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


    <el-dialog title="选择题目" v-model="open" append-to-body>
      <div>
        <ListProblemView :list-id="listId" v-model="addProblemIds"/>
      </div>
      <template #footer>
        <el-button type="primary" @click="submit" :loading="isAddLoading">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>


  </div>
</template>

<script setup lang="ts">
import {computed, reactive, ref} from "vue";
import {
  ProblemAuth,
} from "@/api/problem";
import {useStatuesColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {ElDialog, ElInputNumber, ElMessageBox} from "element-plus";
import {problemTypeToString} from "@/utils/problem";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {useRoute, useRouter} from "vue-router";
import {
  debouncedAddProblemToList,
  debouncedGetProblem, delProblemFromList,
  type ListProblemQuery, type ProblemInListView,
  type ProblemListRelation, updateProblemRelation
} from "@/api/list";
import ListProblemView from "@/views/backend/problem-module/list-edit/list-problem-view/ListProblemView.vue";

const route = useRoute();
const router = useRouter();
const listId = computed((): number => {
  return parseInt(<string>route.params.id);
})
// 查询需要的表单数据
const queryParams = reactive<ListProblemQuery>({
  currentPage: 1,
  pageSize: 20,
  listId: listId.value,
  problemId: undefined,
  title: undefined,
  type: undefined
});

// 添加 删除问题的参数
const relations = reactive<ProblemListRelation[]>([])

// 更新问题的参数
const form = reactive<ProblemListRelation>({
  listId: listId.value,
  problemId: undefined,
  score: 0,
  problemOrder: 0
})




const {columns} = useStatuesColumn(
    ['问题ID', '题目', '问题描述', '问题来源', '问题类型' ,'问题权限', '创建时间', '提示', '问题顺序', '分数'],
    [true, true, false, false, true, true, false, false, true, true]
);

// 弹窗是否打开
const open = ref(false)

const showSearch = ref(true);

const {loading, isLoading, get: getProblem} = debouncedGetProblem(listId.value, (data) => {
  if (data) {
    tableList.length = 0;
    tableList.push(...data)
    tableList.forEach(x => {
      x.tempOrder = x.problemOrder
      x.tempScore = x.score
    })
  }

});


const tableList = reactive<ProblemInListView[]>([]);
const sortedTableList = computed(() => {
  tableList.sort((a, b) => <number>a.problemOrder - <number>b.problemOrder)
  return tableList;
})
const total = ref<number>(0);

// 获取列表
const getList = () => {
  loading();
  getProblem();
}

// 多选或者单选
const single = ref(true)
const multiple = ref(true)

// 选择列的id数组
const ids = ref<number[]>([])

const addProblemIds = ref<number[]>([])

const isEdit = ref(false)



const handleSelectionChange = (selection: ProblemInListView[]) => {
  ids.value = selection.map(item => item.problemId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}



const handleDelete = (row: ProblemInListView | Event) => {
  const relations = ids.value.map(x => {
    return {
      listId: listId.value,
      problemId: x
    }})
  if (row instanceof Event) {
    ElMessageBox.confirm(`您是否要删除ID为${ids.value}的数据项？`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          delProblemFromList(relations).then(getList);
        })
  } else {
    ElMessageBox.confirm('是否确认删除题目为"' + row.title + '"的问题？', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          delProblemFromList(relations).then(getList);
        })
  }
}


const handleAdd = () => {
  open.value = true;
}
const handleUpdate = (data: ProblemInListView) => {
  if (data.tempScore === undefined || data.tempOrder === undefined) {
    data.tempScore = data.score;
    data.tempOrder = data.problemOrder;
  }

  if (data.tempScore === data.score && data.tempOrder === data.problemOrder) {
    return;
  }
  data.score = data.tempScore;
  data.problemOrder = data.tempOrder;
  updateProblemRelation({
    listId: listId.value,
    problemId: data.problemId,
    problemOrder: data.problemOrder,
    score: data.score
  })
}

const getAuthText = (auth: ProblemAuth) => {
  return auth === ProblemAuth.CONTEST ? "比赛题目" : "普通题目";
}

const getAuthCardType = (auth: ProblemAuth) => {
  return auth === ProblemAuth.CONTEST ? "danger" : "success";
}





const reset = () => {
  open.value = false;
  relations.length = 0;
  addProblemIds.value.length = 0;
  form.problemId = undefined;
  form.problemOrder = 0;

}
const {isLoading: isAddLoading, add, loading: addLoading} = debouncedAddProblemToList(relations, () => {
  reset();
  getList();
})


const cancel = () => {
  reset();
}

const submit = () => {
  addLoading();

  relations.length = 0;
  relations.push(...addProblemIds.value.map(x => {return {
    listId: listId.value,
    problemId: x,
    problemOrder: 0,
    score: 0
  }}));

  add();
}


// 返回
const back = () => {
  router.push({name: "list-edit"})
}

// created -> 获取列表
getList();




</script>

<style scoped>

</style>

<style>
.app-container {
  .inline-form {
    .el-input {
      --el-input-width: 220px;
    }

    .el-select {
      --el-select-width: 220px;
    }
  }
  .el-table__row .el-dropdown {
    height: 23px;
  }

}
</style>