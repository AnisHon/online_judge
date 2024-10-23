<template>
  <div class="problem-set common-max-width-page" >
    <ProblemListForm @query="doQuery"/>
    <ProblemList
        :current-page="currentPage.currentPage"
        :page-size="currentPage.pageSize"
        :problem-id="currentPage.problemId"
        :tag-ids="currentPage.tagIds"
        @load-finish="handleLoadFinish"
    />
    <div>
      <div class="footer">

          <el-input-number
              style="position:absolute; left: 0;"
              v-model="pageNav.pageSize"
              :min="20"
              :step="10"
              :max="100"
              controls-position="right"
              size="small"
              @change="handleTotalPageChange"
          />


          <el-pagination
              style="position:absolute; left: 50%; transform: translateX(-50%);"
              layout="prev, pager, next"
              :page-count="pageNav.pageCount"
              @current-change="handlePageChange"
          />

      </div>



    </div>
  </div>
</template>

<script setup lang="ts">
import ProblemList from "@/components/problemset/ProblemList.vue";
import {type ProblemParam} from "@/api/problem"
import {reactive} from "vue";
import ProblemListForm from "@/components/problemset/ProblemListForm.vue";


const pageNav = reactive({
    totalRecords: 0,
    currentPage: 1,
    pageSize: 40,
    pageCount: 0,
  })

  const currentPage = reactive<ProblemParam>({
    currentPage: 1,
    pageSize: pageNav.pageSize,
    problemId: null,
    tagIds: []
  });

  const handleTotalPageChange = () => {
    const page = currentPage.currentPage / pageNav.pageCount;
    pageNav.pageCount = Math.ceil(pageNav.totalRecords / pageNav.pageSize);
    pageNav.currentPage = page * pageNav.pageCount;

    currentPage.pageSize = pageNav.pageSize;
    currentPage.currentPage = pageNav.currentPage
  }


  const doQuery = (value: {id: string, tagIds: number[]}) => {
    currentPage.problemId = value.id;
    currentPage.tagIds = value.tagIds.slice();
  }

  const handlePageChange = (value: number) => {
    currentPage.currentPage = value
  }

  const handleLoadFinish = (currentPage: number, pageSize: number, totalRecords: number) => {

    pageNav.pageCount = Math.ceil(totalRecords / pageSize)
    pageNav.totalRecords = totalRecords
    console.log(pageNav.pageCount)
  }


</script>

<style scoped>
.footer {
  margin: 20px 0;
  position: relative;
  height: 48px;
}
.problem-set {
  margin: auto
}
</style>