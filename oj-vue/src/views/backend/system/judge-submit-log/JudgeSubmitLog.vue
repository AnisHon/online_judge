<template>
  <ContestSubPageShell
    title="判题记录"
    kicker="SYSTEM / SUBMISSIONS"
    description="查看用户每一次完整提交的判题状态、代码与公开结果。"
    :icon="DocumentChecked"
    tone="blue"
    :stats="summaryStats"
  >
    <template #actions>
      <el-button :icon="Refresh" :loading="loading" @click="getList">刷新记录</el-button>
    </template>

    <section class="log-panel">
      <div class="filter-heading">
        <div>
          <span class="panel-eyebrow">SUBMISSION AUDIT</span>
          <h2>完整提交</h2>
          <p>每条记录对应用户的一次判题提交；内部沙箱错误请到“判题日志”查看。</p>
        </div>
        <div class="filter-actions">
          <el-button plain :icon="RefreshRight" @click="resetQuery">重置</el-button>
          <el-button type="primary" :icon="Search" :loading="loading" @click="search">查询</el-button>
        </div>
      </div>

      <div class="filter-grid">
        <el-input v-model="query.submitId" clearable placeholder="提交 ID" @keyup.enter="search" />
        <el-input v-model="query.userId" clearable placeholder="用户 ID" @keyup.enter="search" />
        <el-input v-model="query.problemId" clearable placeholder="题目 ID" @keyup.enter="search" />
        <el-input v-model="query.contestId" clearable placeholder="比赛/作业 ID" @keyup.enter="search" />
        <el-select v-model="query.status" clearable placeholder="判题状态">
          <el-option v-for="item in judgeStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-input v-model="query.language" clearable placeholder="语言，如 java" @keyup.enter="search" />
      </div>

      <el-table v-loading="loading" class="log-table" :data="records" row-key="submitId">
        <el-table-column label="提交" min-width="190">
          <template #default="{row}"><span class="mono primary-id">#{{ row.submitId }}</span></template>
        </el-table-column>
        <el-table-column label="用户 / 题目" min-width="190">
          <template #default="{row}">
            <div class="id-stack"><strong>用户 {{ row.userId }}</strong><span>题目 {{ row.problemId }}</span></div>
          </template>
        </el-table-column>
        <el-table-column label="场景" min-width="135">
          <template #default="{row}">
            <el-tag v-if="row.contestId" size="small" effect="plain">活动 {{ row.contestId }}</el-tag>
            <span v-else class="muted">普通题目</span>
          </template>
        </el-table-column>
        <el-table-column label="语言" width="120">
          <template #default="{row}"><code class="language">{{ row.language || '—' }}</code></template>
        </el-table-column>
        <el-table-column label="结果" width="132">
          <template #default="{row}">
            <span class="status-pill" :class="`status-pill--${judgeStatusMeta(row.status).tone}`">
              <i />{{ judgeStatusMeta(row.status).label }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="测试点" width="110">
          <template #default="{row}"><span class="mono">{{ formatProgress(row) }}</span></template>
        </el-table-column>
        <el-table-column label="资源" width="145">
          <template #default="{row}"><span class="resource-text">{{ formatResource(row) }}</span></template>
        </el-table-column>
        <el-table-column label="提交时间" min-width="175" prop="submitTime" show-overflow-tooltip />
        <el-table-column label="操作" width="110" fixed="right" align="right">
          <template #default="{row}"><el-button link type="primary" :icon="View" @click="openDetail(row.submitId)">查看详情</el-button></template>
        </el-table-column>
        <template #empty><el-empty description="暂无判题记录" /></template>
      </el-table>

      <Pagination
        v-show="total > 0"
        v-model:page="query.currentPage"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="getList"
      />
    </section>

    <el-drawer v-model="detailVisible" title="提交记录详情" size="min(760px, 94vw)" append-to-body>
      <div v-if="detail" class="detail-drawer">
        <div class="detail-summary">
          <div><span>提交 ID</span><strong class="mono">#{{ detail.submitId }}</strong></div>
          <div><span>用户 / 题目</span><strong>{{ detail.userId }} / {{ detail.problemId }}</strong></div>
          <span class="status-pill" :class="`status-pill--${judgeStatusMeta(detail.status).tone}`"><i />{{ judgeStatusMeta(detail.status).label }}</span>
        </div>
        <div class="detail-metrics">
          <span>语言 <b>{{ detail.language || '—' }}</b></span>
          <span>通过 <b>{{ formatProgress(detail) }}</b></span>
          <span>耗时 <b>{{ detail.time ?? '—' }} ms</b></span>
          <span>内存 <b>{{ detail.memory ?? '—' }} KB</b></span>
        </div>
        <section class="code-section">
          <div class="section-title"><strong>提交代码</strong><span>{{ detail.language || 'source' }}</span></div>
          <pre v-if="detail.code" class="code-viewer"><code>{{ detail.code }}</code></pre>
          <el-empty v-else description="这条记录没有保存代码" :image-size="60" />
        </section>
        <section v-if="detail.stderr" class="error-section">
          <div class="section-title"><strong>公开错误信息</strong><span>用户可见</span></div>
          <pre>{{ detail.stderr }}</pre>
        </section>
      </div>
      <el-skeleton v-else :rows="8" animated />
    </el-drawer>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {DocumentChecked, Refresh, RefreshRight, Search, View} from '@element-plus/icons-vue'
import ContestSubPageShell from '../../teacher/contest-manage/component/ContestSubPageShell.vue'
import Pagination from '@/components/pageination/Pagination.vue'
import {
  getAdminSubmitLog,
  getAdminSubmitLogs,
  judgeStatusMeta,
  judgeStatusOptions,
  type AdminJudgeQuery,
  type AdminSubmitLog,
} from '@/api/judge-log'
import type {IdType} from '@/api/common'

const loading = ref(false)
const records = ref<AdminSubmitLog[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<AdminSubmitLog>()
const query = reactive<AdminJudgeQuery>({currentPage: 1, pageSize: 20})

const summaryStats = computed(() => [
  {label: '记录总数', value: total.value, tone: 'blue'},
  {label: '当前页', value: records.value.length, tone: 'violet'},
  {label: '当前通过', value: records.value.filter(item => judgeStatusMeta(item.status).value === 'ACCEPT').length, tone: 'green'},
  {label: '当前异常', value: records.value.filter(item => ['JUDGE_ERROR', 'COMPILE_ERROR'].includes(judgeStatusMeta(item.status).value)).length, tone: 'amber'},
])

const formatProgress = (row: AdminSubmitLog) => row.totalCount == null ? '—' : `${row.passCount ?? 0} / ${row.totalCount}`
const formatResource = (row: AdminSubmitLog) => `${row.time ?? '—'} ms · ${row.memory ?? '—'} KB`

const getList = async () => {
  loading.value = true
  try {
    const data = await getAdminSubmitLogs(query)
    records.value = data?.data || []
    total.value = data?.totalRecords || 0
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.currentPage = 1
  void getList()
}

const resetQuery = () => {
  Object.assign(query, {submitId: undefined, userId: undefined, problemId: undefined, contestId: undefined, status: undefined, language: undefined, currentPage: 1})
  void getList()
}

const openDetail = async (submitId: IdType) => {
  detailVisible.value = true
  detail.value = undefined
  detail.value = await getAdminSubmitLog(submitId)
}

onMounted(getList)
</script>

<style scoped>
.log-panel { padding: 22px 24px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 16px 40px rgb(15 23 42 / 4%); }
.filter-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 18px; }.panel-eyebrow { color: var(--el-color-primary); font-size: 11px; font-weight: 800; letter-spacing: .14em; }.filter-heading h2 { margin: 7px 0 5px; font-size: 21px; }.filter-heading p { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }.filter-actions { display: flex; gap: 8px; }
.filter-grid { display: grid; grid-template-columns: repeat(6, minmax(110px, 1fr)); gap: 10px; margin-bottom: 16px; padding: 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; background: var(--el-fill-color-light); }.log-table { border-radius: 14px; overflow: hidden; }.mono, .language { font-family: var(--code-font-family, Consolas, monospace); }.primary-id { color: var(--el-color-primary); }.id-stack { display: flex; min-width: 0; flex-direction: column; gap: 4px; }.id-stack span, .muted, .resource-text { color: var(--el-text-color-secondary); font-size: 12px; }.language { color: var(--el-color-primary); }.status-pill { display: inline-flex; align-items: center; gap: 6px; padding: 5px 9px; border-radius: 999px; font-size: 12px; white-space: nowrap; }.status-pill i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }.status-pill--success { color: var(--el-color-success); background: var(--el-color-success-light-9); }.status-pill--danger { color: var(--el-color-danger); background: var(--el-color-danger-light-9); }.status-pill--warning { color: var(--el-color-warning); background: var(--el-color-warning-light-9); }.status-pill--info { color: var(--el-text-color-secondary); background: var(--el-fill-color-light); }
.detail-drawer { display: grid; gap: 16px; }.detail-summary { display: flex; align-items: center; gap: 18px; padding: 15px; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; background: var(--el-fill-color-light); }.detail-summary > div { display: flex; min-width: 0; flex-direction: column; gap: 4px; }.detail-summary > div:nth-child(2) { flex: 1; }.detail-summary span:not(.status-pill), .detail-metrics span { color: var(--el-text-color-secondary); font-size: 11px; }.detail-summary strong { font-size: 13px; }.detail-metrics { display: flex; flex-wrap: wrap; gap: 8px 22px; padding: 0 2px; }.detail-metrics b { margin-left: 4px; color: var(--el-text-color-primary); font-weight: 600; }.code-section, .error-section { overflow: hidden; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; }.section-title { display: flex; align-items: center; justify-content: space-between; padding: 12px 14px; border-bottom: 1px solid var(--el-border-color-lighter); background: var(--el-fill-color-light); }.section-title strong { font-size: 13px; }.section-title span { color: var(--el-text-color-secondary); font-size: 11px; }.code-viewer, .error-section pre { margin: 0; padding: 16px; overflow: auto; color: var(--el-text-color-primary); background: var(--el-bg-color-page); font: 12px/1.7 var(--code-font-family, Consolas, monospace); white-space: pre-wrap; word-break: break-word; }.error-section { border-color: color-mix(in srgb, var(--el-color-danger) 30%, var(--el-border-color-lighter)); }.error-section pre { color: var(--el-color-danger); }
@media (max-width: 980px) { .filter-grid { grid-template-columns: repeat(3, minmax(130px, 1fr)); } } @media (max-width: 620px) { .log-panel { padding: 16px 14px 8px; }.filter-heading { align-items: flex-start; flex-direction: column; }.filter-actions { width: 100%; justify-content: flex-end; }.filter-grid { grid-template-columns: 1fr 1fr; }.detail-summary { align-items: flex-start; flex-direction: column; gap: 10px; } }
</style>
