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
    <el-menu-item index="home">
      <template #title><span>首页</span></template>
    </el-menu-item>
    <el-menu-item index="problems">
      <template #title><span>题目</span></template>
    </el-menu-item>
    <el-menu-item index="list">
      <template #title><span>列表</span></template>
    </el-menu-item>
    <el-menu-item index="contest">
      <template #title><span>比赛</span></template>
    </el-menu-item>
    <el-menu-item index="homework">
      <template #title><span>作业</span></template>
    </el-menu-item>
    <sub-form-item v-for="item of routers" :router="item"/>
    <el-sub-menu index="">
      <template #title><el-icon><Setting /></el-icon><span>用户</span></template>
      <el-menu-item index="logout">
        <template #title><el-icon><CloseBold /></el-icon><span>登出</span></template>
      </el-menu-item>
    </el-sub-menu>

  </el-menu>
</template>

<script lang="ts" setup>
import { ref } from 'vue'
import {useMenuStore} from "@/stores/useMenuStore";
import {useRouter} from "vue-router";
import type {RouterType} from "@/router/dynamic";
import SubFormItem from "@/components/menu/SubFormItem.vue";

const router = useRouter();
const activeIndex = ref('index')
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




</script>
