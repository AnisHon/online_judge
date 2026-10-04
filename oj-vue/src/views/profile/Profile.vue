<template>
  <main class="profile-page common-max-width-page" v-loading="loading" :aria-busy="loading">
    <template v-if="profile">
      <header class="profile-hero">
        <div class="profile-identity">
          <button v-if="canChangeAvatar" class="profile-avatar profile-avatar--editable" type="button" aria-label="更换头像"
                  @click="avatarDialogVisible = true">
            <avatar shape="square" :user-id="profile.user.userId"/>
            <span class="avatar-edit"><el-icon><Camera/></el-icon>更换</span>
          </button>
          <avatar v-else class="profile-avatar" shape="square" :user-id="profile.user.userId"/>
          <div class="profile-copy">
            <span class="eyebrow">{{ isOwner ? 'MY PROFILE' : 'PUBLIC PROFILE' }}</span>
            <div class="profile-name-line">
              <h1>{{ profile.user.nikeName || profile.user.userName }}</h1>
              <el-tag v-for="role in profile.user.specialRoles" :key="role" class="profile-role-tag" size="small" effect="dark">{{ role }}</el-tag>
              <el-tag v-if="isOwner" size="small" effect="plain">我的主页</el-tag>
            </div>
            <p class="profile-signature">{{ profile.user.signature || '还没有写下个性签名。' }}</p>
            <div class="profile-meta"><span>@{{
                profile.user.userName
              }}</span><i></i><span>用户 ID {{ shortProfileId(profile.user.userId) }}</span></div>
          </div>
        </div>
        <el-button v-if="isOwner" class="hero-action" plain @click="selectTab('profile')">编辑资料</el-button>
        <el-button v-else class="hero-action" text @click="router.push({name: 'home'})">返回首页</el-button>
        <div class="hero-glow" aria-hidden="true"></div>
      </header>

      <section class="profile-layout">
        <aside class="profile-sidebar">
          <div class="sidebar-card">
            <div class="sidebar-card__heading"><span>PROFILE SNAPSHOT</span>
              <el-icon>
                <TrendCharts/>
              </el-icon>
            </div>
            <div class="snapshot-grid">
              <div><strong>{{ profile.activity.solvedCount }}</strong><span>已通过题目</span></div>
              <div><strong>{{ profile.activity.attemptedCount }}</strong><span>尝试过题目</span></div>
              <div><strong>{{ profile.activity.contests.length + profile.activity.solutions.length }}</strong><span>学习轨迹</span></div>
            </div>
            <dl class="profile-dates">
              <div>
                <dt>积分</dt>
                <dd>{{ profile.user.points ?? 0 }}</dd>
              </div>
              <div>
                <dt>注册时间</dt>
                <dd>{{ formatProfileDate(profile.user.createTime) }}</dd>
              </div>
              <div>
                <dt>上次登录</dt>
                <dd>{{ formatProfileDate(profile.user.lastLoginTime) }}</dd>
              </div>
            </dl>
            <section v-if="followAvailable" class="profile-follow" aria-label="关注关系">
              <div class="profile-follow__stats">
                <button type="button" @click="openFollowList('following')"><strong>{{ followSummary?.followingCount ?? '—' }}</strong><span>关注</span></button>
                <button type="button" @click="openFollowList('followers')"><strong>{{ followSummary?.followersCount ?? '—' }}</strong><span>粉丝</span></button>
              </div>
              <FollowButton v-if="canFollow" :following="followSummary?.following ?? false" :pending="followPending"
                            :disabled="followLoading || !followSummary" @toggle="void toggleFollow()"/>
              <p v-if="followError" class="profile-follow__error" role="alert">{{ followError }}
                <el-button v-if="!followSummary" text type="primary" :disabled="followLoading" @click="void refreshFollow()">重试</el-button>
              </p>
            </section>
          </div>
          <nav class="profile-tabs" aria-label="个人中心导航">
            <button v-for="tab in visibleTabs" :key="tab.key" type="button" class="profile-tab"
                    :class="{active: activeTab === tab.key}" :aria-current="activeTab === tab.key ? 'page' : undefined"
                    @click="void selectTab(tab.key)">
              <el-icon>
                <component :is="tab.icon"/>
              </el-icon>
              <span>{{ tab.label }}</span>
            </button>
          </nav>
        </aside>

        <div class="profile-main">
          <div class="profile-transition">
            <Transition name="profile-tab" mode="out-in">
              <section :key="activeTab" class="profile-panel">
              <template v-if="isActivityTab">
                <div class="panel-heading">
                  <div><span class="eyebrow">LEARNING PROFILE</span>
                    <h2>{{ activityTitle }}</h2></div>
                  <span class="panel-caption">内容按时间与难度整理</span></div>
                <profile-activity-panel :activity="profile.activity" :section="activityTab" @open-problem="openProblem" @open-contest="openContest"
                                        @open-solution="openSolution" @retry-solutions="retrySolutions"/>
              </template>

              <template v-else-if="activeTab === 'profile'">
                <div class="panel-heading">
                  <div><span class="eyebrow">PUBLIC PROFILE</span>
                    <h2>公开资料</h2></div>
                  <span class="panel-caption">这些信息会展示在你的公开主页和题解中</span></div>
                <el-form class="profile-form" label-position="top" :model="profileForm" @submit.prevent="saveProfile">
                  <div class="form-grid">
                    <el-form-item label="用户编号">
                      <el-input :model-value="shortProfileId(profileForm.userId)" disabled/>
                    </el-form-item>
                    <el-form-item label="用户名">
                      <el-input v-model="profileForm.userName" disabled/>
                    </el-form-item>
                  </div>
                  <el-form-item label="昵称">
                    <el-input v-model="profileForm.nikeName" maxlength="32" show-word-limit
                              placeholder="给自己一个容易被记住的名字"/>
                  </el-form-item>
                  <el-form-item label="个性签名">
                    <el-input v-model="profileForm.signature" type="textarea" :rows="4" maxlength="160" show-word-limit
                              placeholder="介绍一下你的学习方向或正在坚持的事情"/>
                  </el-form-item>
                  <div class="form-actions">
                    <el-button type="primary" :loading="profileSaving" @click="saveProfile">保存资料</el-button>
                    <span>上次保存后会立即同步到主页</span></div>
                </el-form>
              </template>

              <template v-else>
                <div class="panel-heading">
                  <div><span class="eyebrow">ACCOUNT SECURITY</span>
                    <h2>账号安全</h2></div>
                  <span class="panel-caption">保护登录凭据与找回方式</span></div>
                <AccountSecurityPanel :email="userStore.user?.email" />
              </template>
              </section>
            </Transition>
          </div>
        </div>
      </section>
    </template>
    <el-empty v-else-if="!loading" :description="profileError || '个人主页不存在或暂时无法访问'">
      <el-button v-if="profileError" type="primary" plain @click="void load()">重新加载</el-button>
    </el-empty>
    <FollowListDialog v-model="followDialogVisible" :user-id="followTarget" :type="followListType"/>

    <el-dialog v-model="avatarDialogVisible" title="更换头像" width="min(560px, calc(100vw - 32px))" align-center
               append-to-body destroy-on-close :close-on-click-modal="!avatarUploading" :close-on-press-escape="!avatarUploading">
      <div class="avatar-dialog__content">
        <div class="avatar-dialog__intro"><span class="avatar-dialog__icon"><el-icon><Camera/></el-icon></span>
          <div><strong>让你的公开身份更有辨识度</strong>
            <p>选择一张清晰的正方形图片，系统会在上传前帮你裁剪。</p></div>
        </div>
        <div class="avatar-cutter-shell">
          <AvatarCutter :uploading="avatarUploading" @cut-down="handleUploadAvatar" @cancel="closeAvatarDialog"/>
        </div>
      </div>
    </el-dialog>
  </main>
</template>

<script setup lang="ts">
import {computed, reactive, ref, watch} from "vue";
import {useRoute, useRouter} from "vue-router";
import {Camera, CircleCheck, Collection, EditPen, Trophy, TrendCharts} from "@element-plus/icons-vue";
import {ElMessage, ElNotification} from "element-plus";
import type {IdType} from "@/api/common";
import type {UserForm} from "@/api/user";
import {saveMyProfile} from "@/api/user";
import {getProfile, getProfileSolutions, StaleProfileResponseError, type ProfileData} from "@/api/profile";
import {useToken} from '@/stores/useToken';
import {uploadAvatar} from "@/api/file";
import Avatar from "@/components/Avatar/Avatar.vue";
import AvatarCutter from "@/components/AvatarCutter/AvatarCutter.vue";
import ProfileActivityPanel from "@/components/ProfileActivityPanel/ProfileActivityPanel.vue";
import AccountSecurityPanel from "@/components/AccountSecurity/AccountSecurityPanel.vue";
import FollowButton from '@/components/FollowButton/FollowButton.vue';
import FollowListDialog from '@/components/FollowListDialog/FollowListDialog.vue';
import {useFollow} from '@/composables/social/useFollow';
import type {FollowListType} from '@/api/follow';
import {formatProfileDate, shortProfileId} from "@/utils/profile";
import {ApiError} from "@/utils/http";
import {useUserStore} from "@/stores/useUserStore";
import {hasPerm} from '@/utils/authUtil';

type ActivityTab = 'practice' | 'contests' | 'solutions';
type ProfileTab = ActivityTab | 'profile' | 'security';
const route = useRoute();
const router = useRouter();
const userStore = useUserStore();
const tokenStore = useToken();
const loading = ref(true);
const profile = ref<ProfileData | null>(null);
const followTarget = computed(() => profile.value?.user.userId === String(route.params.id || '')
  ? profile.value.user.userId : null);
const {summary: followSummary, loading: followLoading, pending: followPending, error: followError,
  available: followAvailable, canFollow, refresh: refreshFollow, toggle: toggleFollow} = useFollow(followTarget);
const followDialogVisible = ref(false);
const followListType = ref<FollowListType>('following');
const openFollowList = (type: FollowListType) => {
  followListType.value = type;
  followDialogVisible.value = true;
};
watch([() => route.params.id, () => tokenStore.sessionVersion], () => {
  followDialogVisible.value = false;
}, {flush: 'sync'});
const profileError = ref('');
const activeTab = ref<ProfileTab>('practice');
const profileSaving = ref(false);
const avatarUploading = ref(false);
const avatarDialogVisible = ref(false);
const profileForm = reactive<UserForm>({userId: undefined, userName: undefined, nikeName: '', signature: ''});
const isOwner = computed(() => profile.value?.activity.owner === true);
const canChangeAvatar = computed(() => isOwner.value && !hasPerm('policy:avatar:deny'));
const profileRequestError = (error: unknown, fallback: string) => {
  if (error instanceof ApiError) return error.code === 0 || error.code >= 500 ? fallback : error.message || fallback;
  return error instanceof Error && error.message ? error.message : fallback;
};
const allTabs: ReadonlyArray<{key: ProfileTab; label: string; icon: typeof Collection; visibility: 'public' | 'owner'}> = [
  {key: 'practice', label: '练习', icon: Collection, visibility: 'public'},
  {key: 'contests', label: '比赛', icon: Trophy, visibility: 'public'},
  {key: 'solutions', label: '题解', icon: EditPen, visibility: 'public'},
  {key: 'profile', label: '公开资料', icon: EditPen, visibility: 'owner'},
  {key: 'security', label: '账号安全', icon: CircleCheck, visibility: 'owner'},
];
const activityTabs = new Set<ActivityTab>(['practice', 'contests', 'solutions']);
const tabKeys = new Set<ProfileTab>(allTabs.map(tab => tab.key));
const visibleTabs = computed(() => allTabs.filter(tab => tab.visibility === 'public' || isOwner.value));
const isActivityTab = computed(() => activityTabs.has(activeTab.value as ActivityTab));
const activityTab = computed<ActivityTab>(() => isActivityTab.value ? activeTab.value as ActivityTab : 'practice');
const activityTitle = computed(() => ({practice: '练习足迹', contests: '参加的比赛', solutions: '写过的题解'}[activityTab.value]));

const validTab = (value: unknown): ProfileTab => tabKeys.has(String(value) as ProfileTab) ? String(value) as ProfileTab : 'practice';
const syncTab = () => {
  const tab = validTab(route.query.tab);
  activeTab.value = visibleTabs.value.some(item => item.key === tab) ? tab : 'practice';
};
const selectTab = async (tab: ProfileTab) => {
  if (!visibleTabs.value.some(item => item.key === tab)) return;
  try {
    await router.replace({name: 'profile', params: {id: route.params.id}, query: {tab}});
    activeTab.value = tab;
  } catch {
    ElMessage.warning('页面切换失败，请稍后重试');
  }
};

let loadRequestId = 0;
const load = async (): Promise<boolean> => {
  const id = String(route.params.id || '');
  if (!id) {
    profile.value = null;
    profileError.value = '缺少用户标识';
    loading.value = false;
    return false;
  }
  const requestId = ++loadRequestId;
  loading.value = true;
  profileError.value = '';
  profile.value = null;
  try {
    const data = await getProfile(id, () => requestId === loadRequestId && id === String(route.params.id || ''));
    if (requestId !== loadRequestId || id !== String(route.params.id || '')) return false;
    profile.value = data;
    Object.assign(profileForm, {
      userId: data.user.userId,
      userName: data.user.userName,
      nikeName: data.user.nikeName || '',
      signature: data.user.signature || ''
    });
    syncTab();
    return true;
  } catch (error) {
    if (error instanceof StaleProfileResponseError) return false;
    if (requestId !== loadRequestId || id !== String(route.params.id || '')) return false;
    profile.value = null;
    profileError.value = error instanceof ApiError && error.code === 404
      ? '这个用户不存在或主页不可见'
      : '个人主页加载失败，请检查网络后重试';
    return false;
  } finally {
    if (requestId === loadRequestId) loading.value = false;
  }
};
const openProblem = (id: IdType) => router.push({name: 'problem', params: {id}});
let solutionsRequestId = 0;
const retrySolutions = async () => {
  const current = profile.value;
  if (!current) return;
  const requestId = ++solutionsRequestId;
  const sessionVersion = tokenStore.getSessionVersion();
  try {
    const data = await getProfileSolutions(current.user.userId);
    if (requestId !== solutionsRequestId || current !== profile.value || sessionVersion !== tokenStore.getSessionVersion()) return;
    Object.assign(current.activity, data, {solutionsError: undefined});
  } catch {
    if (requestId === solutionsRequestId && current === profile.value && sessionVersion === tokenStore.getSessionVersion())
      current.activity.solutionsError = '题解暂时无法加载，请稍后重试';
  }
};
const openContest = (id: IdType) => router.push({name: 'contest-problems', params: {id}});
const openSolution = (id: IdType) => router.push({name: 'solution', params: {id}});
const saveProfile = async () => {
  profileSaving.value = true;
  try {
    await saveMyProfile(profileForm);
    if (profile.value) {
      profile.value.user.nikeName = profileForm.nikeName || '';
      profile.value.user.signature = profileForm.signature || '';
    }
    ElNotification.success('公开资料已保存');
  } catch (error) {
    ElNotification.error(profileRequestError(error, '公开资料保存失败'));
  } finally {
    profileSaving.value = false;
  }
};

const handleUploadAvatar = async (file: File) => {
  avatarUploading.value = true;
  try {
    await uploadAvatar(file);
    useUserStore().refreshAvatar(profile.value?.user.userId);
    avatarDialogVisible.value = false;
    ElNotification.success('头像更换成功');
  } catch (error) {
    ElNotification.error(profileRequestError(error, '头像上传失败'));
  } finally {
    avatarUploading.value = false;
  }
};

const closeAvatarDialog = () => {
  if (!avatarUploading.value) avatarDialogVisible.value = false;
};

watch(() => route.params.id, () => { void load(); }, {immediate: true});
watch(() => tokenStore.sessionVersion, () => { void load(); });
watch(() => route.query.tab, syncTab);
watch(() => isOwner.value, () => syncTab());
</script>

<style scoped>
.profile-page {
  margin: auto;
  padding: 10px 0 38px;
  color: var(--el-text-color-primary);
}

.profile-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 205px;
  padding: 32px 40px;
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 22px;
  background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-bg-color));
}

.profile-identity {
  position: relative;
  z-index: var(--oj-z-content-raised);
  display: flex;
  align-items: center;
  gap: 22px;
  min-width: 0;
}

.profile-avatar {
  width: 92px;
  height: 92px;
  flex: 0 0 auto;
  border: 4px solid var(--el-bg-color);
  border-radius: 26px;
  box-shadow: 0 12px 26px color-mix(in srgb, var(--el-color-primary) 18%, transparent);
}

.profile-avatar--editable {
  position: relative;
  padding: 0;
  overflow: hidden;
  background: var(--el-fill-color-light);
  cursor: pointer;
}

.profile-avatar :deep(.avatar), .profile-avatar :deep(.el-avatar) {
  width: 100%;
  height: 100%;
}

.avatar-edit {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 5px 0;
  color: #fff;
  background: rgb(0 0 0 / 52%);
  font-size: 11px;
  opacity: 0;
  transition: opacity .2s ease;
}

.profile-avatar--editable:hover .avatar-edit {
  opacity: 1;
}

.profile-copy {
  min-width: 0;
}

.eyebrow {
  display: block;
  margin-bottom: 7px;
  color: var(--el-color-primary);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .18em;
}

.profile-name-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.profile-role-tag {
  border: 0;
  background: linear-gradient(135deg, #5b7cfa, #8b5cf6);
  color: #fff;
  font-weight: 650;
}

.profile-name-line h1 {
  margin: 0;
  overflow: hidden;
  font-size: clamp(28px, 4vw, 42px);
  letter-spacing: -.05em;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-signature {
  max-width: 600px;
  margin: 11px 0 15px;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.profile-meta i {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--el-color-primary);
}

.hero-action {
  position: relative;
  z-index: var(--oj-z-content-raised);
  align-self: flex-start;
}

.hero-glow {
  position: absolute;
  right: 7%;
  bottom: -115px;
  width: 290px;
  height: 290px;
  border: 1px solid color-mix(in srgb, var(--el-color-primary) 22%, transparent);
  border-radius: 50%;
  opacity: .7;
}

.profile-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(220px, 280px);
  gap: 18px;
  margin-top: 20px;
  align-items: stretch;
}

.profile-sidebar {
  display: grid;
  gap: 12px;
  align-self: start;
  grid-column: 2;
  grid-row: 1;
}

.sidebar-card, .profile-panel, .profile-tabs {
  border: 1px solid var(--el-border-color-light);
  border-radius: 16px;
  background: var(--el-bg-color);
}

.sidebar-card {
  padding: 19px;
}

.sidebar-card__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: var(--el-text-color-secondary);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: .15em;
}

.sidebar-card__heading .el-icon {
  color: var(--el-color-primary);
  font-size: 17px;
}

.snapshot-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 7px;
  margin: 19px 0;
}

.snapshot-grid div {
  min-width: 0;
  padding: 9px 5px;
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
  text-align: center;
}

.snapshot-grid strong, .snapshot-grid span {
  display: block;
}

.snapshot-grid strong {
  overflow: hidden;
  font-size: 19px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.snapshot-grid span {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 10px;
}

.profile-dates {
  margin: 0;
  padding-top: 13px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.profile-dates div {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  padding: 7px 0;
}

.profile-dates dt {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.profile-dates dd {
  margin: 0;
  color: var(--el-text-color-regular);
  font-size: 12px;
  text-align: right;
}

.profile-main {
  display: flex;
  min-width: 0;
  min-height: 520px;
  align-self: stretch;
  grid-column: 1;
  grid-row: 1;
}

.profile-transition {
  width: 100%;
  min-width: 0;
  min-height: 100%;
  overflow: clip;
}

.profile-tabs {
  align-self: start;
  display: grid;
  gap: 5px;
  padding: 6px;
  background: var(--el-fill-color-lighter);
}

.profile-tab {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid transparent;
  border-radius: 11px;
  color: var(--el-text-color-secondary);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: color .2s ease, border-color .2s ease, background-color .2s ease, box-shadow .2s ease;
}

.profile-tab:hover, .profile-tab.active {
  border-color: var(--el-border-color-light);
  color: var(--el-text-color-primary);
  background: var(--el-bg-color);
  box-shadow: 0 5px 14px color-mix(in srgb, var(--el-color-primary) 9%, transparent);
}

.profile-tab:focus-visible {
  outline: 2px solid var(--el-color-primary);
  outline-offset: 2px;
}

.profile-tab .el-icon {
  flex: 0 0 auto;
  color: var(--el-color-primary);
  font-size: 18px;
}

.profile-tab > span:last-child {
  display: block;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  font-weight: 600;
}

.profile-panel {
  width: 100%;
  min-width: 0;
  min-height: 100%;
  box-sizing: border-box;
  padding: 22px;
}

.profile-tab-enter-active,
.profile-tab-leave-active {
  transition: opacity .18s ease, transform .18s ease;
}

.profile-tab-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.profile-tab-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 15px;
  margin-bottom: 20px;
}

.panel-heading .eyebrow {
  margin-bottom: 5px;
}

.panel-heading h2 {
  margin: 0;
  font-size: 24px;
  letter-spacing: -.04em;
}

.panel-caption {
  padding-top: 12px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.profile-follow {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.profile-follow__stats { display: flex; gap: 20px; }
.profile-follow__stats button {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  padding: 4px 0;
  border: 0;
  color: var(--el-text-color-primary);
  background: transparent;
  cursor: pointer;
}
.profile-follow__stats button span { color: var(--el-text-color-secondary); font-size: 12px; }
.profile-follow__stats button:hover, .profile-follow__stats button:hover span { color: var(--el-color-primary); }
.profile-follow__stats button:focus-visible { outline: 2px solid var(--el-color-primary); outline-offset: 3px; border-radius: 3px; }
.profile-follow__error { margin: 0; color: var(--el-color-danger); font-size: 12px; }

.profile-form {
  max-width: 680px;
}

.form-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.profile-form :deep(.el-form-item__label) {
  color: var(--el-text-color-regular);
  font-weight: 600;
}

.form-actions {
  display: flex;
  align-items: center;
  gap: 13px;
  margin-top: 8px;
}

.form-actions span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.avatar-dialog__content {
  padding-bottom: 4px;
}

.avatar-dialog__intro {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
}

.avatar-dialog__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.avatar-dialog__intro strong {
  font-size: 14px;
}

.avatar-dialog__intro p {
  margin: 4px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.avatar-cutter-shell {
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 14px;
}

@media (max-width: 780px) {
  .profile-page {
    padding: 8px 12px 28px;
  }

  .profile-hero {
    min-height: 280px;
    align-items: flex-start;
    padding: 28px 22px;
  }

  .profile-identity {
    align-items: flex-start;
    flex-direction: column;
    gap: 16px;
  }

  .profile-avatar {
    width: 72px;
    height: 72px;
    border-radius: 20px;
  }

  .profile-name-line h1 {
    font-size: 30px;
  }

  .profile-signature {
    max-width: 280px;
    line-height: 1.7;
    white-space: normal;
  }

  .hero-action {
    position: absolute;
    top: 18px;
    right: 16px;
  }

  .hero-glow {
    right: -100px;
    bottom: -110px;
  }

  .profile-layout {
    display: block;
  }

  .profile-sidebar {
    margin-bottom: 16px;
    grid-column: auto;
    grid-row: auto;
  }

  .profile-main {
    grid-column: auto;
    grid-row: auto;
  }

  .profile-transition,
  .profile-panel {
    min-height: 420px;
  }

  .profile-main {
    min-height: 420px;
  }

  .profile-panel {
    padding: 16px 12px;
  }

  .panel-heading {
    display: block;
  }

  .panel-caption {
    display: block;
    padding-top: 8px;
  }

  .form-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .form-actions {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 980px) and (min-width: 781px) {
  .profile-hero { padding-right: 28px; padding-left: 28px; }
  .profile-layout { grid-template-columns: minmax(0, 1fr) minmax(200px, 235px); gap: 14px; }
  .profile-panel { padding: 18px; }
  .panel-heading h2 { font-size: 21px; }
  .panel-caption { display: none; }
}

@media (max-width: 840px) {
  .profile-layout { display: block; }
  .profile-sidebar { margin-bottom: 16px; }
  .profile-tabs { display: flex; gap: 5px; overflow-x: auto; }
  .profile-tab { width: auto; flex: 0 0 auto; }
}
</style>
