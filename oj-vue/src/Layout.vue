

<template>
  <div class="layout">
    <el-container style="height: 100%">
      <el-header style="padding: 0">
        <MenuBar/>
      </el-header>
      <el-main>
        <div>
          <router-view v-slot="{ Component }">
            <transition name="el-fade-in">
              <component :is="Component" />
            </transition>
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