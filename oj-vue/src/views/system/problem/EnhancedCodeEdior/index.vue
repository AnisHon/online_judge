<template>
  <div>
    <el-form :inline="true" model="codeForm" />
    <code-editor language="c++" :height="800"/>
  </div>
</template>

<script setup lang="ts">

import CodeEditor from "@/components/CodeEditor/CodeEditor.vue";
import {onMounted, reactive} from "vue";
import type {Answer, JudgeForm} from "@/api/problem/judege";

const codeForm = reactive<JudgeForm>({
  contestId: undefined,
  languageId: 1,
  problemId: 0,
  answers: []
})


onMounted(() => {

})



</script>

<style scoped>

</style>