

<template>
  <el-card class="custom-card" shadow="hover">
    <template #header>
      <slot name="header" >
        <div class="custom-card__header">
          <h3 class="custom-card__title">
            <span class="custom-card__icon"><el-icon v-if="icon"><component :is="icon"/></el-icon></span>
            <span>{{title}}</span>
          </h3>
          <button v-if="more" class="custom-card__more" type="button" @click="emit('showMore')">
            <span>查看全部</span><el-icon><ArrowRight/></el-icon>
          </button>
        </div>

      </slot>
    </template>

    <template #default>
      <slot></slot>
    </template>
  </el-card>
</template>

<script setup lang="ts">

import {ArrowRight} from "@element-plus/icons-vue";

const {icon, title, more = false} = defineProps<{
  icon?: string,
  title: string,
  more?: boolean
}>()

const emit = defineEmits<{
  (e: 'showMore'): void
}>()

</script>


<style scoped>
.custom-card { border: 1px solid var(--el-border-color-light); border-radius: 16px; background: var(--el-bg-color); }
.custom-card :deep(.el-card__header) { padding: 17px 20px 14px; border-bottom: 1px solid var(--el-border-color-lighter); }
.custom-card :deep(.el-card__body) { padding: 0 20px 14px; }
.custom-card__header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.custom-card__title { display: flex; min-width: 0; align-items: center; gap: 9px; margin: 0; color: var(--el-text-color-primary); font-size: 15px; font-weight: 700; }
.custom-card__icon { display: grid; width: 28px; height: 28px; flex: 0 0 auto; place-items: center; border-radius: 9px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 15px; }
.custom-card__more { display: inline-flex; align-items: center; gap: 3px; padding: 5px 0 5px 8px; border: 0; color: var(--el-text-color-secondary); background: transparent; font-size: 11px; cursor: pointer; transition: color .18s ease, transform .18s ease; }
.custom-card__more:hover { color: var(--el-color-primary); transform: translateX(2px); }
@media (max-width: 560px) { .custom-card :deep(.el-card__header) { padding: 15px 14px 12px; }.custom-card :deep(.el-card__body) { padding: 0 14px 12px; } }
</style>
