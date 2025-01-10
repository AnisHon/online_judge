<template>
  <div class="app-container">
    <ul class="header-bar">
      <li class="expand-button-wrapper">
        <el-button :icon="icon" class="expand-button" @click="handleIconClick"/>
      </li>

      <li>
        <CustomBreadCrumb/>
      </li>

      <li style="margin-left: auto;">
        <theme-trigger/>
      </li>

      <li style="margin-right: 40px">
        <account-menu/>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">

import {computed} from "vue";
import CustomBreadCrumb from "@/components/CustomBreadCrumb/CustomBreadCrumb.vue";
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
  height: var(--header-bar-height);
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
  box-sizing: content-box;
}

.header-bar li {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 10px;
  min-width: 20px;
}

.expand-button {
  font-size: 30px;
  border: none;
  height: 100%;
}

.header-bar .expand-button-wrapper {
  margin: 0;
}
</style>