<template>
  <el-dropdown size="large" style="height: 100%;" @command="handleCommand">
          <span class="el-dropdown-link" style="height: 100%; display: flex; justify-content: center; align-items: center;">
              <avatar/>
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
const router = useRouter();

const route = useRoute();

const handleCommand = (key: string) => {
  if (key === 'logout') {
    logout();
  } else if (key === 'setting') {
    router.push({name: 'setting'});
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

</style>