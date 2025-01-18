<template>
  <div>
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

</style>