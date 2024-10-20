

<template>
  <div class="layout">
    <el-container>
      <el-header style="padding: 0">
        <MenuBar/>
      </el-header>
      <el-main>
        <router-view v-slot="{ Component }">
          <transition mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">

import MenuBar from "@/components/menu/Menu.vue";
</script>

<style scoped>
.background {
  background-image: url("/auth/auth_background.png");
}

.v-enter-active,
.v-leave-active {
  transition: opacity 0.1s ease-in-out;
}
.v-enter,
.v-leave-to {
  opacity: 0;
}

</style>