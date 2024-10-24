

<template>
  <div class="radio" @click="handleClick">
    <span class="circle" :class="{'active': choose}">
      {{ option }}
    </span>
    <span>
      <markdown-preview :text="content"/>
    </span>
  </div>
</template>
<script setup lang="ts">
import MarkdownPreview from "@/components/MarkdownPreview.vue";

const {option = "", content = "", choose = false} = defineProps<{
  option?: string,
  content?: string,
  choose?: boolean
}>();



const emit = defineEmits<{
  (e: 'click', index: string): void
}>();

const handleClick = () => {
  emit("click", option)
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