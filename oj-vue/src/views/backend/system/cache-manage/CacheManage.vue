<template>
  <ContestSubPageShell
    title="缓存管理"
    kicker="SYSTEM / CACHE EXPLORER"
    description="按缓存类别浏览键值，快速定位异常数据并安全清理无效缓存。"
    :icon="Coin"
    tone="violet"
    :stats="summaryStats"
  >
    <template #actions>
      <el-button :icon="Refresh" :loading="loading" @click="refreshCurrent">刷新缓存</el-button>
    </template>

    <section class="cache-panel">
      <div class="panel-heading">
        <div>
          <span class="panel-eyebrow">RUNTIME STORAGE</span>
          <h2>缓存浏览器</h2>
          <p>先选择缓存类别，再查看该类别下的键和值。</p>
        </div>
        <div class="active-prefix" v-if="prefix">
          <span>当前前缀</span>
          <strong>{{ prefix }}</strong>
        </div>
      </div>

      <div class="cache-grid">
        <article class="cache-card">
          <div class="card-heading">
            <div class="card-icon is-violet"><el-icon><FolderOpened /></el-icon></div>
            <div><strong>缓存类别</strong><span>{{ cacheList.length }} 个类别</span></div>
          </div>
          <el-table
            v-loading="loading"
            class="cache-table category-table"
            :data="cacheList"
            highlight-current-row
            row-key="id"
            @row-click="handleDescClickRow"
          >
            <el-table-column label="类别" min-width="190">
              <template #default="{ row }">
                <div class="category-cell">
                  <strong>{{ row.desc }}</strong>
                  <span>{{ row.type }}</span>
                </div>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无缓存类别" /></template>
          </el-table>
        </article>

        <article class="cache-card">
          <div class="card-heading">
            <div class="card-icon is-blue"><el-icon><Key /></el-icon></div>
            <div><strong>缓存键</strong><span>{{ keyList.length }} 个键</span></div>
          </div>
          <el-table
            v-loading="keysLoading"
            class="cache-table key-table"
            :data="keyList"
            highlight-current-row
            @row-click="handleKeyClickRow"
          >
            <el-table-column label="键" min-width="180">
              <template #default="{ row }">
                <span class="cache-key" :title="row">{{ row }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="72" align="right">
              <template #default="{ row }">
                <el-button
                  v-has="'content:cache:remove'"
                  class="delete-button"
                  link
                  type="danger"
                  :icon="Delete"
                  @click.stop="handleDelete(row)"
                />
              </template>
            </el-table-column>
            <template #empty><el-empty description="请选择缓存类别" /></template>
          </el-table>
        </article>

        <article class="cache-card detail-card">
          <div class="card-heading">
            <div class="card-icon is-amber"><el-icon><Document /></el-icon></div>
            <div><strong>缓存内容</strong><span>{{ selectedKey ? '只读查看' : '尚未选择缓存键' }}</span></div>
          </div>
          <div v-if="selectedKey" class="cache-detail">
            <div class="detail-item"><span>缓存键</span><strong :title="cacheInfo.key">{{ cacheInfo.key }}</strong></div>
            <div class="detail-item"><span>过期时间</span><strong>{{ cacheInfo.expireTime || '未设置' }}</strong></div>
            <div class="value-heading"><span>缓存值</span><el-tag size="small" effect="plain">只读</el-tag></div>
            <el-input v-model="cacheInfo.value" class="value-input" type="textarea" readonly :rows="12" />
          </div>
          <el-empty v-else class="detail-empty" description="点击左侧缓存键查看内容" :image-size="76" />
        </article>
      </div>
    </section>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Coin, Delete, Document, FolderOpened, Key, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ContestSubPageShell from '../../teacher/contest-manage/component/ContestSubPageShell.vue'
import { type CacheDesc, type CacheInfo, getCacheInfo, getCacheKeys, listCacheDesc, removeCache } from '@/api/cache'

const loading = ref(false)
const keysLoading = ref(false)
const cacheList = ref<CacheDesc[]>([])
const keyList = ref<string[]>([])
const prefix = ref('')
const selectedKey = ref('')
const cacheInfo = ref<CacheInfo>({ key: '', value: '', expireTime: undefined })

const summaryStats = computed(() => [
  { label: '缓存类别', value: cacheList.value.length, tone: 'violet' },
  { label: '当前键数', value: keyList.value.length, tone: 'blue' },
  { label: '当前状态', value: prefix.value ? '已选择' : '待选择', tone: 'green' },
  { label: '内容状态', value: selectedKey.value ? '已加载' : '未加载', tone: 'amber' },
])

const getCacheList = async () => {
  loading.value = true
  try {
    cacheList.value = (await listCacheDesc()) || []
  } finally {
    loading.value = false
  }
}

const getKeyList = async () => {
  if (!prefix.value) {
    keyList.value = []
    return
  }
  keysLoading.value = true
  try {
    keyList.value = (await getCacheKeys(prefix.value)) || []
  } finally {
    keysLoading.value = false
  }
}

const handleDescClickRow = async (row: CacheDesc) => {
  prefix.value = row.type
  selectedKey.value = ''
  cacheInfo.value = { key: '', value: '', expireTime: undefined }
  await getKeyList()
}

const handleKeyClickRow = async (row: string) => {
  selectedKey.value = row
  cacheInfo.value = await getCacheInfo(row)
}

const handleDelete = async (key: string) => {
  await ElMessageBox.confirm(`确定删除缓存键“${key}”吗？`, '删除缓存', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await removeCache(key)
  if (selectedKey.value === key) {
    selectedKey.value = ''
    cacheInfo.value = { key: '', value: '', expireTime: undefined }
  }
  await getKeyList()
  ElMessage.success('缓存已删除')
}

const refreshCurrent = async () => {
  await getCacheList()
  await getKeyList()
  if (selectedKey.value) cacheInfo.value = await getCacheInfo(selectedKey.value)
}

onMounted(getCacheList)
</script>

<style lang="scss" scoped>
.cache-panel { padding: 22px 24px 24px; border: 1px solid var(--el-border-color-lighter); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 16px 40px rgb(15 23 42 / 4%); }
.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 20px; }
.panel-eyebrow { color: #8b5cf6; font-size: 11px; font-weight: 800; letter-spacing: .14em; }
.panel-heading h2 { margin: 7px 0 5px; font-size: 21px; }
.panel-heading p { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }
.active-prefix { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; color: var(--el-text-color-secondary); font-size: 12px; }
.active-prefix strong { max-width: 250px; overflow: hidden; color: var(--el-text-color-primary); text-overflow: ellipsis; white-space: nowrap; }
.cache-grid { display: grid; grid-template-columns: minmax(210px, .85fr) minmax(260px, 1.1fr) minmax(300px, 1.35fr); gap: 14px; min-height: 480px; }
.cache-card { min-width: 0; overflow: hidden; border: 1px solid var(--el-border-color-lighter); border-radius: 16px; background: var(--el-bg-color-page); }
.card-heading { display: flex; align-items: center; gap: 10px; padding: 16px; border-bottom: 1px solid var(--el-border-color-lighter); background: var(--el-bg-color); }
.card-heading > div:last-child { display: flex; min-width: 0; flex-direction: column; gap: 3px; }
.card-heading strong { color: var(--el-text-color-primary); font-size: 14px; }
.card-heading span { color: var(--el-text-color-secondary); font-size: 11px; }
.card-icon { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border-radius: 10px; font-size: 17px; }
.card-icon.is-violet { background: rgb(139 92 246 / 12%); color: #8b5cf6; }.card-icon.is-blue { background: rgb(37 99 235 / 12%); color: #2563eb; }.card-icon.is-amber { background: rgb(217 119 6 / 12%); color: #d97706; }
.cache-table { width: 100%; background: transparent; }
.category-cell { display: flex; min-width: 0; flex-direction: column; gap: 4px; }.category-cell strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.category-cell span, .cache-key { color: var(--el-text-color-secondary); font-size: 12px; }.cache-key { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.delete-button { padding: 5px; }
.cache-detail { padding: 18px; }.detail-item { display: flex; align-items: flex-start; justify-content: space-between; gap: 14px; padding: 10px 0; border-bottom: 1px solid var(--el-border-color-lighter); }.detail-item span, .value-heading span { color: var(--el-text-color-secondary); font-size: 12px; }.detail-item strong { max-width: 70%; overflow: hidden; color: var(--el-text-color-primary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.value-heading { display: flex; align-items: center; justify-content: space-between; margin: 18px 0 9px; }.value-input :deep(.el-textarea__inner) { min-height: 200px !important; resize: vertical; font-family: var(--el-font-family); font-size: 12px; line-height: 1.65; }.detail-empty { height: 360px; }
:deep(.el-table__header-wrapper) { display: none; }:deep(.el-table__inner-wrapper::before) { display: none; }:deep(.el-table__row td.el-table__cell) { height: 62px; border-bottom-color: var(--el-border-color-lighter); }:deep(.el-table__row:last-child td.el-table__cell) { border-bottom: 0; }
@media (max-width: 1100px) { .cache-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.detail-card { grid-column: 1 / -1; } }
@media (max-width: 680px) { .cache-panel { padding: 16px 12px; }.panel-heading { flex-direction: column; }.active-prefix { align-items: flex-start; }.cache-grid { grid-template-columns: 1fr; }.detail-card { grid-column: auto; }.detail-empty { height: 220px; } }
</style>
