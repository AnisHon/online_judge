<template>
  <div class="app-container">
    <div class="header">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-text type="success">
            <h1>用户分数</h1>
          </el-text>
          <el-text type="info">
            总分：{{ total }}
          </el-text>
        </el-col>

        <el-col :span="6">

        </el-col>
      </el-row>
    </div>

    <div class="contents" >
      <el-table class="table" :data="list" stripe :border="false" header-cell-class-name="header-cell" row-class-name="row">
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
        <el-table-column label="结果" prop="correct" align="center">
          <template v-slot="scope">
            <el-tag type="success" v-if="scope.row.correct" >正确</el-tag>
            <el-tag type="danger" v-else>错误</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="得分" prop="score" align="center">
          <template v-slot="scope">
            <el-tag v-if="isNullObj(scope.row.score)" type="danger">未提交</el-tag>
            <el-text v-else type="success" size="large">{{ scope.row.score }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center">
          <template v-slot="scope">
            <el-link
                type="primary"
                @click="toUserAnswer(scope.row)"
                :disabled="isNullObj(scope.row.score)"
            >
            查看答案
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
import {getUserScores, type UserScore} from "@/api/record"
import {isNullObj} from "@/utils/valueutil.ts";

const route = useRoute();

const router = useRouter();

const contestId = ref<IdType>(<string>route?.params?.contestId);

const userId = ref<IdType>(<string>route?.params?.userId);

const list = ref<UserScore[]>([])

const total = computed(() => {
  let sum = 0;
  list.value.forEach((item: UserScore) => sum += item.score || 0);
  return sum;
})

const getList = async () => {
  list.value = await getUserScores({userId: userId.value, contestId: contestId.value});
}

const toUserAnswer = (row: UserScore) => {
  router.push({name: 'user-answer', params: {contestId: contestId.value, problemId: row.problemId, userId: userId.value}});
}

getList();

</script>

<style lang="scss" scoped>

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