<template>

</template>

<script setup lang="ts">
import {getDetailProblem, type ProblemDetailView} from "@/api/problem";
import {useRoute} from "vue-router";
import {onMounted} from "vue";

const route = useRoute()

let problem: ProblemDetailView;

onMounted(() => {
  const problemId: number = parseInt(<string>route.params.id)

  getDetailProblem(problemId)
      .then((detailProblem: ProblemDetailView) => {
        problem = detailProblem;
      })

})


</script>


<style scoped>

</style>