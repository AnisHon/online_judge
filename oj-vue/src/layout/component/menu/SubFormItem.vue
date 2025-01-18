
<template>
  <el-sub-menu v-if="isShow" :index="router.path" >

    <template #title>
      <el-icon :size="20">
        <component :is="router.meta.icon" />
      </el-icon>
      <span>{{ router.meta.name }}</span>
    </template>
    <sub-form-item v-for="item of router.children" :router="item" />
  </el-sub-menu>

  <el-menu-item v-if="isItem" :index="router.path" >

    <template #title>
      <el-icon :size="20">
        <component :is="router.meta.icon" />
      </el-icon>
      <span>{{ router.meta.name }}</span>
    </template>
  </el-menu-item>

</template>

<script setup lang="ts">

import type {RouterType} from "@/router/dynamic.ts";
import {computed} from "vue";
import {MenuType} from "@/api/auth/menu.ts";
import __ from 'lodash';


const isItem = computed(() => {
  return router.meta.type === MenuType.MENU_ITEM;
})

const isShow = computed(() => {
  return !__.isEmpty(router.children)
})

const {router} = defineProps<{
  router: RouterType
}>()

</script>


<style scoped>

</style>