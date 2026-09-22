<template>
  <div class="menu-shell">
    <div class="desktop-nav">
      <el-menu :default-active="activeIndex" class="main-nav" mode="horizontal" :ellipsis="false" router>
        <el-menu-item class="brand-item" index="/index">
          <span class="brand-logo-wrap"><el-image class="brand-logo" src="/code_logo.png" alt="延拓Code" /></span>
        </el-menu-item>
        <recursive-menu-item v-for="item of constMenu" :key="item.path" :route="item" />
      </el-menu>
      <div class="nav-actions"><theme-trigger/><account-menu/></div>
    </div>

    <div class="mobile-bar">
      <span class="brand-logo-wrap"><el-image class="brand-logo" src="/code_logo.png" alt="延拓Code" /></span>
      <div class="mobile-actions"><theme-trigger/><el-button class="mobile-menu-button" text aria-label="打开菜单" @click="mobileMenuOpen = true"><el-icon><Menu /></el-icon></el-button></div>
    </div>
    <el-drawer v-model="mobileMenuOpen" class="mobile-menu-drawer" direction="rtl" size="min(320px, 86vw)" title="导航">
      <el-menu :default-active="activeIndex" class="mobile-nav" router @select="mobileMenuOpen = false">
        <recursive-menu-item v-for="item of constMenu" :key="item.path" :route="item" />
      </el-menu>
      <div class="mobile-account"><account-menu @command="closeMobileMenu" @close="closeMobileMenu"/></div>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import ThemeTrigger from "@/components/ThemeTrigger/ThemeTrigger.vue";
import RecursiveMenuItem from "@/components/RecursiveMenuItem/RecursiveMenuItem.vue";
import {constMenu} from "@/router";
import AccountMenu from "@/components/AccountMenu/AccountMenu.vue";
import {computed, ref} from "vue";
import {useRoute} from "vue-router";
import {Menu} from "@element-plus/icons-vue";

const route = useRoute();
const mobileMenuOpen = ref(false);
const activeIndex = computed(() => route.fullPath);
const closeMobileMenu = () => { mobileMenuOpen.value = false; };
</script>

<style scoped>
.menu-shell { width: 100%; height: 100%; border-bottom: 1px solid var(--el-border-color-lighter); background: color-mix(in srgb, var(--el-bg-color) 92%, transparent); }
.desktop-nav { position: relative; height: 100%; }
.main-nav { width: 100%; height: 100%; min-width: 0; overflow: visible; padding: 0 max(18px, calc((100vw - var(--page-max-width)) / 2)); padding-right: max(230px, calc((100vw - var(--page-max-width)) / 2 + 230px)); border-bottom: 0; background: transparent; }
.main-nav :deep(.el-menu-item), .main-nav :deep(.el-sub-menu__title) { height: 100%; padding: 0 16px; border-bottom: 2px solid transparent; color: var(--el-text-color-regular); font-size: 14px; }
.main-nav :deep(.el-menu-item:hover), .main-nav :deep(.el-sub-menu__title:hover) { color: var(--el-color-primary); background: var(--el-fill-color-lighter); }
.main-nav :deep(.el-menu-item.is-active) { border-bottom-color: var(--el-color-primary); color: var(--el-color-primary); background: transparent; }
.brand-item { display: flex; width: 180px; justify-content: flex-start; margin-right: 18px; padding: 0 !important; border-bottom: 0 !important; }
.brand-logo-wrap { display: block; width: 180px; height: 46px; overflow: hidden; }
.brand-logo { display: block; width: 190px; max-width: none; height: auto; transform: translate(-5px, -24px); }
.nav-actions { position: absolute; top: 0; right: max(18px, calc((100vw - var(--page-max-width)) / 2)); display: flex; align-items: center; gap: 14px; height: 100%; padding-left: 24px; background: linear-gradient(90deg, transparent, var(--el-bg-color) 25%); }
.mobile-bar { display: none; }
.mobile-menu-drawer :deep(.el-drawer__header) { margin-bottom: 0; padding: 22px; border-bottom: 1px solid var(--el-border-color-lighter); color: var(--el-text-color-primary); }
.mobile-nav { border-right: 0; }
.mobile-nav :deep(.el-menu-item), .mobile-nav :deep(.el-sub-menu__title) { height: 48px; border-radius: 10px; }
.mobile-account { margin-top: 28px; padding: 20px 8px 0; border-top: 1px solid var(--el-border-color-lighter); }
@media (max-width: 900px) {
  .desktop-nav { display: none; }
  .mobile-bar { display: flex; align-items: center; justify-content: space-between; height: 100%; padding: 0 18px; }
  .mobile-bar .brand-logo-wrap { width: 150px; height: 42px; }
  .mobile-bar .brand-logo { width: 160px; transform: translate(-5px, -21px); }
  .mobile-actions { display: flex; align-items: center; gap: 10px; }
  .mobile-menu-button { color: var(--el-text-color-primary); font-size: 22px; }
}
@media (max-width: 420px) { .mobile-bar { padding: 0 12px; }.mobile-bar .brand-logo-wrap { width: 132px; }.mobile-bar .brand-logo { width: 140px; transform: translate(-4px, -19px); } }
</style>
