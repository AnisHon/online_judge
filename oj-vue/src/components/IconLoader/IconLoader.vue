<template>
  <div>
    <el-icon>
      <component :is="icon"/>
    </el-icon>
  </div>

</template>

<script setup lang="ts">
import { defineProps } from 'vue';

const {icon} = defineProps<{
  icon: string,
}>()
</script>



<style scoped>
</style>