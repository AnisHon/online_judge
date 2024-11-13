<template>

  <transition name="el-zoom-in-top">
    <div class="common-max-width-page" style="margin: auto" v-if="!!userStore.user">
      <el-card>
        <header class="setting-header">
          <el-page-header @back="goBack" style="margin: 10px 0">
            <template #content>
              <span class="text-large font-600 mr-3"> 个人信息 </span>
            </template>
          </el-page-header>
          <el-descriptions border>
            <el-descriptions-item
                :rowspan="2"
                :width="140"
                label="头像"
                align="center"
            >
              <el-image
                  style="width: 100px; height: 100px"
                  src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png"
              />
            </el-descriptions-item>
            <el-descriptions-item label="用户名">{{ user.userName }}</el-descriptions-item>
            <el-descriptions-item label="昵称">{{ user.nikeName }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ user.email }}</el-descriptions-item>
            <el-descriptions-item label="身份">
              <el-space wrap>
                <el-tag size="small">School</el-tag>
              </el-space>
            </el-descriptions-item>
            <el-descriptions-item label="个性签名">
              <el-tag type="danger">未开放</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </header>
        <el-tabs v-model="defaultActive">
          <el-tab-pane label="角色信息" name="first">
            <el-card class="tab-card absoluteCenter">
              <el-form label-width="100" style="min-width: 500px">
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

                <el-form-item>
                  <el-button type="success">更改信息</el-button>
                </el-form-item>


              </el-form>
            </el-card>
          </el-tab-pane>
          <el-tab-pane label="修改密码" name="second" >
            <el-card class="tab-card absoluteCenter">
              <el-form
                  label-width="100px"
                  ref="formRef"
                  style="min-width: 500px"
                  :model="resetForm"
                  status-icon
                  :rules="rules"
                  :aria-autocomplete="false"
              >
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
                      placeholder="请输入邮箱"
                      prefix-icon="Lock"
                  />
                </el-form-item>

                <el-form-item label="确认密码" prop="repeatPassword">
                  <el-input
                      v-model="resetForm.repeatPassword"
                      type="password"
                      autocomplete="off"
                      placeholder="请输入密码"
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
                        style="width: 100px; position: absolute;
              right: 0"
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
            </el-card>
          </el-tab-pane>
          <el-tab-pane label="修改邮箱" name="third" disabled>
            <el-card class="tab-card absoluteCenter">
              <el-form
                  label-width="100px"
                  ref="formRef"
                  style="min-width: 500px"
                  :model="resetForm"
                  status-icon
                  :rules="rules"
                  :aria-autocomplete="false"
              >
                <el-form-item label="用户名" prop="username">
                  <el-input
                      v-model="userForm.userName"
                      type="text"
                      disabled
                      autocomplete="off"
                      placeholder="请输入用户名或邮箱"
                      prefix-icon="UserFilled"
                      disable
                  />
                </el-form-item>

                <el-form-item label="新邮箱" prop="email">
                  <el-input
                      v-model="resetForm.email"
                      type="password"
                      autocomplete="off"
                      placeholder="请输入密码"
                      prefix-icon="IconEmail"
                  />
                </el-form-item>

                <el-form-item label="确认密码" prop="repeatPassword">
                  <el-input
                      v-model="resetForm.repeatPassword"
                      type="password"
                      autocomplete="off"
                      placeholder="请输入密码"
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
                        style="width: 100px; position: absolute;
              right: 0"
                        @click="refreshCaptchaCode"
                    />
                  </el-col>


                </el-row>

                <el-row justify="space-between" style="width: 100%;" >
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
            </el-card>
          </el-tab-pane>
          <!--            <el-tab-pane label="" name="fourth">-->
          <!--              <el-card class="tab-card">-->
          <!--              4-->
          <!--              </el-card>-->
          <!--            </el-tab-pane>-->
        </el-tabs>

      </el-card>
    </div>

  </transition>

</template>
<script setup lang="ts">
import {type LoginUser, useUserStore} from "@/stores/useUserStore";
import {computed, onMounted, reactive, ref} from "vue";
import {useRouter} from "vue-router";
import type {UserForm} from "@/api/user";
import __ from "lodash";
import IconCaptcha from "@/assets/icons/IconCaptcha.vue";
import type {FormInstance, FormRules} from "element-plus";
import {resetPassword} from "@/api/auth/authentication";
import {sendForgetEmailCode} from "@/api/auth/emailCode";
import getCaptcha from "@/api/auth/captchaCode";
import IconEmail from "@/assets/icons/IconEmail.vue";

const userStore = useUserStore();
const router = useRouter();

const defaultActive = ref("first");

const userForm = reactive<UserForm>({
  userId: undefined,
  userName: undefined,
  nikeName: undefined
})

const user = computed((): LoginUser => {
  __.assign(userForm, userStore.user)
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
  // captchaCode: [{ validator: validateNotEmpty, trigger: 'blur' }],
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

onMounted(() => {
  refreshCaptchaCode()
})

// created
userStore.loadUser();

</script>


<style scoped>
.tab-card {
  height: 400px;
}
</style>

<style>

</style>
