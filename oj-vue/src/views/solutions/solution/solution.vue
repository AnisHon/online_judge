<template>
  <main class="common-max-width-page app-container">
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
    <article class="solution-article">
      <div v-if="solution">

          <div class="author">
            <div class="author-avatar">
              <avatar :user-id="solution.userId" shape="circle"/>
<!--              <el-avatar class="portrait" :src="getAvatarPath(solution.userId)"/>-->
            </div>

            <div class="author-info">
              <div>
                <el-text size="large">{{ solution.nikeName }}</el-text>
              </div>

              <div>
                <el-text type="info">
                  <el-icon><Calendar/></el-icon> 发布于 {{ solution.createTime }}
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
    </article>
    <el-divider/>
    <div class="solution-content">
      <MarkdownPreview v-if="solution" :text="solution.content" />
      <el-skeleton v-else :count="10" >
        <el-skeleton-item variant="p"/>
      </el-skeleton>
    </div>

  </main>
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
import Avatar from "@/components/Avatar/Avatar.vue";

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
  padding-bottom: 30px;
}
.solution-article { margin-top: 18px; padding: 28px 34px 34px; border: 1px solid var(--el-border-color-light); border-radius: 18px; background: var(--el-bg-color); }.author { display: flex; align-items: center; gap: 12px; }.author-avatar { flex: 0 0 auto; }.author-info { flex: 1; }.footer { margin-top: 18px; padding-top: 16px; border-top: 1px solid var(--el-border-color-lighter); }.solution-content { margin-top: 8px;}.solution-content :deep(.md-editor-preview) { background: transparent; color: var(--el-text-color-primary); }
@media (max-width: 600px) { .solution-article { margin-top: 12px; padding: 20px 16px 24px; }.solution-article :deep(.el-page-header__content) { max-width: 170px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }.solution-article :deep(.el-page-header__extra) { display: flex; gap: 6px; }.solution-article :deep(.el-page-header__extra .el-button) { padding: 7px 9px; }.solution-article :deep(.el-page-header__extra .el-button) { font-size: 0; }.solution-article :deep(.el-page-header__extra .el-button .el-icon) { margin: 0; font-size: 16px; } }

</style>
