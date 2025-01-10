<template>
  <el-menu
      :default-active="activeIndex"
      class="el-menu-demo"
      mode="horizontal"
      @select="handleSelect"
  >
    <el-menu-item index="index">
      <img
          style="width: 100px"
          src="https://element-plus.org/images/element-plus-logo.svg"
          alt="Element logo"
      />
    </el-menu-item>

    <el-menu-item index="home" route="index">
      <template #title><span>首页</span></template>
    </el-menu-item>
    <el-menu-item index="problems" route="problems">
      <template #title><span>题目</span></template>
    </el-menu-item>
    <el-menu-item index="list" route="list">
      <template #title><span>列表</span></template>
    </el-menu-item>
    <el-menu-item index="contest" route="contest">
      <template #title><span>比赛</span></template>
    </el-menu-item>
    <el-menu-item index="homework" route="homework" v-show="false">
      <template #title><span>作业</span></template>
    </el-menu-item>
    <el-menu-item index="check-in" route="check-in">
      <template #title><span>每日签到</span></template>
    </el-menu-item>

<!--    <sub-menu-item v-for="item of routers" :router="item"/>-->
    <div class="left-item">
      <el-space>
<!--        <el-switch v-model="isDark" size="large" active-action-icon="Moon" inactive-action-icon="Sunny" style="&#45;&#45;el-switch-on-color: #2C2C2C; &#45;&#45;el-switch-off-color: #F2F2F2;"/>-->
        <theme-trigger/>
        <el-dropdown size="large" style="height: 100%;" @command="handleCommand">
          <span class="el-dropdown-link" style="height: 100%; display: flex; justify-content: center; align-items: center;">
<!--             <span>-->
<!--              {{ nikeName }}-->
<!--             </span>-->
              <el-avatar :icon="UserFilled"/>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="setting">账号设置</el-dropdown-item>
                <el-dropdown-item v-if="hasAccessToBackend" command="backend">进入后台</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
        </el-dropdown>

      </el-space>

    </div>
  </el-menu>
</template>

<script lang="ts" setup>
import {computed, ref} from 'vue'
import {useMenuStore} from "@/stores/useMenuStore";
import {useRoute, useRouter} from "vue-router";
import {UserFilled} from "@element-plus/icons-vue";
import {logout} from "@/api/auth/authentication";
import {useUserStore} from "@/stores/useUserStore";
import { useDark } from "@vueuse/core";
import ThemeTrigger from "@/components/ThemeTrigger/ThemeTrigger.vue";

const isDark = useDark()

const router = useRouter();

const route = useRoute();

const menuStore = useMenuStore();


const user = useUserStore()

// menu
const activeIndex = computed(() => {
  return route.name;
});

// 导航栏名称
const nikeName = ref("");

const hasAccessToBackend = computed(() => {
  return !!menuStore.getAuths() && menuStore.getAuths().length > 0;
});

//
// menu.getDynamicRouters()
//     .then((dynamicRouters) => {
//       dynamicRouters.forEach((dynamicRouter ) => {
//         routers.value = dynamicRouters;
//       })
//     });


const handleSelect = (key: string) => {
  router.push({name: key});
}

const handleCommand = (key: string) => {
  if (key === 'logout') {
    logout();
  } else if (key === 'setting') {
    router.push({name: 'setting'});
  } else if (key === 'backend') {
    router.push({name: 'backend'});
  }
}


user.getUser()
    .then((data) => {
      nikeName.value = data.nikeName
    })

</script>

<style scoped>
.left-item {
  position: absolute;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  right: 0;
  padding: 0 20px;
}

</style>

<style>
.el-menu--horizontal {
  position: relative;
  width: 100%;
}
.el-menu--horizontal > .user-options {
  position: absolute;
  right: 0;
}
.el-menu--horizontal > .el-menu-item {
  padding: 0 30px;
}

</style>
