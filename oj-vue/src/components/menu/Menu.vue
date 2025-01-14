<template>
  <el-menu
      :default-active="activeIndex"
      class="el-menu-demo"
      mode="horizontal"
      router
  >
    <el-menu-item index="/index">
      <img
          style="width: 100px"
          src="https://element-plus.org/images/element-plus-logo.svg"
          alt="Element logo"
      />
    </el-menu-item>
    <recursive-menu-item v-for="item of constMenu" :route="item"/>

    <div class="left-item">
      <el-space>

        <theme-trigger/>
        <account-menu/>

      </el-space>

    </div>
  </el-menu>
</template>

<script lang="ts" setup>
import ThemeTrigger from "@/components/ThemeTrigger/ThemeTrigger.vue";
import RecursiveMenuItem from "@/components/RecursiveMenuItem/RecursiveMenuItem.vue";
import {constMenu} from "@/router";
import AccountMenu from "@/components/AccountMenu/AccountMenu.vue";
import {computed} from "vue";
import {useRoute} from "vue-router";

const route = useRoute();

const activeIndex = computed(() => {
  return route.fullPath;
})

</script>

<style scoped>
.left-item {
  position: absolute;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  right: 0;
  padding: 0 20px;
  margin-right: 20px;
}

</style>

<style>
.el-menu--horizontal {
  position: relative;
  width: 100%;
}
.el-menu--horizontal > .user-options {
  position: absolute;
  right: 0;
}
.el-menu--horizontal > .el-menu-item {
  padding: 0 30px;
}

</style>
