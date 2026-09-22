<template>
  <section class="problem-picker">
    <header class="picker-heading">
      <div class="picker-heading__title">
        <span class="picker-icon"><el-icon><Collection /></el-icon></span>
        <div>
          <strong>选择题目</strong>
          <small>{{ isAdminPicker ? '题目管理权限范围内的题目' : '当前可访问的公开题目' }}</small>
        </div>
      </div>
      <span class="picker-count">{{ total }} 个结果</span>
    </header>

    <el-form class="picker-filters" :model="query" label-position="top" @submit.prevent="handleQuery">
      <el-form-item label="题目名称">
        <el-input v-model="query.title" clearable :prefix-icon="Search" placeholder="搜索题目名称" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="题目 ID">
        <el-input v-model="query.problemId" clearable :prefix-icon="Key" placeholder="输入题目 ID" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="题目类型">
        <el-select v-model="query.type" clearable class="full-width" placeholder="全部类型">
          <el-option v-for="item in dict.problemTypeStr" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <div class="picker-actions">
        <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
        <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
      </div>
    </el-form>

    <div class="picker-toolbar">
      <span>点击整行选择题目，名称和 ID 会回填到题解。</span>
      <el-button text :icon="Refresh" :loading="loading" @click="getList">刷新题目</el-button>
    </div>

    <el-table
      v-loading="loading"
      class="picker-table"
      :data="problems"
      row-key="problemId"
      highlight-current-row
      :current-row-key="modelValue"
      @row-click="selectProblem"
    >
      <el-table-column label="题目" min-width="330">
        <template #default="{ row }">
          <div class="problem-cell">
            <span class="problem-cell__icon"><el-icon><Document /></el-icon></span>
            <div class="problem-cell__main">
              <strong :title="row.title || '未命名题目'">{{ row.title || '未命名题目' }}</strong>
              <span :title="String(row.problemId)">ID {{ shortId(row.problemId) }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="120" align="center">
        <template #default="{ row }"><span class="type-pill">{{ problemTypeText(row.type) }}</span></template>
      </el-table-column>
      <el-table-column label="来源" min-width="180" show-overflow-tooltip>
        <template #default="{ row }"><span class="source-text">{{ row.source || '未标注来源' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" width="90" fixed="right" align="right">
        <template #default="{ row }"><el-button link type="primary" :icon="Select" @click.stop="selectProblem(row)">选用</el-button></template>
      </el-table-column>
      <template #empty><el-empty :description="errorMessage || '没有找到符合条件的题目'" :image-size="76"><el-button v-if="errorMessage" type="primary" link @click="getList">重试</el-button></el-empty></template>
    </el-table>

    <Pagination
      v-show="total > 0"
      v-model:page="query.currentPage"
      v-model:limit="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 30]"
      @pagination="getList"
    />
  </section>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { Collection, Document, Key, Refresh, Search, Select } from '@element-plus/icons-vue'
import { dict, getProblems, getProblemsAdmin, ProblemType, type AdminQueryProblem, type ProblemParam, type ProblemView, type TaggedProblemView } from '@/api/problem'
import type { IdType } from '@/api/common'
import Pagination from '@/components/pageination/Pagination.vue'
import { hasPerm } from '@/utils/authUtil'

interface ProblemPickerView extends Pick<ProblemView, 'problemId' | 'title' | 'source' | 'type'> {}

const modelValue = defineModel<IdType>()
const emit = defineEmits<{ (event: 'select', problem: ProblemPickerView): void }>()
const isAdminPicker = computed(() => hasPerm('problem:problem:list'))
const query = reactive({ currentPage: 1, pageSize: 10, title: '', problemId: '', type: undefined as ProblemType | undefined })
const problems = ref<ProblemPickerView[]>([])
const total = ref(0)
const loading = ref(false)
const errorMessage = ref('')
let requestVersion = 0

const shortId = (value: IdType) => {
  const text = String(value)
  return text.length > 18 ? `${text.slice(0, 8)}…${text.slice(-6)}` : text
}

const problemTypeText = (type: ProblemType) => dict.problemType.find(item => item.value === type)?.label || '未知类型'

const getList = async () => {
  const version = ++requestVersion
  loading.value = true
  errorMessage.value = ''
  try {
    if (isAdminPicker.value) {
      const params: AdminQueryProblem = { currentPage: query.currentPage, pageSize: query.pageSize, title: query.title || undefined, problemId: query.problemId || undefined, type: query.type }
      const result = await getProblemsAdmin(params)
      if (version !== requestVersion) return
      problems.value = result.data.map(toPickerView)
      total.value = result.totalRecords
    } else {
      const params: ProblemParam = { currentPage: query.currentPage, pageSize: query.pageSize, title: query.title || undefined, problemId: query.problemId || null, type: query.type }
      const result = await getProblems(params)
      if (version !== requestVersion) return
      problems.value = result.data.map(toPickerView)
      total.value = result.totalRecords
    }
  } catch (error) {
    if (version !== requestVersion) return
    errorMessage.value = error instanceof Error ? error.message : '题目加载失败，请稍后重试'
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

const toPickerView = (problem: ProblemView | TaggedProblemView): ProblemPickerView => ({ problemId: String(problem.problemId), title: problem.title, source: problem.source, type: problem.type })
const handleQuery = () => { query.currentPage = 1; getList() }
const resetQuery = () => { query.title = ''; query.problemId = ''; query.type = undefined; handleQuery() }
const selectProblem = (problem: ProblemPickerView) => { modelValue.value = problem.problemId; emit('select', problem) }

watch(isAdminPicker, () => {
  problems.value = []
  query.currentPage = 1
  void getList()
})

void getList()
</script>

<style scoped lang="scss">
.problem-picker { min-width: 0; color: var(--el-text-color-primary); }
.picker-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 16px; }
.picker-heading__title { display: flex; align-items: center; gap: 10px; min-width: 0; }
.picker-icon, .problem-cell__icon { display: grid; flex: 0 0 auto; place-items: center; border-radius: 10px; background: color-mix(in srgb, var(--el-color-primary) 12%, var(--el-bg-color)); color: var(--el-color-primary); }
.picker-icon { width: 36px; height: 36px; font-size: 18px; }
.picker-heading strong, .picker-heading small { display: block; }
.picker-heading strong { font-size: 15px; }
.picker-heading small { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }
.picker-count { flex: 0 0 auto; color: var(--el-color-primary); font-size: 12px; font-weight: 700; }
.picker-filters { display: grid; grid-template-columns: 2fr 1fr 1fr auto; align-items: end; gap: 0 12px; padding: 13px 14px 2px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-bg-color-page); }
.picker-filters :deep(.el-form-item) { min-width: 0; margin-bottom: 10px; }
.picker-filters :deep(.el-form-item__label) { height: auto; margin-bottom: 5px; color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.2; }
.full-width { width: 100%; }
.picker-actions { display: flex; align-items: flex-end; gap: 7px; height: 68px; padding-bottom: 10px; }
.picker-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; min-height: 44px; color: var(--el-text-color-secondary); font-size: 12px; }
.problem-cell { display: flex; align-items: center; gap: 11px; min-width: 0; }
.problem-cell__icon { width: 34px; height: 34px; font-size: 16px; }
.problem-cell__main { display: flex; min-width: 0; flex-direction: column; gap: 4px; }
.problem-cell__main strong { max-width: 270px; overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.problem-cell__main span, .source-text { color: var(--el-text-color-secondary); font: 11px var(--code-font-family, monospace); }
.source-text { font-family: inherit; font-size: 12px; }
.type-pill { display: inline-flex; padding: 4px 8px; border-radius: 999px; background: color-mix(in srgb, var(--el-color-primary) 10%, var(--el-bg-color)); color: var(--el-color-primary); font-size: 11px; }
.picker-table { overflow: hidden; border-radius: 13px; }
.picker-table :deep(.el-table__row) { cursor: pointer; }
.picker-table :deep(.el-table__row td.el-table__cell) { height: 64px; }
.picker-table :deep(.current-row td.el-table__cell) { background: color-mix(in srgb, var(--el-color-primary) 6%, var(--el-bg-color)); }
.problem-picker :deep(.pagination-container) { padding: 20px 8px 6px; }
@media (max-width: 720px) { .picker-filters { grid-template-columns: repeat(2, minmax(0, 1fr)); }.picker-actions { grid-column: 1 / -1; height: auto; }.picker-toolbar { align-items: flex-start; flex-direction: column; padding: 5px 0; } }
@media (max-width: 480px) { .picker-filters { grid-template-columns: 1fr; }.picker-actions { grid-column: auto; }.picker-heading { align-items: flex-start; flex-direction: column; }.picker-count { align-self: flex-end; } }
</style>
