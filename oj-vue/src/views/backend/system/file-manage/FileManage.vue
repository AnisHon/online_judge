<template>
  <ContestSubPageShell
    title="文件管理"
    kicker="SYSTEM / FILE LIBRARY"
    description="查看平台文件资产、引用关系与存储占用，集中处理无效文件。"
    :icon="Folder"
    tone="blue"
    :stats="summaryStats"
  >
    <template #actions>
      <el-button :icon="Refresh" :loading="loading" @click="getList">刷新文件</el-button>
      <el-button
        v-has="'content:file:remove'"
        type="danger"
        plain
        :disabled="!selectedIds.length"
        :icon="Delete"
        @click="handleDelete()"
      >
        批量删除
      </el-button>
    </template>

    <section class="file-panel">
      <div class="panel-heading">
        <div>
          <span class="panel-eyebrow">ASSET INVENTORY</span>
          <h2>文件资产</h2>
          <p>文件删除会同步清理对象存储中的对应资源，请确认引用数后操作。</p>
        </div>
        <el-input
          v-model="keyword"
          clearable
          class="keyword-input"
          :prefix-icon="Search"
          placeholder="搜索文件名、路径或 MD5"
        />
      </div>

      <div v-if="selectedIds.length" class="selection-bar">
        <span>已选择 {{ selectedIds.length }} 个文件</span>
        <span class="selection-hint">删除前请确认文件没有被业务引用</span>
      </div>

      <el-table
        v-loading="loading"
        class="file-table"
        :data="filteredList"
        row-key="fileId"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="52" align="center" />
        <el-table-column label="文件" min-width="300">
          <template #default="{ row }">
            <div class="file-cell">
              <span class="file-icon"><el-icon><Document /></el-icon></span>
              <div>
                <strong :title="row.fileName">{{ row.fileName }}</strong>
                <span>#{{ row.fileId }} · {{ row.filePath }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <span class="type-pill">{{ row.fileType || '未知' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="大小" width="130" prop="fileSize">
          <template #default="{ row }">{{ bytesToSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="引用数" width="120" prop="reference" sortable>
          <template #default="{ row }">
            <span :class="['reference-count', { 'is-used': row.reference > 0 }]">{{ row.reference || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="MD5" min-width="220" prop="fileMd5" show-overflow-tooltip />
        <el-table-column label="上传时间" min-width="180" prop="uploadTime" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right" align="right">
          <template #default="{ row }">
            <el-space>
              <el-button link type="primary" :icon="Download" @click="handleDownload(row)">下载</el-button>
              <el-button
                v-has="'content:file:remove'"
                link
                type="danger"
                :icon="Delete"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </el-space>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无文件资产" /></template>
      </el-table>

      <Pagination
        v-show="total > 0"
        v-model:page="queryParams.currentPage"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </section>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Delete, Document, Download, Folder, Refresh, Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ContestSubPageShell from '../../teacher/contest-manage/component/ContestSubPageShell.vue'
import Pagination from '@/components/pageination/Pagination.vue'
import type { IdType } from '@/api/common'
import type { PagedType } from '@/api/pagedType'
import { type FileInfo, listFileInfo, removeFileInfo } from '@/api/file/fileInfo'
import { bytesToSize } from '@/utils/byte2size'
import { downloadFile } from '@/api/file'

const loading = ref(false)
const keyword = ref('')
const tableList = ref<FileInfo[]>([])
const selectedIds = ref<IdType[]>([])
const total = ref(0)
const queryParams = reactive<PagedType>({ currentPage: 1, pageSize: 20 })

const filteredList = computed(() => {
  const normalizedKeyword = keyword.value.trim().toLowerCase()
  if (!normalizedKeyword) return tableList.value
  return tableList.value.filter((item) =>
    [item.fileName, item.filePath, item.fileMd5, item.fileType, item.fileId]
      .map((value) => String(value || '').toLowerCase())
      .some((value) => value.includes(normalizedKeyword)),
  )
})

const summaryStats = computed(() => [
  { label: '文件总数', value: total.value, tone: 'blue' },
  { label: '当前页容量', value: bytesToSize(tableList.value.reduce((totalSize, item) => totalSize + (item.fileSize || 0), 0)), tone: 'violet' },
  { label: '已被引用', value: tableList.value.filter((item) => item.reference > 0).length, tone: 'green' },
  { label: '当前页', value: tableList.value.length, tone: 'amber' },
])

const getList = async () => {
  loading.value = true
  try {
    const result = await listFileInfo(queryParams)
    tableList.value = result.data || []
    total.value = result.totalRecords || 0
    selectedIds.value = []
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (selection: FileInfo[]) => {
  selectedIds.value = selection.map((item) => item.fileId)
}

const handleDownload = (row: FileInfo) => {
  const filename = row.fileType ? `${row.fileName}.${row.fileType}` : row.fileName
  downloadFile(row.filePath, filename)
}

const handleDelete = async (row?: FileInfo) => {
  const ids = row ? [row.fileId] : selectedIds.value
  if (!ids.length) return
  await ElMessageBox.confirm(`确定删除选中的 ${ids.length} 个文件吗？删除后无法恢复。`, '删除文件', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning',
  })
  await removeFileInfo(ids)
  ElMessage.success('文件已删除')
  await getList()
}

onMounted(getList)
</script>

<style lang="scss" scoped>
.file-panel { padding: 22px 24px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 16px 40px rgb(15 23 42 / 4%); }
.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 18px; }.panel-eyebrow { color: var(--el-color-primary); font-size: 11px; font-weight: 800; letter-spacing: .14em; }.panel-heading h2 { margin: 7px 0 5px; font-size: 21px; }.panel-heading p { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }.keyword-input { width: 290px; }
.selection-bar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; padding: 8px 12px; border-radius: 10px; background: var(--el-fill-color-light); color: var(--el-text-color-secondary); font-size: 13px; }.selection-hint { color: var(--el-color-warning); font-size: 12px; }.file-table { border-radius: 14px; overflow: hidden; }.file-cell { display: flex; align-items: center; gap: 12px; min-width: 0; }.file-icon { display: grid; width: 36px; height: 36px; flex: 0 0 auto; place-items: center; border-radius: 11px; background: color-mix(in srgb, var(--el-color-primary) 10%, var(--el-bg-color)); color: var(--el-color-primary); }.file-cell > div:last-child { display: flex; min-width: 0; flex-direction: column; gap: 4px; }.file-cell strong { overflow: hidden; color: var(--el-text-color-primary); text-overflow: ellipsis; white-space: nowrap; }.file-cell span { overflow: hidden; color: var(--el-text-color-secondary); font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.type-pill { display: inline-flex; padding: 4px 9px; border-radius: 999px; background: var(--el-fill-color-light); color: var(--el-text-color-secondary); font-size: 12px; }.reference-count { color: var(--el-text-color-secondary); font-variant-numeric: tabular-nums; }.reference-count.is-used { color: var(--el-color-success); font-weight: 700; }
:deep(.el-table__header th.el-table__cell) { color: var(--el-text-color-secondary); font-size: 12px; font-weight: 700; background: var(--el-fill-color-light); }:deep(.el-table__row td.el-table__cell) { height: 68px; }
@media (max-width: 760px) { .panel-heading { flex-direction: column; }.keyword-input { width: 100%; }.selection-bar { align-items: flex-start; flex-direction: column; gap: 5px; } }
@media (max-width: 480px) { .file-panel { padding: 16px 12px 8px; }.file-table :deep(.el-table__fixed-right) { display: none; } }
</style>
