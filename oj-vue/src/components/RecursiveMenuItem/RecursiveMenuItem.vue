<template>


  <el-sub-menu v-if="!!route.children && route.children.length > 0" :index="route.meta?.path">
    <template #title>
      <el-icon><Component :is="route.meta?.icon"/></el-icon>
      <span>{{ route.meta?.name }}</span>
    </template>
    <recursive-menu-item v-for="route_ of route.children" :route="route_"/>
  </el-sub-menu>

  <el-menu-item v-else :index="route.meta?.path">
    <el-icon><Component :is="route.meta?.icon"/></el-icon>
    <span>{{ route.meta?.name }}</span>
  </el-menu-item>


</template>

<script setup lang="ts">
import type {RouteRecordRaw} from "vue-router";


const {route} = defineProps<{route: RouteRecordRaw}>()
// console.log(route.meta?.path)
</script>

<style scoped>

</style>