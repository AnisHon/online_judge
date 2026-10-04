<template>
  <div class="account-security">
    <article class="security-card">
      <el-icon class="security-card__icon"><Lock /></el-icon>
      <div class="security-card__copy"><h3>登录密码</h3><p>定期更换密码，保护你的账号。</p></div>
      <el-button plain @click="open('PASSWORD')">修改密码</el-button>
    </article>
    <article class="security-card">
      <el-icon class="security-card__icon"><Message /></el-icon>
      <div class="security-card__copy"><h3>绑定邮箱 <el-tag v-if="!email" size="small" type="warning">未绑定</el-tag></h3><p>{{ email || '绑定邮箱后可以找回密码。' }}</p></div>
      <el-button plain @click="open('EMAIL')">{{ email ? '修改邮箱' : '绑定邮箱' }}</el-button>
    </article>
    <el-dialog v-model="visible" :title="action === 'PASSWORD' ? '修改密码' : (email ? '修改邮箱' : '绑定邮箱')"
               width="min(500px, calc(100vw - 32px))" append-to-body destroy-on-close align-center
               :close-on-click-modal="false" :before-close="beforeClose" @closed="reset">
      <div class="security-steps" aria-label="操作步骤"><span :class="{active: !grant}">1 验证身份</span><span :class="{active: !!grant}">2 {{ action === 'PASSWORD' ? '设置新密码' : '验证新邮箱' }}</span></div>
      <StepUpVerification v-if="!grant" :key="action" :action="action" :email="email" @verified="verified" />
      <el-form v-else ref="formRef" :model="form" :rules="rules" size="large" label-position="top" @submit.prevent="save">
        <template v-if="action === 'PASSWORD'">
          <el-form-item label="新密码" prop="password"><el-input v-model="form.password" type="password" show-password autocomplete="new-password" placeholder="8—16 位密码" maxlength="16" /></el-form-item>
          <el-form-item label="确认新密码" prop="repeatPassword"><el-input v-model="form.repeatPassword" type="password" show-password autocomplete="new-password" placeholder="再次输入新密码" maxlength="16" /></el-form-item>
        </template>
        <template v-else>
          <el-form-item label="新邮箱" prop="newEmail"><el-input v-model="form.newEmail" type="email" autocomplete="email" maxlength="255" placeholder="请输入新的邮箱地址" :disabled="sending || saving" /></el-form-item>
          <el-form-item label="图形验证码"><CaptchaInput ref="captcha" v-model:code="form.captchaCode" v-model:token="form.token" /></el-form-item>
          <el-form-item label="新邮箱验证码" prop="newEmailCode">
            <div class="security-code"><el-input v-model="form.newEmailCode" maxlength="6" inputmode="numeric" autocomplete="one-time-code" placeholder="6 位验证码" />
              <el-button :loading="sending" :disabled="sending || remaining > 0 || saving" @click="sendCode">{{ label }}</el-button></div>
          </el-form-item>
        </template>
        <p v-if="error" class="security-error" role="alert">{{ error }}</p>
        <p class="security-note">{{ action === 'PASSWORD' ? '修改后需要重新登录。' : '验证新邮箱后，原邮箱将解除绑定。' }}</p>
        <div class="security-actions"><el-button :disabled="saving" @click="restart">重新验证</el-button><el-button type="primary" native-type="submit" :loading="saving" :disabled="saving">保存修改</el-button></div>
      </el-form>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import {onBeforeUnmount, reactive, ref, watch} from 'vue'
import {Lock, Message} from '@element-plus/icons-vue'
import {ElMessage, type FormInstance, type FormRules} from 'element-plus'
import StepUpVerification from './StepUpVerification.vue'
import CaptchaInput from './CaptchaInput.vue'
import {changeEmail, changePassword, sendNewEmailCode, type SensitiveAction, type StepUpGrant} from '@/api/auth/accountSecurity'
import {endLocalSession} from '@/api/auth/authentication'
import {useUserStore} from '@/stores/useUserStore'
import {useToken} from '@/stores/useToken'
import {useEmailCodeCooldown} from '@/composables/auth/useEmailCodeCooldown'
const props = defineProps<{email?: string | null}>()
const tokenStore = useToken()
const emit = defineEmits<{emailChanged: [email: string]}>()
const visible = ref(false)
const action = ref<SensitiveAction>('PASSWORD')
const grant = ref<StepUpGrant | null>(null)
const saving = ref(false)
const error = ref('')
const formRef = ref<FormInstance>()
const captcha = ref<InstanceType<typeof CaptchaInput>>()
const form = reactive({password: '', repeatPassword: '', newEmail: '', newEmailCode: '', token: '', captchaCode: ''})
const {sending, remaining, label, run} = useEmailCodeCooldown()
let expiresAt = 0
let generation = 0
let mounted = true
onBeforeUnmount(() => { mounted = false; reset() })
const rules: FormRules = {
  password: [{required: true, message: '请输入新密码', trigger: 'blur'}, {min: 8, max: 16, message: '密码长度需为 8—16 位', trigger: 'blur'}],
  repeatPassword: [{validator: (_rule, value, callback) => callback(value && value === form.password ? undefined : new Error('两次密码不一致')), trigger: 'blur'}],
  newEmail: [{required: true, message: '请输入新邮箱', trigger: 'blur'}, {type: 'email', message: '邮箱格式不正确', trigger: 'blur'}],
  newEmailCode: [{required: true, pattern: /^\d{6}$/, message: '请输入 6 位邮箱验证码', trigger: 'blur'}],
}
const reset = () => {
  generation++; grant.value = null; expiresAt = 0; error.value = ''
  Object.assign(form, {password: '', repeatPassword: '', newEmail: '', newEmailCode: '', token: '', captchaCode: ''})
}
const open = (next: SensitiveAction) => { reset(); action.value = next; visible.value = true }
const verified = (value: StepUpGrant) => { if (visible.value) { grant.value = value; expiresAt = Date.now() + value.expiresIn * 1000 } }
const restart = () => { reset(); formRef.value?.clearValidate() }
const beforeClose = (done: () => void) => { if (!saving.value && !sending.value) done() }
const requireGrant = () => {
  if (!grant.value || Date.now() >= expiresAt) {
    restart(); ElMessage.warning('身份验证已过期，请重新验证'); return null
  }
  return grant.value.stepUpToken
}
const sendCode = async () => {
  if (saving.value || sending.value || remaining.value > 0) return
  if (!await formRef.value?.validateField('newEmail').then(() => true).catch(() => false)) return
  const stepUpToken = requireGrant()
  if (!stepUpToken) return
  if (!form.token || !form.captchaCode) { error.value = '请先填写图形验证码'; return }
  error.value = ''
  const requestGeneration = generation
  try {
    const sent = await run(() => sendNewEmailCode({stepUpToken, newEmail: form.newEmail.trim(), captchaToken: form.token, captchaCode: form.captchaCode}))
    if (sent && mounted && generation === requestGeneration) ElMessage.success('验证码已发送至新邮箱')
  } catch (cause) {
    if (mounted && generation === requestGeneration) error.value = cause instanceof Error ? cause.message : '验证码发送失败'
  } finally { if (mounted && generation === requestGeneration) void captcha.value?.refresh() }
}
const save = async () => {
  if (saving.value || sending.value) return
  saving.value = true; error.value = ''
  const sessionVersion = tokenStore.getSessionVersion()
  try {
    if (!await formRef.value?.validate().catch(() => false)) return
    const stepUpToken = requireGrant()
    if (!stepUpToken) return
    if (action.value === 'PASSWORD') {
      await changePassword({stepUpToken, password: form.password})
      if (!mounted || tokenStore.getSessionVersion() !== sessionVersion) return
      ElMessage.success('密码已更新，请重新登录')
      visible.value = false
      await endLocalSession()
    } else {
      const email = form.newEmail.trim().toLowerCase()
      await changeEmail({stepUpToken, newEmail: email, newEmailCode: form.newEmailCode})
      if (!mounted || tokenStore.getSessionVersion() !== sessionVersion) return
      const userStore = useUserStore()
      if (userStore.user) userStore.user.email = email
      userStore.dismissEmailReminder(); emit('emailChanged', email)
      ElMessage.success('邮箱已更新'); visible.value = false
    }
  } catch (cause) { if (mounted) error.value = cause instanceof Error ? cause.message : '保存失败，请重试' }
  finally { saving.value = false }
}
watch(() => props.email, () => { if (visible.value && !saving.value) { visible.value = false; reset() } })
</script>

<style scoped>
.account-security { display: grid; gap: 16px; }
.security-card { display: flex; align-items: center; gap: 16px; padding: 22px; border: 1px solid var(--el-border-color-light); border-radius: 16px; background: var(--el-fill-color-extra-light); min-width: 0; }
.security-card__icon { flex: 0 0 auto; width: 42px; height: 42px; border-radius: 12px; background: var(--el-color-primary-light-9); color: var(--el-color-primary); font-size: 21px; }
.security-card__copy { flex: 1; min-width: 0; }
.security-card__copy h3 { display: flex; align-items: center; gap: 8px; margin: 0 0 8px; font-size: 16px; }
.security-card__copy p { margin: 0; color: var(--el-text-color-secondary); font-size: 13px; line-height: 1.6; overflow-wrap: anywhere; }
.security-card > .el-button { flex: 0 0 auto; min-width: 100px; min-height: 40px; }
.security-steps { display: flex; gap: 20px; margin-bottom: 24px; padding-bottom: 16px; border-bottom: 1px solid var(--el-border-color-light); color: var(--el-text-color-placeholder); font-size: 13px; }
.security-steps .active { color: var(--el-color-primary); font-weight: 600; }
.security-code { display: flex; width: 100%; gap: 12px; }
.security-code .el-input { min-width: 0; }
.security-code .el-button { flex: 0 0 auto; margin: 0; min-width: 150px; }
.security-actions { display: flex; justify-content: flex-end; gap: 12px; }
.security-actions .el-button { margin: 0; min-width: 112px; min-height: 40px; }
.security-note { margin: 0 0 20px; color: var(--el-text-color-secondary); font-size: 13px; }
.security-error { color: var(--el-color-danger); font-size: 13px; line-height: 1.6; }
@media (max-width: 520px) { .security-card { flex-wrap: wrap; padding: 18px; } .security-card__copy { flex-basis: calc(100% - 58px); } .security-card > .el-button { margin-left: 58px; } }
@media (max-width: 420px) { .security-code { flex-direction: column; } .security-code .el-button { flex-basis: auto; } }
</style>
