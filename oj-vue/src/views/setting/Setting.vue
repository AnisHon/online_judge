<template>


  <main class="profile-page common-max-width-page" v-if="!!userStore.user">
    <header class="profile-hero">
      <div class="profile-identity">
        <button class="profile-avatar" type="button" aria-label="更换头像" @click="openUploadAvatarDialog = true">
          <avatar shape="square" :user-id="user.userId" />
          <span class="avatar-edit">更换</span>
        </button>
        <div class="profile-copy">
          <span class="eyebrow">MY PROFILE</span>
          <h1>{{ user.nikeName || user.userName }}</h1>
          <p>管理你的公开资料与账号安全，让延拓Code更贴合你的学习节奏。</p>
          <div class="profile-meta"><span>@{{ user.userName }}</span><i></i><span>{{ user.email || '暂未设置邮箱' }}</span></div>
        </div>
      </div>
      <el-button class="back-button" text @click="goBack">返回首页</el-button>
      <div class="hero-glow" aria-hidden="true"></div>
    </header>

    <section class="profile-content">
      <div class="section-heading"><div><span class="eyebrow">ACCOUNT CENTER</span><h2>个人资料</h2></div><span class="section-caption">更新后会同步到你的公开信息</span></div>
      <div class="account-layout">
      <aside class="profile-summary">
      <el-card class="profile-card" shadow="never">
        <div class="summary-avatar"><avatar shape="square" class="portrait" :user-id="user.userId" /></div>
        <h3>{{ user.nikeName || '未设置昵称' }}</h3>
        <p class="summary-handle">@{{ user.userName }}</p>
        <div class="summary-divider"></div>
        <div class="summary-row"><span>用户编号</span><strong>{{ user.userId }}</strong></div>
        <div class="summary-row"><span>邮箱</span><strong>{{ user.email || '暂未设置' }}</strong></div>
        <div class="summary-row"><span>个人签名</span><strong class="muted">暂未开放</strong></div>
      </el-card>
      <div class="summary-tip"><strong>延拓Code 身份</strong><p>你的昵称和头像会展示在题解、公告与互动内容中。</p></div>
      </aside>
      <div class="account-settings">
      <div class="settings-nav" role="tablist">
        <button class="settings-nav-item" :class="{active: activeTab === 'first'}" type="button" @click="activeTab = 'first'"><span class="nav-index">01</span><span><strong>角色信息</strong><small>公开资料与身份</small></span><span class="nav-arrow">›</span></button>
        <button class="settings-nav-item" :class="{active: activeTab === 'second'}" type="button" @click="activeTab = 'second'"><span class="nav-index">02</span><span><strong>修改密码</strong><small>保护账号安全</small></span><span class="nav-arrow">›</span></button>
      </div>
      <div class="settings-panel">
        <section v-if="activeTab === 'first'" class="settings-pane">
          <el-card class="tab-card" shadow="never">
            <el-form class="auth-form settings-form" label-position="top" :model="userForm" >
              <el-form-item class="title-item">
                <div><p class="welcome-label">PUBLIC PROFILE</p><h1>角色信息</h1><p class="subtitle">完善你的公开身份，让其他同学更容易认识你。</p></div>
              </el-form-item>
              <el-form-item label="用户编号">
                <el-input v-model="userForm.userId" disabled/>
              </el-form-item>

              <el-form-item label="用户名">
                <el-input v-model="userForm.userName" disabled/>
              </el-form-item>
              <el-form-item label="用户昵称">
                <el-input v-model="userForm.nikeName" />
              </el-form-item>

              <el-form-item label="个性签名">
                <el-input  type="textarea" model-value="禁用" disabled />
              </el-form-item>

              <el-form-item class="submit-item">
                <el-button type="primary" class="submit-button" @click="changeSelf">保存资料</el-button>
              </el-form-item>


            </el-form>
          </el-card>
        </section>
        <section v-else class="settings-pane">
          <el-card class="tab-card" shadow="never">
            <el-form
                class="auth-form settings-form"
                label-position="top"
                label-width="100px"
                ref="formRef"
                style="min-width: 500px"
                :model="resetForm"
                status-icon
                :rules="rules"
                :aria-autocomplete="false"
            >
              <el-form-item class="title-item">
                <div><p class="welcome-label">ACCOUNT SECURITY</p><h1>修改密码</h1><p class="subtitle">定期更新密码，保护你的账号和学习记录。</p></div>
              </el-form-item>
              <el-form-item label="用户名" prop="username">
                <el-input
                    v-model="userForm.userName"
                    type="text"
                    disabled
                    autocomplete="off"
                    placeholder="请输入用户名或邮箱"
                    prefix-icon="UserFilled"
                />
              </el-form-item>

              <el-form-item label="密码" prop="password">
                <el-input
                    v-model="resetForm.password"
                    type="password"
                    autocomplete="off"
                    placeholder="请输入密码"
                    prefix-icon="Lock"
                />
              </el-form-item>

              <el-form-item label="确认密码" prop="repeatPassword">
                <el-input
                    v-model="resetForm.repeatPassword"
                    type="password"
                    autocomplete="off"
                    placeholder="请重复密码"
                    prefix-icon="Lock"
                />
              </el-form-item>

              <el-row justify="space-between" style="width: 100%;">
                <el-col :span="14" >
                  <el-form-item label="验证码" prop="captchaCode">
                    <el-input
                        v-model="resetForm.captchaCode"
                        type="text"
                        autocomplete="off"
                        placeholder="请输入验证码"
                        :prefix-icon="IconCaptcha"
                    />
                  </el-form-item>
                </el-col>


                <el-col :span="10" style="position: relative;">
                      <el-image
                          :src="imgData"
                          class="captcha-image"
                      @click="refreshCaptchaCode"
                  />
                </el-col>


              </el-row>

              <el-row justify="space-between" style="width: 100%;">
                <el-col :span="14" >
                  <el-form-item label="邮箱验证码" prop="emailCode">
                    <el-input
                        v-model="resetForm.emailCode"
                        type="text"
                        autocomplete="off"
                        placeholder="请输入验证码"
                        :prefix-icon="IconCaptcha"
                    />
                  </el-form-item>
                </el-col>


                <el-col :span="10" style="position: relative;">
                  <el-button
                      @click="sendEmailCode()"
                      class="code-button"
                      :loading="isLoading"
                      :disabled="isLoading"
                  >
                    获取邮箱验证码
                  </el-button>
                </el-col>


              </el-row>

              <el-form-item>
                <el-button
                    type="primary"
                    @click="submitResetPassword(formRef)"
                    class="submit-button"
                    :loading="isLoading"
                    :disabled="isLoading"
                >
                  重设密码
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </section>
      </div>
      </div>
      </div>
    </section>



    <el-dialog v-model="openUploadAvatarDialog" class="avatar-dialog" title="更换头像" width="min(560px, calc(100vw - 32px))" align-center append-to-body destroy-on-close>
      <div class="avatar-dialog__content">
        <div class="avatar-dialog__intro"><span class="avatar-dialog__icon"><el-icon><Camera /></el-icon></span><div><strong>让你的公开身份更有辨识度</strong><p>选择一张清晰的正方形图片，系统会在上传前帮你裁剪。</p></div></div>
        <div class="avatar-cutter-shell"><AvatarCutter @cut-down="handleUploadAvatar" @cancel="openUploadAvatarDialog = false" /></div>
        <p class="avatar-dialog__tip">支持 JPG、PNG、GIF，建议使用 1:1 图片，过小图片也会始终在预览区域居中。</p>
      </div>
    </el-dialog>
  </main>



</template>
<script setup lang="ts">
import {type LoginUser, useUserStore} from "@/stores/useUserStore";
import {computed, onMounted, reactive, ref} from "vue";
import {useRouter} from "vue-router";
import {change, type UserForm} from "@/api/user";
import __ from "lodash";
import IconCaptcha from "@/assets/icons/IconCaptcha.vue";

import {Camera} from "@element-plus/icons-vue";
import {ElNotification, type FormInstance, type FormRules} from "element-plus";
import {resetPassword} from "@/api/auth/authentication";
import {sendForgetEmailCode} from "@/api/auth/emailCode";
import getCaptcha from "@/api/auth/captchaCode";
import {uploadAvatar} from "@/api/file";
import AvatarCutter from "@/components/AvatarCutter/AvatarCutter.vue";
import Avatar from "@/components/Avatar/Avatar.vue";

const userStore = useUserStore();
const router = useRouter();

const openUploadAvatarDialog = ref(false);

const activeTab = ref("first");

const userForm = reactive<UserForm>({
  userId: undefined,
  userName: undefined,
  nikeName: undefined
})

const user = computed((): LoginUser => {

  return <LoginUser> userStore.user;
})

const goBack = () => {
  router.push({name: "home"});
}



const formRef = ref<FormInstance>()
const isLoading = ref(false)
const resetForm = reactive({
  email: "",
  password: "",
  repeatPassword: "",
  captchaCode: "",
  token: "",
  emailCode: ""
})

const imgData = ref("")


const validatePassword = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else if (value.length < 8) {
    callback(new Error('密码长度不得小于8'))
  } else if (value.length > 16) {
    callback(new Error('密码长度不得大于16'))
  } else {
    callback()
  }
}


const validateNotEmpty = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error("不能为空"))
  } else {
    callback()
  }
}

const repeatPassword = (rule: any, value: string, callback: any) => {
  if (value !== resetForm.password) {
    callback(new Error("两次密码不一致"))
  } else {
    callback()
  }
}

const rules = reactive<FormRules<typeof resetForm>>({
  password: [{ validator: validatePassword, trigger: 'blur' }],
  repeatPassword: [{validator: repeatPassword, trigger: 'blur' }],
  emailCode: [{ validator: validateNotEmpty, trigger: 'blur' }]
})

const doResetPassword = () => {
  resetPassword({
    password: resetForm.password,
    code: resetForm.emailCode,
  })
      .then(()  => {
        ElMessage.success("重设成功")
      })
      .catch((msg: string) => {
        ElMessage.error(msg)
        refreshCaptchaCode()
      })
      .finally(() => {
        isLoading.value = false
      })
}


const submitResetPassword = (formEl: FormInstance | undefined) => {
  if (!formEl) return
  formEl.validate((valid) => {
    if (valid) {
      isLoading.value = true
      doResetPassword()
    } else {
      ElMessage.warning("请确认表单")
    }
  })
}

const sendEmailCode = () => {
  if (resetForm.captchaCode === '') {
    ElMessage.error("请输入验证码")
  } else {
    sendForgetEmailCode({
      username: <string>userForm.userName,
      captchaCode: resetForm.captchaCode,
      captchaToken: resetForm.token,
    }).then(() => {
      ElMessage.success("发送成功")
    }).catch((message) => {
      refreshCaptchaCode()
      ElMessage.warning(message)
    })
  }
}

const refreshCaptchaCode = async () => {
  const {image, token} = await getCaptcha()
  imgData.value = image
  resetForm.token = token
}

// 修改个人信息
const changeSelf = () => {
  change(userForm);
}


const handleUploadAvatar = async (file: File) => {
  openUploadAvatarDialog.value = false;
  if (!file) {
    ElNotification.error("上传失败, 请重试");
    return;
  }

  const data = await uploadAvatar(file);

  if (data) {
    userStore.refreshAvatar(userStore.user?.userId);
    await userStore.loadUser();
    ElNotification.success("头像更换成功");
  } else {
    ElNotification.error("上传失败, 请重试");
  }

}


onMounted(() => {
  refreshCaptchaCode()
  userStore.getUser().then((user) => {
    __.assign(userForm, user);
  })
  activeTab.value = "first";
})



// created
userStore.loadUser();

</script>


<style scoped>
.profile-page { margin: auto; padding: 10px 0 36px; color: var(--el-text-color-primary); }
.profile-hero { position: relative; display: flex; align-items: center; justify-content: space-between; min-height: 218px; padding: 34px 42px; overflow: hidden; border: 1px solid var(--el-border-color-light); border-radius: 22px; background: linear-gradient(135deg, var(--el-color-primary-light-9), var(--el-bg-color)); }
.profile-identity { position: relative; z-index: 1; display: flex; align-items: center; gap: 24px; }
.profile-avatar { position: relative; width: 96px; height: 96px; padding: 0; overflow: hidden; border: 4px solid var(--el-bg-color); border-radius: 28px; background: var(--el-fill-color-light); box-shadow: 0 12px 26px color-mix(in srgb, var(--el-color-primary) 18%, transparent); cursor: pointer; }
.profile-avatar :deep(.avatar) { width: 100%; height: 100%; }
.profile-avatar :deep(.avatar__image) { width: 100%; height: 100%; }
.profile-avatar :deep(.el-avatar) { width: 100%; height: 100%; }
.avatar-edit { position: absolute; right: 0; bottom: 0; left: 0; padding: 5px 0; color: #fff; background: rgb(0 0 0 / 48%); font-size: 11px; opacity: 0; transition: opacity .2s ease; }
.profile-avatar:hover .avatar-edit { opacity: 1; }
.profile-copy { max-width: 650px; }
.eyebrow { display: block; margin-bottom: 7px; color: var(--el-color-primary); font-size: 11px; font-weight: 800; letter-spacing: .18em; }
.profile-copy h1, .section-heading h2 { margin: 0; letter-spacing: -.04em; }
.profile-copy h1 { font-size: clamp(30px, 4vw, 44px); }
.profile-copy p { margin: 11px 0 15px; color: var(--el-text-color-secondary); font-size: 14px; }
.profile-meta { display: flex; align-items: center; gap: 10px; color: var(--el-text-color-regular); font-size: 13px; }
.profile-meta i { width: 4px; height: 4px; border-radius: 50%; background: var(--el-color-primary); }
.back-button { position: relative; z-index: 1; align-self: flex-start; color: var(--el-text-color-regular); }
.hero-glow { position: absolute; right: 8%; bottom: -100px; width: 280px; height: 280px; border: 1px solid color-mix(in srgb, var(--el-color-primary) 25%, transparent); border-radius: 50%; opacity: .65; }
.profile-content { margin-top: 28px; }
.section-heading { display: flex; align-items: end; justify-content: space-between; margin: 0 4px 14px; }
.section-heading .eyebrow { margin-bottom: 4px; }
.section-heading h2 { font-size: 24px; }
.section-caption { color: var(--el-text-color-secondary); font-size: 13px; }
.account-layout { display: grid; grid-template-columns: minmax(250px, 310px) minmax(0, 1fr); gap: 20px; align-items: start; }
.profile-summary { min-width: 0; }
.profile-card, .tab-card { border: 1px solid var(--el-border-color-light); border-radius: 16px; background: var(--el-bg-color); }
.profile-card :deep(.el-card__body) { padding: 24px; }
.summary-avatar { display: flex; justify-content: center; margin-bottom: 16px; }
.summary-avatar :deep(.portrait) { display: inline-flex; width: 88px; height: 88px; align-items: center; justify-content: center; border: 4px solid var(--el-color-primary-light-9); border-radius: 24px; }
.summary-avatar :deep(.portrait .avatar__image), .summary-avatar :deep(.portrait .el-avatar) { width: 100%; height: 100%; }
.profile-card h3 { margin: 0; color: var(--el-text-color-primary); font-size: 21px; text-align: center; }
.summary-handle { margin: 5px 0 0; color: var(--el-text-color-secondary); font-size: 13px; text-align: center; }
.summary-divider { height: 1px; margin: 22px 0 16px; background: var(--el-border-color-lighter); }
.summary-row { display: flex; align-items: center; justify-content: space-between; gap: 14px; padding: 9px 0; color: var(--el-text-color-secondary); font-size: 12px; }
.summary-row strong { overflow: hidden; color: var(--el-text-color-regular); font-size: 13px; font-weight: 600; text-overflow: ellipsis; white-space: nowrap; }
.summary-row .muted { color: var(--el-text-color-placeholder); font-weight: 400; }
.portrait { border-radius: 12px; cursor: pointer; }
.tab-card { min-height: 400px; margin-top: 16px; }
.tab-card :deep(.el-card__body) { display: flex; justify-content: center; padding: 32px clamp(18px, 8vw, 100px); }
.tab-card :deep(.el-form) { width: min(100%, 560px) !important; min-width: 0 !important; }
.settings-form .title-item { margin-bottom: 25px; }
.settings-form .el-form-item__label { color: var(--el-text-color-regular); font-weight: 600; }
.settings-form .el-textarea__inner { border: 0; border-radius: 12px; box-shadow: 0 0 0 1px var(--oj-auth-input-border) inset; }
.settings-form .el-textarea__inner:focus { box-shadow: 0 0 0 2px var(--oj-auth-input-focus) inset; }
.settings-form .submit-button { width: 100%; }
.settings-form .el-row { margin: 0 !important; }
.tab-card :deep(.el-form-item:last-child) { margin-bottom: 0; }
.settings-nav { display: flex; gap: 10px; padding: 6px; border: 1px solid var(--el-border-color-light); border-radius: 16px; background: var(--el-fill-color-lighter); }
.settings-nav-item { display: flex; flex: 1; align-items: center; gap: 12px; min-width: 0; padding: 13px 15px; border: 1px solid transparent; border-radius: 11px; color: var(--el-text-color-secondary); background: transparent; text-align: left; cursor: pointer; transition: .2s ease; }
.settings-nav-item:hover { color: var(--el-text-color-primary); background: var(--el-bg-color); }
.settings-nav-item.active { border-color: var(--el-border-color-light); color: var(--el-text-color-primary); background: var(--el-bg-color); box-shadow: 0 5px 14px color-mix(in srgb, var(--el-color-primary) 9%, transparent); }
.nav-index { color: var(--el-color-primary); font-size: 11px; font-weight: 800; letter-spacing: .08em; }
.settings-nav-item strong, .settings-nav-item small { display: block; }
.settings-nav-item strong { font-size: 13px; }
.settings-nav-item small { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 11px; }
.nav-arrow { margin-left: auto; color: var(--el-text-color-placeholder); font-size: 22px; line-height: 1; }
.settings-nav-item.active .nav-arrow { color: var(--el-color-primary); }
.settings-panel { min-width: 0; }
.settings-pane { min-width: 0; }
.summary-tip { margin-top: 12px; padding: 16px 18px; border: 1px solid color-mix(in srgb, var(--el-color-primary) 18%, var(--el-border-color-light)); border-radius: 14px; background: var(--el-color-primary-light-9); }
.summary-tip strong { color: var(--el-color-primary); font-size: 13px; }
.summary-tip p { margin: 6px 0 0; color: var(--el-text-color-secondary); font-size: 12px; line-height: 1.7; }
.avatar-dialog :deep(.el-dialog__header) { margin-right: 0; padding: 22px 24px 10px; }
.avatar-dialog :deep(.el-dialog__title) { color: var(--el-text-color-primary); font-size: 17px; font-weight: 750; }
.avatar-dialog :deep(.el-dialog__body) { padding: 10px 24px 24px; }
.avatar-dialog__intro { display: flex; align-items: center; gap: 11px; padding: 13px 14px; border: 1px solid var(--el-border-color-lighter); border-radius: 13px; background: var(--el-fill-color-light); }
.avatar-dialog__icon { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border-radius: 10px; background: color-mix(in srgb, var(--el-color-primary) 13%, var(--el-bg-color)); color: var(--el-color-primary); font-size: 17px; }
.avatar-dialog__intro strong, .avatar-dialog__intro p { display: block; }
.avatar-dialog__intro strong { font-size: 13px; }
.avatar-dialog__intro p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.5; }
.avatar-cutter-shell { display: flex; min-height: 118px; align-items: center; justify-content: center; margin: 18px 0 8px; padding: 18px; border: 1px dashed var(--el-border-color); border-radius: 14px; background: var(--el-bg-color-page); }
.avatar-cutter-shell :deep(.btn) { display: inline-flex; min-width: 142px; align-items: center; justify-content: center; border-radius: 10px; font-size: 13px; line-height: 1.2; }
.avatar-dialog__tip { margin: 0; color: var(--el-text-color-secondary); font-size: 11px; line-height: 1.6; text-align: center; }
@media (max-width: 650px) {
  .profile-page { padding: 8px 12px 28px; }
  .profile-hero { min-height: 300px; align-items: flex-start; padding: 28px 22px; }
  .profile-identity { align-items: flex-start; flex-direction: column; gap: 16px; }
  .profile-avatar { width: 72px; height: 72px; border-radius: 20px; }
  .profile-copy h1 { font-size: 30px; }
  .profile-copy p { max-width: 280px; line-height: 1.7; }
  .profile-meta { flex-wrap: wrap; }
  .back-button { position: absolute; top: 18px; right: 16px; }
  .hero-glow { right: -100px; bottom: -110px; }
  .section-heading { align-items: flex-start; flex-direction: column; gap: 7px; }
  .account-layout { display: block; }
  .summary-tip { display: none; }
  .account-settings { margin-top: 18px; }
  .settings-nav { display: grid; grid-template-columns: 1fr 1fr; }
  .settings-nav-item { gap: 8px; padding: 12px 10px; }
  .settings-nav-item small { font-size: 10px; }
  .nav-arrow { display: none; }
  .profile-card :deep(.el-card__body) { padding: 14px; overflow-x: auto; }
  .profile-card :deep(.el-descriptions) { min-width: 560px; }
  .tab-card :deep(.el-card__body) { padding: 24px 14px; }
  .avatar-dialog :deep(.el-dialog__header) { padding: 18px 16px 8px; }
  .avatar-dialog :deep(.el-dialog__body) { padding: 8px 16px 18px; }
  .avatar-cutter-shell { margin-top: 14px; padding: 14px; }
}
</style>
