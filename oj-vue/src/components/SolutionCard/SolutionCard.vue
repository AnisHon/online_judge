<template>
  <el-card class="solution-card" shadow="hover" @click="$emit('open', solution.solutionId)">
    <div class="solution-card__body">
      <button class="solution-card__author-link" type="button" @click.stop="openProfile"><avatar class="solution-card__avatar" :user-id="solution.userId" /><span class="solution-card__author">{{ solution.nikeName || '未设置昵称' }}</span></button>
      <div class="solution-card__main">
        <h2 class="solution-card__title">
          {{ solution.title }}
          <el-tag v-if="solution.topUp" type="warning" size="small">置顶</el-tag>
        </h2>
        <p class="solution-card__excerpt">{{ solution.content }}</p>
        <div class="solution-card__meta">
          <span><el-icon><Calendar /></el-icon>{{ solution.createTime }}</span>
          <span>{{ solution.private_ ? "私有" : "公开" }}</span>
          <span class="solution-card__problem">{{ solution.problemTitle }}</span>
        </div>
      </div>
      <el-icon class="solution-card__arrow"><ArrowRight /></el-icon>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import {ArrowRight, Calendar} from "@element-plus/icons-vue";
import Avatar from "@/components/Avatar/Avatar.vue";
import type {Solution} from "@/api/solution";
import {useRouter} from "vue-router";

const props = defineProps<{solution: Solution}>();
defineEmits<{(event: "open", id: Solution["solutionId"]): void}>();
const router = useRouter();
const openProfile = () => router.push({name: 'profile', params: {id: props.solution.userId}});
</script>

<style scoped>
.solution-card { cursor: pointer; border-radius: 16px; transition: transform .2s, border-color .2s, box-shadow .2s; }
.solution-card:hover { transform: translateY(-2px); border-color: var(--el-color-primary-light-5); }
.solution-card__body { display: flex; align-items: flex-start; gap: 15px; }.solution-card__author-link { display: flex; flex: 0 0 42px; flex-direction: column; align-items: center; gap: 5px; padding: 0; border: 0; color: var(--el-text-color-secondary); background: transparent; cursor: pointer; }.solution-card__author-link:hover { color: var(--el-color-primary); }.solution-card__avatar { margin-top: 3px; }.solution-card__author { max-width: 72px; overflow: hidden; font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.solution-card__main { min-width: 0; flex: 1; }.solution-card__title { display: flex; align-items: center; gap: 9px; margin: 5px 0 7px; color: var(--el-text-color-primary); font-size: 18px; line-height: 1.4; }.solution-card__excerpt { overflow: hidden; margin: 0 0 12px; color: var(--el-text-color-regular); white-space: nowrap; text-overflow: ellipsis; }.solution-card__meta { display: flex; flex-wrap: wrap; gap: 12px; color: var(--el-text-color-secondary); font-size: 12px; }.solution-card__meta span { display: inline-flex; align-items: center; gap: 4px; }.solution-card__problem { max-width: 220px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }.solution-card__arrow { align-self: center; color: var(--el-text-color-placeholder); transition: color .2s, transform .2s; }.solution-card:hover .solution-card__arrow { color: var(--el-color-primary); transform: translateX(3px); }
@media (max-width: 560px) { .solution-card__body { gap: 10px; }.solution-card__title { font-size: 16px; }.solution-card__meta { gap: 7px; }.solution-card__problem { max-width: 130px; } }
</style>
