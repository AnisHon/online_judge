<template>
  <el-scrollbar height="100%" class="backend-scrollbar">
      <div class="brand-header" :class="{collapsed: props.collapse}">
        <el-icon class="brand-icon" size="30"><IconManagement/></el-icon>
        <span class="brand-title" :class="{hidden: props.collapse}">延拓Code <small>管理后台</small></span>
      </div>

    <el-menu
        class="menu"
        mode="vertical"
        :collapse="props.collapse"
        :collapse-transition="false"
        background-color="var(--vertical-menu-color)"
        text-color="var(--vertical-menu-text-color)"
        :default-active="currentIndex"
        :router="true"
        popper-class="backend-menu-popper"
        :popper-offset="10"
    >
        <recursive-menu-item v-for="route of routes" :key="route.path" :route="route"
                             popper-class="backend-menu-popper" :teleported="true"/>
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

const props = withDefaults(defineProps<{collapse?: boolean}>(), {
  collapse: false
})

</script>

<style scoped>

.brand-header { display: flex; align-items: center; width: 100%; height: 72px; box-sizing: border-box; gap: 12px; padding: 0 20px; color: #f8fafc; white-space: nowrap; }
.brand-header.collapsed { justify-content: center; gap: 0; padding: 0; }
.brand-icon { flex: 0 0 auto; color: #67e8f9; }
.brand-title { display: flex; flex: 0 1 auto; max-width: 180px; flex-direction: column; overflow: hidden; color: #f8fafc; font-size: 16px; font-weight: 750; letter-spacing: -.02em; line-height: 1.15; opacity: 1; transition: max-width .22s ease, opacity .14s ease; }
.brand-title.hidden { max-width: 0; opacity: 0; pointer-events: none; }
.brand-title small { margin-top: 4px; color: #94a3b8; font-size: 10px; font-weight: 500; letter-spacing: .12em; }

</style>

<style scoped lang="scss">

.backend-scrollbar,
.backend-scrollbar :deep(.el-scrollbar__wrap) { overflow-x: hidden; }

.backend-scrollbar :deep(.menu) {
  width: 100%;
  min-height: calc(100% - 72px);
  border: none;
  background: var(--vertical-menu-color);
  overflow: hidden;
}

.backend-scrollbar :deep(.menu > .el-menu-item:hover),
.backend-scrollbar :deep(.menu .el-sub-menu__title:hover) {
  background-color: var(--vertical-menu-hover-color);
}


.backend-scrollbar :deep(.menu .el-sub-menu .el-menu),
.backend-scrollbar :deep(.menu .el-sub-menu .el-menu-item) {
  /* 子菜单沿用主菜单底色，避免展开时出现一整块突兀的深色区域。 */
  background-color: var(--vertical-menu-color);
}

/* 子项有上下 margin；flow-root 防止折叠动画结束时首尾 margin 穿透父级造成位移。 */
.backend-scrollbar :deep(.menu .el-sub-menu > .el-menu) {
  display: flow-root;
}

.backend-scrollbar :deep(.menu .el-sub-menu .el-menu-item:hover),
.backend-scrollbar :deep(.menu .el-sub-menu .el-menu-item.is-active) {
  background-color: var(--vertical-menu-submenu-hover-color);
}

.backend-scrollbar :deep(.el-scrollbar__view) { min-height: 100%; }
.backend-scrollbar :deep(.el-menu-item), .backend-scrollbar :deep(.el-sub-menu__title) {
  box-sizing: border-box;
  height: 48px;
  margin: 3px 10px;
  border-radius: 10px;
  line-height: 48px;
  transition: color .18s ease, background-color .18s ease;
}

/* 不卸载整棵菜单，文字和侧栏宽度各自连续过渡，避免折叠时白闪。 */
.backend-scrollbar :deep(.menu .el-menu-item > span),
.backend-scrollbar :deep(.menu .el-sub-menu__title > span) {
  display: inline-block;
  max-width: 180px;
  overflow: hidden;
  white-space: nowrap;
  opacity: 1;
  transition: max-width .22s ease, opacity .14s ease, margin .22s ease;
}

.backend-scrollbar :deep(.menu.el-menu--collapse > .el-menu-item > span),
.backend-scrollbar :deep(.menu.el-menu--collapse > .el-sub-menu > .el-sub-menu__title > span) {
  visibility: visible;
  width: auto;
  height: auto;
  max-width: 0;
  margin-left: 0;
  margin-right: 0;
  opacity: 0;
  pointer-events: none;
}
.backend-scrollbar :deep(.el-menu-item.is-active) { color: #fff; background: linear-gradient(90deg, #2563eb, #0891b2); box-shadow: 0 6px 14px rgb(8 145 178 / 18%); }

/* Element Plus 默认折叠宽度是 64px；侧栏是 72px。项目项不能再额外加左右 margin，
   否则 52 + 10 + 10 会超出容器，出现偏移、抖动和点击区域变宽。 */
.backend-scrollbar :deep(.menu.el-menu--collapse) { width: 100%; }
.backend-scrollbar :deep(.el-menu--collapse .el-menu-item),
.backend-scrollbar :deep(.el-menu--collapse .el-sub-menu__title) {
  width: 100%;
  margin: 3px 0;
  padding: 0;
  justify-content: center;
  text-align: center;
}
.backend-scrollbar :deep(.el-menu--collapse .el-menu-item > .el-icon),
.backend-scrollbar :deep(.el-menu--collapse .el-sub-menu__title > .el-icon) { margin: 0; }
.backend-scrollbar :deep(.el-menu--collapse .el-sub-menu__icon-arrow) { display: none; }

</style>
