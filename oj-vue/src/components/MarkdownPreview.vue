<template>
  <div class="reading-preview-shell" :class="`reading-preview-shell--${props.variant}`">
    <MdPreview
        :id="previewId"
        :modelValue="props.text || ''"
        :theme="theme"
        :previewTheme="previewTheme"
        :codeTheme="codeTheme"
        :sanitize="sanitizeMarkdownHtml"
        class="reading-preview"
    />
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {useDark} from '@vueuse/core'
import {MdPreview} from 'md-editor-v3'
import 'md-editor-v3/lib/preview.css'
import useConfig from '@/stores/useConfig.ts'
import {sanitizeMarkdownHtml} from '@/utils/markdown/sanitize'

type PreviewVariant = 'article' | 'compact' | 'embedded'

let previewInstance = 0

const props = withDefaults(defineProps<{
  text?: string
  id?: string
  variant?: PreviewVariant
}>(), {
  text: '',
  variant: 'article'
})

const isDark = useDark()
const config = useConfig()
const previewId = props.id || `markdown-preview-${++previewInstance}`

const theme = computed(() => isDark.value ? 'dark' : 'light')
const previewTheme = computed(() => config.readonly.previewTheme || 'default')
const codeTheme = computed(() => config.readonly.codeTheme || 'atom')
</script>

<style scoped>
.reading-preview-shell {
  min-width: 0;
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 16px;
  background: var(--el-bg-color);
}

.reading-preview-shell :deep(.md-editor-preview-wrapper),
.reading-preview-shell :deep(.md-editor-preview) {
  background: transparent !important;
}

.reading-preview-shell :deep(.md-editor-preview) {
  max-width: none;
  color: var(--el-text-color-primary);
  overflow-wrap: break-word;
  word-break: break-word;
}

.reading-preview-shell :deep(.md-editor-preview p),
.reading-preview-shell :deep(.md-editor-preview li) {
  color: var(--el-text-color-regular);
  line-height: 1.9;
}

.reading-preview-shell :deep(.md-editor-preview h1),
.reading-preview-shell :deep(.md-editor-preview h2),
.reading-preview-shell :deep(.md-editor-preview h3) {
  color: var(--el-text-color-primary);
}

.reading-preview-shell :deep(.md-editor-preview pre) {
  max-width: 100%;
  overflow: auto;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
}

.reading-preview-shell :deep(.md-editor-preview img) {
  max-width: 100%;
  height: auto;
  border-radius: 10px;
}

.reading-preview-shell :deep(.md-editor-preview iframe) {
  display: block;
  width: 100%;
  max-width: 100%;
  border: 0;
}

.reading-preview-shell--article :deep(.md-editor-preview) {
  padding: clamp(22px, 4vw, 48px);
}

.reading-preview-shell--compact {
  border-radius: 12px;
}

.reading-preview-shell--compact :deep(.md-editor-preview) {
  padding: 14px 16px;
}

.reading-preview-shell--compact :deep(.md-editor-preview p:last-child),
.reading-preview-shell--embedded :deep(.md-editor-preview p:last-child) {
  margin-bottom: 0;
}

.reading-preview-shell--embedded {
  border: 0;
  border-radius: 0;
  background: transparent;
}

.reading-preview-shell--embedded :deep(.md-editor-preview) {
  padding: 0;
}

@media (max-width: 600px) {
  .reading-preview-shell--article :deep(.md-editor-preview) {
    padding: 18px 16px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .reading-preview-shell :deep(*) {
    scroll-behavior: auto;
    transition-duration: 0.01ms !important;
  }
}
</style>
