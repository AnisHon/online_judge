<template>
  <main class="admin-dashboard">
    <section class="dashboard-heading">
      <div>
        <p class="dashboard-kicker">延拓 CODE / OPERATIONS</p>
        <h1>控制台</h1>
        <p class="dashboard-description">集中查看平台运行数据，快速进入日常管理工作。</p>
      </div>

      <div class="heading-actions">
        <span class="sync-state" :class="`sync-state--${syncState}`">
          <i aria-hidden="true"></i>{{ syncLabel }}<span v-if="lastUpdated"> · {{ lastUpdated }}</span>
        </span>
        <el-button :loading="loading" :icon="Refresh" @click="loadDashboard">刷新数据</el-button>
        <el-button type="primary" :icon="Monitor" @click="router.push({name: 'home'})">查看前台</el-button>
      </div>
    </section>

    <section class="metric-grid" aria-label="平台指标">
      <article v-for="metric in metrics" :key="metric.label" class="metric-card">
        <div class="metric-card__top">
          <span class="metric-icon" :class="`metric-icon--${metric.tone}`">
            <el-icon><component :is="metric.icon" /></el-icon>
          </span>
          <span class="metric-caption">{{ metric.caption }}</span>
        </div>
        <div class="metric-value">
          {{ metric.value }}<small v-if="metric.unit">{{ metric.unit }}</small>
        </div>
        <p>{{ metric.description }}</p>
        <el-progress
            v-if="metric.progress !== undefined"
            :percentage="metric.progress"
            :show-text="false"
            :stroke-width="6"
            :color="metric.progressColor"
        />
      </article>
    </section>

    <section class="dashboard-grid dashboard-grid--primary">
      <article class="panel activity-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">CONTENT</p>
            <h2>最近公告</h2>
          </div>
          <el-button link type="primary" :icon="ArrowRight" @click="goToRoute('notice-manage')">管理公告</el-button>
        </div>

        <div v-loading="loading" class="notice-list">
          <button v-for="notice in notices" :key="notice.noticeId" class="notice-row" type="button"
                  @click="router.push({name: 'notice', params: {id: notice.noticeId}})">
            <span class="notice-marker" :class="{'notice-marker--important': notice.topUp}"></span>
            <span class="notice-main">
              <strong>{{ notice.title }}</strong>
              <small>{{ formatDate(notice.createTime) }}</small>
            </span>
            <el-tag v-if="notice.topUp" size="small" type="danger" effect="plain">重要</el-tag>
            <el-icon class="notice-arrow"><ArrowRight /></el-icon>
          </button>
          <el-empty v-if="!loading && notices.length === 0" description="暂无公告" :image-size="72" />
        </div>
      </article>

      <article class="panel quick-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">WORKSPACE</p>
            <h2>快捷管理</h2>
          </div>
        </div>
        <div class="quick-actions">
          <button v-for="action in quickActions" :key="action.label" class="quick-action" type="button"
                  @click="goToRoute(action.route)">
            <span class="quick-action__icon" :class="`quick-action__icon--${action.tone}`">
              <el-icon><component :is="action.icon" /></el-icon>
            </span>
            <span><strong>{{ action.label }}</strong><small>{{ action.description }}</small></span>
            <el-icon class="quick-action__arrow"><ArrowRight /></el-icon>
          </button>
        </div>
      </article>
    </section>

    <section class="dashboard-grid dashboard-grid--secondary">
      <article class="panel resource-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">INFRASTRUCTURE</p>
            <h2>资源概览</h2>
          </div>
          <span class="panel-note">本机磁盘</span>
        </div>
        <div class="resource-summary">
          <div class="resource-ring" :style="{ '--resource-progress': `${usedPercentage}%` }">
            <div><strong>{{ usedPercentage }}%</strong><small>已使用</small></div>
          </div>
          <div class="resource-copy">
            <div class="resource-number"><strong>{{ formatNumber(freeMemory) }}</strong><span>GB 可用</span></div>
            <p>总空间 {{ formatNumber(totalMemory) }} GB，数据来自 content service。</p>
            <div class="resource-legend"><span></span>已使用 {{ formatNumber(usedMemory) }} GB <b></b>剩余 {{ formatNumber(freeMemory) }} GB</div>
          </div>
        </div>
      </article>

      <article class="panel tools-panel">
        <div class="panel-heading">
          <div>
            <p class="panel-kicker">OPERATIONS</p>
            <h2>运维工具</h2>
          </div>
          <el-tooltip content="复制通过 oj-server 访问内网管理端口的 SSH 转发命令" placement="top">
            <el-icon class="panel-help"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
        <div class="tool-list">
          <button v-for="tool in copyButtons" :key="tool.label" class="tool-row" type="button" @click="handleCopy(tool.port)">
            <span class="tool-row__icon"><el-icon><component :is="tool.icon" /></el-icon></span>
            <span><strong>{{ tool.label }}</strong><small>{{ tool.port.join(' · ') }} 端口</small></span>
            <el-icon class="tool-row__copy"><DocumentCopy /></el-icon>
          </button>
        </div>
      </article>
    </section>
  </main>
</template>

<script setup lang="ts">
import {computed, markRaw, ref} from "vue";
import {useRouter} from "vue-router";
import {
  ArrowRight,
  Collection,
  DataAnalysis,
  DocumentCopy,
  EditPen,
  Monitor,
  QuestionFilled,
  Refresh,
  Setting,
  User
} from "@element-plus/icons-vue";
import {countProblems} from "@/api/problem";
import {countOnline, freeDisk} from "@/api/file";
import {type Notice, recentNotices} from "@/api/notice";
import {ElNotification} from "element-plus";
import {copyTextToClipboard} from "@/utils/clipboard.ts";

const router = useRouter();
const loading = ref(false);
const syncState = ref<'loading' | 'ready' | 'partial'>('loading');
const lastUpdated = ref("");
const online = ref(0);
const problemNumber = ref(0);
const freeMemory = ref(0);
const totalMemory = ref(0);
const notices = ref<Notice[]>([]);

const usedMemory = computed(() => Math.max(totalMemory.value - freeMemory.value, 0));
const usedPercentage = computed(() => totalMemory.value > 0
    ? Math.min(100, Math.max(0, Math.round(usedMemory.value / totalMemory.value * 100)))
    : 0);
const syncLabel = computed(() => ({loading: "正在同步", ready: "数据已同步", partial: "部分数据暂不可用"}[syncState.value]));

const formatNumber = (value: number) => new Intl.NumberFormat('zh-CN', {maximumFractionDigits: 1}).format(value || 0);
const formatDate = (value: Date | string | undefined) => value ? new Date(value).toLocaleDateString('zh-CN', {month: '2-digit', day: '2-digit'}) : '—';

const metrics = computed(() => [
  {label: "在线用户", caption: "LIVE USERS", value: formatNumber(online.value), description: "当前活跃会话", icon: markRaw(User), tone: "blue"},
  {label: "题目总数", caption: "PROBLEM BANK", value: formatNumber(problemNumber.value), description: "题库中的全部题目", icon: markRaw(Collection), tone: "violet"},
  {label: "可用空间", caption: "AVAILABLE DISK", value: formatNumber(freeMemory.value), unit: " GB", description: `共 ${formatNumber(totalMemory.value)} GB 存储空间`, icon: markRaw(DataAnalysis), tone: "green", progress: usedPercentage.value, progressColor: "#10b981"},
  {label: "公告数量", caption: "ANNOUNCEMENTS", value: formatNumber(notices.value.length), description: "最近同步的公告", icon: markRaw(EditPen), tone: "amber"}
]);

const quickActions = [
  {label: "题目管理", description: "编辑与维护题库", route: "problem-edit", icon: markRaw(Collection), tone: "blue"},
  {label: "用户管理", description: "查看用户与角色", route: "user-manage", icon: markRaw(User), tone: "violet"},
  {label: "公告管理", description: "发布平台通知", route: "notice-manage", icon: markRaw(EditPen), tone: "amber"},
  {label: "系统设置", description: "配置后台权限", route: "auth-manage", icon: markRaw(Setting), tone: "green"}
];

const copyButtons = [
  {label: "全部基础设施", port: [8848, 15672, 3306, 6379, 9090], icon: markRaw(Monitor)},
  {label: "Nacos 配置中心", port: [8848], icon: markRaw(Setting)},
  {label: "RabbitMQ 控制台", port: [15672], icon: markRaw(DataAnalysis)},
  {label: "MySQL 数据库", port: [3306], icon: markRaw(Collection)},
  {label: "Redis 数据库", port: [6379], icon: markRaw(Collection)},
  {label: "MinIO 控制台", port: [9090], icon: markRaw(Monitor)}
];

const goToRoute = (name: string) => {
  router.push(router.hasRoute(name) ? {name} : {name: "backend-index"});
};

const handleCopy = (ports: number[]) => {
  // 端口监听在服务器本机，远端 SSH 使用本地配置中的 oj-server 别名。
  // 不能使用当前网页域名：那会让 SSH 尝试连接公网域名，而不是服务器 SSH 主机。
  const content = [
    "ssh",
    ...ports.map(port => `-L ${port}:127.0.0.1:${port}`),
    "root@oj-server"
  ].join(" ");
  copyTextToClipboard(content).then(() => {
    ElNotification({type: "success", message: "SSH 命令已复制", duration: 1400});
  }).catch(() => {
    ElNotification({type: "error", message: "复制失败，请检查浏览器权限", duration: 1800});
  });
};

const loadDashboard = async () => {
  loading.value = true;
  syncState.value = 'loading';
  const results = await Promise.allSettled([countProblems(), countOnline(), freeDisk(), recentNotices()]);
  const [problemResult, onlineResult, diskResult, noticeResult] = results;
  if (problemResult.status === 'fulfilled') problemNumber.value = problemResult.value;
  if (onlineResult.status === 'fulfilled') online.value = onlineResult.value;
  if (diskResult.status === 'fulfilled') {
    freeMemory.value = diskResult.value.free;
    totalMemory.value = diskResult.value.total;
  }
  if (noticeResult.status === 'fulfilled') notices.value = noticeResult.value;
  syncState.value = results.every(result => result.status === 'fulfilled') ? 'ready' : 'partial';
  lastUpdated.value = new Date().toLocaleTimeString('zh-CN', {hour: '2-digit', minute: '2-digit'});
  loading.value = false;
};

loadDashboard();
</script>

<style scoped>
.admin-dashboard { width: 100%; max-width: 1440px; margin: 0 auto; color: var(--el-text-color-primary); }
.dashboard-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 22px; }
.dashboard-kicker, .panel-kicker { margin: 0 0 7px; color: var(--el-color-primary); font-size: 10px; font-weight: 800; letter-spacing: .18em; }
.dashboard-heading h1 { margin: 0; color: var(--el-text-color-primary); font-size: 30px; font-weight: 750; letter-spacing: -.04em; }
.dashboard-description { margin: 7px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
.heading-actions { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; justify-content: flex-end; }
.sync-state { display: inline-flex; align-items: center; color: var(--el-text-color-secondary); font-size: 12px; white-space: nowrap; }.sync-state i { width: 7px; height: 7px; margin-right: 7px; border-radius: 50%; background: var(--el-color-success); }.sync-state--loading i { background: var(--el-color-warning); animation: pulse 1.2s ease-in-out infinite; }.sync-state--partial i { background: var(--el-color-danger); }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin-bottom: 16px; }
.metric-card, .panel { border: 1px solid var(--el-border-color-lighter); border-radius: 12px; background: var(--el-bg-color); box-shadow: 0 4px 18px rgb(15 23 42 / 4%); }
.metric-card { min-height: 142px; padding: 18px 20px; box-sizing: border-box; }.metric-card__top { display: flex; align-items: center; justify-content: space-between; gap: 12px; }.metric-icon, .quick-action__icon, .tool-row__icon { display: inline-grid; place-items: center; flex: 0 0 auto; border-radius: 9px; }.metric-icon { width: 34px; height: 34px; font-size: 17px; }
.metric-icon--blue, .quick-action__icon--blue { color: #2563eb; background: rgb(37 99 235 / 12%); }.metric-icon--violet, .quick-action__icon--violet { color: #7c3aed; background: rgb(124 58 237 / 12%); }.metric-icon--green, .quick-action__icon--green { color: #059669; background: rgb(5 150 105 / 12%); }.metric-icon--amber, .quick-action__icon--amber { color: #d97706; background: rgb(217 119 6 / 12%); }
.metric-caption { color: var(--el-text-color-secondary); font-size: 10px; font-weight: 700; letter-spacing: .1em; }.metric-value { margin-top: 15px; color: var(--el-text-color-primary); font-size: 28px; font-weight: 750; letter-spacing: -.04em; line-height: 1; }.metric-value small { margin-left: 3px; font-size: 13px; font-weight: 600; letter-spacing: 0; }.metric-card p { margin: 9px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }.metric-card :deep(.el-progress) { margin-top: 11px; }
.dashboard-grid { display: grid; gap: 16px; margin-bottom: 16px; }.dashboard-grid--primary { grid-template-columns: minmax(0, 1.35fr) minmax(340px, .85fr); }.dashboard-grid--secondary { grid-template-columns: minmax(0, 1.1fr) minmax(340px, .9fr); }.panel { min-width: 0; padding: 20px; }.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 15px; margin-bottom: 16px; }.panel-kicker { margin-bottom: 4px; color: var(--el-text-color-secondary); }.panel-heading h2 { margin: 0; font-size: 17px; font-weight: 700; letter-spacing: -.02em; }.panel-note { color: var(--el-text-color-secondary); font-size: 12px; }.panel-help { color: var(--el-text-color-secondary); cursor: help; }
.notice-list { min-height: 208px; }.notice-row, .quick-action, .tool-row { width: 100%; border: 0; border-bottom: 1px solid var(--el-border-color-lighter); background: transparent; color: inherit; text-align: left; cursor: pointer; }.notice-row { display: flex; align-items: center; gap: 12px; min-height: 52px; padding: 8px 0; }.notice-row:last-child, .tool-row:last-child { border-bottom: 0; }.notice-row:hover strong, .quick-action:hover strong { color: var(--el-color-primary); }.notice-marker { width: 7px; height: 7px; border-radius: 50%; background: var(--el-color-primary-light-5); }.notice-marker--important { background: var(--el-color-danger); }.notice-main { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 3px; }.notice-main strong, .quick-action strong, .tool-row strong { overflow: hidden; font-size: 13px; font-weight: 600; white-space: nowrap; text-overflow: ellipsis; }.notice-main small, .quick-action small, .tool-row small { color: var(--el-text-color-secondary); font-size: 11px; }.notice-arrow, .quick-action__arrow, .tool-row__copy { color: var(--el-text-color-placeholder); font-size: 15px; transition: transform .18s ease, color .18s ease; }.notice-row:hover .notice-arrow, .quick-action:hover .quick-action__arrow { color: var(--el-color-primary); transform: translateX(3px); }
.quick-actions { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 9px; }.quick-action { display: flex; align-items: center; gap: 10px; min-height: 76px; padding: 11px; border: 1px solid var(--el-border-color-lighter); border-radius: 9px; transition: border-color .18s ease, background .18s ease; }.quick-action:hover { border-color: var(--el-color-primary-light-5); background: var(--el-fill-color-light); }.quick-action__icon { width: 31px; height: 31px; font-size: 16px; }.quick-action > span:nth-child(2) { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 3px; }.quick-action__arrow { font-size: 13px; }
.resource-summary { display: flex; align-items: center; gap: 28px; min-height: 150px; }.resource-ring { position: relative; display: grid; width: 126px; height: 126px; flex: 0 0 auto; place-items: center; border-radius: 50%; background: conic-gradient(var(--el-color-primary) var(--resource-progress), var(--el-fill-color-darker) 0); }.resource-ring::before { position: absolute; width: 98px; height: 98px; border-radius: 50%; background: var(--el-bg-color); content: ""; }.resource-ring > div { position: relative; display: flex; align-items: center; flex-direction: column; }.resource-ring strong { font-size: 25px; letter-spacing: -.04em; }.resource-ring small { color: var(--el-text-color-secondary); font-size: 11px; }.resource-copy { min-width: 0; }.resource-number { display: flex; align-items: baseline; gap: 7px; }.resource-number strong { font-size: 26px; letter-spacing: -.04em; }.resource-number span { color: var(--el-text-color-secondary); font-size: 12px; }.resource-copy p { margin: 8px 0 16px; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.7; }.resource-legend { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; color: var(--el-text-color-secondary); font-size: 11px; }.resource-legend span, .resource-legend b { width: 6px; height: 6px; border-radius: 50%; background: var(--el-color-primary); }.resource-legend b { margin-left: 6px; background: var(--el-fill-color-darker); }
.tool-list { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); column-gap: 22px; }.tool-row { display: flex; align-items: center; gap: 10px; min-height: 58px; padding: 7px 0; }.tool-row__icon { width: 28px; height: 28px; color: var(--el-text-color-secondary); background: var(--el-fill-color-light); }.tool-row > span:nth-child(2) { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 2px; }.tool-row__copy { font-size: 14px; }.tool-row:hover .tool-row__copy { color: var(--el-color-primary); }
@keyframes pulse { 50% { opacity: .35; } }
@media (max-width: 1050px) { .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.dashboard-grid--primary, .dashboard-grid--secondary { grid-template-columns: 1fr; } }
@media (max-width: 650px) { .dashboard-heading { align-items: flex-start; flex-direction: column; gap: 16px; }.heading-actions { justify-content: flex-start; }.metric-card { padding: 15px; }.panel { padding: 16px; }.quick-actions, .tool-list { grid-template-columns: 1fr; }.resource-summary { align-items: flex-start; gap: 18px; }.resource-ring { width: 100px; height: 100px; }.resource-ring::before { width: 78px; height: 78px; }.resource-ring strong { font-size: 20px; } }
</style>
