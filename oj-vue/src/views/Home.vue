<template>
  <div class="common-max-width-page middle">


    <el-row :gutter="20">
      <el-col class="cards-wrapper" :span="16">

          <custom-card class="card" icon="Notification" title="公告" more @show-more="notificationShowMore">
            <el-table :data="notices" :show-header="false">
              <el-table-column prop="title"  show-overflow-tooltip >
                <template v-slot="scope">
                  <el-link
                      :underline="false"
                      type="primary"
                      @click="router.push({name: 'notice', params:{id: scope.row.noticeId}})"
                  >
                    {{ scope.row.title }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column prop="createTime" align="right"/>
            </el-table>
          </custom-card>

          <custom-card  class="card"  icon="EditPen" title="最近题解" more @show-more="solutionShowMore">
            <el-table :data="solutions" :show-header="false">
              <el-table-column prop="title" show-overflow-tooltip>
                <template v-slot="scope">
                  <el-link
                      :underline="false"
                      type="primary"
                      @click="router.push({name: 'solution', params:{id: scope.row.solutionId}})"
                  >
                    {{ scope.row.title }}
                  </el-link>
                  <el-tag style="margin-left: 10px" v-if="scope.row.topUp" type="danger">置顶</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </custom-card>

          <custom-card  class="card"  icon="Notification" title="最近题目" more @show-more="problemShowMore">
            <el-table :show-header="false" :data="problems">
              <el-table-column prop="title" label="题目" show-overflow-tooltip>
                <template #default="scope">
                  <el-link target="_blank" type="primary" @click="router.push({name: 'problem', params: {id: scope.row.problemId}})">{{ scope.row.title }}</el-link>
                </template>
              </el-table-column>
              <el-table-column label="问题来源" align="center" prop="source" show-overflow-tooltip />
              <el-table-column label="问题类型" align="center" prop="type" >
                <template v-slot="scope">
                  <el-tag type="primary">{{ problemTypeToString(scope.row.type) }}</el-tag>
                </template>
              </el-table-column>
            </el-table>
          </custom-card>


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
import {recentSolution, type Solution} from "@/api/solution";
import {type Notice, recentNotices} from "@/api/notice";

const ranks = ref<UserView[]>([]);

const problems = ref<ProblemView[]>([]);

const solutions = ref<Solution[]>([]);

const notices = ref<Notice[]>([]);

const router = useRouter();

// created
rank(20)
    .then((data) => {
      data.length = Math.min(data.length, 20);
      ranks.value = data;
    })

recentNotices()
    .then(data => {
      notices.value = data;
    })



recentProblem().then((data) => {
  problems.value = data;
})

recentSolution().then((data) => {
  solutions.value = data;
})

const problemShowMore = () => {
  router.push({name: "problems"})
}

const notificationShowMore = () => {
  router.push({name: "notification"})
}

const solutionShowMore = () => {
  router.push({name: "solutions"})
}

</script>

<style scoped>
.cards-wrapper {
  display: flex;
  flex-direction: column;
}
.card {
  flex-grow: 1;
  margin-bottom: 20px;
}

.cards-wrapper .card:last-child {
  margin-bottom: 0;
}

</style>