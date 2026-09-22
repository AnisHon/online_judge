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
          type="text"
          autocomplete="off"
          placeholder="请输入用户名或邮箱"
          prefix-icon="UserFilled"
      />
    </el-form-item>
    <el-form-item prop="password">
      <el-input
          v-model="loginForm.password"
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
            :loading="captchaLoading"
            :alt="captchaError || '点击刷新验证码'"
            class="captcha-image"
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

    <el-form-item class="submit-item">
      <el-button
          type="primary"
          native-type="submit"
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
import {login} from "@/api/auth/authentication.ts"
import {type LoginForm} from "@/api/auth/authentication.ts"
import IconCaptcha from "@/assets/icons/IconCaptcha.vue";
import {useCaptchaCode} from '@/composables/auth/useCaptchaCode'
import {authErrorMessage} from '@/utils/authError'

const formRef = ref<FormInstance>()
const isLoading = ref(false)
const loginForm = reactive<LoginForm>({
  username: "",
  password: "",
  captchaCode: "",
  token: "",
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

const rules = reactive<FormRules<typeof loginForm>>({
  username: [{ required: true, message: "用户名不能为空", trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  captchaCode: [{ required: true, message: "验证码不能为空", trigger: 'blur' }]
})

const {image: imgData, loading: captchaLoading, error: captchaError, refresh: refreshCaptchaCode} = useCaptchaCode(loginForm)

const submitLogin = async (formEl: FormInstance | undefined = formRef.value) => {
  if (!formEl) return
  if (isLoading.value) return
  isLoading.value = true
  try {
    const valid = await formEl.validate().catch(() => false)
    if (!valid) {
      ElNotification.warning("请确认表单")
      return
    }
    await login(loginForm)
    ElNotification.success("欢迎登录")
  } catch (error: unknown) {
    ElNotification.error(authErrorMessage(error, "登录失败，请稍后重试"))
    void refreshCaptchaCode()
  } finally {
    isLoading.value = false
  }
}

onMounted(() => {
  void refreshCaptchaCode()
})

</script>
