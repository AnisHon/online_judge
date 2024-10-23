
<template>
<div class="problem-result">
  <div>
    <h3>我的答案</h3>
    <div>
      <div class="answer">
        <span><markdown-preview text="a"/></span>
        <span :class="resultStyle">{{ resultText }}</span>
      </div>
    </div>
  </div>

</div>
</template>
<script setup lang="ts">

import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {computed} from "vue";

const isCorrect = computed(() => {
  return false;
});

const resultText = computed(() => {
  return isCorrect.value ? "正确" : "错误";
});

const resultStyle = computed(() => {
  return isCorrect.value ? "right" : "wrong";
});






</script>

<style scoped>

.wrong {
  color: #F56C6C;
}

.right {
  color: #67C23A;
}

.answer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
}

</style>