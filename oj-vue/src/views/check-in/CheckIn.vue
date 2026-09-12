<template>
  <main class="check-in-container common-max-width-page">
    <header class="check-in-hero">
      <div class="hero-copy"><p class="eyebrow">DAILY PROGRESS</p><h1>每日签到</h1><p>保持一点点坚持，让今天的学习留下可见的进度。</p><span class="greeting">你好，{{ nikeName || '同学' }}</span></div>
      <div class="check-in-action">
        <div class="status-badge" :class="{'is-done': isCheckIn}"><el-icon><component :is="isCheckIn ? Select : Calendar" /></el-icon><span>{{ checkInText }}</span></div>
        <el-button
                type="success"
                class="check-in-button"
                size="large" icon="Edit"
                v-if="!isCheckIn"
                @click="handleClick"
                :loading="isLoading"
            >
              今日签到
            </el-button>
            <el-button
                type="success"
                class="check-in-button"
                size="large"
                icon="Select"
                disabled="disabled"
                v-else
            >
              今日已完成
            </el-button>
      </div>
      <div class="hero-orbit" aria-hidden="true"><el-icon><Calendar /></el-icon></div>
    </header>
    <section class="check-in-stats">
      <div class="stat-card"><span class="stat-icon"><el-icon><User /></el-icon></span><div><strong>{{ todayCheckInCount }}</strong><small>今日已签到</small></div></div>
      <div class="stat-card"><span class="stat-icon"><el-icon><Medal /></el-icon></span><div><strong>{{ latestContinuity }}</strong><small>连续签到天数</small></div></div>
      <div class="stat-card"><span class="stat-icon"><el-icon><TrendCharts /></el-icon></span><div><strong>{{ checkInRecords.length }}</strong><small>最近签到记录</small></div></div>
    </section>
    <section class="records-panel">
      <div class="panel-heading"><div><strong>最近签到</strong><small>看看大家正在坚持什么</small></div><el-icon><Clock /></el-icon></div>
      <el-table class="records-table" :data="checkInRecords" v-loading="isLoading">
          <el-table-column prop="userId" label="用户ID"/>
          <el-table-column prop="nikeName" label="用户昵称"/>
          <el-table-column prop="continuityDays" label="连续天数"/>
          <el-table-column prop="rewardPoint" label="奖励积分"/>
          <el-table-column prop="signTime" label="签到时间"/>
      </el-table>
      <el-empty v-if="checkInRecords.length === 0" description="还没有签到记录" />
    </section>

    <el-dialog
        v-model="centerDialogVisible"
        width="500"
        align-center
        class="absoluteCenter"
    >
      <div class="absoluteCenter">
        <el-icon size="50" color="#67C23A"><Select/></el-icon>
        <h3>{{ dialogTitle }}</h3>
        <p>{{ dialogBodyText }}</p>
      </div>

    </el-dialog>
  </main>
</template>

<script setup lang="ts">

import {computed, onMounted, ref} from "vue";
import {useUserStore} from "@/stores/useUserStore";
import {checkInFetcher, checkInList, isCheckedIn, todayCount, type UserCheckIn} from "@/api/check-in";
import {Calendar, Clock, Medal, Select, TrendCharts, User} from "@element-plus/icons-vue";
// 签到数据
const checkInRecords = ref<UserCheckIn[]>([]);
// 对话框显示变量
const centerDialogVisible = ref(false);
// 签到个数
const todayCheckInCount = ref(0);

// 签到对话框标题
const dialogTitle = ref<string>("")
// 签到对话框文字
const dialogBodyText = ref<string>("")

const nikeName = computed(() => {
  const userStore = useUserStore();
  return userStore.user?.nikeName;
});

const isCheckIn = ref(false)
const latestContinuity = computed(() => checkInRecords.value[0]?.continuityDays || 0);
const checkInText = computed(() => {
  return isCheckIn.value ? "您已经签到" : "您还未签到";
});
const {isLoading, loading, sendCheckIn} = checkInFetcher((data) => {
  // @ts-ignore
  data.userCheckIn.currentTime = new Date(Date.now()).toDateString();
  checkInRecords.value.unshift(data.userCheckIn);

  centerDialogVisible.value = true;
  dialogTitle.value = data.success ? "签到成功！" : "签到失败";
  dialogBodyText.value = data.success ?`${data.msg},得到${data.award}积分` : data.msg;
  isCheckIn.value = true;

  window.dispatchEvent(new Event('oj:point-refresh'));

  refresh();
});

const handleClick = () => {
  loading();
  sendCheckIn();
}

const refresh = () => {
  isCheckedIn().then((data) => {
    isCheckIn.value = data;
  });

  todayCount().then((data) => {todayCheckInCount.value = data;});
  checkInList().then(data => {
    checkInRecords.value = data;
  });
}


refresh();


</script>


<style scoped>
.check-in-container { margin: auto; padding-bottom: 28px; color: var(--el-text-color-primary); }.check-in-hero { position: relative; display: flex; justify-content: space-between; align-items: center; min-height: 220px; margin-bottom: 18px; padding: 36px 48px; overflow: hidden; border: 1px solid var(--el-border-color-light); border-radius: 20px; background: linear-gradient(135deg, var(--el-color-success-light-9), var(--el-bg-color)); }.hero-copy { position: relative; z-index: 1; }.eyebrow { margin: 0 0 8px; color: var(--el-color-success); font-size: 11px; font-weight: 800; letter-spacing: .18em; }.hero-copy h1 { margin: 0; font-size: clamp(30px, 4vw, 46px); letter-spacing: -.05em; }.hero-copy > p:not(.eyebrow) { margin: 12px 0 15px; color: var(--el-text-color-secondary); }.greeting { color: var(--el-text-color-regular); font-size: 14px; }.check-in-action { position: relative; z-index: 1; display: flex; flex-direction: column; align-items: center; gap: 15px; min-width: 190px; }.status-badge { display: flex; align-items: center; gap: 7px; color: var(--el-color-warning); font-size: 14px; }.status-badge.is-done { color: var(--el-color-success); }.check-in-button { width: 180px; height: 48px; border: 0; border-radius: 13px; font-size: 16px; font-weight: 700; box-shadow: 0 9px 20px color-mix(in srgb, var(--el-color-success) 24%, transparent); }.hero-orbit { position: absolute; right: 22%; top: 20%; display: grid; width: 200px; height: 200px; place-items: center; border: 1px solid color-mix(in srgb, var(--el-color-success) 25%, transparent); border-radius: 50%; color: var(--el-color-success-light-5); font-size: 70px; opacity: .45; }.check-in-stats { display: grid; grid-template-columns: repeat(3, 1fr); gap: 14px; margin-bottom: 18px; }.stat-card { display: flex; align-items: center; gap: 12px; padding: 17px 20px; border: 1px solid var(--el-border-color-light); border-radius: 14px; background: var(--el-bg-color); }.stat-icon { display: grid; width: 38px; height: 38px; place-items: center; border-radius: 11px; color: var(--el-color-success); background: var(--el-color-success-light-9); font-size: 19px; }.stat-card strong, .stat-card small { display: block; }.stat-card strong { font-size: 21px; }.stat-card small { margin-top: 2px; color: var(--el-text-color-secondary); }.records-panel { padding: 22px 24px; border: 1px solid var(--el-border-color-light); border-radius: 18px; background: var(--el-bg-color); }.panel-heading { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }.panel-heading strong, .panel-heading small { display: block; }.panel-heading small { margin-top: 4px; color: var(--el-text-color-secondary); font-size: 12px; }.panel-heading > .el-icon { color: var(--el-color-success); font-size: 22px; }.records-table :deep(.el-table__cell) { padding: 12px 8px; }.records-table :deep(.el-table__inner-wrapper::before) { display: none; }
@media (max-width: 650px) { .check-in-container { padding: 8px 12px 28px; }.check-in-hero { min-height: 300px; padding: 28px 22px; align-items: flex-start; }.check-in-action { position: absolute; right: 22px; bottom: 25px; min-width: 0; }.hero-orbit { right: -35px; top: -35px; }.check-in-stats { gap: 8px; }.stat-card { padding: 12px 10px; gap: 7px; }.stat-icon { width: 30px; height: 30px; }.stat-card strong { font-size: 17px; }.stat-card small { font-size: 11px; }.records-panel { padding: 16px 12px; overflow: auto; }.records-table { min-width: 560px; } }
</style>
