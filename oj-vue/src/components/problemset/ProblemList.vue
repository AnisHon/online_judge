<template>
  <section class="problem-list" aria-live="polite">
    <el-skeleton v-if="state === 'loading'" :rows="6" animated />
    <el-alert v-else-if="state === 'error'" type="error" title="题目列表加载失败" show-icon>
      <template #default>
        <el-button link type="primary" @click="loadProblems">重新加载</el-button>
      </template>
    </el-alert>
    <el-empty v-else-if="!problems.length" description="没有找到符合条件的题目" />
    <el-table v-else class="problem-table" :data="problems" stripe table-layout="fixed" style="width: 100%">
      <el-table-column label="状态" width="62" align="center">
        <template #default="{row}">
          <el-tooltip v-if="row.finish" content="已完成" placement="top">
            <el-icon color="var(--el-color-success)" aria-label="已完成"><CircleCheck /></el-icon>
          </el-tooltip>
          <span v-else class="not-finished" aria-label="未完成">·</span>
        </template>
      </el-table-column>
      <el-table-column label="题目" min-width="260">
        <template #default="{row}">
          <router-link target="_blank" :to="{name: 'problem', params: {id: row.id}}" class="problem-title">
            <strong>{{ row.title }}</strong>
            <span>{{ row.source || '题库' }}</span>
          </router-link>
        </template>
      </el-table-column>
      <el-table-column label="题目 ID" width="190" show-overflow-tooltip>
        <template #default="{row}"><code class="problem-id">{{ shortId(row.id) }}</code></template>
      </el-table-column>
      <el-table-column label="类型" width="100">
        <template #default="{row}"><el-tag :type="row.typeTone" effect="plain">{{ row.type }}</el-tag></template>
      </el-table-column>
      <el-table-column label="标签" min-width="180" show-overflow-tooltip>
        <template #default="{row}">
          <div class="tag-list">
            <el-tag v-for="item in row.tag.slice(0, 3)" :key="String(item.tagId)" :color="item.tagColor" effect="dark">
              {{ item.tagName }}
            </el-tag>
            <span v-if="row.tag.length > 3" class="more-tags">+{{ row.tag.length - 3 }}</span>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<script lang="ts" setup>
import {inject, onBeforeUnmount, ref, watch} from 'vue';
import {getProblems, type ProblemParam, type TaggedProblemView} from '@/api/problem';
import type {TagView} from '@/api/problem/label';
import {getProblemTypeMeta} from '@/utils/problem';
import {CircleCheck} from '@element-plus/icons-vue';
import debounce from 'lodash/debounce';
import type {IdType} from '@/api/common.ts';

interface ProblemTableView {
  id: IdType;
  type: string;
  typeTone: 'primary' | 'success' | 'warning' | 'info' | 'danger';
  title: string;
  tag: TagView[];
  source: string;
  finish: boolean;
}

const props = defineProps<{param: ProblemParam}>();
const emit = defineEmits<{(event: 'loadFinish', currentPage: number, pageSize: number, totalRecords: number): void}>();
const elMain = inject<{elMainRef?: {value?: {$el?: HTMLElement}}}>('elMain');
const problems = ref<ProblemTableView[]>([]);
const state = ref<'loading' | 'ready' | 'error'>('loading');
let requestSequence = 0;

const shortId = (id: IdType) => {
  const value = String(id);
  return value.length > 18 ? `${value.slice(0, 8)}…${value.slice(-6)}` : value;
};

const normalizeProblem = (item: TaggedProblemView): ProblemTableView => {
  const meta = getProblemTypeMeta(item?.type);
  return {
    id: String(item?.problemId || ''),
    type: meta.label,
    typeTone: meta.tone,
    title: item?.title || '未命名题目',
    tag: Array.isArray(item?.tags) ? item.tags.filter(Boolean) : [],
    source: item?.source || '',
    finish: item?.finish === true,
  };
};

const loadProblems = async () => {
  const sequence = ++requestSequence;
  state.value = 'loading';
  try {
    const result = await getProblems({...props.param, tagIds: props.param.tagIds ? [...props.param.tagIds] : []});
    if (sequence !== requestSequence) return;
    problems.value = result.data.map(normalizeProblem);
    state.value = 'ready';
    emit('loadFinish', result.currentPage, result.pageSize, result.totalRecords);
    const target = elMain?.elMainRef?.value?.$el;
    target?.scrollTo?.({top: 0, behavior: 'smooth'});
  } catch {
    if (sequence !== requestSequence) return;
    problems.value = [];
    state.value = 'error';
    emit('loadFinish', props.param.currentPage, props.param.pageSize, 0);
  }
};

const debouncedLoad = debounce(loadProblems, 280);
watch(() => props.param, () => debouncedLoad(), {immediate: true, deep: true});
onBeforeUnmount(() => {
  requestSequence++;
  debouncedLoad.cancel();
});
</script>

<style scoped>
.problem-list { min-height: 260px; }
.problem-table :deep(.el-table__row) { transition: background-color .2s; }
.problem-table :deep(.el-table__row:hover) { cursor: pointer; }
.problem-table :deep(.el-table__cell) { padding: 12px 8px; }
.problem-table :deep(.el-table__inner-wrapper::before) { display: none; }
.problem-title { display: flex; min-width: 0; flex-direction: column; gap: 4px; color: var(--el-color-primary); text-decoration: none; }
.problem-title strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.problem-title span { overflow: hidden; color: var(--el-text-color-secondary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }
.problem-id { color: var(--el-text-color-secondary); font: 12px/1.4 var(--code-font-family, monospace); }
.not-finished { color: var(--el-text-color-placeholder); font-size: 20px; }
.tag-list { display: flex; min-width: 0; flex-wrap: wrap; gap: 5px; align-items: center; }
.tag-list :deep(.el-tag) { max-width: 110px; overflow: hidden; border: 0; text-overflow: ellipsis; white-space: nowrap; }
.more-tags { color: var(--el-text-color-secondary); font-size: 12px; }
@media (max-width: 700px) {
  .problem-table :deep(.el-table__cell) { padding: 10px 5px; }
  .problem-table :deep(.el-table__body-wrapper) { overflow-x: auto; }
  .problem-table :deep(.el-table__header-wrapper), .problem-table :deep(.el-table__body-wrapper) { min-width: 720px; }
}
</style>
