<template>
  <main class="app-container common-max-width-page notification-page" ref="container">
    <header class="notification-header"><div><p class="eyebrow">UPDATES & NEWS</p><h1>平台公告</h1><p>重要通知、版本动态和社区消息都在这里。</p></div><el-icon class="header-icon"><Bell /></el-icon></header>
    <section class="notice-list">
      <button v-for="item in list" :key="item.noticeId" class="notice-item" type="button" @click="router.push({name: 'notice', params:{id: item.noticeId}})">
        <span class="notice-mark"><el-icon><Bell /></el-icon></span>
        <span class="notice-main"><span class="notice-title">{{ item.title }} <el-tag v-if="item.topUp" type="danger" size="small">重要</el-tag></span><span class="notice-date">{{ item.createTime }}</span></span>
        <el-icon class="notice-arrow"><ArrowRight /></el-icon>
      </button>
      <el-empty v-if="total === 0" description="没有公告" />
    </section>
    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="query.currentPage"
        v-model:limit="query.pageSize"
        @pagination="getList"
        :scroll-element="<HTMLElement>container"
    />
  </main>
</template>

<script setup lang="ts">

import Pagination from "@/components/pageination/Pagination.vue";
import {reactive, ref} from "vue";
import type {PagedType} from "@/api/pagedType.ts";
import {listNotice, type Notice} from "@/api/notice";
import {useRouter} from "vue-router";
import {ArrowRight, Bell} from "@element-plus/icons-vue";

const router = useRouter();

const container = ref<HTMLElement | null>(null);

const query = reactive<PagedType>({
  currentPage: 1,
  pageSize: 10,
})

const total = ref(0)

const list = ref<Notice[]>([]);

const getList = async () => {
  const data = await listNotice(query);
  list.value = data.data;
  total.value = data.totalRecords;
}



getList();


</script>



<style scoped>
.notification-page { margin: auto; }.notification-header { position: relative; display: flex; justify-content: space-between; align-items: center; min-height: 130px; margin-bottom: 18px; padding: 24px 32px; overflow: hidden; border: 1px solid var(--el-border-color-light); border-radius: 18px; background: linear-gradient(135deg, var(--el-color-warning-light-9), var(--el-bg-color)); color: var(--el-text-color-primary); }.eyebrow { margin: 0 0 7px; color: var(--el-color-warning); font-size: 11px; font-weight: 800; letter-spacing: .18em; }.notification-header h1 { margin: 0; font-size: 30px; letter-spacing: -.04em; }.notification-header p:last-child { margin: 9px 0 0; color: var(--el-text-color-secondary); }.header-icon { margin-right: 7%; color: var(--el-color-warning-light-5); font-size: 78px; transform: rotate(10deg); opacity: .55; }.notice-list { display: flex; flex-direction: column; gap: 10px; }.notice-item { display: flex; align-items: center; width: 100%; padding: 18px 20px; border: 1px solid var(--el-border-color-light); border-radius: 14px; color: inherit; text-align: left; background: var(--el-bg-color); cursor: pointer; transition: border-color .2s, transform .2s, background .2s; }.notice-item:hover { border-color: var(--el-color-warning-light-5); background: var(--el-fill-color-light); transform: translateX(3px); }.notice-mark { display: grid; width: 38px; height: 38px; margin-right: 14px; place-items: center; border-radius: 12px; color: var(--el-color-warning); background: var(--el-color-warning-light-9); }.notice-main { min-width: 0; flex: 1; }.notice-title, .notice-date { display: block; }.notice-title { overflow: hidden; color: var(--el-text-color-primary); font-size: 15px; font-weight: 600; white-space: nowrap; text-overflow: ellipsis; }.notice-date { margin-top: 5px; color: var(--el-text-color-secondary); font-size: 12px; }.notice-arrow { margin-left: 14px; color: var(--el-text-color-placeholder); }.notice-item:hover .notice-arrow { color: var(--el-color-warning); }@media (max-width: 560px) { .notification-header { min-height: 110px; padding: 20px; }.notification-header h1 { font-size: 25px; }.notification-header p:last-child { font-size: 13px; }.header-icon { display: none; }.notice-item { padding: 14px; } }
</style>
