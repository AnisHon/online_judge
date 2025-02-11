<template>

  <el-skeleton animated :loading="loading">
    <template #template>
      <el-table table-layout="auto" :data="loadingArray">
        <el-table-column prop="type" label="题目类型">
          <template #default>
            <el-skeleton-item variant="text"/>
          </template>
        </el-table-column>
        <el-table-column label="题目ID" >
          <template #default>
            <el-skeleton-item variant="text"/>
          </template>
        </el-table-column>
        <el-table-column label="题目名称">
          <template #default>
            <el-skeleton-item variant="text"/>
          </template>
        </el-table-column>
        <el-table-column label="标签">
          <template #default>
            <el-skeleton-item variant="text"/>
          </template>
        </el-table-column>
        <el-table-column label="题目来源">
          <template #default>
            <el-skeleton-item variant="text"/>
          </template>
        </el-table-column>
      </el-table>
    </template>

    <template #default>
      <el-table table-layout="auto" :data="problems" stripe style="width: 100%">
        <el-table-column prop="finish" label="状态">
          <template v-slot="scope">
            <el-tooltip v-if="scope.row.finish" content="已完成" placement="top">
              <el-icon color="var(--el-color-success)">
                <CircleCheck/>
              </el-icon>
            </el-tooltip>
            <div v-else></div>
          </template>

        </el-table-column>
        <el-table-column prop="id" label="题目ID" show-overflow-tooltip />
        <el-table-column prop="title" label="题目名称" show-overflow-tooltip>
          <template #default="scope">
            <router-link target="_blank" :to="{name: 'problem', params: {id: scope.row.id}}" class="router-link">{{ scope.row.title }}</router-link>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="题目类型">
          <template #default="scope">
            <el-tag type="success">
              {{ scope.row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="标签" show-overflow-tooltip>
          <template #default="scope">
            <el-space wrap>
              <el-tag  v-for="item of scope.row.tag" :color="item.tagColor" :key="item.tagId">
                <span style="color: white">
                  {{ item.tagName}}
                </span>
              </el-tag>
            </el-space>

          </template>
        </el-table-column>
        <el-table-column prop="source" label="题目来源" show-overflow-tooltip/>
      </el-table>
    </template>
  </el-skeleton>

</template>

<script lang="ts" setup>
import {getProblems, type ProblemParam, type TaggedProblemView} from '@/api/problem'
import {computed, inject, ref, watch} from "vue";
import type {TagView} from "@/api/problem/label";
import {problemTypeToString} from "@/utils/problem";
import {CircleCheck} from "@element-plus/icons-vue";
import __ from "lodash";

interface ProblemTableView {
  id: number;
  type: string;
  title: string;
  tag: TagView[];
  source: string;
  finish: boolean;
}

const problems = ref<ProblemTableView[]>([]);

const loading = ref(true);

const {param} = defineProps<{param: ProblemParam}>()

const emit = defineEmits(['loadFinish'])


const doGetProblems = () => {

  getProblems(param)
      .then(({currentPage, pageSize, totalRecords, data}) => {
        let tempProblems: ProblemTableView[] = [];
        data.forEach((item: TaggedProblemView) => {
          tempProblems.push({
            id: item.problemId,
            type: problemTypeToString(item.type),
            title: item.title,
            tag: item.tags,
            source: item.source,
            finish: item.finish
          })
        })
        problems.value = tempProblems
        emit('loadFinish', currentPage, pageSize, totalRecords)
      })
      .catch(() => {})
      .finally(() => {loading.value = false});
}

const loadingArray = computed(() => {
  return Array.from({length: param.pageSize})
})

const debouncedGetProblems = __.debounce(doGetProblems, 1000)
const elMain = inject("elMain");

watch(() => param, () => {
  loading.value = true;
  debouncedGetProblems()
  //@ts-ignore
  scrollTo(0, 800, undefined, elMain?.elMainRef.value?.$el);
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