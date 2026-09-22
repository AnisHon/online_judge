<template>
  <main class="my-class-page">
    <section class="my-class-card">
      <div class="my-class-icon"><el-icon><School /></el-icon></div>
      <div>
        <p class="eyebrow">TEACHING / CLASSES</p>
        <h1>我的班级</h1>
        <p>班级管理入口正在整理中。你仍然可以从用户模块的班级管理进入已开放的班级功能。</p>
      </div>
      <el-button type="primary" plain @click="goClassManage">前往班级管理</el-button>
    </section>
  </main>
</template>

<script setup lang="ts">
import {School} from '@element-plus/icons-vue';
import {useRouter} from 'vue-router';

const router = useRouter();
const goClassManage = () => router.push('/backend/user-module/class-manage');
</script>

<style scoped>
.my-class-page { width: 100%; max-width: 1120px; margin: 0 auto; }.my-class-card { display: flex; align-items: center; gap: 18px; padding: 28px; border: 1px solid var(--el-border-color-lighter); border-radius: 18px; background: var(--el-bg-color); box-shadow: 0 12px 36px rgb(15 23 42 / 5%); }.my-class-icon { display: grid; width: 48px; height: 48px; flex: 0 0 auto; place-items: center; border-radius: 14px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 23px; }.my-class-card > div:nth-child(2) { min-width: 0; flex: 1; }.eyebrow { margin: 0 0 5px; color: var(--el-text-color-secondary); font-size: 10px; font-weight: 800; letter-spacing: .14em; }.my-class-card h1 { margin: 0; font-size: 22px; }.my-class-card p:last-child { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 13px; line-height: 1.7; }
@media (max-width: 620px) { .my-class-card { align-items: flex-start; flex-wrap: wrap; padding: 20px; }.my-class-card > .el-button { margin-left: 66px; } }
</style>
