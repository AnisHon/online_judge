<template>
  <div>
    <h2>输入描述</h2>
    <markdown-preview :text="problem.input"/>
    <h2>输出描述</h2>
    <markdown-preview :text="problem.output"/>

    <div class="examples">
      <h2>样例</h2>
      <el-row :gutter="20">
        <el-col :span="12" class="example_wrapper">
          <h3>输入</h3>
          <p class="example" v-text="problem.inputExample">
          </p>

        </el-col>
        <el-col :span="12" class="example_wrapper">
          <h3>输出</h3>
          <p class="example" v-text="problem.outputExample">
          </p>

        </el-col>
      </el-row>
    </div>
  </div>

</template>

<script setup lang="ts">

import MarkdownPreview from "@/components/MarkdownPreview.vue";
import type {OjProblemView} from "@/api/problem";

const {problem} = defineProps<{problem: OjProblemView}>()

</script>
<style scoped>
.examples {
  display: flex;
  flex-direction: column;

  p {
    white-space: pre-wrap;
  }

  .example_wrapper {
    display: flex;
    flex-direction: column;
    justify-content: flex-start;
    align-items: stretch;
  }


  .example {
    flex: 1;
    max-height: 100px;
    padding: 10px;
    border: 1px solid black;
    border-radius: 4px;
    white-space: pre-wrap;
    align-items: stretch;
    overflow: auto;


  }
}
</style>