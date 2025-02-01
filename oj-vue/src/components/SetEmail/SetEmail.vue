<template>
  <div class="app-container" style="display: none">
    <el-dialog v-model="open" title="请设置邮箱" append-to-body :close-on-click-modal="false" modal>
      <div class="dialog-content">

        <el-form label-position="top" style="min-width: 300px">
          <el-form-item label="邮箱">
            <el-input  v-model="form.email"/>
          </el-form-item>
          <el-form-item label="邮箱验证码">
            <el-input v-model="form.emailCode"/>
          </el-form-item>
          <el-form-item label="验证码">
            <el-input v-model="form.captcha"/>
          </el-form-item>
          <el-form-item>
            <el-button>设置邮箱</el-button>
          </el-form-item>
        </el-form>
        <p>您的账号没有绑定任何邮箱，者可能会导致账号无法被找回，请您设置您的邮箱</p>
      </div>

    </el-dialog>
  </div>
</template>

<script setup lang="ts">

import {computed, reactive, ref, watch} from "vue";
import {useUserStore} from "@/stores/useUserStore.ts";

const userStore = useUserStore();

const form = reactive({
  email: "",
  captcha: "",
  emailCode: ""
})

const close = ref(false);

const open = computed({

  get() {
    return false;
    // if (!userStore.user) {
    //   return false;
    // }
    // return !userStore.user.email;
  },
  set(value) {
  }
})

watch(open, () => {
  console.log(open.value);
})

</script>

<style scoped>
.dialog-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
</style>