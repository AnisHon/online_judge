<template>
  <el-dialog v-model="open" title="上传文件">
    <el-upload
        drag
        class="upload"
        ref="updateInstance"
        :auto-upload="false"
        :http-request="sendUpload"
        multiple
    >
      <el-icon class="el-icon--upload"><upload-filled /></el-icon>
      <div class="el-upload__text">
        将文件拖至此处或者 <em>点击上传</em>
      </div>
    </el-upload>

    <template #footer>
      <el-button type="primary" @click="onClear">清空列表</el-button>
      <el-button type="success" @click="onSubmit">开始上传</el-button>
    </template>
  </el-dialog>
</template>
<script setup lang="ts">
import {UploadFilled} from "@element-plus/icons-vue";
import {ref} from "vue";
import {ElNotification, type UploadInstance, type UploadRequestOptions} from "element-plus";
import {md5} from "@/utils/md5.ts";
import {addFile, getProgress, initSlice, mergeFile, type Splice, upload} from "@/api/file";
import type {IdType} from "@/api/common.ts";
import to from "await-to-js";
import {round} from "lodash";

const emit = defineEmits<{(e: "finished"): void}>()

const open = defineModel<boolean>();

const {parentId} = defineProps<{parentId: IdType}>();

const updateInstance = ref<UploadInstance>();


const onSubmit = () => {
  updateInstance.value?.submit();
}

const onClear = () => {
  updateInstance.value?.clearFiles();
}

const sendUpload = async (option: UploadRequestOptions) => {

  const file = option.file

  // @ts-ignore
  option.onProgress({percent: 0});
  // 获取或初始文件参数
  const slice = await getSlice(file);

  // 初始化错误
  if (!slice) {
    ElNotification.error({
      title: '文件上传错误',
      message: '获取上传任务失败'
    })
    return;
  }

  const {finished, path, fileMd5} = slice
  if (!finished) {
    // 没做完需要继续上传
    const [err] = await to(handleUpload(file, slice, option))

    if (err) {
      ElNotification.error({
        title: '文件上传错误',
        message: '部分分片上次失败，请尝试重新上传文件'
      })
      return;
    }

    // 上传结束后合并
    const [err2] = await to(mergeFile(fileMd5))
    if (err2) {
      ElNotification.error({
        title: '文件上传错误',
        message: err2.message
      })
      return;
    }
  }
  // 添加最终文件
  await addFile(fileMd5, file.name, parentId);
  emit("finished");
}

/**
 * 获取一个上传任务，没有则初始化一个
 */
const getSlice = async (file: File) => {
  const fileMd5: string = await md5(file)

  let [err, splice] = await to(getProgress(fileMd5))
  if (err) {
    ElNotification.error({
      title: '文件上传错误',
      message: err.message
    });
    return splice;
  }


  // 分片不存在从头开始
  if (!splice) {
    const [err, data] = await to(initSlice( {
      md5: fileMd5,
      fileName: file.name,
      chunk: file,
      index: 0,
      chunkSize: 5 * 1024 * 1024,
      totalSize: file.size,
    }))
    splice = data;
    if (err) {
      ElNotification.error({
        title: '文件上传错误',
        message: err.message
      })
      return splice;
    }
  }
  return splice;
}

/**
 * 上传逻辑处理，如果文件已经上传完成（完成分块合并操作），则不会进入到此方法中
 */
const handleUpload = async (file: File, splice: Splice, options: UploadRequestOptions) => {

  let lastUploadedSize = 0; // 上次断点续传时上传的总大小
  const {partHashes, chunkNum, chunkSize, fileMd5} = splice

  const uploadNext = async (md5: string, partNumber: number) => {
    const start = Number(chunkSize) * (partNumber - 1)
    const end = start + Number(chunkSize);
    const blob = file.slice(start, end);
    const [err, data] = await to(upload(md5, partNumber, blob));

    if (err || !data) {
      return Promise.reject(`分片${partNumber}， 获取上传地址失败`);
    }


    return Promise.resolve({partNumber: partNumber, uploadedSize: blob.size});
  }

  const queue = []

  for (let partNumber = 1; partNumber <= chunkNum; partNumber++) {
    const exitPart = (partHashes || []).find(partHash => partHash.chuckIndex == partNumber)

    if (exitPart) {
      // 分片已上传完成，累计到上传完成的总额中,同时记录一下上次断点上传的大小，用于计算上传速度
      lastUploadedSize += Number(chunkSize)
    } else {
      queue.push(partNumber);
    }
  }


  while (queue.length > 0) {
    const partNumber: number = queue.shift() || 0;
    const [err] = await to(uploadNext(fileMd5, partNumber));
    if (err) {
      return Promise.reject(err);
    }
    const percent = round(partNumber / chunkNum * 100);
    console.log(percent)
    // @ts-ignore
    options.onProgress({percent: percent})
  }

}


</script>
<style lang="scss" scoped>
</style>
