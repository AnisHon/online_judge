<template>
  <el-dialog v-model="open" title="绑定邮箱，方便找回账号" width="min(440px, calc(100vw - 32px))"
             append-to-body align-center :close-on-click-modal="false">
    <p class="email-reminder">你的账号还未绑定邮箱。绑定后可以通过邮箱找回密码，也能验证重要的账号操作。</p>
    <template #footer>
      <el-button @click="userStore.dismissEmailReminder()">稍后设置</el-button>
      <el-button type="primary" @click="goToSettings">前往绑定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">

import {computed} from 'vue';
import {useRouter} from 'vue-router';
import {useUserStore} from "@/stores/useUserStore.ts";

const userStore = useUserStore();

const router = useRouter();
const open = computed({
  get: () => userStore.emailReminderPending && !!userStore.user && !userStore.user.email,
  set: (value: boolean) => { if (!value) userStore.dismissEmailReminder(); },
});
const goToSettings = async () => {
  const id = userStore.user?.userId;
  if (!id) return;
  await router.push({name: 'profile', params: {id: String(id)}, query: {tab: 'security'}});
  userStore.dismissEmailReminder();
};

</script>

<style scoped>
.email-reminder { margin: 0; color: var(--el-text-color-secondary); line-height: 1.8; }
</style>
