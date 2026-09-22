<template>
  <el-dropdown class="account-menu" size="large" popper-class="account-menu-popper"
               @command="handleCommand" @visible-change="menuOpen = $event">
    <button
      class="account-trigger"
      type="button"
      aria-haspopup="menu"
      :aria-expanded="menuOpen"
      aria-label="打开账号菜单"
    >
      <avatar class="account-avatar" :size="32" />
      <el-icon class="account-trigger__chevron"><ArrowDown /></el-icon>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item v-if="!userStore.user" command="login">登录</el-dropdown-item>
        <el-dropdown-item v-else command="profile">个人中心</el-dropdown-item>
        <el-dropdown-item v-if="userStore.user && hasAccessToBackend" command="backend">进入后台</el-dropdown-item>
        <el-dropdown-item v-if="returnToUserPage" command="frontend">返回前台</el-dropdown-item>
        <el-dropdown-item v-if="userStore.user" command="logout" divided>退出登录</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">

import {logout} from "@/api/auth/authentication.ts";
import {computed, ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import {useUserStore} from "@/stores/useUserStore.ts";
import Avatar from "@/components/Avatar/Avatar.vue";
import {ArrowDown} from "@element-plus/icons-vue";
import {hasBackendAccess} from "@/utils/authUtil.ts";

const emit = defineEmits<{
  (event: 'command', key: string): void;
  (event: 'close'): void;
}>();

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();
const menuOpen = ref(false);

const goToProfile = async () => {
  const id = userStore.user?.userId;
  if (id !== undefined && id !== null) {
    await router.push({name: 'profile', params: {id}});
  }
};

const handleCommand = async (key: string) => {
  emit('command', key);
  try {
    if (key === 'logout') {
      await logout();
    } else if (key === 'login') {
      await router.push({name: 'login'});
    } else if (key === 'profile') {
      await goToProfile();
    } else if (key === 'backend') {
      await router.push({name: 'backend'});
    } else if (key === 'frontend') {
      await router.push({name: 'home'});
    }
  } catch {
    // 导航或登出失败不能留下未处理 Promise，也不能阻塞移动端抽屉关闭。
  } finally {
    menuOpen.value = false;
    emit('close');
  }
}

const hasAccessToBackend = computed(() => {
  return hasBackendAccess(userStore.getAuths()) && !route.fullPath.startsWith("/backend");
});

const returnToUserPage = computed(() => {
  return route.fullPath.startsWith("/backend");
})

</script>



<style scoped>
.account-menu { display: flex; height: 100%; align-items: center; }
.account-trigger { box-sizing: border-box; display: flex; height: 38px; align-items: center; justify-content: center; gap: 3px; padding: 2px 3px 2px 2px; border: 1px solid transparent; border-radius: 999px; color: var(--el-text-color-secondary); background: transparent; cursor: pointer; font: inherit; line-height: 0; transition: border-color .18s ease, background-color .18s ease, color .18s ease, transform .18s ease; }
.account-trigger:hover, .account-trigger:focus-visible { border-color: var(--el-border-color); background: var(--el-fill-color-light); color: var(--el-color-primary); outline: none; }
.account-trigger:active { transform: scale(.98); }
.account-avatar { display: flex; align-items: center; justify-content: center; cursor: pointer; line-height: 0; transition: filter .18s ease; }
.account-avatar :deep(.avatar__image), .account-avatar :deep(.el-avatar) { display: flex; align-items: center; justify-content: center; }
.account-trigger:hover .account-avatar { filter: saturate(1.08); }
.account-trigger__chevron { margin-right: 3px; font-size: 12px; }
@media (prefers-reduced-motion: reduce) {
  .account-trigger, .account-avatar { transition: none; }
}
</style>
