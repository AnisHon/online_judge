<template>
  <div>
    <h2>输入描述</h2>
    <markdown-preview :text="problem.input"/>
    <h2>输出描述</h2>
    <markdown-preview :text="problem.output"/>
    <h2>样例</h2>
    <div class="examples">

      <el-row :gutter="20">
        <el-col :span="12" class="example_wrapper">
          <h3>输入数据</h3>
          <el-scrollbar class="box" max-height="240">
            <p class="example" v-text="problem.inputExample"></p>
            <copy-link class="link" :value="problem.inputExample">复制</copy-link>
          </el-scrollbar>
        </el-col>
        <el-col :span="12" class="example_wrapper">
          <h3>输出数据</h3>
          <el-scrollbar class="box" max-height="120">
            <p class="example" v-text="problem.outputExample"></p>
            <copy-link class="link" :value="problem.outputExample">复制</copy-link>

          </el-scrollbar>
        </el-col>
      </el-row>
    </div>
  </div>

</template>

<script setup lang="ts">

import MarkdownPreview from "@/components/MarkdownPreview.vue";
import type {OjProblemView} from "@/api/problem";
import CopyLink from "@/components/DetailProblem/component/CopyLink.vue";

const {problem} = defineProps<{problem: OjProblemView}>()

</script>
<style lang="scss" scoped>
@use "@/assets/styles/color" as *;
h3 {
  font-weight: 400;
}
.examples {
  display: flex;
  flex-direction: column;
  margin: 10px;
  p {
    white-space: pre-wrap;
  }



  .example_wrapper {
    .box {
      position: relative;
      min-height: 80px;
      max-height: 120px;


      .example {
        display: block;
        margin: 0;
        padding: 20px;
        border: 1px solid $dark-border-color;
        border-radius: 4px;
        white-space: pre-wrap;
        align-items: stretch;
        font-family: Monospaced Consolas, Menlo, Monaco, 'Lucida Console', 'Liberation Mono', 'DejaVu Sans Mono', 'Bitstream Vera Sans Mono', 'Courier New', serif;
        color: $primary-text-color;
      }




    }
    .box .link {
      content: "复制";
      position: absolute;
      top: 0;
      right: 5px;
      color: $secondary-text-color;
    }
  }



}
</style>
