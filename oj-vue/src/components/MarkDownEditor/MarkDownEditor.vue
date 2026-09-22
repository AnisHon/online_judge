<template>
  <div
    class="markdown-editor-shell markdown-render-scope"
    :class="[`markdown-editor-shell--${props.mode}`, { 'markdown-render-scope--dark': isDark }]"
  >
    <MdEditor
        v-model="text"
        :style="editorStyle"
        :theme="theme"
        :previewTheme="previewTheme"
        :codeTheme="codeTheme"
        :sanitize="sanitizeMarkdownHtml"
        @on-upload-img="onUploadImg"
    />
  </div>
</template>

<script setup lang="ts">
import {computed} from 'vue'
import {useDark} from '@vueuse/core'
import {ElNotification} from 'element-plus'
import {MdEditor} from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import type {UploadImgCallBack, UploadImgCallBackParam} from 'md-editor-v3/lib/types/MdEditor/type'
import useConfig from '@/stores/useConfig.ts'
import {uploadImages} from '@/api/file'
import {MAX_IMAGE_COUNT, MAX_IMAGE_SIZE, MAX_IMAGE_SIZE_BYTES} from '@/utils/file'
import {baseURL} from '@/utils/http.ts'
import {sanitizeMarkdownHtml} from '@/utils/markdown/sanitize'

const props = withDefaults(defineProps<{
  height?: string
  mode?: 'fixed' | 'flex'
}>(), {
  height: '500px',
  mode: 'fixed'
})

const config = useConfig()
const text = defineModel<string | undefined>()
const isDark = useDark()

const theme = computed(() => isDark.value ? 'dark' : 'light')
const codeTheme = computed(() => config.readonly.codeTheme || 'atom')
const previewTheme = computed(() => config.readonly.previewTheme || 'default')
const editorStyle = computed(() => props.mode === 'flex'
    ? {height: '100%', minHeight: '0'}
    : {height: props.height})

const onUploadImg = async (files: File[], callback: UploadImgCallBack) => {
  if (files.length > MAX_IMAGE_COUNT) {
    ElNotification.warning(`最多同时上传 ${MAX_IMAGE_COUNT} 张图片`)
    callback([])
    return
  }

  if (files.some(file => file.size > MAX_IMAGE_SIZE_BYTES)) {
    ElNotification.warning(`文件大小不能超过 ${MAX_IMAGE_SIZE} MB`)
    callback([])
    return
  }

  try {
    const urls = await uploadImages(files)
    if (!Array.isArray(urls) || urls.length !== files.length || urls.some(url => !url)) {
      throw new Error('图片上传结果无效')
    }

    const callbackUrls: UploadImgCallBackParam = urls.map((url, index) => ({
      url: `${baseURL}/image/${url}`,
      alt: files[index].name,
      title: files[index].name
    }))
    callback(callbackUrls)
  } catch (error) {
    callback([])
    ElNotification.error(error instanceof Error ? error.message : '图片上传失败，请稍后重试')
  }
}
</script>

<style scoped>
.markdown-editor-shell {
  display: flex;
  min-height: 0;
  flex-direction: column;
  width: 100%;
  max-width: 100%;
  min-width: 0;
}

.markdown-editor-shell > :deep(.md-editor),
.markdown-editor-shell > :deep(.md-editor .ͼo) {
  box-sizing: border-box;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  background-color: var(--el-bg-color);
}

.markdown-editor-shell--flex,
.markdown-editor-shell--flex > :deep(.md-editor) {
  height: 100%;
  min-height: 0;
}

.markdown-editor-shell--flex > :deep(.md-editor) {
  flex: 1 1 auto;
}

.markdown-editor-shell :deep(.md-editor-content),
.markdown-editor-shell :deep(.md-editor-content-wrapper),
.markdown-editor-shell :deep(.md-editor-input-wrapper),
.markdown-editor-shell :deep(.md-editor-preview-wrapper) {
  min-width: 0;
  min-height: 0;
}

.markdown-editor-shell :deep(.md-editor-preview code) {
  font-family: "OJCodeFont", "JetBrains Mono", "Cascadia Code", Consolas,
    "Liberation Mono", "DejaVu Sans Mono", monospace;
  font-variant-ligatures: none;
}
</style>
