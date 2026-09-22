<template>
  <div class="problem-filter">
    <el-form :inline="true" :model="queryForm" class="search-row" @submit.prevent="handleQuery">
      <el-form-item label="搜索">
        <el-select v-model="select" class="search-type" @change="onSelectChange">
          <el-option value="1" label="标题" />
          <el-option value="2" label="ID" />
        </el-select>
      </el-form-item>
      <el-form-item class="search-input">
        <el-input v-model="searchInput" placeholder="搜索题目" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="onResetHandler">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="filter-row">
      <div class="type-filter">
        <span class="filter-label">题目类型</span>
        <el-radio-group v-model="queryForm.type" @change="handleQuery">
          <el-radio :value="undefined">全部</el-radio>
          <el-radio :value="ProblemType.OJ">OJ</el-radio>
          <el-radio :value="ProblemType.FILL">填空</el-radio>
          <el-radio :value="ProblemType.CHOICE">选择</el-radio>
          <el-radio :value="ProblemType.MULTI_CHOICE">多选</el-radio>
        </el-radio-group>
      </div>
      <div class="tag-filter">
        <span class="filter-label">选中标签</span>
        <div v-if="queryForm.tagIds.length" class="selected-tags">
          <el-tag v-for="tagId in queryForm.tagIds" :key="String(tagId)" :color="tagFor(tagId).tagColor">
            <span class="tag-text">{{ tagFor(tagId).tagName }}</span>
          </el-tag>
        </div>
        <span v-else class="filter-empty">未选择</span>
      </div>
      <el-button class="tag-action" link type="primary" @click="handleChooseTag">
        选择标签<span v-if="queryForm.tagIds.length">（{{ queryForm.tagIds.length }}）</span>
      </el-button>
    </div>

    <el-dialog v-model="tagDialogVisible" class="tag-dialog" title="选择标签" width="min(680px, calc(100vw - 32px))"
               append-to-body destroy-on-close>
      <div class="tag-dialog-toolbar">
        <span>按知识点筛选题目，可多选</span>
        <el-button v-if="dialogTagIds.length" link type="primary" @click="dialogTagIds = []">清空选择</el-button>
      </div>
      <div v-if="tagsLoading" class="tag-dialog-state"><el-skeleton :rows="3" animated /></div>
      <el-alert v-else-if="tagsError" type="error" title="标签加载失败" show-icon>
        <template #default>
          <el-button link type="primary" @click="loadTags">重新加载</el-button>
        </template>
      </el-alert>
      <div v-else-if="tags.length" class="tag-grid" role="list">
        <button
          v-for="tag in tags"
          :key="String(tag.tagId)"
          type="button"
          class="tag-option"
          :class="{ 'is-selected': isDialogTagSelected(tag.tagId) }"
          :style="{ '--tag-color': tag.tagColor || '#64748b' }"
          role="listitem"
          :aria-pressed="isDialogTagSelected(tag.tagId)"
          @click="handleCheckTag(tag.tagId)"
        >
          <span class="tag-option__dot" aria-hidden="true" />
          <span class="tag-option__name">{{ tag.tagName }}</span>
          <el-icon v-if="isDialogTagSelected(tag.tagId)" class="tag-option__check"><Check /></el-icon>
        </button>
      </div>
      <el-empty v-else description="暂无标签" />
      <template #footer>
        <el-button @click="cancelTagSelection">取消</el-button>
        <el-button type="primary" @click="applyTagSelection">应用筛选</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue';
import {Check} from '@element-plus/icons-vue';
import {getAllTags, type TagView} from '@/api/problem/label';
import {ProblemType} from '@/api/problem';
import type {IdType} from '@/api/common.ts';

interface ProblemQueryForm {
  id: IdType;
  tagIds: IdType[];
  title: string;
  type?: ProblemType;
}

const tags = ref<TagView[]>([]);
const tagsLoading = ref(false);
const tagsError = ref(false);
const tagDialogVisible = ref(false);
const dialogTagIds = ref<IdType[]>([]);
const select = ref<'1' | '2'>('1');
const queryForm = reactive<ProblemQueryForm>({id: '', tagIds: [], title: '', type: undefined});

const emit = defineEmits<{(event: 'query', value: ProblemQueryForm): void}>();

const searchInput = computed({
  get: () => select.value === '1' ? queryForm.title : queryForm.id,
  set: (value: string) => {
    if (select.value === '1') queryForm.title = value;
    else queryForm.id = value;
  },
});

const unknownTag = (id: IdType): TagView => ({tagId: id, tagName: `标签 ${id}`, tagColor: '#909399', createTime: new Date()});
const tagFor = (id: IdType) => tags.value.find(tag => String(tag.tagId) === String(id)) || unknownTag(id);

const emitQuery = () => emit('query', {
  id: queryForm.id,
  title: queryForm.title,
  type: queryForm.type,
  tagIds: [...queryForm.tagIds],
});

const handleQuery = () => emitQuery();
const onSelectChange = () => {
  queryForm.id = '';
  queryForm.title = '';
};
const onResetHandler = () => {
  queryForm.id = '';
  queryForm.title = '';
  queryForm.tagIds = [];
  queryForm.type = undefined;
  select.value = '1';
  emitQuery();
};
const handleChooseTag = () => {
  dialogTagIds.value = [...queryForm.tagIds];
  tagDialogVisible.value = true;
  if (!tags.value.length && !tagsLoading.value) void loadTags();
};
const handleCheckTag = (id: IdType) => {
  const index = dialogTagIds.value.findIndex(item => String(item) === String(id));
  if (index === -1) dialogTagIds.value.push(id);
  else dialogTagIds.value.splice(index, 1);
};
const isDialogTagSelected = (id: IdType) => dialogTagIds.value.some(item => String(item) === String(id));
const cancelTagSelection = () => { tagDialogVisible.value = false; };
const applyTagSelection = () => {
  queryForm.tagIds = [...dialogTagIds.value];
  tagDialogVisible.value = false;
  handleQuery();
};
const loadTags = async () => {
  tagsLoading.value = true;
  tagsError.value = false;
  try {
    tags.value = await getAllTags();
  } catch {
    tagsError.value = true;
  } finally {
    tagsLoading.value = false;
  }
};

onMounted(() => { void loadTags(); });
</script>

<style scoped>
.problem-filter { width: 100%; }
.search-row { display: flex; justify-content: center; margin: 0 0 18px; }
.search-type { width: 100px; }
.search-input { width: min(360px, 35%); }
.filter-row { display: grid; grid-template-columns: minmax(260px, 1fr) minmax(220px, 1.5fr) auto; align-items: center; gap: 18px; }
.type-filter, .tag-filter { min-width: 0; }
.type-filter { white-space: nowrap; }
.filter-label { display: inline-block; margin-right: 8px; color: var(--el-text-color-secondary); font-size: 13px; }
.selected-tags { display: inline-flex; flex-wrap: wrap; gap: 5px; vertical-align: middle; }
.selected-tags :deep(.el-tag) { border: 0; }
.tag-text { color: #fff; }
.filter-empty { color: var(--el-text-color-placeholder); font-size: 13px; }
.tag-action { justify-self: end; }
.tag-dialog-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: -4px 0 14px; color: var(--el-text-color-secondary); font-size: 12px; }
.tag-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(145px, 1fr)); gap: 10px; max-height: min(52vh, 460px); overflow-y: auto; padding: 3px; }
.tag-option { display: flex; min-width: 0; min-height: 48px; align-items: center; gap: 9px; padding: 9px 11px; border: 1px solid var(--el-border-color-lighter); border-radius: 11px; color: var(--el-text-color-primary); background: var(--el-bg-color); cursor: pointer; text-align: left; transition: border-color .18s ease, background-color .18s ease, transform .18s ease; }
.tag-option:hover { border-color: color-mix(in srgb, var(--tag-color) 52%, var(--el-border-color)); background: color-mix(in srgb, var(--tag-color) 7%, var(--el-bg-color)); transform: translateY(-1px); }
.tag-option.is-selected { border-color: color-mix(in srgb, var(--tag-color) 66%, var(--el-border-color)); background: color-mix(in srgb, var(--tag-color) 12%, var(--el-bg-color)); }
.tag-option__dot { width: 9px; height: 9px; flex: 0 0 auto; border-radius: 50%; background: var(--tag-color); box-shadow: 0 0 0 4px color-mix(in srgb, var(--tag-color) 14%, transparent); }
.tag-option__name { min-width: 0; overflow: hidden; flex: 1; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.tag-option__check { flex: 0 0 auto; color: var(--tag-color); }
.tag-dialog-state { padding: 4px 0; }
.problem-filter :deep(.el-form-item) { margin-bottom: 0; }
@media (max-width: 900px) { .filter-row { grid-template-columns: 1fr 1fr; }.tag-action { justify-self: start; }.type-filter { grid-column: 1 / -1; white-space: normal; } }
@media (max-width: 620px) { .search-row { justify-content: stretch; flex-wrap: wrap; }.search-input { width: auto; flex: 1; min-width: 160px; }.filter-row { display: block; }.type-filter { margin-bottom: 14px; }.tag-filter { margin-bottom: 10px; }.tag-action { padding-left: 0; } }
</style>
