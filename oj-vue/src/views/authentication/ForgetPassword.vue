<template>
  <el-form
      ref="formRef"
      style="max-width: 600px"
      :model="forgetPasswordForm"
      status-icon
      :rules="rules"
      :aria-autocomplete="false"
      @submit="submitResetPassword(formRef)"
  >
    <el-form-item>
      <h1 style="margin: 0; color: #303133; text-align: center; width: 100%;">忘记密码</h1>
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
          placeholder="请输入密码"
          prefix-icon="Lock"
      />
    </el-form-item>

    <el-form-item prop="repeatPassword">
      <el-input
          v-model="forgetPasswordForm.repeatPassword"
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
          @click="submitResetPassword(formRef)"
          style="width: 80%; margin: auto"
          :loading="isLoading"
          :disabled="isLoading"
      >
        重设密码
      </el-button>
    </el-form-item>
  </el-form>
</template>

<script lang="ts" setup>
import {reactive, ref} from 'vue'
import {ElNotification, type FormInstance, type FormRules} from 'element-plus'
import getCaptcha from '@/api/auth/captchaCode.ts'
import {forgetPassword} from "@/api/auth/authentication.ts"
import {sendForgetEmailCode} from "@/api/auth/emailCode.ts";
import IconCaptcha from "@/assets/icons/IconCaptcha.vue";
import router from "@/router";

const formRef = ref<FormInstance>()
const isLoading = ref(false)
const forgetPasswordForm = reactive({
  username: "",
  nikeName: "",
  email: "",
  password: "",
  repeatPassword: "",
  captchaCode: "",
  token: "",
  emailCode: ""
})

// 验证码图片，base64编码内容
const imgData = ref("")

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

// 可直接使用required
const validateNotEmpty = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error("不能为空"))
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
  emailCode: [{ required: true, message: "邮箱验证码不能为空", trigger: 'blur' }]
})

// 发送重置密码
const doResetPassword = () => {
  forgetPassword({
    username: forgetPasswordForm.username,
    password: forgetPasswordForm.password,
    code: forgetPasswordForm.emailCode,
  })
      .then(()  => {
        ElNotification.success("重设成功")
        router.push({name: "login"})
      })
      .catch((msg) => {
        ElNotification.error(msg)
        refreshCaptchaCode();
      })
      .finally(() => {
        isLoading.value = false;
      })
}

// 发送重置密码（表单验证）
const submitResetPassword = (formEl: FormInstance | undefined) => {
  if (!formEl) return
  formEl.validate((valid) => {
    if (valid) {
      isLoading.value = true;
      doResetPassword();
    } else {
      ElNotification.warning("请确认表单")
    }
  })
}

// 发送邮箱验证码
const sendEmailCode = () => {
  if (forgetPasswordForm.captchaCode === '') {
    ElNotification.error("请输入验证码")
  } else {
    sendForgetEmailCode({
      captchaCode: forgetPasswordForm.captchaCode,
      username: forgetPasswordForm.username,
      captchaToken: forgetPasswordForm.token,
    }).then(() => {
      ElNotification.success("发送成功")
    }).catch((message) => {
      refreshCaptchaCode();
      ElNotification.warning(message);
    })
  }
}

// 刷新验证码
const refreshCaptchaCode = async () => {
  const {image, token} = await getCaptcha()
  imgData.value = image;
  forgetPasswordForm.token = token;
}

// created
refreshCaptchaCode();

</script>

<style scoped>
</style>
