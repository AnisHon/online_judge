<template>
  <el-breadcrumb separator="/">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item :to="{name: item.name}" v-for="item of breadcrumbs" :key="item.name">
        <span  class="unselectable">
          {{ item.title }}
        </span>
      </el-breadcrumb-item>
    </transition-group>

  </el-breadcrumb>
</template>

<script setup lang="ts">

import {computed} from "vue";
import {useRoute, useRouter} from "vue-router";

const route = useRoute();
const router = useRouter();

const allRoutes = computed(() => {
  const mapping = new Map();
  router.getRoutes().forEach((route) => mapping.set(route.name, route));
  return mapping;
})

const breadcrumbs = computed(() => {
  const names: {name: string, title: string}[] = []
  const paths = route.fullPath.split("/");

  paths.forEach(path => {
    if (path === 'index') {
      return;
    }
    if (allRoutes.value.has(path)) {
      const name = allRoutes.value.get(path).meta.name
      names.push({name: path, title: name});
    }
  })
  return names;
});


</script>

<style scoped>

</style>