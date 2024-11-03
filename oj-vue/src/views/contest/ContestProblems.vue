<template>
  <splitpanes class="contest-problem-container">
    <pane min-size="10" max-size="30" v-loading="isLoading"  ref="problemsPane" class="problem-list" >

      <el-table
          :data="sortedProblemList"
          ref="tableRef"
          height="100%"
          :max-height="maxProblemListHeight"
          @row-click="selectProblem"
          row-class-name="problem-row"
          highlight-current-row
      >
        <el-table-column width="50" label="#" prop="problemOrder"/>
        <el-table-column label="题目" prop="title"/>
        <el-table-column label="分数" prop="score"/>
      </el-table>

    </pane>
    <pane>

      <detail-problem
          ref="detailProblemRef"
          v-if="currentRow"
          :problem-id="currentRow.problemId"
          :contest-id="contestId"
      />
      <div v-else>
        123
      </div>
    </pane>
  </splitpanes>
</template>

<script setup lang="ts">
import { Splitpanes, Pane } from 'splitpanes'
import 'splitpanes/dist/splitpanes.css'
import {type ComponentInstance, computed, onMounted, onUnmounted, reactive, ref} from "vue";
import type {ProblemInListView} from "@/api/list";
import {debouncedGetProblems} from "@/api/list/problem";
import {useRoute} from "vue-router";
import DetailProblem from "@/components/detail-problem/DetailProblem.vue";
import {ElTable} from "element-plus";

const route = useRoute();

const tableRef = ref<typeof ElTable | undefined>(undefined);

const detailProblemRef = ref<typeof DetailProblem | undefined>(undefined);

// 当前题目
const currentRow = ref<ProblemInListView>();

// 题目列表那个Pane的引用
const problemsPane = ref<ComponentInstance<Pane>>();

// 题目列表最大高度
const maxProblemListHeight = ref<number>(999);

const contestId = computed(() => parseInt(<string>route.params.id))

const problemList = reactive<ProblemInListView[]>([])

const sortedProblemList = computed(() => {
  problemList.sort((a, b) => <number>a.problemOrder - <number>b.problemOrder);
  return problemList;
})

const {isLoading, loading, get} = debouncedGetProblems((data) => {
  problemList.length = 0;
  problemList.push(...data);
})

const getProblems = () => {
  loading();
  get(contestId.value);
}

const selectProblem = (row: ProblemInListView) => {

  if (detailProblemRef.value?.isProblemLoading) {
    ElMessage.warning("不要频繁切换页面");
    tableRef.value?.setCurrentRow(row);
    return;
  }
  currentRow.value = row;
}


const setMaxHeight = () => {
  maxProblemListHeight.value = (<HTMLElement>problemsPane.value?.$el).clientHeight;
}

// created
getProblems();


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

<style scoped>

</style>

<style>

.contest-problem-container {
  height: var(--in-main-content-height);
}

.splitpanes--vertical > .splitpanes__splitter {
  min-width: 6px;
}

</style>