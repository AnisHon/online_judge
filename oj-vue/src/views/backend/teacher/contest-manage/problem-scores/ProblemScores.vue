<template>
  <div class="app-container">
    <div class="header">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-text type="success">
            <h1>题目分数</h1>
          </el-text>
        </el-col>
        <el-col :span="12">
<!--          <ScorePie :max="problemStatistic?.score" :data="scores" style="margin-left: auto"/>-->
        </el-col>
      </el-row>
    </div>

    <div class="contents" >
      <el-table class="table" :data="list" stripe :border="false" header-cell-class-name="header-cell" row-class-name="row">
        <el-table-column label="用户ID" prop="userId" align="center">
          <template v-slot="scope">
            <el-text type="info" size="large">{{ scope.row.userId }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="昵称" prop="nikeName" align="center">
          <template v-slot="scope">
            <el-text type="info" size="large">{{ scope.row.nikeName }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="分数" prop="score" align="center">
          <template v-slot="scope">
            <el-text
                type="success"
                size="large"
            >
              <span v-if="isNullObj(scope.row.score)">未提交</span>
              <span v-else> {{ scope.row.score }}</span>
            </el-text>
          </template>
        </el-table-column>
        <el-table-column label="结果" prop="correct" align="center">
          <template v-slot="scope">
            <el-tag v-if="scope.row.correct" type="success">正确</el-tag>
            <el-tag v-else type="danger">错误</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center">
          <template v-slot="scope">
            <el-link type="primary" @click="router.push({name: 'user-scores', params: {contestId: scope.row.contestId, userId: scope.row.userId}})">
              学生分数
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
import {getProblemScore, type ProblemScore, type ProblemStatistic} from "@/api/record"
import __, {round} from "lodash";
import {isNullObj} from "@/utils/valueutil.ts";
import {useParamStore} from "@/stores/useParam.ts";

const useParam = useParamStore();

const route = useRoute();

const router = useRouter();

const contestId = ref<IdType>(<string>route?.params?.contestId);

const problemId = ref<IdType>(<string>route?.params?.problemId);

const problemStatistic = ref<ProblemStatistic>(useParam.get("ProblemStatistic"));

const list = ref<ProblemScore[]>([])

const scores = computed(() => {
  return list.value.map((item: ProblemScore) => item?.score || 0);
})

const getList = async () => {
  list.value = await getProblemScore({
    problemId: problemId.value,
    contestId: contestId.value,
  });
}

const getAccuracy = (row: ProblemStatistic) => {
  const total = row.absentNum + row.rightNum + row.wrongNum;
  if (total === 0) {
    return 0;
  }

  return round(row.rightNum / total, 2) * 100;
}



getList();

</script>



<style scoped>


</style>

<style lang="scss">
@import "@/assets/color.scss";

.app-container .table .header-cell {
  height: 64px;
  background-color: $table-header-color;

}
.table .el-progress__text {
  font-size: 14px !important;
}

.row {
  height: 64px;
}

</style>