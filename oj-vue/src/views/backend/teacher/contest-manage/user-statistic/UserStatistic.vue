<template>
  <div class="app-container">
    <div class="header">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-text type="success">
            <h1>用户统计</h1>
          </el-text>
        </el-col>

        <el-col :span="6">

        </el-col>
      </el-row>
    </div>

    <div class="contents" >
      <el-table class="table" :data="list" stripe :border="false" header-cell-class-name="header-cell" row-class-name="row">
        <el-table-column label="用户ID" prop="userId" align="center" />
        <el-table-column label="昵称" prop="nikeName"  align="left">
          <template v-slot="scope">
              {{ scope.row.nikeName }}
          </template>
        </el-table-column>
        <el-table-column label="是否提交" prop="submitted" align="center">
          <template v-slot="scope">
            <el-tag type="success" v-if="scope.row.submitted">
              已提交
            </el-tag>
            <el-tag v-else type="danger">
              未提交
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="是否迟交" prop="late" align="center">
          <template v-slot="scope">
            <el-tag type="danger" v-if="!scope.row.submitted">
              未交
            </el-tag>
            <el-tag type="success" v-else-if="!scope.row.late">
              未迟交
            </el-tag>
            <el-tag type="warning" v-else>
              迟交
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分数" prop="score" align="center">
          <template v-slot="scope">
            <el-text type="success" size="large">{{ scope.row.score }}</el-text>
          </template>
        </el-table-column>

        <el-table-column label="正确题数" prop="correctNum" align="center">
          <template v-slot="scope">
            <el-text type="success" size="large">{{ scope.row.rightNum }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="错误题数" prop="wrongNum" align="center">
          <template v-slot="scope">
            <el-text type="danger" size="large">{{ scope.row.wrongNum }}</el-text>
          </template>
        </el-table-column>
        <el-table-column label="未做题数" prop="absentNum" align="center">
          <template v-slot="scope">
            <el-text type="warning" size="large">{{ scope.row.absentNum }}</el-text>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center">
          <template v-slot="scope">
            <el-link
                type="primary"
                @click="toUserScore(scope.row)"
            >
              用户分数
            </el-link>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="当前没有用户"/>
        </template>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">

import {useRoute, useRouter} from "vue-router";
import {ref} from "vue";
import type {IdType} from "@/api/common.ts";
import {getUserStatistic, type UserStatistic} from "@/api/record"

const route = useRoute();

const router = useRouter();

const contestId = ref<IdType>(<string>route?.params?.contestId);

const list = ref<UserStatistic[]>([])

const getList = async () => {
  list.value = await getUserStatistic(contestId.value);
}

const toUserScore = (row: UserStatistic) => {
  router.push({name: 'user-scores', params: {contestId: contestId.value, userId: row.userId}})
}



getList();

</script>

<style lang="scss" scoped>
@use "@/assets/styles/color" as *;

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