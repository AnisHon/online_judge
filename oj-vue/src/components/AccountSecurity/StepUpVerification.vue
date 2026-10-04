<template>
  <el-form class="verification-form" size="large" label-position="top" @submit.prevent="verify">
    <p class="verification-hint">为保护账号，请先确认是你本人操作。</p>
    <el-form-item v-if="email" label="验证方式">
      <el-radio-group v-model="method" :disabled="busy || sending">
        <el-radio-button value="PASSWORD">当前密码</el-radio-button>
        <el-radio-button value="EMAIL">绑定邮箱</el-radio-button>
      </el-radio-group>
    </el-form-item>
    <el-form-item v-if="method === 'PASSWORD'" label="当前密码">
      <el-input v-model="password" type="password" show-password autocomplete="current-password" maxlength="128" placeholder="请输入当前登录密码" :disabled="busy" />
    </el-form-item>
    <template v-else>
      <p class="verification-email">验证码将发送至 {{ maskedEmail }}</p>
      <el-form-item label="图形验证码"><CaptchaInput ref="captcha" v-model:code="form.captchaCode" v-model:token="form.token" /></el-form-item>
      <el-form-item label="邮箱验证码">
        <div class="verification-code">
          <el-input v-model="emailCode" maxlength="6" inputmode="numeric" autocomplete="one-time-code" placeholder="6 位验证码" />
          <el-button :loading="sending" :disabled="sending || remaining > 0 || busy" @click="sendCode">{{ label }}</el-button>
        </div>
      </el-form-item>
    </template>
    <p v-if="error" class="verification-error" role="alert">{{ error }}</p>
    <el-button class="verification-submit" type="primary" native-type="submit" :loading="busy" :disabled="busy || sending">验证身份</el-button>
  </el-form>
</template>

<script setup lang="ts">
import {computed, reactive, ref, watch, onBeforeUnmount} from 'vue'
import CaptchaInput from './CaptchaInput.vue'
import {sendIdentityCode, verifySensitiveAction, type SensitiveAction, type VerificationMethod, type StepUpGrant} from '@/api/auth/accountSecurity'
import {useEmailCodeCooldown} from '@/composables/auth/useEmailCodeCooldown'
import {useToken} from '@/stores/useToken'
const props = defineProps<{action: SensitiveAction; email?: string | null}>()
const emit = defineEmits<{verified: [grant: StepUpGrant]}>()
const tokenStore = useToken()
const method = ref<VerificationMethod>('PASSWORD')
const password = ref('')
const emailCode = ref('')
const busy = ref(false)
const error = ref('')
const form = reactive({token: '', captchaCode: ''})
const captcha = ref<InstanceType<typeof CaptchaInput>>()
const {sending, remaining, label, run} = useEmailCodeCooldown()
const maskedEmail = computed(() => props.email?.replace(/^(.{1,2}).*(@.*)$/, '$1***$2') || '')
let mounted = true
onBeforeUnmount(() => { mounted = false; password.value = ''; emailCode.value = '' })
watch(method, () => { error.value = ''; password.value = ''; emailCode.value = '' })
const sendCode = async () => {
  if (!form.token || !form.captchaCode) { error.value = '请先填写图形验证码'; return }
  error.value = ''
  try { await run(() => sendIdentityCode({action: props.action, captchaToken: form.token, captchaCode: form.captchaCode})) }
  catch (cause) { if (mounted) error.value = cause instanceof Error ? cause.message : '验证码发送失败' }
  finally { if (mounted) void captcha.value?.refresh() }
}
const verify = async () => {
  if (busy.value || sending.value) return
  if (method.value === 'PASSWORD' ? !password.value : !/^\d{6}$/.test(emailCode.value)) {
    error.value = method.value === 'PASSWORD' ? '请输入当前密码' : '请输入 6 位邮箱验证码'
    return
  }
  busy.value = true
  error.value = ''
  const sessionVersion = tokenStore.getSessionVersion()
  try {
    const grant = await verifySensitiveAction({action: props.action, method: method.value,
      ...(method.value === 'PASSWORD' ? {password: password.value} : {code: emailCode.value})})
    password.value = ''; emailCode.value = ''
    if (mounted && tokenStore.hasToken() && sessionVersion === tokenStore.getSessionVersion()) emit('verified', grant)
  } catch (cause) { if (mounted) error.value = cause instanceof Error ? cause.message : '身份验证失败' }
  finally { if (mounted) busy.value = false }
}
</script>

<style scoped>
.verification-hint, .verification-email { margin: 0 0 20px; color: var(--el-text-color-secondary); line-height: 1.7; overflow-wrap: anywhere; }
.verification-code { display: flex; width: 100%; gap: 12px; }
.verification-code .el-input { min-width: 0; }
.verification-code .el-button { flex: 0 0 auto; min-width: 150px; margin: 0; }
.verification-error { color: var(--el-color-danger); font-size: 13px; line-height: 1.6; }
.verification-submit { min-width: 130px; min-height: 40px; }
@media (max-width: 420px) { .verification-code { flex-direction: column; } .verification-code .el-button { flex-basis: auto; } }
</style>
