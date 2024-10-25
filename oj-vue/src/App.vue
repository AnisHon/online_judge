<template>
  <div class="app-container">
    <RouterView />
  </div>
</template>
<script setup lang="ts">
import {useMenuStore} from "@/stores/useMenuStore";

// const menu = useMenuStore();
// menu.getDynamicRouters()
</script>

<style scoped>
.app-container {
  height: 100%;
}
</style>