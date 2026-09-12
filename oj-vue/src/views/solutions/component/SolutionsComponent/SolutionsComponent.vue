<template>
  <div class="solutions-list">
    <el-card class="share-card" shadow="never">
      <div class="share-card__content">
        <div><el-icon size="24"><DocumentAdd /></el-icon><div><strong>分享你的解题思路</strong><small>把方法沉淀下来，也帮助更多同学</small></div></div>
        <el-button type="primary" plain @click="addSolution">发布题解</el-button>
      </div>
    </el-card>
    <el-empty v-if="total === 0" description="还没有人发题解" />
    <solution-card v-for="item of solutions" :key="item.solutionId" :solution="item" @open="detailSolution" />

    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="param.currentPage"
        v-model:limit="param.pageSize"
        @pagination="getList"
        :scroll-element="scrollElement"
    />
  </div>
</template>

<script setup lang="ts">

import {ref} from "vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {listSolution, type QuerySolution, type Solution} from "@/api/solution";
import {DocumentAdd} from "@element-plus/icons-vue";
import {useRouter} from "vue-router";
import type {IdType} from "@/api/common.ts";
import SolutionCard from "@/components/SolutionCard/SolutionCard.vue";

const router = useRouter();

// 题解列表
const solutions = ref<Solution[]>([]);

// 当前题解总数
const total = ref(1);

// 题解查询参数
const param = defineModel<QuerySolution>("param", {required: true});

// 滚动元素
const {scrollElement = undefined} = defineProps<{scrollElement?: HTMLElement}>();

// 获取列表
const getList = async () => {
  const data = await listSolution(param.value);
  total.value = data.totalRecords
  solutions.value = data.data;
}

// 添加题解
const addSolution = () => {
  router.push({name: "solution_edit", query: {problemId: param.value.problemId}});
}

// 查看题解详情
const detailSolution = (solutionId: IdType) => {
  router.push({name: "solution", params: {id: solutionId}});
}

getList();

</script>

<style scoped>
.solutions-list { display: flex; flex-direction: column; gap: 14px; }.share-card { border: 1px solid var(--el-border-color-light); border-radius: 16px; background: var(--el-color-primary-light-9); }.share-card__content, .share-card__content > div:first-child { display: flex; align-items: center; }.share-card__content { justify-content: space-between; gap: 16px; }.share-card__content > div:first-child { gap: 12px; color: var(--el-color-primary); }.share-card strong, .share-card small { display: block; }.share-card strong { color: var(--el-text-color-primary); }.share-card small { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 12px; }
</style>
