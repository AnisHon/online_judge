<template>
  <MdEditor v-model="text" :theme="theme" :previewTheme="previewTheme"  :codeTheme="codeTheme" @on-upload-img="onUploadImg" />
</template>
<script setup lang="ts">
import {MdEditor} from 'md-editor-v3';
import 'md-editor-v3/lib/style.css';
import {computed} from "vue";
import {useDark} from "@vueuse/core";
import useConfig from "@/stores/useConfig.ts";
import {uploadImages} from "@/api/file";
import {fileSize, MAX_IMAGE_COUNT, MAX_IMAGE_SIZE} from "@/utils/file";
import {ElNotification} from "element-plus";
import type {UploadImgCallBack, UploadImgCallBackParam} from "md-editor-v3/lib/types/MdEditor/type";

const config = useConfig();

const text = defineModel<string | undefined>();

const isDark = useDark();

const theme = computed(() => {
  return isDark.value ? "dark" : "light";
})
const codeTheme = computed(() => {
  return config.readonly.codeTheme || "atom";
})



const previewTheme = computed(() => {
  return config.readonly.previewTheme || "default";
})

const onUploadImg = async (files: File[], callback: UploadImgCallBack) => {
  if (files.length > MAX_IMAGE_COUNT) {
    ElNotification.warning("最多同时上传 " + MAX_IMAGE_COUNT + " 张图片");
    return;
  }
  for (let file of files) {
    const sizeMb = fileSize(file.size);
    if (sizeMb > MAX_IMAGE_SIZE) {
      ElNotification.warning("文件大小不能超过 " + MAX_IMAGE_SIZE + " MB");
      return;
    }
  }
  const urls = await uploadImages(files);


  const callbackUrls: UploadImgCallBackParam = urls
      .map((url, index) => ({url: "/api/" + url, alt: files[index].name, title: files[index].name}))


  callback(callbackUrls);
}

</script>

<style>
.md-editor,
.md-editor .ͼo {
  background-color: var(--el-bg-color);
}

iframe {
  width: 100%;
  min-height: 500px;
}
</style>