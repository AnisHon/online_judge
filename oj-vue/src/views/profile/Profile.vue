<template>
  <main class="profile-page common-max-width-page" v-loading="loading" :aria-busy="loading">
    <template v-if="profile">
      <header class="profile-hero">
        <div class="profile-identity">
          <button v-if="isOwner" class="profile-avatar profile-avatar--editable" type="button" aria-label="更换头像"
                  @click="avatarDialogVisible = true">
            <avatar shape="square" :user-id="profile.user.userId"/>
            <span class="avatar-edit"><el-icon><Camera/></el-icon>更换</span>
          </button>
          <avatar v-else class="profile-avatar" shape="square" :user-id="profile.user.userId"/>
          <div class="profile-copy">
            <span class="eyebrow">{{ isOwner ? 'MY PROFILE' : 'PUBLIC PROFILE' }}</span>
            <div class="profile-name-line">
              <h1>{{ profile.user.nikeName || profile.user.userName }}</h1>
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
                                        @open-solution="openSolution"/>
              </template>

              <template v-else-if="activeTab === 'profile'">
                <div class="panel-heading">
                  <div><span class="eyebrow">PUBLIC PROFILE</span>
                    <h2>公开资料</h2></div>
                  <span class="panel-caption">这些信息会展示在你的公开主页和题解中</span></div>
                <el-form class="profile-form" label-position="top" :model="profileForm">
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
                  <span class="panel-caption">修改密码需要完成验证码校验</span></div>
                <el-form ref="passwordFormRef" class="profile-form" label-position="top" :model="passwordForm"
                         :rules="passwordRules">
                  <el-form-item label="当前账号">
                    <el-input :model-value="profile.user.userName" disabled/>
                  </el-form-item>
                  <div class="form-grid">
                    <el-form-item label="新密码" prop="password">
                      <el-input v-model="passwordForm.password" type="password" show-password autocomplete="new-password"
                                placeholder="8—16 位密码"/>
                    </el-form-item>
                    <el-form-item label="确认新密码" prop="repeatPassword">
                      <el-input v-model="passwordForm.repeatPassword" type="password" show-password
                                autocomplete="new-password" placeholder="再次输入新密码"/>
                    </el-form-item>
                  </div>
                  <div class="verify-grid">
                    <el-form-item label="图形验证码" prop="captchaCode">
                      <el-input v-model="passwordForm.captchaCode" :prefix-icon="IconCaptcha"
                                placeholder="输入图片中的字符"/>
                    </el-form-item>
                    <button class="captcha-image" type="button" aria-label="刷新验证码" :disabled="captchaLoading"
                            @click="void refreshCaptchaCode()">
                      <img v-if="captchaImage" :src="captchaImage" alt="图形验证码"/>
                      <span v-else>{{ captchaLoading ? '加载中' : '点击刷新' }}</span>
                    </button>
                    <span v-if="captchaError" class="profile-captcha-error" role="button" tabindex="0"
                          aria-live="polite" @click="void refreshCaptchaCode()"
                          @keydown.enter="void refreshCaptchaCode()">{{ captchaError }}</span>
                  </div>
                  <div class="verify-grid">
                    <el-form-item label="邮箱验证码" prop="emailCode">
                      <el-input v-model="passwordForm.emailCode" :prefix-icon="IconCaptcha" placeholder="输入邮箱验证码"/>
                    </el-form-item>
                    <el-button class="send-code-button" :loading="sendingCode" :disabled="sendingCode || emailCooldown > 0"
                               @click="void sendEmailCode">{{ emailCodeLabel }}
                    </el-button>
                  </div>
                  <div class="form-actions">
                    <el-button type="primary" :loading="passwordSaving" @click="submitPassword">更新密码</el-button>
                    <span>修改完成后请使用新密码重新登录</span></div>
                </el-form>
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
import {ElMessage, ElNotification, type FormInstance, type FormRules} from "element-plus";
import type {IdType} from "@/api/common";
import type {UserForm} from "@/api/user";
import {saveMyProfile} from "@/api/user";
import {getProfile, type ProfileData} from "@/api/profile";
import {resetPassword} from "@/api/auth/authentication";
import {sendForgetEmailCode} from "@/api/auth/emailCode";
import {uploadAvatar} from "@/api/file";
import IconCaptcha from "@/assets/icons/IconCaptcha.vue";
import Avatar from "@/components/Avatar/Avatar.vue";
import AvatarCutter from "@/components/AvatarCutter/AvatarCutter.vue";
import ProfileActivityPanel from "@/components/ProfileActivityPanel/ProfileActivityPanel.vue";
import {useCaptchaCode} from "@/composables/auth/useCaptchaCode";
import {useEmailCodeCooldown} from "@/composables/auth/useEmailCodeCooldown";
import {formatProfileDate, shortProfileId} from "@/utils/profile";
import {ApiError} from "@/utils/http";
import {useUserStore} from "@/stores/useUserStore";

type ActivityTab = 'practice' | 'contests' | 'solutions';
type ProfileTab = ActivityTab | 'profile' | 'security';
const route = useRoute();
const router = useRouter();
const loading = ref(true);
const profile = ref<ProfileData | null>(null);
const profileError = ref('');
const activeTab = ref<ProfileTab>('practice');
const profileSaving = ref(false);
const passwordSaving = ref(false);
const avatarUploading = ref(false);
const avatarDialogVisible = ref(false);
const passwordFormRef = ref<FormInstance>();
const profileForm = reactive<UserForm>({userId: undefined, userName: undefined, nikeName: '', signature: ''});
const passwordForm = reactive({password: '', repeatPassword: '', captchaCode: '', token: '', emailCode: ''});
const isOwner = computed(() => profile.value?.activity.owner === true);
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
    const data = await getProfile(id);
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

const validatePassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (!value) callback(new Error('请输入密码')); else if (value.length < 8 || value.length > 16) callback(new Error('密码长度需为 8—16 位')); else callback();
};
const validateRepeatPassword = (_rule: unknown, value: string, callback: (error?: Error) => void) => {
  if (value !== passwordForm.password) callback(new Error('两次密码不一致')); else callback();
};
const required = (_rule: unknown, value: string, callback: (error?: Error) => void) => value ? callback() : callback(new Error('请填写此项'));
const passwordRules: FormRules = {
  password: [{validator: validatePassword, trigger: 'blur'}],
  repeatPassword: [{validator: validateRepeatPassword, trigger: 'blur'}],
  captchaCode: [{validator: required, trigger: 'blur'}],
  emailCode: [{validator: required, trigger: 'blur'}]
};
const {image: captchaImage, loading: captchaLoading, error: captchaError, refresh: refreshCaptchaCode} = useCaptchaCode(passwordForm);
const {sending: sendingCode, remaining: emailCooldown, label: emailCodeLabel, run: runEmailCode} = useEmailCodeCooldown();
const sendEmailCode = async () => {
  if (emailCooldown.value > 0 || sendingCode.value) return;
  if (!passwordForm.captchaCode || !passwordForm.token) return ElMessage.warning('请先填写有效的图形验证码');
  try {
    const sent = await runEmailCode(() => sendForgetEmailCode({
      username: String(profile.value?.user.userName || ''),
      captchaCode: passwordForm.captchaCode,
      captchaToken: passwordForm.token,
    }));
    if (sent) ElMessage.success('验证码已发送');
  } catch (error) {
    ElMessage.error(profileRequestError(error, '验证码发送失败'));
    void refreshCaptchaCode();
  }
};
const submitPassword = async () => {
  if (!passwordFormRef.value) return;
  const valid = await passwordFormRef.value.validate().catch(() => false);
  if (!valid) return;
  passwordSaving.value = true;
  try {
    await resetPassword({password: passwordForm.password, code: passwordForm.emailCode});
    ElMessage.success('密码已更新');
    Object.assign(passwordForm, {password: '', repeatPassword: '', captchaCode: '', token: '', emailCode: ''});
    passwordFormRef.value?.clearValidate();
    void refreshCaptchaCode();
  } catch (error) {
    ElMessage.error(profileRequestError(error, '密码更新失败'));
    void refreshCaptchaCode();
  } finally {
    passwordSaving.value = false;
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
watch(() => route.query.tab, syncTab);
watch(activeTab, (tab) => {
  if (tab === 'security' && isOwner.value) void refreshCaptchaCode();
});
watch(() => isOwner.value, () => syncTab());
watch(() => route.params.id, () => { passwordFormRef.value?.clearValidate(); });
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
  z-index: 1;
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
  z-index: 1;
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
  align-self: stretch;
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
  align-self: stretch;
  grid-column: 1;
  grid-row: 1;
}

.profile-transition {
  width: 100%;
  min-width: 0;
  min-height: 520px;
  overflow: clip;
}

.profile-tabs {
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
  min-height: 520px;
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

.profile-form {
  max-width: 680px;
}

.form-grid, .verify-grid {
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

.captcha-image {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 40px;
  margin-top: 30px;
  padding: 0;
  overflow: hidden;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
  cursor: pointer;
}

.captcha-image:disabled {
  cursor: wait;
  opacity: .7;
}

.captcha-image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.profile-captcha-error {
  position: absolute;
  top: 74px;
  right: 0;
  max-width: 150px;
  color: var(--el-color-danger);
  font-size: 11px;
  line-height: 1.35;
  text-align: right;
  cursor: pointer;
}

.send-code-button {
  width: 100%;
  height: 40px;
  margin-top: 30px;
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

  .form-grid, .verify-grid {
    grid-template-columns: 1fr;
    gap: 0;
  }

  .captcha-image, .send-code-button {
    margin-top: 0;
    margin-bottom: 18px;
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
