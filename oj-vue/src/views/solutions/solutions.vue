<template>
  <div class="common-max-width-page app-container">
    <header class="solutions-header">
      <div><p class="eyebrow">KNOWLEDGE BASE</p><h1>题解广场</h1><p>阅读方法、分享思路，把每一次提交沉淀成可复用的经验。</p></div>
      <el-icon class="header-icon" aria-hidden="true"><EditPen /></el-icon>
    </header>
    <div class="content">
      <solutions-component :param="param"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import SolutionsComponent from "@/views/solutions/component/SolutionsComponent/SolutionsComponent.vue";
import {reactive} from "vue";
import type {QuerySolution} from "@/api/solution";
import {EditPen} from "@element-plus/icons-vue";


const param = reactive<QuerySolution>({
  asc: true,
  pageSize: 20,
  currentPage: 1,
  userId: undefined,
  problemId: undefined,
})

</script>

<style scoped>
.app-container {
  margin: auto;
}
.solutions-header { position: relative; display: flex; justify-content: space-between; align-items: center; min-height: 132px; margin-bottom: 18px; padding: 24px 32px; overflow: hidden; border: 1px solid var(--el-border-color-light); border-radius: 18px; background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-bg-color)); color: var(--el-text-color-primary); }.eyebrow { margin: 0 0 7px; color: var(--el-color-primary); font-size: 11px; font-weight: 800; letter-spacing: .18em; }.solutions-header h1 { margin: 0; font-size: 30px; letter-spacing: -.04em; }.solutions-header p:last-child { margin: 9px 0 0; color: var(--el-text-color-secondary); }.header-icon { margin-right: 6%; color: var(--el-color-primary-light-5); font-size: 82px; transform: rotate(-12deg); opacity: .45; }
@media (max-width: 560px) { .solutions-header { min-height: 110px; padding: 20px; }.solutions-header h1 { font-size: 25px; }.solutions-header p:last-child { font-size: 13px; }.header-icon { display: none; } }
</style>
