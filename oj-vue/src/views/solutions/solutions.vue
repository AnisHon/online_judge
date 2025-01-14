<template>
  <div class="common-max-width-page app-container">
    <div class="header">

    </div>
    <div class="content">
      <solutions-component :param="param"/>
    </div>
  </div>
</template>

<script setup lang="ts">
import SolutionsComponent from "@/components/SolutionsComponent/SolutionsComponent.vue";
import {reactive} from "vue";
import type {QuerySolution} from "@/api/solution";


const param = reactive<QuerySolution>({
  asc: true,
  pageSize: 20,
  currentPage: 1,
  userId: undefined,
  problemId: undefined,
})

</script>

<style scoped>
.app-container {
  margin: auto;
}
</style>