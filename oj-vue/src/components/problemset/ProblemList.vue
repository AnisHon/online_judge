<template>
  <el-table :data="problems" stripe style="width: 100%">
    <el-table-column prop="id" label="题目ID" />
    <el-table-column prop="title" label="题目名称"/>
    <el-table-column prop="author" label="题目作者"/>
    <el-table-column prop="source" label="题目来源"/>
  </el-table>
</template>

<script lang="ts" setup>
import {getProblems} from '@/api/problem'
import {type ProblemParam} from '@/api/problem'
import {reactive} from "vue";


const pageConfig = reactive({
  pageSize: 10,
  currentPage: 1,
})

const problems = reactive([])


const queryData = reactive<ProblemParam>({
  currentPage: 1,
  pageSize: pageConfig.currentPage,
  problemId: null,
  tagIds: null,
})

getProblems(queryData)
    .then(({currentPage, pageSize, data}) => {
      data.forEach((item) => {
        problems.push({
          id: item.problemId,
          title: item.title,
          author: item.author,
          source: item.source,
        })
      })
    })



</script>