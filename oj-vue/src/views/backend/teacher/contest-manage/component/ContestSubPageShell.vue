<template>
  <main class="contest-subpage">
    <section class="subpage-heading">
      <div class="subpage-heading__main">
        <el-button class="back-button" text :icon="ArrowLeft" aria-label="返回管理" @click="goBack" />
        <span class="subpage-icon" :class="`subpage-icon--${tone}`"><el-icon><component :is="icon" /></el-icon></span>
        <div>
          <p class="subpage-kicker">{{ kicker }}</p>
          <h1>{{ title }}</h1>
          <p>{{ description }}</p>
        </div>
      </div>
      <div class="subpage-actions"><slot name="actions" /></div>
    </section>

    <section v-if="stats.length" class="subpage-stats" aria-label="数据概览">
      <div v-for="stat in stats" :key="stat.label" class="subpage-stat">
        <span class="subpage-stat__label">{{ stat.label }}</span>
        <strong :class="`subpage-stat__value subpage-stat__value--${stat.tone || 'blue'}`">{{ stat.value }}</strong>
      </div>
    </section>

    <section class="subpage-content"><slot /></section>
  </main>
</template>

<script setup lang="ts">
import {computed, type Component} from "vue";
import {ArrowLeft} from "@element-plus/icons-vue";
import {useRoute, useRouter} from "vue-router";

withDefaults(defineProps<{
  title: string;
  kicker: string;
  description: string;
  icon: Component;
  tone?: 'blue' | 'amber' | 'violet' | 'green' | 'orange';
  stats?: Array<{label: string; value: string | number; tone?: string}>;
}>(), {tone: 'blue', stats: () => []});

const router = useRouter();
const route = useRoute();
const parentPath = computed(() => route.path.includes('/homework-manage/') ? '/backend/teacher/homework-manage' : '/backend/teacher/contest-manage');
const goBack = () => router.replace(parentPath.value);
</script>

<style scoped>
.contest-subpage { width: 100%; max-width: 1440px; margin: 0 auto; color: var(--el-text-color-primary); }.subpage-heading { display: flex; align-items: center; justify-content: space-between; gap: 22px; margin-bottom: 16px; }.subpage-heading__main { display: flex; align-items: center; min-width: 0; gap: 12px; }.back-button { flex: 0 0 auto; width: 30px; height: 30px; padding: 0; color: var(--el-text-color-secondary); }.subpage-icon { display: grid; width: 42px; height: 42px; flex: 0 0 auto; place-items: center; border-radius: 11px; font-size: 21px; }.subpage-icon--blue { color: #2563eb; background: rgb(37 99 235 / 12%); }.subpage-icon--amber { color: #d97706; background: rgb(217 119 6 / 12%); }.subpage-icon--violet { color: #7c3aed; background: rgb(124 58 237 / 12%); }.subpage-icon--green { color: #059669; background: rgb(5 150 105 / 12%); }.subpage-kicker { margin: 0 0 3px; color: var(--el-text-color-secondary); font-size: 10px; font-weight: 800; letter-spacing: .14em; }.subpage-heading h1 { margin: 0; font-size: 24px; letter-spacing: -.035em; }.subpage-heading p:last-child { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }.subpage-actions { display: flex; align-items: center; justify-content: flex-end; gap: 8px; flex-wrap: wrap; }.subpage-stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(170px, 1fr)); gap: 12px; margin-bottom: 16px; }.subpage-stat, .subpage-content { border: 1px solid var(--el-border-color-lighter); border-radius: 11px; background: var(--el-bg-color); box-shadow: 0 4px 18px rgb(15 23 42 / 4%); }.subpage-stat { display: flex; align-items: center; justify-content: space-between; min-height: 64px; padding: 12px 16px; }.subpage-stat__label { color: var(--el-text-color-secondary); font-size: 12px; }.subpage-stat__value { font-size: 22px; letter-spacing: -.03em; }.subpage-stat__value--blue { color: var(--el-color-primary); }.subpage-stat__value--green { color: var(--el-color-success); }.subpage-stat__value--amber { color: var(--el-color-warning); }.subpage-stat__value--violet { color: #8b5cf6; }.subpage-stat__value--orange { color: #f97316; }.subpage-content { min-width: 0; padding: 18px 20px 12px; overflow: hidden; }.subpage-content :deep(.el-table__cell) { padding: 11px 0; }.subpage-content :deep(.el-table__header-wrapper th) { color: var(--el-text-color-secondary); font-size: 12px; background: var(--el-fill-color-light); }.subpage-content :deep(.el-table__body-wrapper) { overflow-x: auto; }
@media (max-width: 800px) { .subpage-heading { align-items: flex-start; flex-direction: column; }.subpage-actions { width: 100%; justify-content: flex-start; }.subpage-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 520px) { .subpage-heading__main { align-items: flex-start; }.subpage-icon { width: 36px; height: 36px; font-size: 18px; }.subpage-heading h1 { font-size: 21px; }.subpage-content { padding: 14px; }.subpage-stats { gap: 8px; }.subpage-stat { padding: 10px 12px; }.subpage-stat__value { font-size: 18px; } }
</style>
