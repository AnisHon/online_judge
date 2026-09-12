<template>
  <el-dropdown class="account-menu" size="large" popper-class="account-menu-popper" @command="handleCommand">
          <span class="account-trigger" title="打开账号菜单">
              <avatar class="account-avatar" @click.stop="goToSetting"/>
              <el-icon class="account-trigger__chevron"><ArrowDown /></el-icon>
            </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="setting">账号设置</el-dropdown-item>
        <el-dropdown-item v-if="hasAccessToBackend" command="backend">进入后台</el-dropdown-item>
        <el-dropdown-item v-if="returnToUserPage" command="frontend">返回前台</el-dropdown-item>
        <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">

import {logout} from "@/api/auth/authentication.ts";
import {computed} from "vue";
import {useRoute, useRouter} from "vue-router";
import {useMenuStore} from "@/stores/useMenuStore.ts";
import Avatar from "@/components/Avatar/Avatar.vue";
import {ArrowDown} from "@element-plus/icons-vue";
const router = useRouter();

const route = useRoute();

const goToSetting = () => router.push({name: 'setting'});

const handleCommand = (key: string) => {
  if (key === 'logout') {
    logout();
  } else if (key === 'setting') {
    goToSetting();
  } else if (key === 'backend') {
    router.push({name: 'backend'});
  } else if (key === 'frontend') {
    router.push({name: 'home'});
  }
}

const menuStore = useMenuStore();

const hasAccessToBackend = computed(() => {
  return !!menuStore.getAuths() && menuStore.getAuths().length > 0 && !route.fullPath.startsWith("/backend");
});

const returnToUserPage = computed(() => {
  return route.fullPath.startsWith("/backend");
})

</script>



<style scoped>
.account-menu { height: 100%; }
.account-trigger { display: inline-flex; height: 38px; align-items: center; gap: 3px; padding: 2px 3px 2px 2px; border: 1px solid transparent; border-radius: 999px; color: var(--el-text-color-secondary); cursor: pointer; transition: border-color .18s ease, background-color .18s ease, color .18s ease; }
.account-trigger:hover, .account-trigger:focus-visible { border-color: var(--el-border-color); background: var(--el-fill-color-light); color: var(--el-color-primary); outline: none; }
.account-avatar { cursor: pointer; transition: transform .18s ease, filter .18s ease; }
.account-trigger:hover .account-avatar { transform: scale(1.05); filter: saturate(1.08); }
.account-trigger__chevron { margin-right: 3px; font-size: 12px; }
</style>

<style>
.account-menu-popper { z-index: 3000 !important; }
</style>
