<template>
  <div class="app-container">
    <RouterView />
  </div>
  <FloatingBall><h1>123123123 </h1> </FloatingBall>
</template>
<script setup lang="ts">
//@ts-ignore
import FloatingBall from "@/components/FloatingBall/index.vue";
</script>

<style scoped>
.app-container {
  height: 100%;
}
</style>