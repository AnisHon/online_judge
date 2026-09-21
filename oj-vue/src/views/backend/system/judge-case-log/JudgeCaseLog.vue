<template>
  <ContestSubPageShell
    title="判题日志"
    kicker="SYSTEM / JUDGE DIAGNOSTICS"
    description="按测试用例查看判题机执行过程和内部异常，仅对具备诊断权限的管理员开放。"
    :icon="DataAnalysis"
    tone="amber"
    :stats="summaryStats"
  >
    <template #actions>
      <el-button :icon="Refresh" :loading="loading" @click="getList">刷新日志</el-button>
    </template>

    <section class="log-panel">
      <div class="filter-heading">
        <div>
          <span class="panel-eyebrow">CASE DIAGNOSTICS</span>
          <h2>测试用例日志</h2>
          <p>这里展示每个测试用例的状态和判题机错误，不向普通用户暴露内部细节。</p>
        </div>
        <div class="filter-actions">
          <el-button plain :icon="RefreshRight" @click="resetQuery">重置</el-button>
          <el-button type="primary" :icon="Search" :loading="loading" @click="search">查询</el-button>
        </div>
      </div>

      <div class="filter-grid">
        <el-input v-model="query.submitId" clearable placeholder="提交 ID" @keyup.enter="search" />
        <el-input v-model="query.problemId" clearable placeholder="题目 ID" @keyup.enter="search" />
        <el-input v-model="query.caseId" clearable placeholder="测试用例 ID" @keyup.enter="search" />
        <el-input-number v-model="query.caseIndex" class="case-index" :min="0" :controls="false" placeholder="测试点序号" />
        <el-select v-model="query.status" clearable placeholder="判题状态">
          <el-option v-for="item in judgeStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </div>

      <el-table v-loading="loading" class="log-table" :data="records" row-key="id">
        <el-table-column label="日志" min-width="175">
          <template #default="{row}"><span class="mono primary-id">#{{ row.id }}</span><small class="sub-id">提交 {{ row.submitId }}</small></template>
        </el-table-column>
        <el-table-column label="题目 / 测试点" min-width="180">
          <template #default="{row}">
            <div class="id-stack"><strong>题目 {{ row.problemId }}</strong><span>Case {{ row.caseIndex == null ? '—' : row.caseIndex + 1 }} · {{ row.caseId || '兜底日志' }}</span></div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="140">
          <template #default="{row}"><span class="status-pill" :class="`status-pill--${judgeStatusMeta(row.status).tone}`"><i />{{ judgeStatusMeta(row.status).label }}</span></template>
        </el-table-column>
        <el-table-column label="得分" width="100">
          <template #default="{row}"><span class="mono">{{ row.score ?? '—' }}</span></template>
        </el-table-column>
        <el-table-column label="资源" width="155">
          <template #default="{row}"><span class="resource-text">{{ row.time ?? '—' }} ms · {{ row.memory ?? '—' }} KB</span></template>
        </el-table-column>
        <el-table-column label="日志时间" min-width="175" prop="createTime" show-overflow-tooltip />
        <el-table-column label="诊断" width="120" fixed="right" align="right">
          <template #default="{row}">
            <el-button v-if="row.internalError" link type="danger" :icon="WarningFilled" @click="openDetail(row)">查看异常</el-button>
            <el-button v-else link type="primary" :icon="View" @click="openDetail(row)">查看</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无测试用例日志" /></template>
      </el-table>

      <Pagination
        v-show="total > 0"
        v-model:page="query.currentPage"
        v-model:limit="query.pageSize"
        :total="total"
        @pagination="getList"
      />
    </section>

    <el-drawer v-model="detailVisible" title="测试用例诊断" size="min(680px, 94vw)" append-to-body>
      <div v-if="detail" class="case-detail">
        <div class="detail-head">
          <div><span>提交 {{ detail.submitId }}</span><strong>Case {{ detail.caseIndex == null ? '—' : detail.caseIndex + 1 }}</strong></div>
          <span class="status-pill" :class="`status-pill--${judgeStatusMeta(detail.status).tone}`"><i />{{ judgeStatusMeta(detail.status).label }}</span>
        </div>
        <div class="detail-grid">
          <span>日志 ID <b class="mono">{{ detail.id }}</b></span>
          <span>题目 ID <b class="mono">{{ detail.problemId }}</b></span>
          <span>测试用例 ID <b class="mono">{{ detail.caseId || '—' }}</b></span>
          <span>得分 <b>{{ detail.score ?? '—' }}</b></span>
          <span>耗时 <b>{{ detail.time ?? '—' }} ms</b></span>
          <span>内存 <b>{{ detail.memory ?? '—' }} KB</b></span>
        </div>
        <section class="error-section">
          <div class="section-title"><strong>内部诊断信息</strong><span>仅管理员可见</span></div>
          <pre v-if="detail.internalError">{{ detail.internalError }}</pre>
          <el-empty v-else description="该测试点没有记录内部错误" :image-size="60" />
        </section>
      </div>
    </el-drawer>
  </ContestSubPageShell>
</template>

<script setup lang="ts">
import {computed, onMounted, reactive, ref} from 'vue'
import {DataAnalysis, Refresh, RefreshRight, Search, View, WarningFilled} from '@element-plus/icons-vue'
import ContestSubPageShell from '../../teacher/contest-manage/component/ContestSubPageShell.vue'
import Pagination from '@/components/pageination/Pagination.vue'
import {
  getAdminCaseLogs,
  judgeStatusMeta,
  judgeStatusOptions,
  type AdminJudgeCaseLog,
  type AdminJudgeQuery,
} from '@/api/judge-log'

const loading = ref(false)
const records = ref<AdminJudgeCaseLog[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<AdminJudgeCaseLog>()
const query = reactive<AdminJudgeQuery>({currentPage: 1, pageSize: 20})

const summaryStats = computed(() => [
  {label: '日志总数', value: total.value, tone: 'amber'},
  {label: '当前页', value: records.value.length, tone: 'violet'},
  {label: '当前异常', value: records.value.filter(item => Boolean(item.internalError)).length, tone: 'amber'},
  {label: '当前通过', value: records.value.filter(item => judgeStatusMeta(item.status).value === 'ACCEPT').length, tone: 'green'},
])

const getList = async () => {
  loading.value = true
  try {
    const data = await getAdminCaseLogs(query)
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
  Object.assign(query, {submitId: undefined, userId: undefined, problemId: undefined, contestId: undefined, caseId: undefined, caseIndex: undefined, status: undefined, language: undefined, currentPage: 1})
  void getList()
}

const openDetail = (row: AdminJudgeCaseLog) => {
  detail.value = row
  detailVisible.value = true
}

onMounted(getList)
</script>

<style scoped>
.log-panel { padding: 22px 24px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 20px; background: var(--el-bg-color); box-shadow: 0 16px 40px rgb(15 23 42 / 4%); }
.filter-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 18px; }.panel-eyebrow { color: var(--el-color-warning); font-size: 11px; font-weight: 800; letter-spacing: .14em; }.filter-heading h2 { margin: 7px 0 5px; font-size: 21px; }.filter-heading p { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; }.filter-actions { display: flex; gap: 8px; }
.filter-grid { display: grid; grid-template-columns: repeat(5, minmax(130px, 1fr)); gap: 10px; margin-bottom: 16px; padding: 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; background: var(--el-fill-color-light); }.case-index { width: 100%; }.log-table { border-radius: 14px; overflow: hidden; }.mono { font-family: var(--code-font-family, Consolas, monospace); }.primary-id { color: var(--el-color-primary); }.sub-id { display: block; margin-top: 4px; color: var(--el-text-color-secondary); font: 11px var(--code-font-family, Consolas, monospace); }.id-stack { display: flex; min-width: 0; flex-direction: column; gap: 4px; }.id-stack span, .resource-text { color: var(--el-text-color-secondary); font-size: 12px; }.status-pill { display: inline-flex; align-items: center; gap: 6px; padding: 5px 9px; border-radius: 999px; font-size: 12px; white-space: nowrap; }.status-pill i { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }.status-pill--success { color: var(--el-color-success); background: var(--el-color-success-light-9); }.status-pill--danger { color: var(--el-color-danger); background: var(--el-color-danger-light-9); }.status-pill--warning { color: var(--el-color-warning); background: var(--el-color-warning-light-9); }.status-pill--info { color: var(--el-text-color-secondary); background: var(--el-fill-color-light); }
.case-detail { display: grid; gap: 18px; }.detail-head { display: flex; align-items: center; justify-content: space-between; padding: 15px; border: 1px solid var(--el-border-color-lighter); border-radius: 14px; background: var(--el-fill-color-light); }.detail-head div { display: flex; flex-direction: column; gap: 4px; }.detail-head span:first-child { color: var(--el-text-color-secondary); font: 11px var(--code-font-family, Consolas, monospace); }.detail-head strong { font-size: 17px; }.detail-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; padding: 2px; }.detail-grid span { color: var(--el-text-color-secondary); font-size: 12px; }.detail-grid b { display: block; margin-top: 4px; color: var(--el-text-color-primary); font-size: 13px; font-weight: 600; }.error-section { overflow: hidden; border: 1px solid color-mix(in srgb, var(--el-color-danger) 30%, var(--el-border-color-lighter)); border-radius: 14px; }.section-title { display: flex; align-items: center; justify-content: space-between; padding: 12px 14px; background: var(--el-fill-color-light); }.section-title strong { font-size: 13px; }.section-title span { color: var(--el-color-danger); font-size: 11px; }.error-section pre { max-height: 520px; margin: 0; padding: 16px; overflow: auto; color: var(--el-color-danger); background: var(--el-bg-color-page); font: 12px/1.7 var(--code-font-family, Consolas, monospace); white-space: pre-wrap; word-break: break-word; }
@media (max-width: 900px) { .filter-grid { grid-template-columns: repeat(3, minmax(130px, 1fr)); } } @media (max-width: 620px) { .log-panel { padding: 16px 14px 8px; }.filter-heading { align-items: flex-start; flex-direction: column; }.filter-actions { width: 100%; justify-content: flex-end; }.filter-grid { grid-template-columns: 1fr 1fr; }.detail-grid { grid-template-columns: 1fr; } }
</style>
