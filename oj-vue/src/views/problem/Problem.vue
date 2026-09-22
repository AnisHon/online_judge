<template>
  <div class="problem-page">
    <detail-problem :problem-id="problemId" :contest-id="undefined"/>
  </div>
</template>

<script setup lang="ts">

import {useRoute} from "vue-router";
import {computed} from "vue";
import DetailProblem from "@/components/DetailProblem/DetailProblem.vue";

const route = useRoute();
const problemId = computed(() => {
  return  <string>route.params.id
})

</script>


<style scoped>
.problem-page {
  width: 100%;
  height: var(--in-main-content-height);
  min-height: 0;
  overflow: hidden;
}

.problem-page :deep(.detail-problem-root),
.problem-page :deep(.oj-workbench) {
  height: 100%;
  min-height: 0;
  max-height: 100%;
}
</style>
