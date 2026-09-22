<template>
  <el-form
      ref="formRef"
      class="auth-form"
      :model="forgetPasswordForm"
      status-icon
      :rules="rules"
      :aria-autocomplete="false"
      @submit.prevent="submitResetPassword(formRef)"
  >
    <el-form-item class="title-item">
      <div>
        <p class="welcome-label">RESET PASSWORD</p>
        <h1>找回密码</h1>
        <p class="subtitle">验证身份后设置一个新的登录密码</p>
      </div>
    </el-form-item>

    <el-form-item prop="username">
      <el-input
          v-model="forgetPasswordForm.username"
          type="text"
          autocomplete="off"
          placeholder="请输入用户名或邮箱"
          prefix-icon="UserFilled"
      />
    </el-form-item>

    <el-form-item prop="password">
      <el-input
          v-model="forgetPasswordForm.password"
          type="password"
          autocomplete="off"
          placeholder="请输入新密码"
          prefix-icon="Lock"
      />
    </el-form-item>

    <el-form-item prop="repeatPassword">
      <el-input
          v-model="forgetPasswordForm.repeatPassword"
          type="password"
          autocomplete="off"
          placeholder="请再次输入新密码"
          prefix-icon="Lock"
      />
    </el-form-item>

    <el-row justify="space-between" style="width: 100%;">
      <el-col :span="14" >
        <el-form-item prop="captchaCode">
          <el-input
              v-model="forgetPasswordForm.captchaCode"
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
            :alt="captchaError || '点击刷新验证码'"
            class="captcha-image"
            @click="void refreshCaptchaCode()"
        />
        <span
            v-if="captchaError"
            class="captcha-error"
            role="button"
            tabindex="0"
            aria-live="polite"
            @click="void refreshCaptchaCode()"
            @keydown.enter="void refreshCaptchaCode()"
        >{{ captchaError }}</span>
      </el-col>


    </el-row>

    <el-row justify="space-between" style="width: 100%;">
      <el-col :span="14" >
        <el-form-item prop="emailCode">
          <el-input
              v-model="forgetPasswordForm.emailCode"
              type="text"
              autocomplete="off"
              placeholder="请输入验证码"
              :prefix-icon="IconCaptcha"
          />
        </el-form-item>
      </el-col>


      <el-col :span="10" style="position: relative;">
        <el-button
            native-type="button"
            @click="void sendEmailCode()"
            class="code-button"
            :loading="emailSending"
            :disabled="emailSending || emailCooldown > 0"
        >
          {{ emailCodeLabel }}
        </el-button>
      </el-col>


    </el-row>

    <el-form-item class="submit-item">
      <el-button
          type="primary"
          native-type="submit"
          class="submit-button"
          :loading="isLoading"
          :disabled="isLoading"
      >
          确认重设密码
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script lang="ts" setup>
import {reactive, ref} from 'vue'
import {ElNotification, type FormInstance, type FormRules} from 'element-plus'
import {forgetPassword} from "@/api/auth/authentication.ts"
import {sendForgetEmailCode} from "@/api/auth/emailCode.ts";
import IconCaptcha from "@/assets/icons/IconCaptcha.vue";
import router from "@/router";
import {useCaptchaCode} from '@/composables/auth/useCaptchaCode'
import {useEmailCodeCooldown} from '@/composables/auth/useEmailCodeCooldown'
import {authErrorMessage} from '@/utils/authError'

const formRef = ref<FormInstance>()
const isLoading = ref(false)
const forgetPasswordForm = reactive({
  username: "",
  password: "",
  repeatPassword: "",
  captchaCode: "",
  token: "",
  emailCode: ""
})

// 可替换成正则
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

const repeatPassword = (rule: any, value: string, callback: any) => {
  if (value !== forgetPasswordForm.password) {
    callback(new Error("两次密码不一致"))
  } else {
    callback()
  }
}

const rules = reactive<FormRules<typeof forgetPasswordForm>>({
  username: [{ required: true, message: "用户名不能为空", trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  repeatPassword: [{validator: repeatPassword, trigger: 'blur' }],
  captchaCode: [{ required: true, message: "图形验证码不能为空", trigger: 'blur' }],
  emailCode: [{ required: true, message: "邮箱验证码不能为空", trigger: 'blur' }]
})

const {image: imgData, error: captchaError, refresh: refreshCaptchaCode} = useCaptchaCode(forgetPasswordForm)
const {sending: emailSending, remaining: emailCooldown, label: emailCodeLabel, run: runEmailCode} = useEmailCodeCooldown()

const submitResetPassword = async (formEl: FormInstance | undefined = formRef.value) => {
  if (!formEl) return
  if (isLoading.value) return
  isLoading.value = true
  try {
    const valid = await formEl.validate().catch(() => false)
    if (!valid) {
      ElNotification.warning("请确认表单")
      return
    }
    await forgetPassword({
      username: forgetPasswordForm.username,
      password: forgetPasswordForm.password,
      code: forgetPasswordForm.emailCode,
    })
    ElNotification.success("重设成功")
    await router.push({name: "login"})
  } catch (error: unknown) {
    ElNotification.error(authErrorMessage(error, "重设密码失败"))
    void refreshCaptchaCode()
  } finally {
    isLoading.value = false
  }
}

// 发送邮箱验证码
const sendEmailCode = async () => {
  if (emailSending.value || emailCooldown.value > 0) return
  if (forgetPasswordForm.captchaCode === '') {
    ElNotification.error("请输入验证码")
  } else if (forgetPasswordForm.username === '') {
    ElNotification.error("请输入用户名或邮箱")
  } else {
    try {
      const sent = await runEmailCode(() => sendForgetEmailCode({
        captchaCode: forgetPasswordForm.captchaCode,
        username: forgetPasswordForm.username,
        captchaToken: forgetPasswordForm.token,
      }))
      if (sent) ElNotification.success("发送成功，请查收邮件")
    } catch (error: unknown) {
      void refreshCaptchaCode()
      ElNotification.warning(authErrorMessage(error, "验证码发送失败"))
    }
  }
}

void refreshCaptchaCode()

</script>
