<template>
  <el-form
      ref="formRef"
      label-position="left"
      style="max-width: 600px"
      :model="loginForm"
      status-icon
      :rules="rules"
      :aria-autocomplete="false"
      class="auth-form"
      @submit.prevent="submitLogin(formRef)"
  >
    <el-form-item class="title-item">
      <div>
        <p class="welcome-label">WELCOME BACK</p>
        <h1>欢迎登录</h1>
        <p class="subtitle">登录后继续你的编程旅程</p>
      </div>
    </el-form-item>

    <el-form-item prop="username">
      <el-input
          v-model="loginForm.username"
          @keyup.enter="submitLogin(formRef)"
          type="text"
          autocomplete="off"
          placeholder="请输入用户名或邮箱"
          prefix-icon="UserFilled"
      />
    </el-form-item>
    <el-form-item prop="password">
      <el-input
          v-model="loginForm.password"
          @keyup.enter="submitLogin(formRef)"
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
              v-model="loginForm.captchaCode"
              @keyup.enter="submitLogin(formRef)"
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
            style="width: 100px; position: absolute;
              right: 0"
            @click="refreshCaptchaCode"
        />
      </el-col>


    </el-row>

    <el-form-item class="submit-item">
      <el-button
          type="primary"
          @click="submitLogin(formRef)"
          class="submit-button"
          :loading="isLoading"
          :disabled="isLoading"
      >
        登录
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script lang="ts" setup>
import {onMounted, reactive, ref} from 'vue'
import {ElNotification, type FormInstance, type FormRules} from 'element-plus'
import getCaptcha from '@/api/auth/captchaCode.ts'
import {login} from "@/api/auth/authentication.ts"
import {type LoginForm} from "@/api/auth/authentication.ts"
import IconCaptcha from "@/assets/icons/IconCaptcha.vue";

const formRef = ref<FormInstance>()
const isLoading = ref(false)
const loginForm = reactive<LoginForm>({
  username: "",
  password: "",
  captchaCode: "",
  token: "",
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

const rules = reactive<FormRules<typeof loginForm>>({
  username: [{ required: true, message: "用户名不能为空", trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  captchaCode: [{ required: true, message: "验证码不能为空", trigger: 'blur' }]
})

const doLogin = () => {
  login(loginForm)
      .then(()  => {
        ElNotification.success("欢迎登录")
      })
      .catch((error: unknown) => {
        const message = error instanceof Error ? error.message : typeof error === "string" ? error : "登录失败，请稍后重试"
        ElNotification.error(message)
        refreshCaptchaCode()
      })
      .finally(() => {
        isLoading.value = false
      })
}

const submitLogin = (formEl: FormInstance | undefined) => {
  if (!formEl) return
  formEl.validate((valid) => {
    if (valid) {
      isLoading.value = true
      doLogin()
    } else {
      ElNotification.warning("请确认表单")
    }
  })
}

const refreshCaptchaCode = async () => {
  const {image, token} = await getCaptcha()
  imgData.value = image
  loginForm.token = token
}

onMounted(() => {
  refreshCaptchaCode()
})

</script>
