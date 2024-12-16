<template>
  <MdPreview :id="id" :modelValue="text" :theme="theme" :previewTheme="previewTheme"/>
  <MdCatalog :editorId="id" :theme="theme" :scrollElement="scrollElement" />
</template>

<script setup lang="ts">

import MarkdownIt from "markdown-it";
// @ts-ignore
import mk from 'markdown-it-katex';

import {computed} from "vue";

import { MdPreview, MdCatalog } from 'md-editor-v3';
// preview.css相比style.css少了编辑器那部分样式
import 'md-editor-v3/lib/preview.css';
import {useDark} from "@vueuse/core";
import useConfig from "@/stores/useConfig.ts";

const isDark = useDark();

const config = useConfig()

const id = 'preview-only';
const scrollElement = document.documentElement;

const {text = ""} = defineProps<{text?: string}>();

const theme = computed(() => {
  return isDark.value ?  "dark" : "light";
})

const previewTheme = computed(() => {
  return config.get.previewTheme;
})


const md = new MarkdownIt({
  html:true,
  linkify: true,
  typographer: true
});
md.use(mk)






</script>

<style scoped>
@import "https://cdnjs.cloudflare.com/ajax/libs/KaTeX/0.5.1/katex.min.css";

</style>