<template>
  <div>
    <ImgCutter
        label="选择本地图片"
        fileType="png"
        rate="1:1"
        :isModal="false"
        :showChooseBtn="true"
        :lockScroll="true"
        :sizeChange="true"
        :moveAble="true"
        :imgMove="true"
        :originalGraph="false"
        :smallToUpload="true"
        :scaleAble="true"
        :previewMode="true"
        :quality="0.5"
        @cutDown="cutDown"
        accept="image/gif, image/jpeg ,image/png"
        >
      <template #open>
        <div class="btn btn-primary">选择本地图片</div>
      </template>

    </ImgCutter>
  </div>
</template>

<script setup lang="ts">
//@ts-ignore
import ImgCutter from 'vue-img-cutter';


const emit = defineEmits<{(e: 'cutDown', file: File): void}>()

const cutDown = (file: any) => {
  emit('cutDown', file.file);
}

</script>

<style scoped>

</style>