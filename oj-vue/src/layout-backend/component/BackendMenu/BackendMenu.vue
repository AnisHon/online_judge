<template>
  <el-scrollbar height="100%" class="vertical-menu-animation" :view-class="collapseClass" :wrap-class="collapseClass">
    <transition name="el-fade-in" mode="out-in" :duration="{enter: 500, leave: 100}">
      <div class="header" :key="collapse ? 'collapsed' : 'expanded'">
        <h3 v-show="!collapse">
          <el-icon size="32"><IconManagement/></el-icon>
          <span>丁真无敌后台管理</span>
        </h3>
        <h3 v-show="collapse">
          <el-icon size="32"><IconManagement/></el-icon>
        </h3>
      </div>
    </transition>

    <el-menu
        class="menu"
        mode="vertical"
        :collapse="collapse"
        background-color="var(--vertical-menu-color)"
        text-color="var(--vertical-menu-text-color)"
        :default-active="currentIndex"
        :router="true"
    >
      <recursive-menu-item v-for="route of routes" :route="route"/>
    </el-menu>
  </el-scrollbar>

</template>

<script setup lang="ts">

import {computed,} from "vue";
import RecursiveMenuItem from "@/components/RecursiveMenuItem/RecursiveMenuItem.vue";
import {useMenuStore} from "@/stores/useMenuStore.ts";
import {useRoute} from "vue-router";
import IconManagement from "@/assets/icons/IconManagement.vue";

const menuStore = useMenuStore();

const route = useRoute();

const routes = menuStore.getMenu();

const currentIndex = computed(() => {
  return route.fullPath;
});

const {collapse = false} = defineProps<{collapse?: boolean}>()

const collapseClass = computed(() => {
  return (collapse ? "vertical-menu-collapse" : "vertical-menu-expand") + " vertical-menu-animation"
})


</script>

<style scoped>

.header {
  h3 {
    display: flex;
    align-items: center;
    justify-content: center;
    color: white;
  }
}

</style>

<style lang="scss">

.menu {
  border: none;
}

.el-menu>.el-menu-item:hover,
.el-menu .el-sub-menu__title:hover {
  background-color: var(--vertical-menu-hover-color);
}


.el-menu .el-sub-menu .el-menu-item {
  background-color: var(--vertical-menu-submenu-color);
}

.el-menu .el-sub-menu .el-menu-item:hover {
  background-color: var(--vertical-menu-submenu-hover-color);
}

</style>