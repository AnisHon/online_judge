

<template>
  <div class="layout">
    <el-container style="height: 100%">
      <el-header style="padding: 0; height: var(--menu-height);">
        <MenuBar/>
      </el-header>
      <el-main style="box-sizing: border-box; height: var(--content-height);">
        <div>
          <router-view>

          </router-view>
        </div>

      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">

import MenuBar from "@/components/menu/Menu.vue";
</script>

<style scoped>
.layout {
  height: 100%;
}
</style>

<style>
</style>