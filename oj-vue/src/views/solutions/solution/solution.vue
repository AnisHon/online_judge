<template>
  <div class="common-max-width-page app-container">
    <el-page-header @back="router.back" title="返回" :content="solution?.title || ''">
      <template #extra>
        <el-button
            v-if="isUserIdEqual(solution?.userId)"
            icon="delete"
            type="warning"
            @click="handleDelete"
            :loading="isLoading"
        >删除题解</el-button>
        <el-button
            v-if="isUserIdEqual(solution?.userId)"
            icon="edit"
            type="success"
            @click="handleEdit"
        >修改题解</el-button>
      </template>
    </el-page-header>
    <div class="header">
      <div v-if="solution">

          <div class="author">
            <div style="margin-right: 5px">
              <el-avatar class="portrait" :src="getAvatarPath(solution.userId)"/>
            </div>

            <div style="flex-grow: 1">
              <div>
                <el-text size="large">{{ solution.nikeName }}</el-text>
              </div>

              <div>
                <el-text type="info">
                  <el-icon>
                    <Calendar/>

                  </el-icon>
                  发布日期: {{ solution.createTime }}
                </el-text>
              </div>

            </div>

          </div>

          <div class="footer">
            <el-space>
              <el-tag v-if="solution.topUp" type="warning">置顶</el-tag>

              <el-tag type="info">
                {{ solution.private_ ? "私有" : "公开" }}
              </el-tag>

              <el-tag type="info">
                题目: {{ solution.problemTitle }}
              </el-tag>
            </el-space>

          </div>


      </div>
      <div v-else>
        <el-skeleton animated :count="1">
          <el-skeleton-item variant="h3"/>
        </el-skeleton>
        <div class="author">
          <div style="margin-right: 5px">
            <el-skeleton animated :count="1">
              <el-skeleton-item variant="circle"/>
            </el-skeleton>
          </div>

          <div style="flex-grow: 1">
            <div>
              <el-skeleton animated :count="1">
                <el-skeleton-item variant="text"/>
              </el-skeleton>
            </div>

            <div>

              <el-skeleton animated :count="1">
                <el-skeleton-item variant="text"/>
              </el-skeleton>

            </div>

          </div>

        </div>

        <div class="footer">
          <el-skeleton animated :count="1">
            <el-skeleton-item variant="text"/>
          </el-skeleton>
        </div>
      </div>
    </div>
    <el-divider/>
    <div class="content">
      <MarkdownPreview v-if="solution" :text="solution.content" />
      <el-skeleton v-else :count="10" >
        <el-skeleton-item variant="p"/>
      </el-skeleton>
    </div>

  </div>
</template>

<script setup lang="ts">
import {ref} from "vue";
import {debouncedDeleteSolution, getSolution, type Solution} from "@/api/solution";
import {useRoute, useRouter} from "vue-router";
import {Calendar} from "@element-plus/icons-vue";
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {isUserIdEqual} from "@/utils/authUtil.ts";
import {ElMessageBox, ElNotification} from "element-plus";
import useLoading from "@/hooks/useLoading.ts";
import {getAvatarPath} from "@/api/file";

const router = useRouter();

const route = useRoute();

const solution = ref<Solution>();

const {loading, isLoading, finish} = useLoading();

const deleteSolution = debouncedDeleteSolution(finish);

const loadSolution = async () => {
  solution.value = await getSolution(<string>route.params.id);
  if (!solution.value) {
    ElNotification.error("不存在")
  }
}

const handleEdit = () => {
  router.push({name: "solution_edit", query: {solutionId: solution?.value?.solutionId}})
}

const handleDelete = () => {
  ElMessageBox.confirm(`您确定要删除自己的题解吗?`, {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(() => {
    if (solution?.value?.solutionId) {
      loading();
      deleteSolution(solution.value.solutionId);
    }
    router.back();
  })

}

loadSolution();

</script>

<style scoped>
.app-container {
  margin: auto;
}

.header {
  margin: 5px 0;
}

.author {
  display: flex;
  align-items: center;

}

</style>