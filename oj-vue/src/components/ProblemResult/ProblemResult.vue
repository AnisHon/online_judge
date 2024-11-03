
<template>
<div class="problem-result">
  <div>
    <h3 :class="style">{{ resultText }}</h3>
  </div>
  <div>
    <strong>得分：</strong>
    <span>{{ score }}</span>
  </div>

  <div>
    <h3>正确答案</h3>
    <div>
      <div class="answer">
        <span v-for="(value, key) of groupResult">
          <markdown-preview :text="itemIndexText(parseInt(key))"/>
            <span v-for="i of value" :key="i.index">
              <markdown-preview :text="itemText(i)"/>
            </span>

        </span>
      </div>
    </div>
  </div>

</div>
</template>
<script setup lang="ts">

import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {computed} from "vue";
import type {Answer, JudgeResponse} from "@/api/problem/judge";
import {ProblemType} from "@/api/problem";
import {numberToLetter} from "@/utils/stringUtils";
import __ from "lodash";


const {
  result = {
      answers: [],
      correct: false,
      totalScore: '0',
    },
  type = ProblemType.FILL} = defineProps<{
  result?: JudgeResponse,
  type?: ProblemType
}>();

const correct = computed(() => {
  return result.correct;
})

const groupResult = computed(() => {
  return __.groupBy(result.answers, (x) => x.index);
})

const style = computed(() => {
  return correct.value ? "right" : "wrong";
})

const isFill = computed(() => {
  return type === ProblemType.FILL;
})

const answers = computed(() => {
  return result.answers
})

const score = computed(() => {
  return result.totalScore;
})

const resultText = computed(() => {
  return correct.value ? '正确' : '错误';
})

const itemIndexText = (idx: number) => {
  if (type === ProblemType.FILL) {
    return `__第${idx + 1}空__`
  } else {
    return "";
  }
}

const itemText = (item: Answer): string => {

  if (isFill.value) {
    return ` ${item.answer} `
  } else {
    return `__${numberToLetter(item.index)}__`
  }

}
</script>

<style scoped>
.wrong {
  color: #F56C6C;
}

.right {
  color: #67C23A;
}

</style>