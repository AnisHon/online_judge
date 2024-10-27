<template>
  <div class="layout">
    <el-container style="height: 100%;">
      <el-header class="header" >

        <MenuBar/>

      </el-header>
      <el-main ref="elMainRef" id="main-box" style="box-sizing: border-box; height: var(--content-height);">
        <div>
          <router-view v-slot="{Component}">
              <component :is="Component" />
          </router-view>
        </div>

      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">

import MenuBar from "@/components/menu/Menu.vue";
import {inject, onMounted, provide, readonly, ref} from "vue";
import {type ElMain} from "element-plus"
const elMainRef = ref<InstanceType<typeof ElMain>>();
provide('elMain', {elMainRef: elMainRef});

</script>

<style scoped>
.layout {
  height: 100%;
}


.header {
  padding: 0;
  height: var(--menu-height);
  display: flex;
}

</style>

<style>
</style>