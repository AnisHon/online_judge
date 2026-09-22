<template>
  <el-scrollbar height="100%" class="backend-scrollbar">
      <div class="brand-header" :class="{collapsed: collapse}">
        <el-icon class="brand-icon" size="30"><IconManagement/></el-icon>
        <span v-show="!collapse" class="brand-title">延拓Code <small>管理后台</small></span>
      </div>

    <el-menu
        class="menu"
        mode="vertical"
        :collapse="collapse"
        background-color="var(--vertical-menu-color)"
        text-color="var(--vertical-menu-text-color)"
        :default-active="currentIndex"
        :router="true"
    >
        <recursive-menu-item v-for="route of routes" :key="route.path" :route="route"/>
    </el-menu>
  </el-scrollbar>

</template>

<script setup lang="ts">

import {computed} from "vue";
import RecursiveMenuItem from "@/components/RecursiveMenuItem/RecursiveMenuItem.vue";
import {useMenuStore} from "@/stores/useMenuStore.ts";
import {useRoute} from "vue-router";
import IconManagement from "@/assets/icons/IconManagement.vue";

const menuStore = useMenuStore();

const route = useRoute();

const routes = computed(() => menuStore.getMenu());

const currentIndex = computed(() => {
  return route.fullPath;
});

const {collapse = false} = defineProps<{collapse?: boolean}>()

</script>

<style scoped>

.brand-header { display: flex; align-items: center; height: 72px; gap: 12px; padding: 0 20px; color: #f8fafc; white-space: nowrap; }
.brand-header.collapsed { justify-content: center; padding: 0; }
.brand-icon { flex: 0 0 auto; color: #67e8f9; }
.brand-title { display: flex; flex-direction: column; color: #f8fafc; font-size: 16px; font-weight: 750; letter-spacing: -.02em; line-height: 1.15; }
.brand-title small { margin-top: 4px; color: #94a3b8; font-size: 10px; font-weight: 500; letter-spacing: .12em; }

</style>

<style scoped lang="scss">

.backend-scrollbar :deep(.menu) {
  border: none;
  width: 100%;
}

.backend-scrollbar :deep(.menu > .el-menu-item:hover),
.backend-scrollbar :deep(.menu .el-sub-menu__title:hover) {
  background-color: var(--vertical-menu-hover-color);
}


.backend-scrollbar :deep(.menu .el-sub-menu .el-menu-item) {
  background-color: var(--vertical-menu-submenu-color);
}

.backend-scrollbar :deep(.menu .el-sub-menu .el-menu-item:hover) {
  background-color: var(--vertical-menu-submenu-hover-color);
}

.backend-scrollbar :deep(.el-scrollbar__view) { min-height: 100%; }
.backend-scrollbar :deep(.el-menu-item), .backend-scrollbar :deep(.el-sub-menu__title) { height: 48px; margin: 3px 10px; border-radius: 10px; line-height: 48px; }
.backend-scrollbar :deep(.el-menu-item.is-active) { color: #fff; background: linear-gradient(90deg, #2563eb, #0891b2); box-shadow: 0 6px 14px rgb(8 145 178 / 18%); }
.backend-scrollbar :deep(.el-menu--collapse .el-menu-item), .backend-scrollbar :deep(.el-menu--collapse .el-sub-menu__title) { width: 52px; margin-right: 10px; margin-left: 10px; padding: 0; text-align: center; }

</style>
