<script setup lang="ts">
import {useDark} from "@vueuse/core";

const isDark = useDark()
</script>

<template>
  <el-switch v-model="isDark" size="large" active-action-icon="Moon" inactive-action-icon="Sunny" style="--el-switch-on-color: #2C2C2C; --el-switch-off-color: #F2F2F2;"/>
</template>

<style scoped>

</style>