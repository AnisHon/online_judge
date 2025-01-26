<template>
  <splitpanes class="app-container">
    <pane min-size="10" max-size="30" v-loading="isLoading"  ref="problemsPane" class="problem-list" >
      <el-scrollbar>
        <el-table
            :data="sortedProblemList"
            ref="tableRef"
            @row-click="selectProblem"
            row-class-name="problem-row"
            highlight-current-row
        >
          <el-table-column width="50" type="index" label="#"/>
          <el-table-column label="题目" prop="title"/>
          <el-table-column label="分数" prop="score"/>
        </el-table>
        <el-button
            v-if="isContestEnabled"
            :disabled="!isContestEnabled"
            type="warning"
            size="large"
            style="width: 100%"
            @click="handIn"
            plain
        >提交</el-button>
        <el-button
          :disabled="true"
          type="success"
          size="large"
          style="width: 100%"
          @click="handIn"
          plain
          v-else
        >已交卷</el-button>
      </el-scrollbar>

    </pane>
    <pane>

      <detail-problem
          ref="detailProblemRef"
          v-if="currentRow"
          :problem-id="currentRow.problemId"
          :contest-id="contestId"
          :disable-submit="!isContestEnabled"
      />
      <div v-else>
        <div v-if="!!contest" style="position: relative;">
          <div style="position: absolute;left: 50px;top:50px;font-size: 36px; color: darkred; " v-if="score === 0 || !!score">
            分数：{{ score }}
          </div>

          <el-card style="max-height: 30vh">
            <div class="absoluteCenter">
              <h1 style="text-align: center">{{ contest.title }}</h1>
              <el-space wrap alignment="center">
                <el-tag type="info">{{ formatDate(contest.startTime) }}</el-tag>
                <el-tag type="info">{{ formatDate(contest.endTime) }}</el-tag>
                <el-tag :type="authTagType(contest.auth)">{{ authText(contest.auth) }}</el-tag>
              </el-space>
              <el-divider />
              <div class="absoluteCenter">
                <el-tag style="margin-left: auto" type="danger" v-if="isContestOver(contest.endTime)">
                  已结束
                </el-tag>
                <el-tag style="margin-left: auto" type="success" v-else-if="isNotStart(contest.startTime)">
                  未开始
                </el-tag>
                <el-tag style="margin-left: auto" type="primary" v-else>
                  正在进行
                </el-tag>
              </div>
            </div>

          </el-card>


          <div style="margin: 20px; overflow: auto; max-height: 65vh;">
            <markdown-preview :text="contest.description"/>
          </div>



        </div>
      </div>
    </pane>
  </splitpanes>
</template>

<script setup lang="ts">
import {Pane, Splitpanes} from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'
import {type ComponentInstance, computed, onMounted, onUnmounted, reactive, ref} from "vue";
import type {ProblemInListView} from "@/api/list";
import {debouncedGetProblems} from "@/api/list/problem.ts";
import {useRoute} from "vue-router";
import DetailProblem from "@/components/DetailProblem/DetailProblem.vue";
import {ElMessageBox, ElTable} from "element-plus";
import {type ContestView, fetchContestById, getContestStatus, getScore, handInPaper} from "@/api/contest";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {authTagType, authText, formatDate, isContestOver, isNotStart} from "@/utils/contest";

const route = useRoute();

const tableRef = ref<typeof ElTable | undefined>(undefined);

const detailProblemRef = ref<typeof DetailProblem | undefined>(undefined);

// 当前题目
const currentRow = ref<ProblemInListView>();

// 题目列表那个Pane的引用
const problemsPane = ref<ComponentInstance<Pane>>();

// 题目列表最大高度
const maxProblemListHeight = ref<number>(999);

const contestId = computed(() => <string>route.params.id);

const problemList = reactive<ProblemInListView[]>([]);

const contest = ref<ContestView>();

const score = ref<number | null>(null);

const isSubmitted = ref(true);

const sortedProblemList = computed(() => {
  problemList.sort((a, b) => <number>a.problemOrder - <number>b.problemOrder);
  return problemList;
})

const isContestEnabled = computed(() => {
  return !isContestOver(contest.value?.endTime) && !isNotStart(contest.value?.startTime) && isSubmitted.value;
})

const {isLoading, loading, get} = debouncedGetProblems((data) => {
  problemList.length = 0;
  problemList.push(...data);
})

const getProblems = async () => {
  loading();
  get(contestId.value);
  contest.value = await fetchContestById(contestId.value);
  if (!isContestEnabled.value) {
    getScore(contestId.value).then(data => {
      score.value = !!data ? data : 0;
    });
  }
}

const selectProblem = (row: ProblemInListView) => {

  if (detailProblemRef.value?.isProblemLoading) {
    ElMessage.warning("不要频繁切换页面");
    tableRef.value?.setCurrentRow(row);
    return;
  }
  currentRow.value = row;
}

const getStatus = () => {
  getContestStatus(contestId.value)
      .then(status => isSubmitted.value = status)

}

const setMaxHeight = () => {
  maxProblemListHeight.value = (<HTMLElement>problemsPane.value?.$el).clientHeight;
}

// 交卷
const handIn = () => {
  ElMessageBox.confirm("您确认要交卷？交卷后无法提交", {cancelButtonText: "取消", confirmButtonText: "确定"})
      .then(() => {
        handInPaper(contestId.value)
        .then(getStatus)
      }).catch(() => {})
}


// created
getProblems();

// 获取当前是否可以写
getStatus();

onMounted(() => {
  setMaxHeight();
  window.onresize = () => {
    setMaxHeight();
  }
})

onUnmounted(() => {
  window.onresize = null;
})


</script>

<style lang="scss" scoped>
.app-container {
  height: var(--in-main-content-height);
}

::v-deep(.splitpanes--vertical) > .splitpanes__splitter {
  min-width: 6px;
}
</style>
