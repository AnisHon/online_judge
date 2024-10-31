

<template>
  <el-card shadow="hover">
    <template #header>
      <slot name="header">
        <h3 style="margin: 0">
          <el-icon size="large" style="vertical-align: middle"><component v-if="icon" :is="icon"/></el-icon>
          <span style="vertical-align: middle">{{title}}</span>
        </h3>
      </slot>
    </template>

    <template #default>
      <slot></slot>
    </template>
  </el-card>
</template>

<script setup lang="ts">

const {icon} = defineProps<{
  icon?: string,
  title: string
}>()

</script>


<style scoped>

</style>