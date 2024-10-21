<template>
  <el-table :data="problems" stripe style="width: 100%">
    <el-table-column prop="id" label="题目ID" />
    <el-table-column prop="title" label="题目名称">
      <template #default="scope">
        <router-link :to="{name: 'problem', params: {id: scope.row.id}}" class="router-link">{{scope.row.title}}</router-link>
      </template>
    </el-table-column>
    <el-table-column prop="author" label="标签">
      <template #default="scope">
        <el-tag  v-for="item of scope.row.tag" :color="item.tagColor" :key="item.tagId">
          <span style="color: white">
            {{ item.tagName}}
          </span>
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="source" label="题目来源"/>
  </el-table>
</template>

<script lang="ts" setup>
import {getProblems, type ProblemParam, type TaggedProblemView} from '@/api/problem'
import {ref, watch} from "vue";

const problems = ref([]);

const queryData = defineProps<ProblemParam>()

const emit = defineEmits(['loadFinish'])

watch(queryData, () => {
  getProblems(queryData)
      .then(({currentPage, pageSize, totalRecords, data}) => {
        let tempProblems = [];
        data.forEach((item: TaggedProblemView) => {
          tempProblems.push({
            id: item.problemId,
            title: item.title,
            tag: item.tags,
            source: item.source,
          })
        })
        problems.value = tempProblems
        emit('loadFinish', currentPage, pageSize, totalRecords)
      })
}, {immediate: true, deep: true})

</script>

<style scoped>
  .router-link,
  .router-link:active,
  .router-link:focus
  {
    color: #3498db;
  }



</style>