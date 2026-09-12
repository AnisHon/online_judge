<template>
  <main class="problem-set common-max-width-page" >
    <header class="problem-header">
      <div><p class="eyebrow">PRACTICE LIBRARY</p><h1>题库</h1><p>从基础练习到综合挑战，找到下一道值得解决的问题。</p></div>
      <div class="problem-header__mark">#</div>
    </header>
    <section class="problem-toolbar"><ProblemListForm @query="doQuery"/></section>
    <section class="problem-results">
      <div class="result-heading"><strong>题目列表</strong><span>共 {{ pageNav.totalRecords }} 道题目</span></div>
      <ProblemList :param="currentPage"
        @load-finish="handleLoadFinish"
      />
    </section>
    <footer class="footer">

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

    </footer>
  </main>
</template>

<script setup lang="ts">
import ProblemList from "@/components/problemset/ProblemList.vue";
import {type ProblemParam, ProblemType} from "@/api/problem"
import {reactive} from "vue";
import ProblemListForm from "@/components/problemset/ProblemListForm.vue";
import type {IdType} from "@/api/common.ts";


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
  title: "",
  type: undefined,
  tagIds: []
});


const handleTotalPageChange = () => {
  currentPage.currentPage = 1;
  currentPage.pageSize = pageNav.pageSize;
}


const doQuery = (value: {id: string, tagIds: IdType[], title: string, type: ProblemType}) => {
  currentPage.problemId = value.id;
  currentPage.tagIds = value.tagIds.slice();
  currentPage.title = value.title
  currentPage.type = value.type
}



const handlePageChange = (value: number) => {
  currentPage.currentPage = value
}

const handleLoadFinish = (currentPage: number, pageSize: number, totalRecords: number) => {

  pageNav.pageCount = Math.ceil(totalRecords / pageSize)
  pageNav.totalRecords = totalRecords
}


</script>

<style scoped>
.footer {
  margin: 20px 0;
  position: relative;
  height: 48px;
}
.problem-set {
  margin: auto;
  padding-bottom: 24px;
}
.problem-header { display: flex; justify-content: space-between; align-items: center; min-height: 132px; margin-bottom: 18px; padding: 24px 32px; overflow: hidden; border: 1px solid var(--el-border-color-light); border-radius: 18px; background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-bg-color)); color: var(--el-text-color-primary); }.eyebrow { margin: 0 0 7px; color: var(--el-color-primary); font-size: 11px; font-weight: 800; letter-spacing: .18em; }.problem-header h1 { margin: 0; font-size: 30px; letter-spacing: -.04em; }.problem-header p:last-child { margin: 9px 0 0; color: var(--el-text-color-secondary); }.problem-header__mark { margin-right: 8%; color: var(--el-color-primary-light-5); font: 900 88px/1 var(--code-font-family, monospace); transform: rotate(-12deg); opacity: .42; }.problem-toolbar, .problem-results { padding: 18px 22px; border: 1px solid var(--el-border-color-light); border-radius: 16px; background: var(--el-bg-color); }.problem-results { margin-top: 14px; }.result-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; color: var(--el-text-color-primary); }.result-heading span { color: var(--el-text-color-secondary); font-size: 13px; }
@media (max-width: 600px) { .problem-set { padding: 8px 12px 24px; }.problem-header { min-height: 108px; padding: 20px; }.problem-header h1 { font-size: 25px; }.problem-header p:last-child { font-size: 13px; }.problem-header__mark { display: none; }.problem-toolbar, .problem-results { padding: 14px; }.result-heading { margin-bottom: 8px; } }
</style>
