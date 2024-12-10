<template>
  <div>
    <div v-html="getRenderText" >
    </div>
  </div>

</template>

<script setup lang="ts">

import MarkdownIt from "markdown-it";
// @ts-ignore
import mk from 'markdown-it-katex';

import {computed} from "vue";




const md = new MarkdownIt({
  html:true,
  linkify: true,
  typographer: true
});
md.use(mk)


const {text = ""} = defineProps<{text?: string}>();

const getRenderText = computed(() => {
  return md.render(text);
})



</script>

<style scoped>
@import "https://cdnjs.cloudflare.com/ajax/libs/KaTeX/0.5.1/katex.min.css";

</style>