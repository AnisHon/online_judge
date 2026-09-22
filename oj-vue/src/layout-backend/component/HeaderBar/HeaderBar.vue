<template>
  <header class="app-container">
    <ul class="header-bar">
      <li class="expand-button-wrapper">
        <el-button :icon="icon" class="expand-button" @click="handleIconClick"/>
      </li>

      <li class="breadcrumb-wrapper">
        <CustomBreadCrumb/>
      </li>

      <li class="header-action theme-action">
        <theme-trigger/>
      </li>

      <li class="header-action account-action">
        <account-menu/>
      </li>
    </ul>
  </header>
</template>

<script setup lang="ts">

import {computed} from "vue";
import CustomBreadCrumb from "@/layout-backend/component/CustomBreadCrumb/CustomBreadCrumb.vue";
import ThemeTrigger from "@/components/ThemeTrigger/ThemeTrigger.vue";
import AccountMenu from "@/components/AccountMenu/AccountMenu.vue";

const collapse = defineModel<boolean>("collapse");

// 展开或者收起图片的icon
const icon = computed(() => {
  return collapse.value ? "Fold" : "Expand"
})

const handleIconClick = () => {
  collapse.value = !collapse.value
}

</script>


<style scoped>

.app-container {
  position: relative;
  z-index: 0;
  height: var(--header-bar-height);
  width: 100%;
  flex: 0 0 var(--header-bar-height);
  box-sizing: border-box;
  overflow: visible;
}

ul,
li {
  list-style: none;  /* 移除列表项前的标记（如圆点、数字） */
  padding: 0;        /* 移除 padding */
  margin: 0;         /* 移除 margin */
}

.header-bar {
  display: flex;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  padding: 0 18px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.header-bar li {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 6px;
  min-width: 20px;
}

.expand-button {
  font-size: 30px;
  border: none;
  height: 100%;
  color: var(--el-text-color-secondary);
  background: transparent;
}

.header-bar .expand-button-wrapper {
  margin: 0;
}
.breadcrumb-wrapper { min-width: 0 !important; overflow: hidden; }
.breadcrumb-wrapper :deep(.backend-breadcrumb) { max-width: 100%; }
.account-action :deep(.el-dropdown) { display: flex; align-items: center; height: 100%; }
.account-action :deep(.account-trigger) { display: inline-flex; min-width: 38px; height: 38px !important; overflow: visible; border-radius: 999px; }
.account-action :deep(.el-avatar) { flex: 0 0 32px; width: 32px; height: 32px; }
.account-action :deep(.el-avatar img) { width: 100%; height: 100%; object-fit: cover; }
.theme-action { margin-left: auto !important; }
.account-action { margin-left: 8px !important; margin-right: 0 !important; }
@media (max-width: 700px) { .header-bar { padding: 0 8px; }.theme-action { display: none !important; }.account-action { margin-left: auto !important; } }
</style>
