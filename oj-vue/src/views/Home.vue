<template>
  <div style="margin: auto; max-width: var(--page-max-width);">


    <el-row :gutter="20">
      <el-col :span="16">
        <el-space direction="vertical" alignment="stretch" style="width: 100%">

          <custom-card icon="Notification" title="公告">
            <p v-for="i in 4" :key="i"><el-link type="primary">item {{i}}</el-link> </p>
          </custom-card>

          <custom-card icon="Notification" title="公告">
            <p v-for="i in 4" :key="i">item {{i}}</p>
          </custom-card>

          <custom-card icon="Notification" title="最近题目" more @show-more="handleShowMore">
            <el-table :show-header="false" :data="problems">
              <el-table-column label="题目ID" prop="problemId"/>
              <el-table-column prop="title" label="题目">
                <template #default="scope">
                  <el-link target="_blank" type="primary" @click="router.push({name: 'problem', params: {id: scope.row.problemId}})">{{ scope.row.title }}</el-link>
                </template>
              </el-table-column>
              <el-table-column label="问题来源" align="center" prop="source" />
              <el-table-column label="问题类型" align="center" prop="type" >
                <template v-slot="scope">
                  <el-tag type="primary">{{ problemTypeToString(scope.row.type) }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </custom-card>

        </el-space>

      </el-col>
      <el-col :span="8">
        <custom-card icon="Histogram" title="排名" style="height: 100%">
          <el-table :data="ranks">
            <el-table-column type="index" label="#" />
            <el-table-column label="分数" align="center" prop="points" />
            <el-table-column label="用户名"  prop="nikeName" />
          </el-table>
        </custom-card>
      </el-col>
    </el-row>

  </div>

</template>

<script setup lang="ts">
import CustomCard from "@/components/CustomCard/CustomCard.vue";
import {rank, type UserView} from "@/api/user";
import {ref} from "vue";
import {useRouter} from "vue-router";
import {type ProblemView, recentProblem} from "@/api/problem";
import {problemTypeToString} from "@/utils/problem";

const ranks = ref<UserView[]>([])

const problems = ref<ProblemView[]>([])

const router = useRouter();

// created
rank(20)
    .then((data) => {
      ranks.value = data;
    })



recentProblem().then((data) => {
  problems.value = data;
})

const handleShowMore = () => {
  router.push({name: "problems"})
}

</script>

<style scoped>

</style>