

<template>
  <el-card shadow="hover">
    <template #header>
      <slot name="header" >
        <div style="display: flex; justify-content: space-between">
          <h3 style="margin: 0">
            <el-icon size="large" style="vertical-align: middle"><component v-if="icon" :is="icon"/></el-icon>
            <span style="vertical-align: middle">{{title}}</span>
          </h3>
          <el-link type="info" v-if="more" @click="emit('showMore')" :underline="false">
          更多<el-icon><ArrowRight/></el-icon>
        </el-link>
        </div>

      </slot>
    </template>

    <template #default>
      <slot></slot>
    </template>
  </el-card>
</template>

<script setup lang="ts">

import {ArrowRight} from "@element-plus/icons-vue";

const {icon, title, more = false} = defineProps<{
  icon?: string,
  title: string,
  more?: boolean
}>()

const emit = defineEmits<{
  (e: 'showMore'): void
}>()

</script>


<style scoped>

</style>