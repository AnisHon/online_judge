<template>
  <el-form
      ref="formRef"
      label-position="left"
      style="max-width: 600px"
      :model="signUpForm"
      status-icon
      :rules="rules"
      :aria-autocomplete="false"
      class="auth-form"
      @submit.prevent="submitSignUp(formRef)"
  >
    <el-form-item class="title-item">
      <div>
        <p class="welcome-label">CREATE YOUR ACCOUNT</p>
        <h1>创建账号</h1>
        <p class="subtitle">注册后开始你的编程旅程</p>
      </div>
    </el-form-item>

    <el-form-item prop="username">
      <el-input
          v-model="signUpForm.username"
          type="text"
          autocomplete="off"
          placeholder="请输入用户名"
          prefix-icon="UserFilled"
      />
    </el-form-item>

    <el-form-item prop="nikeName">
      <el-input
          v-model="signUpForm.nikeName"
          type="text"
          autocomplete="off"
          placeholder="请输入昵称"
          prefix-icon="User"
      />
    </el-form-item>
    <el-form-item prop="email">
      <el-input
          v-model="signUpForm.email"
          type="text"
          autocomplete="off"
          placeholder="请输入邮箱"
          :prefix-icon="IconEmail"
      />
    </el-form-item>
    <el-form-item prop="password">
      <el-input
          v-model="signUpForm.password"
          type="password"
          autocomplete="off"
          placeholder="请输入密码"
          prefix-icon="Lock"
      />
    </el-form-item>

    <el-form-item prop="repeatPassword">
      <el-input
          v-model="signUpForm.repeatPassword"
          type="password"
          autocomplete="off"
          placeholder="请输入密码"
          prefix-icon="Lock"
      />
    </el-form-item>

    <el-row justify="space-between" style="width: 100%;">
      <el-col :span="14" >
        <el-form-item prop="captchaCode">
          <el-input
              v-model="signUpForm.captchaCode"
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
            style="width: 100px; position: absolute;
              right: 0"
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
              v-model="signUpForm.emailCode"
              type="text"
              autocomplete="off"
              placeholder="请输入邮箱验证码"
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
        注册
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {ElNotification, type FormInstance, type FormRules} from 'element-plus'
import {sendEmailCodePromise} from '@/api/auth/emailCode.ts'
import {signUp, checkAvailableUsername, checkAvailableEmail} from "@/api/auth/authentication.ts"
import IconEmail from "@/assets/icons/IconEmail.vue";
import IconCaptcha from "@/assets/icons/IconCaptcha.vue";
import {useCaptchaCode} from '@/composables/auth/useCaptchaCode'
import {useEmailCodeCooldown} from '@/composables/auth/useEmailCodeCooldown'
import {authErrorMessage} from '@/utils/authError'


const emailRe = /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/

const formRef = ref<FormInstance>()
const isLoading = ref(false)
const signUpForm = reactive({
  username: "",
  nikeName: "",
  email: "",
  password: "",
  repeatPassword: "",
  captchaCode: "",
  token: "",
  emailCode: ""
})

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

let usernameValidationId = 0
const validateUsername = (rule: any, value: string, callback: any) => {
  const pattern = /^[a-zA-Z0-9_-]{3,15}$/;
  if (!pattern.test(value)) {
    callback(new Error("只能由数字、字母_、-组成，长度3-15"))
  } else {
    const validationId = ++usernameValidationId
    checkAvailableUsername(value)
        .then((data) => {
          if (validationId !== usernameValidationId) return callback()
          if (data) {
            callback()
          } else {
            callback(new Error("用户名不可用请重试"))
          }
        })
        .catch(() => {
          if (validationId === usernameValidationId) callback(new Error("用户名可用性校验失败，请稍后重试"))
          else callback()
        })
  }
}

const repeatPassword = (rule: any, value: string, callback: any) => {
  if (value !== signUpForm.password) {
    callback(new Error("两次密码不一致"))
  } else {
    callback()
  }
}

let emailValidationId = 0
const validateEmail = (rule: any, value: string, callback: any) => {
  if (!emailRe.test(value)) {
    callback(new Error("无效邮箱"))

  } else {
    const validationId = ++emailValidationId
    checkAvailableEmail(value)
        .then((data) => {
          if (validationId !== emailValidationId) return callback()
          if (data) {
            callback()
          } else {
            callback(new Error("邮箱不可用请重试"))
          }
        })
        .catch(() => {
          if (validationId === emailValidationId) callback(new Error("邮箱可用性校验失败，请稍后重试"))
          else callback()
        })
  }
}

const rules = reactive<FormRules<typeof signUpForm>>({
  username: [{ validator: validateUsername, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  nikeName: [{required: true, message: "昵称不能为空", trigger: 'blur'}],
  email: [{ validator: validateEmail, trigger: 'blur' }],
  captchaCode: [{ required: true, message: "图形验证码不能为空", trigger: 'blur' }],
  repeatPassword: [{validator: repeatPassword, trigger: 'blur' }],
  emailCode: [{ required: true, message: "邮箱验证码不能为空", trigger: 'blur' }]
})

const {image: imgData, error: captchaError, refresh: refreshCaptchaCode} = useCaptchaCode(signUpForm)
const {sending: emailSending, remaining: emailCooldown, label: emailCodeLabel, run: runEmailCode} = useEmailCodeCooldown()

const submitSignUp = async (formEl: FormInstance | undefined = formRef.value) => {
  if (!formEl) return
  if (isLoading.value) return
  isLoading.value = true
  try {
    const valid = await formEl.validate().catch(() => false)
    if (!valid) {
      ElNotification.warning("请确认表单")
      return
    }
    await signUp({
      userName: signUpForm.username,
      password: signUpForm.password,
      nikeName: signUpForm.nikeName,
      email: signUpForm.email,
      code: signUpForm.emailCode,
    })
    ElNotification.success("注册成功，欢迎加入")
  } catch (error: unknown) {
    ElNotification.error(authErrorMessage(error, "注册失败，请稍后重试"))
    void refreshCaptchaCode()
  } finally {
    isLoading.value = false
  }
}

const sendEmailCode = async () => {
  if (emailSending.value || emailCooldown.value > 0) return
  if (signUpForm.captchaCode === '') {
    ElNotification.error("请输入验证码")
  } else if (!emailRe.test(signUpForm.email)) {
    ElNotification.error("邮箱无效")
  } else {
    try {
      const sent = await runEmailCode(() => sendEmailCodePromise({
        captchaCode: signUpForm.captchaCode,
        email: signUpForm.email,
        captchaToken: signUpForm.token,
      }))
      if (sent) ElNotification.success("发送成功，请查收邮件")
    } catch (error: unknown) {
      void refreshCaptchaCode()
      ElNotification.warning(authErrorMessage(error, "验证码发送失败"))
    }
  }
}

onMounted(() => {
  void refreshCaptchaCode()
})

</script>
