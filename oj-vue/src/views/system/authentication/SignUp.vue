<template>
  <el-form
      ref="formRef"
      label-position="left"
      style="max-width: 600px"
      :model="signUpForm"
      status-icon
      :rules="rules"
      :aria-autocomplete="false"
  >
    <el-form-item>
      <h1 style="margin: 0; color: #303133; text-align: center; width: 100%;">注册</h1>
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

    <el-form-item>
      <el-button
          type="primary"
          @click="submitSignUp(formRef)"
          style="width: 80%; margin: auto"
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
import {type FormInstance, type FormRules} from 'element-plus'
import getCaptcha from '@/api/auth/captchaCode'
import getEmailCode from '@/api/auth/emailCode'
import {signUp, checkAvailableUsername, checkAvailableEmail} from "@/api/auth/authentication"
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
  if (value === '') {
    callback(new Error("请输入用户名"))
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
  nikeName: [{validator: validateNotEmpty, trigger: 'blur'}],
  email: [{ validator: validateEmail, trigger: 'blur' }],
  // captchaCode: [{ validator: validateNotEmpty, trigger: 'blur' }],
  repeatPassword: [{validator: repeatPassword, trigger: 'blur' }],
  emailCode: [{ validator: validateNotEmpty, trigger: 'blur' }]
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
        ElMessage.success("欢迎登录")
      })
      .catch((msg) => {
        ElMessage.error(msg)
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
      ElMessage.warning("请确认表单")
    }
  })
}

const sendEmailCode = () => {
  if (signUpForm.captchaCode === '') {
    ElMessage.error("请输入验证码")
  } else if (!emailRe.test(signUpForm.email)) {
    ElMessage.error("邮箱无效")
  } else {
    getEmailCode({
      captchaCode: signUpForm.captchaCode,
      email: signUpForm.email,
      captchaToken: signUpForm.token,
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
  signUpForm.token = token
}

onMounted(() => {
  refreshCaptchaCode()
})

</script>

<style scoped>


</style>
