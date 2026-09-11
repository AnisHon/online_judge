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
          @keyup.enter="submitSignUp(formRef)"
          type="text"
          autocomplete="off"
          placeholder="请输入用户名"
          prefix-icon="UserFilled"
      />
    </el-form-item>

    <el-form-item prop="nikeName">
      <el-input
          v-model="signUpForm.nikeName"
          @keyup.enter="submitSignUp(formRef)"
          type="text"
          autocomplete="off"
          placeholder="请输入昵称"
          prefix-icon="User"
      />
    </el-form-item>
    <el-form-item prop="email">
      <el-input
          v-model="signUpForm.email"
          @keyup.enter="submitSignUp(formRef)"
          type="text"
          autocomplete="off"
          placeholder="请输入邮箱"
          :prefix-icon="IconEmail"
      />
    </el-form-item>
    <el-form-item prop="password">
      <el-input
          v-model="signUpForm.password"
          @keyup.enter="submitSignUp(formRef)"
          type="password"
          autocomplete="off"
          placeholder="请输入密码"
          prefix-icon="Lock"
      />
    </el-form-item>

    <el-form-item prop="repeatPassword">
      <el-input
          v-model="signUpForm.repeatPassword"
          @keyup.enter="submitSignUp(formRef)"
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
              @keyup.enter="submitSignUp(formRef)"
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
            style="width: 100px; position: absolute;
              right: 0"
            @click="refreshCaptchaCode"
        />
      </el-col>


    </el-row>

    <el-row justify="space-between" style="width: 100%;">
      <el-col :span="14" >
        <el-form-item prop="emailCode">
          <el-input
              @keyup.enter="submitSignUp(formRef)"
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
            @click="sendEmailCode()"
            style="position: absolute; right: 0;"
            :loading="isLoading"
            :disabled="isLoading"
        >
          获取邮箱验证码
        </el-button>
      </el-col>


    </el-row>

    <el-form-item class="submit-item">
      <el-button
          type="primary"
          @click="submitSignUp(formRef)"
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
import getCaptcha from '@/api/auth/captchaCode.ts'
import {sendEmailCodePromise} from '@/api/auth/emailCode.ts'
import {signUp, checkAvailableUsername, checkAvailableEmail} from "@/api/auth/authentication.ts"
import IconEmail from "@/assets/icons/IconEmail.vue";
import IconCaptcha from "@/assets/icons/IconCaptcha.vue";


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

const validateUsername = (rule: any, value: string, callback: any) => {
  const pattern = /^[a-zA-Z0-9_-]{3,15}$/;
  if (!pattern.test(value)) {
    callback(new Error("只能由数字、字母_、-组成，长度3-15"))
  } else {
    checkAvailableUsername(value)
        .then((data) => {
          if (data) {
            callback()
          } else {
            callback(new Error("用户名不可用请重试"))
          }
        })
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
  if (value !== signUpForm.password) {
    callback(new Error("两次密码不一致"))
  } else {
    callback()
  }
}

const validateEmail = (rule: any, value: string, callback: any) => {
  if (!emailRe.test(value)) {
    callback(new Error("无效邮箱"))

  } else {
    checkAvailableEmail(value)
        .then((data) => {
          if (data) {
            callback()
          } else {
            callback(new Error("邮箱不可用请重试"))
          }
        })
  }
}

const rules = reactive<FormRules<typeof signUpForm>>({
  username: [{ validator: validateUsername, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  nikeName: [{required: true, message: "昵称不能为空", trigger: 'blur'}],
  email: [{ validator: validateEmail, trigger: 'blur' }],
  // captchaCode: [{ validator: validateNotEmpty, trigger: 'blur' }],
  repeatPassword: [{validator: repeatPassword, trigger: 'blur' }],
  emailCode: [{ required: true, message: "邮箱验证码不能为空", trigger: 'blur' }]
})

const doSignUp = () => {
  signUp({
    userName: signUpForm.username,
    password: signUpForm.password,
    nikeName: signUpForm.nikeName,
    email: signUpForm.email,
    code: signUpForm.emailCode,
  })
      .then(()  => {
        ElNotification.success("欢迎登录")
      })
      .catch((error: unknown) => {
        const message = error instanceof Error ? error.message : typeof error === "string" ? error : "注册失败，请稍后重试"
        ElNotification.error(message)
        refreshCaptchaCode()
      })
      .finally(() => {
        isLoading.value = false
      })
}

const submitSignUp = (formEl: FormInstance | undefined) => {
  if (!formEl) return
  formEl.validate((valid) => {
    if (valid) {
      isLoading.value = true
      doSignUp()
    } else {
      ElNotification.warning("请确认表单")
    }
  })
}

const sendEmailCode = () => {
  if (signUpForm.captchaCode === '') {
    ElNotification.error("请输入验证码")
  } else if (!emailRe.test(signUpForm.email)) {
    ElNotification.error("邮箱无效")
  } else {
    sendEmailCodePromise({
      captchaCode: signUpForm.captchaCode,
      email: signUpForm.email,
      captchaToken: signUpForm.token,
    }).then(() => {
      ElNotification.success("发送成功")
    }).catch((message) => {
      refreshCaptchaCode()
      ElNotification.warning(message)
    })
  }
}

const refreshCaptchaCode = async () => {
  const {image, token} = await getCaptcha()
  imgData.value = image
  signUpForm.token = token
}

onMounted(() => {
  refreshCaptchaCode()
})

</script>

<style scoped>
.auth-form { width: 100%; }
.title-item { margin-bottom: 28px; }
.title-item :deep(.el-form-item__content) { display: block; }
.welcome-label { margin: 0 0 8px; color: #2563eb; font-size: 11px; font-weight: 800; letter-spacing: .18em; }
.title-item h1 { margin: 0; color: #172554; font-size: 32px; letter-spacing: -.04em; }
.subtitle { margin: 10px 0 0; color: #94a3b8; font-size: 14px; }
.auth-form :deep(.el-input__wrapper) { min-height: 46px; border-radius: 12px; box-shadow: 0 0 0 1px #e2e8f0 inset; transition: box-shadow .2s, background .2s; }
.auth-form :deep(.el-input__wrapper.is-focus) { box-shadow: 0 0 0 2px #93c5fd inset; background: #f8fbff; }
.auth-form :deep(.el-form-item) { margin-bottom: 18px; }
.submit-item { margin-top: 8px; }
.submit-button { width: 100%; height: 46px; border: 0; border-radius: 12px; font-weight: 700; background: linear-gradient(135deg, #2563eb, #4f46e5); box-shadow: 0 10px 22px rgba(37, 99, 235, .22); }
.submit-button:hover { transform: translateY(-1px); box-shadow: 0 12px 25px rgba(37, 99, 235, .3); }
</style>
