<template>
  <div class="app-container">
    <el-card class="card" shadow="hover" >
      <div class="share">
         <span>
        <span>
          <el-icon size="24" style="margin: 0 5px">
            <DocumentAdd/>
          </el-icon>
          <el-text type="info">
            分享你的题解
          </el-text>
        </span>

        <el-button type="success" @click="addSolution">
          发布题解
        </el-button>

      </span>
      </div>

    </el-card>
    <el-empty v-if="total == 0" description="还没有人发题解"/>
    <el-card
        class="solution-card"
        body-class="solution-card-body"
        shadow="hover"
        v-for="item of solutions"
        :key="item.solutionId"
        @click="detailSolution(item.solutionId)"
    >
      <el-row :gutter="20">
        <el-col :span="2">
          <el-avatar class="portrait" :src="getAvatarPath(item.userId)" />
        </el-col>

        <el-col class="main-content" :span=22>
          <div class="author">
            <el-text type="info">{{ item.nikeName }}</el-text>
          </div>

          <div>
            <span class="solution-title">
              {{ item.title }}
              <el-tag v-if="item.topUp" type="warning">置顶</el-tag>
            </span>
          </div>

          <div class="content">
            <el-text line-clamp="1" type="info">{{ item.content }}</el-text>
          </div>
          <div class="footer">
            <el-space>
              <el-tag type="info">
                发布日期: {{ item.createTime }}
              </el-tag>

              <el-tag type="info">
                {{ item.private_ ? "私有" : "公开" }}
              </el-tag>

              <el-tag type="info">
                题目: {{ item.problemTitle }}
              </el-tag>
            </el-space>
          </div>
        </el-col>

      </el-row>
    </el-card>

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
import {getAvatarPath} from "@/api/file";

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
const detailSolution = (solutionId: number) => {
  router.push({name: "solution", params: {id: solutionId}});
}

getList();

</script>

<style scoped>
.portrait {
  margin: 5px;
}
.content {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis
}

.main-content div {
  margin: 5px 0;
}

.card,
.solution-card {
  margin: 10px 5px;
}

.share>span {
  display: flex;
  justify-content: space-between;
}

.share>span>span {
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>

<style>
.solution-card-body:hover {
  cursor: pointer;
}


</style>