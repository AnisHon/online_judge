<template>
  <main class="activity-page">
    <header class="activity-header" :class="`activity-header--${kind.toLowerCase()}`">
      <div><p class="eyebrow">{{ activityMeta.eyebrow }}</p>
        <h1>{{ activityMeta.title }}</h1>
        <p>{{ activityMeta.description }}</p></div>
      <div class="activity-header__mark">
        <el-icon>
          <component :is="activityMeta.icon"/>
        </el-icon>
      </div>
    </header>
    <section class="activity-list" v-loading="isLoading || actionLoadingIds.size > 0">
      <el-card v-for="item in list" :key="item.contestId" class="activity-card" shadow="hover">
        <div class="activity-card__icon">
          <el-icon>
            <component :is="activityMeta.icon"/>
          </el-icon>
        </div>
        <div class="activity-card__main">
          <div class="activity-card__title"><h2>{{ item.title }}</h2>
            <el-badge :value="item.joinedNumber || 0" :show-zero="false"/>
          </div>
          <div class="activity-card__meta"><span><el-icon><Calendar/></el-icon>{{
              formatDate(item.startTime)
            }} — {{ formatDate(item.endTime) }}</span><span><el-icon><Clock/></el-icon>{{
              differ(item.startTime, item.endTime)
            }}</span></div>
        </div>
        <div class="activity-card__action">
          <el-tag :type="authTagType(item.auth)">{{ authText(item.auth) }}</el-tag>
          <el-button class="enter-button" :type="buttonType(item)"
                     :disabled="!isTimeValid(item) || isNotStarted(item.startTime)"
                     :loading="isActivityLoading(item.contestId)"
                     @click="joinActivity(item)">{{ buttonText(item) }}
          </el-button>
        </div>
      </el-card>
      <el-alert v-if="error" class="activity-error" type="error" :closable="false" show-icon>
        <template #title>
          <span>{{ error }}</span>
          <el-button link type="primary" @click="getList">重试</el-button>
        </template>
      </el-alert>
      <el-empty v-if="!isLoading && !error && list.length === 0" :description="`还没有任何${activityMeta.title}`"/>
    </section>
    <div class="activity-pagination">
      <pagination v-show="total > 0" :total="total" :background="false" v-model:page="page.currentPage"
                  v-model:limit="page.pageSize" @pagination="getList"/>
    </div>
    <el-dialog v-model="passwordDialog" :title="`进入私有${activityMeta.title}`" width="min(520px, 92vw)"
               @closed="resetPasswordDialog">
      <el-form :model="form" @submit.prevent="submit">
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入活动密码" autocomplete="off"
                    maxlength="32" show-password/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialog = false">取消</el-button>
        <el-button type="primary" :loading="isPasswordSubmitting" :disabled="isPasswordSubmitting"
                   @click="submit">确认进入</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import {computed, onMounted, onUnmounted, reactive, ref} from "vue";
import {Calendar, Clock, Collection, Trophy} from "@element-plus/icons-vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {
  ContestAuth,
  type ContestView,
  getContest,
  isContestJoined,
  joinContest,
  type JoinContestRequest,
  type PageContest
} from "@/api/contest";
import {authTagType, authText, differ, formatDate, getActivityTimeState, isContestOver, isNotStart} from "@/utils/contest";
import {ElNotification} from "element-plus";
import {useRouter} from "vue-router";
import type {IdType} from "@/api/common.ts";
import dayjs from "dayjs";

type ActivityKind = "CONTEST" | "HOMEWORK";
const {kind} = defineProps<{ kind: ActivityKind }>();
const router = useRouter();
const activityMeta = computed(() => ACTIVITY_META[kind]);
const list = ref<ContestView[]>([]);
const total = ref(0);
const passwordDialog = ref(false);
const currentActivity = ref<ContestView>();
const page = reactive<PageContest>({pageSize: 10, currentPage: 1, type: kind});
const form = reactive<JoinContestRequest>({contestId: "", password: undefined});
const error = ref('');
const isLoading = ref(false);
const isPasswordSubmitting = ref(false);
const actionLoadingIds = ref(new Set<string>());
const now = ref(dayjs());
let clockTimer: ReturnType<typeof setInterval> | undefined;
let listRequestSequence = 0;

const ACTIVITY_META: Record<ActivityKind, {title: string; eyebrow: string; description: string; icon: typeof Trophy}> = {
  CONTEST: {
    title: '比赛',
    eyebrow: 'CHALLENGE ARENA',
    description: '在限定时间内解决问题，和大家一起检验你的能力。',
    icon: Trophy,
  },
  HOMEWORK: {
    title: '作业',
    eyebrow: 'LEARNING SPACE',
    description: '按计划完成练习，把知识点一步步变成真正的能力。',
    icon: Collection,
  },
};

const enter = (id: IdType) => router.push({name: "contest-problems", params: {id}});

const setActivityLoading = (id: IdType, loading: boolean) => {
  const next = new Set(actionLoadingIds.value);
  const key = String(id);
  if (loading) next.add(key); else next.delete(key);
  actionLoadingIds.value = next;
};
const isActivityLoading = (id: IdType) => actionLoadingIds.value.has(String(id));
const isNotStarted = (start: string | undefined) => isNotStart(start, now.value);
const isActivityOver = (end: string | undefined) => isContestOver(end, now.value);
const isTimeValid = (item: ContestView) => getActivityTimeState(item.startTime, item.endTime, now.value).valid;

const getList = async () => {
  const sequence = ++listRequestSequence;
  isLoading.value = true;
  error.value = '';
  try {
    const data = await getContest({...page, type: kind});
    if (sequence !== listRequestSequence) return;
    total.value = data.totalRecords;
    list.value = data.data;
  } catch (reason) {
    if (sequence === listRequestSequence) {
      error.value = reason instanceof Error ? reason.message : '活动列表加载失败，请稍后重试';
    }
  } finally {
    if (sequence === listRequestSequence) isLoading.value = false;
  }
};

const joinActivity = async (activity: ContestView) => {
  if (isActivityLoading(activity.contestId) || !isTimeValid(activity) || isNotStarted(activity.startTime)) return;
  setActivityLoading(activity.contestId, true);
  try {
    const joined = await isContestJoined(activity.contestId);
    if (isActivityOver(activity.endTime) && !joined) {
      ElNotification.warning(`您未参加该${activityMeta.value.title}`);
      return;
    }
    if (joined) {
      enter(activity.contestId);
      return;
    }
    if (activity.auth === ContestAuth.PRIVATE) {
      currentActivity.value = {...activity};
      form.contestId = activity.contestId;
      form.password = undefined;
      passwordDialog.value = true;
      return;
    }
    if (activity.auth === ContestAuth.WHITE_LIST) {
      ElNotification.warning('当前活动仅限白名单用户');
      return;
    }
    await completeJoin(activity);
  } catch (reason) {
    ElNotification.error(reason instanceof Error ? reason.message : '进入活动失败，请稍后重试');
  } finally {
    setActivityLoading(activity.contestId, false);
  }
};

const completeJoin = async (activity: ContestView) => {
  const data = await joinContest({contestId: activity.contestId, password: form.password});
  if (!data.success) {
    ElNotification.error(data.message || '加入活动失败');
    return;
  }
  passwordDialog.value = false;
  ElNotification.success(`${activityMeta.value.title}加入成功`);
  enter(activity.contestId);
};

const resetPasswordDialog = () => {
  if (isPasswordSubmitting.value) return;
  currentActivity.value = undefined;
  form.contestId = '';
  form.password = undefined;
};

const submit = async () => {
  const activity = currentActivity.value;
  if (!activity || isPasswordSubmitting.value) return;
  isPasswordSubmitting.value = true;
  setActivityLoading(activity.contestId, true);
  try {
    await completeJoin(activity);
  } catch (reason) {
    ElNotification.error(reason instanceof Error ? reason.message : '加入活动失败，请检查密码后重试');
  } finally {
    isPasswordSubmitting.value = false;
    setActivityLoading(activity.contestId, false);
  }
};

const buttonText = (item: ContestView) => {
  const state = getActivityTimeState(item.startTime, item.endTime, now.value);
  return !state.valid ? '时间异常' : state.over ? '查看活动' : state.notStarted ? '未开始' : '进入活动';
};
const buttonType = (item: ContestView) => {
  const state = getActivityTimeState(item.startTime, item.endTime, now.value);
  return !state.valid ? 'danger' : state.over ? 'info' : state.notStarted ? 'warning' : 'primary';
};

onMounted(() => {
  clockTimer = setInterval(() => { now.value = dayjs(); }, 30_000);
  void getList();
});
onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer);
  listRequestSequence++;
});
</script>

<style scoped>
.activity-page {
  max-width: var(--page-max-width);
  margin: 0 auto;
  padding: 0 clamp(10px, 2vw, 24px) 28px;
  color: var(--el-text-color-primary);
}

.activity-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 132px;
  margin-bottom: 18px;
  padding: 24px clamp(22px, 4vw, 40px);
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 18px;
  background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-bg-color));
}

.activity-header--homework {
  background: linear-gradient(135deg, var(--el-color-success-light-9), var(--el-bg-color));
}

.eyebrow {
  margin: 0 0 7px;
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .18em;
}

.activity-header--homework .eyebrow {
  color: var(--el-color-success);
}

.activity-header h1 {
  margin: 0;
  font-size: 30px;
  letter-spacing: -.04em;
}

.activity-header p:last-child {
  margin: 9px 0 0;
  color: var(--el-text-color-secondary);
}

.activity-header__mark {
  margin-right: 7%;
  color: var(--el-color-primary-light-5);
  font-size: 78px;
  transform: rotate(-10deg);
  opacity: .45;
}

.activity-header--homework .activity-header__mark {
  color: var(--el-color-success-light-5);
}

.activity-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 220px;
  padding-inline: clamp(2px, 1vw, 10px);
}

.activity-error {
  margin: 4px 0;
}

.activity-error :deep(.el-alert__title) {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.activity-card {
  display: flex;
  align-items: center;
  border-radius: 16px;
  padding: 0 12px;
  transition: transform .2s, border-color .2s;
}

.activity-card:hover {
  transform: translateY(-2px);
  border-color: var(--el-color-primary-light-5);
}

.activity-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 18px;
  padding: 22px clamp(24px, 4vw, 40px);
}

.activity-card__icon {
  display: grid;
  flex: 0 0 auto;
  width: 52px;
  height: 52px;
  place-items: center;
  border-radius: 15px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  font-size: 25px;
}

.activity-header--homework ~ .activity-list .activity-card__icon {
  color: var(--el-color-success);
  background: var(--el-color-success-light-9);
}

.activity-card__main {
  min-width: 0;
  flex: 1;
}

.activity-card__title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.activity-card h2 {
  overflow: hidden;
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 18px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.activity-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.activity-card__meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.activity-card__action {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
}

.enter-button {
  min-width: 92px;
}

.activity-pagination {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

@media (max-width: 650px) {
  .activity-page {
    padding: 8px 12px 28px;
  }

  .activity-header {
    min-height: 108px;
    padding: 20px;
  }

  .activity-header h1 {
    font-size: 25px;
  }

  .activity-header p:last-child {
    font-size: 13px;
  }

  .activity-header__mark {
    display: none;
  }

  .activity-card :deep(.el-card__body) {
    display: grid;
    grid-template-columns: 42px minmax(0, 1fr);
    padding: 17px 18px;
    gap: 12px;
  }

  .activity-card__icon {
    width: 42px;
    height: 42px;
    font-size: 20px;
  }

  .activity-card__action {
    grid-column: 1 / -1;
    width: 100%;
    justify-content: space-between;
  }

  .activity-card__meta {
    min-width: 0;
    gap: 8px;
  }

  .activity-card__title,
  .activity-card__title h2 {
    min-width: 0;
  }

  .activity-card__title h2 {
    flex: 1;
  }

  .activity-card__action .el-tag {
    max-width: 52%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .enter-button {
    min-width: 84px;
  }
}
</style>
