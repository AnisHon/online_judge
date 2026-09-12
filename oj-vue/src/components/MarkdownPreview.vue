<template>
  <div class="reading-preview-shell">
    <MdPreview
        :id="id"
        :modelValue="text"
        :theme="theme"
        :previewTheme="previewTheme"
        :codeTheme="codeTheme"
        class="reading-preview"/>
  </div>
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
import {baseURL} from "@/utils/http.ts";

const isDark = useDark();

const config = useConfig()

const id = 'preview-only';

const {text = ""} = defineProps<{text?: string}>();

const theme = computed(() => {
  return isDark.value ?  "dark" : "light";
})

const previewTheme = computed(() => {
  return config.readonly.previewTheme || "default";
})

const codeTheme = computed(() => {
  return config.readonly.codeTheme || "atom";
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

<style>
iframe {
  width: 100%;
  height: 100%;
  min-height: 500px;
  resize: both;
  overflow: auto;
}

.reading-preview-shell { overflow: hidden; border: 1px solid var(--el-border-color-lighter); border-radius: 16px; background: var(--el-bg-color); }
.reading-preview-shell .md-editor-preview-wrapper,
.reading-preview-shell .md-editor-preview { background: transparent !important; }
.reading-preview-shell .md-editor-preview { max-width: none; padding: clamp(22px, 4vw, 48px); color: var(--el-text-color-primary); overflow-wrap: anywhere; }
.reading-preview-shell .md-editor-preview p,
.reading-preview-shell .md-editor-preview li { color: var(--el-text-color-regular); line-height: 1.9; }
.reading-preview-shell .md-editor-preview h1,
.reading-preview-shell .md-editor-preview h2,
.reading-preview-shell .md-editor-preview h3 { color: var(--el-text-color-primary); }
.reading-preview-shell .md-editor-preview pre { border: 1px solid var(--el-border-color-lighter); border-radius: 12px; }
</style>
