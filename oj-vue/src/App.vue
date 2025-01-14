<template>
  <div class="app-container">
    <SetEmail />
    <router-view />
  </div>
</template>
<script setup lang="ts">
import SetEmail from "@/components/SetEmail/SetEmail.vue";
</script>

<style scoped>
.app-container {
  height: 100%;
}
</style>