<template>
  <el-config-provider :z-index="elementLayerBase">
    <div class="app-container">
      <SetEmail />
      <router-view />
    </div>
  </el-config-provider>
</template>
<script setup lang="ts">
import {ElConfigProvider} from 'element-plus'
import SetEmail from "@/components/SetEmail/SetEmail.vue";

const elementLayerBase = 2000
</script>

<style scoped>
.app-container {
  height: 100%;
}
</style>
