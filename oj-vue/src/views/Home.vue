<template>
  <main class="home-page">
    <section class="home-hero">
      <div class="home-hero__copy">
        <p class="home-eyebrow">延拓 CODE · ONLINE JUDGE</p>
        <h1>让每一次提交，<span>都更接近答案</span></h1>
        <p class="home-description">练习、竞赛与分享，构建属于你的编程成长空间。</p>
        <div class="hero-actions">
          <el-button type="primary" round @click="go('problems')">开始刷题</el-button>
          <el-button round @click="go('solutions')">浏览题解</el-button>
        </div>
      </div>
      <div class="hero-decoration" aria-hidden="true"><span>&lt;/&gt;</span><i /></div>
    </section>

    <section class="home-stats" aria-label="平台概览">
      <div v-for="stat in stats" :key="stat.label" class="stat-item">
        <span class="stat-icon"><el-icon><component :is="stat.icon" /></el-icon></span>
        <span><strong>{{ stat.value }}</strong><small>{{ stat.label }}</small></span>
      </div>
    </section>

    <section class="home-grid">
      <div class="home-feed">
        <home-section-card title="公告" icon="Notification" more @show-more="go('notification')">
          <div class="home-list" v-loading="loading">
            <button v-for="(item, index) in notices" :key="item.noticeId" class="home-row home-row--notice" type="button" @click="go('notice', item.noticeId)">
              <span class="home-row__index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="home-row__content">
                <span class="home-row__title"><span>{{ item.title }}</span><el-tag v-if="item.topUp" type="danger" size="small">重要</el-tag></span>
                <span class="home-row__meta">平台公告 <i /> {{ formatDate(item.createTime) }}</span>
              </span>
              <el-icon class="home-row__arrow"><ArrowRight /></el-icon>
            </button>
            <el-empty v-if="!loading && notices.length === 0" :image-size="56" description="暂无公告" />
          </div>
        </home-section-card>

        <home-section-card title="最近题解" icon="EditPen" more @show-more="go('solutions')">
          <div class="home-list" v-loading="loading">
            <button v-for="item in solutions" :key="item.solutionId" class="home-row home-row--solution" type="button" @click="go('solution', item.solutionId)">
              <avatar class="home-row__avatar" :user-id="item.userId" />
              <span class="home-row__content">
                <span class="home-row__title"><span>{{ item.title || '未命名题解' }}</span><el-tag v-if="item.topUp" type="warning" size="small">置顶</el-tag></span>
                <span class="home-row__meta"><b>{{ item.nikeName || '匿名用户' }}</b><i /> {{ item.problemTitle || '通用题解' }} <i /> {{ formatDate(item.createTime) }}</span>
              </span>
              <el-icon class="home-row__arrow"><ArrowRight /></el-icon>
            </button>
            <el-empty v-if="!loading && solutions.length === 0" :image-size="56" description="暂无题解" />
          </div>
        </home-section-card>

        <home-section-card title="最近题目" icon="Collection" more @show-more="go('problems')">
          <div class="home-list" v-loading="loading">
            <button v-for="item in problems" :key="item.problemId" class="home-row home-row--problem" type="button" @click="go('problem', item.problemId)">
              <span class="problem-mark">#</span>
              <span class="home-row__content">
                <span class="home-row__title"><span>{{ item.title }}</span><el-tag size="small" type="primary">{{ problemTypeToString(item.type) }}</el-tag></span>
                <span class="home-row__meta"><span class="code-id" :title="String(item.problemId)">ID {{ shortId(item.problemId) }}</span><i /> {{ item.source || '平台题目' }}</span>
              </span>
              <el-icon class="home-row__arrow"><ArrowRight /></el-icon>
            </button>
            <el-empty v-if="!loading && problems.length === 0" :image-size="56" description="暂无题目" />
          </div>
        </home-section-card>
      </div>

      <home-section-card class="ranking-card" title="排行榜" icon="Histogram">
        <div class="ranking-intro">
          <div><strong>积分榜</strong><span>持续练习，保持领先</span></div>
          <span class="ranking-count">TOP {{ ranks.length }}</span>
        </div>
        <div v-loading="loading" class="ranking-list">
          <div v-for="(item, index) in ranks" :key="item.userId" class="ranking-row" :class="`ranking-row--${index + 1}`">
            <span class="ranking-position">
              <el-icon v-if="index < 3"><Trophy /></el-icon>
              <b v-else>{{ String(index + 1).padStart(2, '0') }}</b>
            </span>
            <avatar class="ranking-avatar" :user-id="item.userId" />
            <span class="ranking-user"><strong>{{ item.nikeName || item.userName || '未设置昵称' }}</strong><small>@{{ item.userName || 'anonymous' }}</small></span>
            <span class="ranking-points"><strong>{{ item.points ?? 0 }}</strong><small>积分</small></span>
          </div>
          <el-empty v-if="!loading && ranks.length === 0" :image-size="56" description="暂无排行数据" />
        </div>
      </home-section-card>
    </section>
  </main>
</template>

<script setup lang="ts">
import {computed, onMounted, ref} from "vue";
import {useRouter} from "vue-router";
import {ArrowRight, Trophy} from "@element-plus/icons-vue";
import HomeSectionCard from "@/components/HomeSectionCard/HomeSectionCard.vue";
import Avatar from "@/components/Avatar/Avatar.vue";
import {rank, type UserView} from "@/api/user";
import {type ProblemView, recentProblem} from "@/api/problem";
import {problemTypeToString} from "@/utils/problem";
import {recentSolution, type Solution} from "@/api/solution";
import {type Notice, recentNotices} from "@/api/notice";
import type {IdType} from "@/api/common.ts";

const router = useRouter();
const loading = ref(true);
const ranks = ref<UserView[]>([]);
const problems = ref<ProblemView[]>([]);
const solutions = ref<Solution[]>([]);
const notices = ref<Notice[]>([]);
const stats = computed(() => [
  {icon: "Collection", label: "近期题目", value: problems.value.length},
  {icon: "EditPen", label: "近期题解", value: solutions.value.length},
  {icon: "Trophy", label: "榜单用户", value: ranks.value.length},
]);

const shortId = (value: IdType) => {
  const text = String(value);
  return text.length > 18 ? `${text.slice(0, 8)}…${text.slice(-6)}` : text;
};

const formatDate = (value: Date | string | undefined) => value
  ? new Date(value).toLocaleDateString('zh-CN', {month: '2-digit', day: '2-digit'})
  : '—';

const loadHomeData = async () => {
  loading.value = true;
  const [rankData, noticeData, problemData, solutionData] = await Promise.all([
    rank(20).catch(() => [] as UserView[]),
    recentNotices().catch(() => [] as Notice[]),
    recentProblem().catch(() => [] as ProblemView[]),
    recentSolution().catch(() => [] as Solution[]),
  ]);
  ranks.value = rankData.slice(0, 20);
  notices.value = noticeData;
  problems.value = problemData;
  solutions.value = solutionData;
  loading.value = false;
};

const go = (name: string, id?: IdType) => router.push(id === undefined ? {name} : {name, params: {id}});
onMounted(loadHomeData);
</script>

<style scoped>
.home-page { max-width: var(--page-max-width); margin: 0 auto; padding: 8px 0 28px; color: var(--el-text-color-primary); }
.home-hero { position: relative; display: flex; min-height: 220px; align-items: center; justify-content: space-between; margin-bottom: 18px; padding: 38px 48px; overflow: hidden; border: 1px solid var(--el-border-color-light); border-radius: 22px; background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-bg-color)); }
.home-hero__copy { position: relative; z-index: 1; }
.home-eyebrow { margin: 0 0 12px; color: var(--el-color-primary); font-size: 11px; font-weight: 800; letter-spacing: .18em; }
.home-hero h1 { margin: 0; font-size: clamp(28px, 4vw, 46px); letter-spacing: -.05em; }.home-hero h1 span { color: var(--el-color-primary); }
.home-description { margin: 14px 0 20px; color: var(--el-text-color-secondary); font-size: 15px; }.hero-actions { display: flex; gap: 8px; }
.hero-decoration { position: absolute; right: 7%; color: var(--el-color-primary-light-5); font: 900 120px/1 var(--code-font-family, monospace); transform: rotate(-8deg); opacity: .32; }.hero-decoration i { position: absolute; right: -20px; bottom: -10px; width: 92px; height: 16px; border-radius: 50%; background: var(--el-color-primary-light-7); filter: blur(12px); }
.home-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 18px; }.stat-item { display: flex; align-items: center; gap: 12px; padding: 16px 20px; border: 1px solid var(--el-border-color-light); border-radius: 14px; background: var(--el-bg-color); }.stat-icon { display: grid; width: 38px; height: 38px; place-items: center; border-radius: 11px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font-size: 19px; }.stat-item strong, .stat-item small { display: block; }.stat-item strong { font-size: 21px; }.stat-item small { margin-top: 2px; color: var(--el-text-color-secondary); }
.home-grid { display: grid; grid-template-columns: minmax(0, 1.7fr) minmax(300px, .8fr); gap: 18px; align-items: stretch; }.home-feed { display: flex; flex-direction: column; gap: 18px; }.ranking-card { display: flex; height: 100%; box-sizing: border-box; flex-direction: column; }.ranking-card :deep(.el-card__body) { flex: 1; }
.home-list { min-height: 40px; }.home-row { display: flex; width: 100%; min-width: 0; align-items: center; gap: 12px; padding: 13px 0; border: 0; border-bottom: 1px solid var(--el-border-color-lighter); color: inherit; text-align: left; background: transparent; cursor: pointer; transition: padding .18s ease, color .18s ease, background-color .18s ease; }.home-row:last-of-type { border-bottom: 0; }.home-row:hover { padding-right: 5px; padding-left: 5px; border-radius: 10px; background: var(--el-fill-color-light); }.home-row__index { width: 25px; flex: 0 0 auto; color: var(--el-color-primary); font: 700 11px var(--code-font-family, monospace); }.home-row__content { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 5px; }.home-row__title { display: flex; min-width: 0; align-items: center; gap: 8px; }.home-row__title > span:first-child { overflow: hidden; font-size: 13px; font-weight: 650; text-overflow: ellipsis; white-space: nowrap; }.home-row__title :deep(.el-tag) { flex: 0 0 auto; }.home-row__meta { display: flex; min-width: 0; align-items: center; gap: 7px; overflow: hidden; color: var(--el-text-color-secondary); font-size: 11px; text-overflow: ellipsis; white-space: nowrap; }.home-row__meta i { width: 3px; height: 3px; flex: 0 0 auto; border-radius: 50%; background: var(--el-text-color-placeholder); }.home-row__meta b { overflow: hidden; max-width: 120px; color: var(--el-color-primary); font-weight: 550; text-overflow: ellipsis; }.home-row__arrow { flex: 0 0 auto; color: var(--el-text-color-placeholder); transition: transform .18s ease, color .18s ease; }.home-row:hover .home-row__arrow { color: var(--el-color-primary); transform: translateX(3px); }.home-row__avatar { width: 34px; height: 34px; }.home-row__avatar :deep(.avatar__image) { width: 34px; height: 34px; }.problem-mark { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border: 1px solid var(--el-border-color-light); border-radius: 11px; color: var(--el-color-primary); background: var(--el-color-primary-light-9); font: 700 15px var(--code-font-family, monospace); }.code-id { font-family: var(--code-font-family, monospace); }
.ranking-intro { display: flex; align-items: center; justify-content: space-between; padding: 14px 0 11px; }.ranking-intro strong, .ranking-intro span { display: block; }.ranking-intro strong { font-size: 13px; }.ranking-intro span { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }.ranking-intro .ranking-count { margin-top: 0; color: var(--el-color-warning); font: 700 11px var(--code-font-family, monospace); }.ranking-list { min-height: 120px; }.ranking-row { display: flex; min-width: 0; align-items: center; gap: 9px; min-height: 58px; border-top: 1px solid var(--el-border-color-lighter); }.ranking-position { display: grid; width: 23px; flex: 0 0 auto; place-items: center; color: var(--el-text-color-placeholder); font: 700 11px var(--code-font-family, monospace); }.ranking-position .el-icon { color: var(--el-text-color-placeholder); font-size: 16px; }.ranking-row--1 .ranking-position .el-icon { color: #e5a819; }.ranking-row--2 .ranking-position .el-icon { color: #9ba8b6; }.ranking-row--3 .ranking-position .el-icon { color: #bd7c52; }.ranking-avatar { width: 34px; height: 34px; }.ranking-avatar :deep(.avatar__image) { width: 34px; height: 34px; }.ranking-user { display: flex; min-width: 0; flex: 1; flex-direction: column; gap: 2px; }.ranking-user strong, .ranking-user small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.ranking-user strong { font-size: 12px; font-weight: 650; }.ranking-user small { color: var(--el-text-color-secondary); font-size: 10px; }.ranking-points { flex: 0 0 auto; text-align: right; }.ranking-points strong { display: block; color: var(--el-color-primary); font: 750 15px var(--code-font-family, monospace); }.ranking-points small { display: block; margin-top: 1px; color: var(--el-text-color-secondary); font-size: 10px; }
@media (max-width: 900px) { .home-page { padding: 8px 12px 28px; }.home-hero { padding: 30px; }.hero-decoration { right: -2%; }.home-grid { grid-template-columns: 1fr; }.ranking-card { height: auto; } }
@media (max-width: 560px) { .home-hero { min-height: 190px; padding: 25px 22px; }.hero-decoration { display: none; }.home-stats { gap: 8px; }.stat-item { padding: 12px 10px; gap: 7px; }.stat-icon { width: 30px; height: 30px; }.stat-item strong { font-size: 17px; }.stat-item small { font-size: 11px; }.home-row { gap: 9px; padding: 12px 0; }.home-row__meta b { max-width: 86px; }.home-row__avatar, .home-row__avatar :deep(.avatar__image), .problem-mark { width: 32px; height: 32px; }.home-row__index { width: 21px; }.ranking-row { min-height: 54px; } }
</style>
