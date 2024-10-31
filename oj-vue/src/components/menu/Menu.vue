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
    <el-menu-item index="homework" route="homework">
      <template #title><span>作业</span></template>
    </el-menu-item>
    <el-menu-item index="check-in" route="check-in">
      <template #title><span>每日签到</span></template>
    </el-menu-item>

    <sub-form-item v-for="item of routers" :router="item"/>
<!--      <template #title><el-icon><Setting/></el-icon>您好，<strong>{{ nikeName }}</strong></template>-->
    <el-menu-item class="user-options">
      <template #title>
        <el-dropdown style="height: 100%;" @command="handleCommand">
        <span class="el-dropdown-link" style="height: 100%; display: flex; justify-content: center; align-items: center;">
          <div >
             {{ nikeName }}
            <el-icon class="el-icon--right">
              <UserFilled />
            </el-icon>
          </div>

        </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="setting">账号设置</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

      </template>
    </el-menu-item>
<!--      <el-menu-item index="$logout">-->
<!--        <template #title><el-icon><CloseBold/></el-icon>退出登录</template>-->
<!--      </el-menu-item>-->

  </el-menu>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import {useMenuStore} from "@/stores/useMenuStore";
import {useRoute, useRouter} from "vue-router";
import type {RouterType} from "@/router/dynamic";
import SubFormItem from "@/components/menu/SubFormItem.vue";
import {UserFilled} from "@element-plus/icons-vue";
import {logout} from "@/api/auth/authentication";
import {useUserStore} from "@/stores/useUserStore";


const router = useRouter();
const route = useRoute();
const activeIndex = ref(route.name)
const user = useUserStore()
const nikeName = ref("");
const handleSelect = (key: string) => {
  router.push({name: key});
}

const menu = useMenuStore();

const routers = ref<RouterType[]>([]);

menu.getDynamicRouters()
    .then((dynamicRouters) => {
      dynamicRouters.forEach((dynamicRouter ) => {
        routers.value = dynamicRouters;
      })
    });

user.getUser().then((data) => {nikeName.value = data.nikeName})

const handleCommand = (key: string) => {
  if (key === 'logout') {
    logout();
  }
}


</script>

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
