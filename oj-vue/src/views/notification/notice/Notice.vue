<template>
<div class="app-container common-max-width-page" style="margin: auto">
  <div v-if="notify">
    <el-page-header title="返回" @back="router.back"/>
    <h1>{{ notify.title }}</h1>
    <el-divider/>
    <div>
      <MarkdownPreview :text="notify.content"/>
    </div>
  </div>

</div>
</template>

<script setup lang="ts">

import MarkdownPreview from "@/components/MarkdownPreview.vue";
import {ref} from "vue";
import {getNotice, type Notice} from "@/api/notice";
import {useRoute, useRouter} from "vue-router";
import type {IdType} from "@/api/common.ts";

const route = useRoute();

const router = useRouter();

const noticeId: IdType = <IdType>route.params.id;

const notify = ref<Notice>();

getNotice(noticeId)
    .then(x => notify.value = x)


</script>



<style scoped>

</style>