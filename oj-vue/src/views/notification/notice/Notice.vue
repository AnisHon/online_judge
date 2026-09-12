<template>
<main class="app-container common-max-width-page notice-page">
  <div v-if="notify">
    <el-page-header title="返回公告" @back="router.back"/>
    <article class="notice-article">
      <header class="notice-article__header"><p class="eyebrow">PLATFORM UPDATE</p><h1>{{ notify.title }}</h1><p class="notice-date">发布于 {{ notify.createTime }}</p></header>
      <MarkdownPreview :text="notify.content"/>
    </article>
  </div>
</main>
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
.notice-page { margin: auto; padding-bottom: 30px; }.notice-page > div { margin-top: 16px; }.notice-article { margin-top: 16px; }.notice-article__header { margin-bottom: 16px; padding: 28px 34px 22px; border: 1px solid var(--el-border-color-light); border-radius: 18px; background: var(--el-bg-color); }.eyebrow { margin: 0 0 8px; color: var(--el-color-warning); font-size: 11px; font-weight: 800; letter-spacing: .18em; }.notice-article h1 { margin: 0; color: var(--el-text-color-primary); font-size: clamp(26px, 4vw, 38px); letter-spacing: -.04em; }.notice-date { margin: 10px 0 0; color: var(--el-text-color-secondary); font-size: 13px; }
@media (max-width: 600px) { .notice-article__header { padding: 22px 18px 18px; }.notice-article h1 { font-size: 26px; } }
</style>
