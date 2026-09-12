<template>
  <main class="problem-module-shell">
    <section class="module-heading">
      <div class="module-heading__main">
        <el-button class="back-button" text :icon="ArrowLeft" aria-label="返回" @click="router.back()" />
        <span class="module-icon" :class="`module-icon--${tone}`"><el-icon><component :is="icon" /></el-icon></span>
        <div>
          <p class="module-kicker">{{ kicker }}</p>
          <h1>{{ title }}</h1>
          <p>{{ description }}</p>
        </div>
      </div>
      <div class="module-actions"><slot name="actions" /></div>
    </section>
    <section class="module-content"><slot /></section>
  </main>
</template>

<script setup lang="ts">
import type { Component } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

withDefaults(defineProps<{
  title: string
  kicker: string
  description: string
  icon: Component
  tone?: 'blue' | 'amber' | 'violet' | 'green'
}>(), { tone: 'blue' })

const router = useRouter()
</script>

<style scoped>
.problem-module-shell { width: 100%; max-width: 1440px; margin: 0 auto; color: var(--el-text-color-primary); }
.module-heading { display: flex; align-items: center; justify-content: space-between; gap: 22px; margin-bottom: 16px; }
.module-heading__main { display: flex; align-items: center; min-width: 0; gap: 12px; }
.back-button { flex: 0 0 auto; width: 30px; height: 30px; padding: 0; color: var(--el-text-color-secondary); }
.module-icon { display: grid; width: 42px; height: 42px; flex: 0 0 auto; place-items: center; border-radius: 11px; font-size: 21px; }
.module-icon--blue { color: #2563eb; background: rgb(37 99 235 / 12%); }.module-icon--amber { color: #d97706; background: rgb(217 119 6 / 12%); }.module-icon--violet { color: #7c3aed; background: rgb(124 58 237 / 12%); }.module-icon--green { color: #059669; background: rgb(5 150 105 / 12%); }
.module-kicker { margin: 0 0 3px; color: var(--el-text-color-secondary); font-size: 10px; font-weight: 800; letter-spacing: .14em; }
.module-heading h1 { margin: 0; font-size: 24px; letter-spacing: -.035em; }.module-heading p:last-child { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }
.module-actions { display: flex; align-items: center; justify-content: flex-end; gap: 8px; flex-wrap: wrap; }.module-content { min-width: 0; padding: 18px 20px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 16px; background: var(--el-bg-color); box-shadow: 0 12px 34px rgb(15 23 42 / 4%); }.module-content :deep(.el-table__header th.el-table__cell) { color: var(--el-text-color-secondary); font-size: 12px; font-weight: 700; background: var(--el-fill-color-light); }.module-content :deep(.el-table__row td.el-table__cell) { height: 64px; }
@media (max-width: 800px) { .module-heading { align-items: flex-start; flex-direction: column; }.module-actions { width: 100%; justify-content: flex-start; } }
@media (max-width: 520px) { .module-heading__main { align-items: flex-start; }.module-icon { width: 36px; height: 36px; font-size: 18px; }.module-heading h1 { font-size: 21px; }.module-content { padding: 14px; } }
</style>
