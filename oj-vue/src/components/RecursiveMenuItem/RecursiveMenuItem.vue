<template>


  <el-sub-menu v-if="!!route.children && route.children.length > 0" :index="route.meta?.path"
               :popper-class="popperClass" :teleported="teleported">
    <template #title>
      <el-icon><Component :is="route.meta?.icon"/></el-icon>
      <span>{{ route.meta?.name }}</span>
    </template>
    <recursive-menu-item v-for="route_ of route.children" :key="route_.path" :route="route_"
                         :popper-class="popperClass" :teleported="teleported"/>
  </el-sub-menu>

  <el-menu-item v-else :index="route.meta?.path">
    <el-icon v-if="route.meta?.icon"><Component :is="route.meta?.icon"/></el-icon>
    <span>{{ route.meta?.name }}</span>
  </el-menu-item>


</template>

<script setup lang="ts">
import type {RouteRecordRaw} from "vue-router";


const {route, popperClass, teleported} = defineProps<{
  route: RouteRecordRaw;
  /** 只由后台菜单传入，前台横向菜单不应继承后台弹层样式。 */
  popperClass?: string;
  teleported?: boolean;
}>()
// console.log(route.meta?.path)
</script>

<style scoped>

</style>
