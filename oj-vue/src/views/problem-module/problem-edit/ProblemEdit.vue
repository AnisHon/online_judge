<template>
  <div class="tag-container">
    <el-form :model="queryParams" class="inline-form" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="题目" prop="problemName">
        <el-input
            v-model="queryParams.title"
            placeholder="请输入题目"

            @keyup.enter.native="handleQuery"
            clearable
        />
      </el-form-item>
      <el-form-item label="题目类型" prop="problemType">
        <el-select
            v-model="queryParams.type"
            placeholder="菜单类型"
            clearable
            style="width: 120px"
        >
          <el-option
              v-for="item in dict.problemType"
              :key="item.value"
              :label="item.label"
              :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="题目ID" prop="parentId">
        <el-input-number v-model="queryParams.problemId" :controls="false"/>
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
            v-has="'problem:problem:add'"
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
            v-has="'problem:problem:update'"
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
            v-has="'problem:problem:remove'"
        >删除</el-button>
      </el-col>
      <right-tool-bar style="margin-left: auto" v-model:showSearch="showSearch" :columns="columns" @queryTable="getList"/>
    </el-row>

<!--    ['问题ID', '题目', '问题描述', '问题来源', '问题类型' ,'问题权限', '创建时间', '提示']-->
    <el-table v-loading="isLoading" :data="tableList" @selection-change="handleSelectionChange">
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
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template v-slot:default="scope">
          <el-link
              size="small"
              type="primary"
              icon="edit"
              @click="handleUpdate(scope.row)"
              v-has="'problem:problem:update'"
          >修改</el-link>
          <el-link
              size="small"
              type="primary"
              icon="delete"
              @click="handleDelete(scope.row)"
              v-has="'problem:problem:remove'"
          >删除</el-link>
          <el-dropdown size="small" @command="(command: string) => handleCommand(command, scope.row)"
                       v-has-any="['user:user:edit'] ">
            <el-link size="small" type="primary" icon="arrow-right">更多</el-link>
            <template #dropdown>
              <el-dropdown-menu>
                <div v-has="'problem:tag:add'" >
                  <el-dropdown-item command="handleCard" icon="Postcard"
                  >设置标签</el-dropdown-item>
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


    <el-dialog title="管理题目标签" v-model="open" width="680px" append-to-body>
      <div >
        <el-space v-loading="loadingCard">
          <el-check-tag
              v-for="item of allCards"
              :key="item.tagId"
              :checked="currentCards.some(x => x.tagId === item.tagId)"
              @change="(bool: boolean) => onChange(bool, item)"
          >
            {{ item.tagName }}
          </el-check-tag>
        </el-space>
      </div>
      <template #footer>
        <el-button type="primary" @click="submit" :loading="addIsLoading">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </template>
    </el-dialog>


  </div>
</template>

<script setup lang="ts">
import {reactive, ref} from "vue";
import {
  type AdminQueryProblem,
  debouncedGetProblem,
  dict,
  ProblemAuth,
  type ProblemView,
  removeProblems
} from "@/api/problem";
import {useColumn} from "@/hooks/useColumn";
import RightToolBar from "@/components/right-toolbar/RightToolBar.vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {ElDialog, ElMessageBox} from "element-plus";
import {problemTypeToString} from "@/utils/problem";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {
  debouncedAddTagProblem, delTagForProblem,
  fetchTagByProblemId,
  getAllTags,
  type ProblemTagRelation,
  type TagView
} from "@/api/problem/label";
import __ from "lodash";


// 查询需要的表单数据
const queryParams = reactive<AdminQueryProblem>({
  currentPage: 1,
  pageSize: 20,
  problemId: undefined,
  title: undefined,
  type: undefined
});

const {columns} = useColumn(['问题ID', '题目', '问题描述', '问题来源', '问题类型' ,'问题权限', '创建时间', '提示']);

// 重制列表
const resetQuery = () => {
  queryParams.problemId = undefined;
  queryParams.title = undefined;
  queryParams.type =  undefined;

  getList();
};


const showSearch = ref(true);

const {loading, isLoading, get: getProblem} = debouncedGetProblem(queryParams, (data) => {
  tableList.length = 0;
  total.value = data.totalRecords
  tableList.push(...data.data)
});


const tableList = reactive<ProblemView[]>([]);
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

const handleSelectionChange = (selection: ProblemView[]) => {
  ids.value = selection.map(item => item.problemId);
  single.value = selection.length != 1;
  multiple.value = !selection.length;
}



const handleDelete = (row: ProblemView | Event) => {
  if (row instanceof Event) {
    ElMessageBox.confirm(`您是否要删除ID为${ids.value}的数据项？`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeProblems(ids.value).then(getList);
        })
  } else {
    ElMessageBox.confirm('是否确认删除题目为"' + row.title + '"的问题？', {
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })
        .then(() => {
          removeProblems(row.problemId).then(getList);
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
}
const handleUpdate = (data: ProblemView) => {

}

const getAuthText = (auth: ProblemAuth) => {
  return auth === ProblemAuth.CONTEST ? "比赛题目" : "普通题目";
}

const getAuthCardType = (auth: ProblemAuth) => {
  return auth === ProblemAuth.CONTEST ? "danger" : "success";
}

const delCardIds = ref<ProblemTagRelation[]>([]);
const addCardIds = ref<ProblemTagRelation[]>([]);
const currentCards = ref<TagView[]>([]);
const originCards = ref<TagView[]>([]);
const allCards = ref<TagView[]>([]);
const status = reactive<boolean[]>([])
const open = ref(false)
const currentProblemId = ref(0)

const onChange = (bool: boolean, id: TagView) => {

  if (!bool) {
    __.remove(currentCards.value, x => x.tagId === id.tagId);
  } else {
    currentCards.value.push(id);
  }
}



const cancel = () => {
  currentCards.value.length = 0;
  addCardIds.value.length = 0;
  delCardIds.value.length = 0;
  open.value = false;
}

const loadingCard = ref(false)
const {isLoading: addIsLoading, loading: addLoading,add: tagAdd, finish} = debouncedAddTagProblem(addCardIds.value, () => {
  open.value = false;
})

const submit = () => {

  addCardIds.value.length = 0;
  addCardIds.value.push(...__.difference(currentCards.value, originCards.value).map(item => {return {tagId: item.tagId,  problemId: currentProblemId.value}}));
  delCardIds.value = __.difference(originCards.value, currentCards.value).map(item => {return {tagId: item.tagId,  problemId: currentProblemId.value}});
  if (addCardIds.value.length > 0) {
    addLoading();
    tagAdd();
  }

  if (delCardIds.value.length > 0) {
    addLoading();
    delTagForProblem(delCardIds.value).then(() => {
      finish();
      open.value = false;
    });
  }

}

const manageTag = (id: number) => {
  open.value = true;
  loadingCard.value = true
  fetchTagByProblemId(id)
      .then((data) => {
        originCards.value.length = 0;
        currentCards.value = data;
        originCards.value.push(...data);
        loadingCard.value = false
      })

}

const handleCommand = (command: string, row: ProblemView) => {

  if (command === 'handleCard') {
    manageTag(row.problemId);
    currentProblemId.value = row.problemId;
  }
}



// created -> 获取列表
getList();
getAllTags()
    .then((data) => {
      allCards.value = data;
      data.forEach(() => status.push(false));
    } )



</script>

<style scoped>

</style>

<style>
.tag-container {
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