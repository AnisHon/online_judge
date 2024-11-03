

<template>
  <div class="radio" @click="handleClick">
    <span class="circle" :class="{'active': isSelected}">
      {{ option }}
    </span>
    <span>
      <markdown-preview :text="content"/>
    </span>
  </div>
</template>
<script setup lang="ts">
import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {letterToNumber} from "@/utils/stringUtils";
import type {Answer} from "@/api/problem/judge";
import {computed} from "vue";

const {option = 'A', content = "", answers} = defineProps<{
  option?: string,
  content?: string,
  answers: Answer[]
}>();

const isSelected = computed(() => {
  const index = answers.findIndex(x => x.index === letterToNumber(option));
  return index !== -1;
});


const emit = defineEmits<{
  (e: 'click', index: number, b: boolean): void
}>();

const handleClick = () => {
  emit("click", letterToNumber(option), isSelected.value)
}


</script>
<style scoped>
.radio {
  display: flex;
  align-items: center;
  gap: 8px;
  .circle {
    height: 32px;
    width: 32px;
    border-radius: 50%;
    border: 1px solid rgb(121.3, 187.1, 255);
    user-select: none;

    line-height: 32px;
    text-align: center;
    font-weight: 400;

    transition: background-color 0.1s, border-color 0.1s;

  }

  .circle:hover {
    border: 1px solid rgb(121.3, 187.1, 255);
    background-color: rgb(235.9, 245.3, 255);
    color: rgb(121.3, 187.1, 255);
  }

  .circle.active {
    background-color: #6592FF;
    border-color: #6592FF;
    color: white;
  }
}



</style>

<style>

</style>