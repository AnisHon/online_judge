<template>
  <section class="problem-list" :aria-busy="isLoading" aria-live="polite">
    <div v-if="isRefreshing" class="problem-list__progress" aria-label="正在更新题目列表"><span /></div>
    <el-alert v-if="refreshFailed" class="refresh-warning" type="warning" :closable="false" show-icon title="刷新失败，仍显示上一次结果">
      <template #default><el-button link type="primary" @click="loadProblems">重试</el-button></template>
    </el-alert>
    <el-skeleton v-if="state === 'loading' && !hasLoaded" :rows="6" animated/>
    <el-alert v-else-if="state === 'error'" type="error" title="题目列表加载失败" show-icon>
      <template #default>
        <el-button link type="primary" @click="loadProblems">重新加载</el-button>
      </template>
    </el-alert>
    <div v-else-if="hasLoaded" class="problem-list__results" :class="{ 'is-updating': isRefreshing }">
    <el-empty v-if="!problems.length" description="没有找到符合条件的题目"/>
    <el-table v-else class="problem-table" :data="problems" stripe table-layout="fixed" style="width: 100%">
      <el-table-column label="状态" width="70" align="center">
        <template #default="{row}">
          <el-tooltip v-if="row.finish" content="已完成" placement="top">
            <el-icon color="var(--el-color-success)" aria-label="已完成">
              <CircleCheck/>
            </el-icon>
          </el-tooltip>
          <span v-else class="not-finished" aria-label="未完成">·</span>
        </template>
      </el-table-column>
      <el-table-column label="题目" min-width="260">
        <template #default="{row}">
          <!-- 题目按原有产品设计在新页面打开，便于保留题库筛选上下文。 -->
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
        <template #default="{row}">
          <el-tag :type="row.typeTone" effect="plain">{{ row.type }}</el-tag>
        </template>
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
    </div>
  </section>
</template>

<script lang="ts" setup>
import {inject, onBeforeUnmount, ref, watch} from 'vue';
import {getProblems, type ProblemParam, type TaggedProblemView} from '@/api/problem';
import type {TagView} from '@/api/problem/label';
import {getProblemTypeMeta} from '@/utils/problem';
import {CircleCheck} from '@element-plus/icons-vue';
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

const props = defineProps<{ param: ProblemParam }>();
const emit = defineEmits<{
  (event: 'loadFinish', currentPage: number, pageSize: number, totalRecords: number): void
}>();
const elMain = inject<{ elMainRef?: { value?: { $el?: HTMLElement } } }>('elMain');
const problems = ref<ProblemTableView[]>([]);
const state = ref<'loading' | 'ready' | 'error'>('loading');
const isLoading = ref(false);
const hasLoaded = ref(false);
const refreshFailed = ref(false);
const isRefreshing = ref(false);
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
  const refreshing = hasLoaded.value;
  isLoading.value = true;
  isRefreshing.value = refreshing;
  refreshFailed.value = false;
  if (!refreshing) state.value = 'loading';
  if (refreshing) {
    const target = elMain?.elMainRef?.value?.$el;
    target?.scrollTo?.({top: 0, behavior: 'smooth'});
  }
  try {
    const result = await getProblems({...props.param, tagIds: props.param.tagIds ? [...props.param.tagIds] : []});
    if (sequence !== requestSequence) return;
    problems.value = result.data.map(normalizeProblem);
    state.value = 'ready';
    hasLoaded.value = true;
    emit('loadFinish', result.currentPage, result.pageSize, result.totalRecords);
  } catch {
    if (sequence !== requestSequence) return;
    if (refreshing) {
      refreshFailed.value = true;
    } else {
      problems.value = [];
      state.value = 'error';
      emit('loadFinish', props.param.currentPage, props.param.pageSize, 0);
    }
  } finally {
    if (sequence === requestSequence) {
      isLoading.value = false;
      isRefreshing.value = false;
    }
  }
};

watch(() => props.param, () => void loadProblems(), {immediate: true, deep: true});
onBeforeUnmount(() => {
  requestSequence++;
});
</script>

<style scoped>
.problem-list {
  position: relative;
  min-height: 260px;
}

.problem-list__progress { position: absolute; z-index: var(--oj-z-panel-control); top: 0; left: 0; width: 100%; height: 3px; overflow: hidden; border-radius: 99px; background: var(--el-fill-color-light); }
.problem-list__progress span { display: block; width: 32%; height: 100%; border-radius: inherit; background: linear-gradient(90deg, var(--el-color-primary-light-5), var(--el-color-primary)); animation: problem-loading-slide 1.15s cubic-bezier(.45, 0, .55, 1) infinite; }
.problem-list__results { transition: opacity .22s ease, transform .22s ease; transform-origin: top center; }
.problem-list__results.is-updating { opacity: .48; transform: translateY(3px); pointer-events: none; }
.refresh-warning { margin-bottom: 10px; }
@keyframes problem-loading-slide { from { transform: translateX(-110%); } to { transform: translateX(330%); } }

.problem-table :deep(.el-table__row) {
  transition: background-color .2s;
}

.problem-table :deep(.el-table__row:hover) {
  cursor: pointer;
}

.problem-table :deep(.el-table__cell) {
  padding: 12px 8px;
}

.problem-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.problem-title {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
  color: var(--el-color-primary);
  text-decoration: none;
}

.problem-title strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.problem-title span {
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.problem-id {
  color: var(--el-text-color-secondary);
  font: 12px/1.4 var(--code-font-family, monospace);
}

.not-finished {
  color: var(--el-text-color-placeholder);
  font-size: 20px;
}

.tag-list {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 5px;
  align-items: center;
}

.tag-list :deep(.el-tag) {
  max-width: 110px;
  overflow: hidden;
  border: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.more-tags {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

@media (max-width: 700px) {
  .problem-table :deep(.el-table__cell) {
    padding: 10px 5px;
  }

  .problem-table :deep(.el-table__body-wrapper) {
    overflow-x: auto;
  }

  .problem-table :deep(.el-table__header-wrapper), .problem-table :deep(.el-table__body-wrapper) {
    min-width: 720px;
  }
}
</style>
