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
import {computed, reactive, ref} from "vue";
import {Calendar, Clock, Collection, Trophy} from "@element-plus/icons-vue";
import Pagination from "@/components/pageination/Pagination.vue";
import {
  ContestAuth,
  type ContestView,
  debouncedGetContest,
  debouncedIsJoined,
  debouncedJoin,
  type JoinContestRequest,
  type PageContest
} from "@/api/contest";
import {authTagType, authText, differ, isContestOver, isNotStart} from "@/utils/contest";
import {formatDate} from "@/utils/contest";
import {ElNotification} from "element-plus";
import {useRouter} from "vue-router";
import type {IdType} from "@/api/common.ts";

type ActivityKind = "CONTEST" | "HOMEWORK";
const {kind} = defineProps<{ kind: ActivityKind }>();
const router = useRouter();
const pageTitle = computed(() => kind === "CONTEST" ? "比赛" : "作业");
const list = reactive<ContestView[]>([]);
const total = ref(0);
const passwordDialog = ref(false);
const currentActivity = ref<ContestView>();
const page = reactive<PageContest>({pageSize: 10, currentPage: 1, type: kind});
const form = reactive<JoinContestRequest>({contestId: "", password: undefined});
const enter = (id: IdType) => router.push({name: "contest-problems", params: {id}});
const {isLoading, loading, get} = debouncedGetContest(page, data => {
  total.value = data.totalRecords;
  list.splice(0, list.length, ...data.data);
});
const {post: join} = debouncedJoin(form, data => {
  passwordDialog.value = false;
  if (data.success) {
    ElNotification.success(`${pageTitle.value}加入成功`);
    enter(currentActivity.value!.contestId);
  } else ElNotification.error(data.message);
});
const {isLoading: isJoinedLoading, loading: joinedLoading, get: joinedGet} = debouncedIsJoined(success => {
  const activity = currentActivity.value!;
  if (isContestOver(activity.endTime) && !success) return ElNotification.warning(`您未参加该${kind === 'CONTEST' ? '比赛' : '作业'}`);
  if (success) return enter(activity.contestId);
  if (activity.auth === ContestAuth.PUBLIC) join();
  else if (activity.auth === ContestAuth.PRIVATE) passwordDialog.value = true;
  else ElNotification.warning("当前活动仅限白名单用户");
});
const joinActivity = (activity: ContestView) => {
  currentActivity.value = activity;
  form.contestId = activity.contestId;
  form.password = undefined;
  joinedLoading();
  joinedGet(activity.contestId);
};
const submit = () => join();
const buttonText = (item: ContestView) => isContestOver(item.endTime) ? "查看活动" : isNotStart(item.startTime) ? "未开始" : "进入活动";
const buttonType = (item: ContestView) => isContestOver(item.endTime) ? "info" : isNotStart(item.startTime) ? "warning" : "primary";
const getList = () => {
  loading();
  get();
};
getList();
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
    flex-wrap: wrap;
    padding: 17px 18px;
    gap: 12px;
  }

  .activity-card__icon {
    width: 42px;
    height: 42px;
    font-size: 20px;
  }

  .activity-card__action {
    width: 100%;
    justify-content: flex-end;
  }

  .activity-card__meta {
    gap: 8px;
  }

  .enter-button {
    min-width: 84px;
  }
}
</style>
