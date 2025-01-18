<template>
  <div class="app-container">
    <div class="header">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-text type="success">
            <h1>题目统计</h1>
          </el-text>
        </el-col>
        <el-col :span="6">
          <div class="absoluteCenter">
            <el-progress
                type="circle"
                :percentage="averageCorrect"
                :width="150"
            />
            <el-text type="info" size="large" >
              <h3>平均正确率</h3>
            </el-text>
          </div>
        </el-col>

        <el-col :span="6">

        </el-col>
      </el-row>
    </div>

    <div class="contents" >
      <el-table class="table" :data="list" stripe :border="false" header-cell-class-name="header-cell">
        <el-table-column label="题目ID" prop="problemId" align="center" />
        <el-table-column label="题目" prop="title"  align="left">
          <template v-slot="scope">
            <el-link
                type="primary"
                :underline="false"
                target="_blank"
                :href="`/problem/${scope.row.problemId}`"
            >
              {{ scope.row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="题目分数" prop="score" align="center">
          <template v-slot="scope">
            <el-text type="info" size="large">{{ scope.row.score }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="平均分数" prop="average" align="center">
          <template v-slot="scope">
            <el-text type="primary" size="large">{{ scope.row.average }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="正确人数" prop="rightNum" align="center">
          <template v-slot="scope">
            <el-text type="success" size="large">{{ scope.row.rightNum }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="错误人数" prop="wrongNum" align="center">
          <template v-slot="scope">
            <el-text type="danger" size="large">{{ scope.row.wrongNum }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="未做人数" prop="absentNum" align="center">
          <template v-slot="scope">
            <el-text type="warning" size="large">{{ scope.row.absentNum }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="正确率" type="default" align="center">
          <template v-slot="scope">
            <el-progress
                type="dashboard"
                :percentage="getAccuracy(scope.row)"
                :width="64"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center">
          <template v-slot="scope">
            <el-link
                type="primary"
                @click="toStudentScore(scope.row)"
            >
              题目分数
            </el-link>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="当前没有任何题目"/>
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">

import {useRoute, useRouter} from "vue-router";
import {computed, ref} from "vue";
import type {IdType} from "@/api/common.ts";
import {getProblemStatistic, type ProblemStatistic} from "@/api/record"
import {round} from "lodash";
import {useParamStore} from "@/stores/useParam.ts";

const route = useRoute();

const router = useRouter();

const useParam = useParamStore();

const contestId = ref<IdType>(<string>route?.params?.contestId);

const list = ref<ProblemStatistic[]>([])

const averageCorrect = computed(() => {
  if (list.value.length === 0) {
    return 0;
  }
  let total = 0;
  list.value.forEach((element) => {
    total += getAccuracy(element);
  });

  return round(total / list.value.length) * 100;
})

const getList = async () => {
  list.value = await getProblemStatistic(contestId.value);
}

const getAccuracy = (row: ProblemStatistic) => {
  const total = row.absentNum + row.rightNum + row.wrongNum;
  if (total === 0) {
    return 0;
  }

  return round(row.rightNum / total, 2) * 100;
}

const toStudentScore = (row: ProblemStatistic) => {

  router.push({name: 'problem-scores', params: {contestId: contestId.value, problemId: row.problemId}});
  useParam.set("ProblemStatistic", row);
}



getList();

</script>

<style lang="scss" scoped>
@use '@/assets/color' as *;

::v-deep(.table) {
  .header-cell {
    height: 64px;
    background-color: $table-header-color;
  }

  .el-progress__text {
    font-size: 14px !important;
  }

  .row {
    height: 64px;
  }

}
</style>