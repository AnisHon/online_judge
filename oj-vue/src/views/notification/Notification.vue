<template>
  <div class="app-container common-max-width-page" ref="container" style="margin: auto">

    <h1>公告</h1>
    <el-divider/>
    <el-table :data="list" :show-header="false">
      <el-table-column prop="title">
        <template v-slot="scope">
          <el-link
              :underline="false"
              type="primary"
              @click="router.push({name: 'notice', params:{id: scope.row.noticeId}})"
          >
            {{ scope.row.title }}
          </el-link>
          <el-tag type="danger" v-if="scope.row.topUp">重要</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" align="right"/>
      <template #empty>
        <el-empty description="没有公告"/>
      </template>
    </el-table>
    <pagination
        v-show="total>0"
        :total="total"
        v-model:page="query.currentPage"
        v-model:limit="query.pageSize"
        @pagination="getList"
        :scroll-element="<HTMLElement>container"
    />
  </div>
</template>

<script setup lang="ts">

import Pagination from "@/components/pageination/Pagination.vue";
import {reactive, ref} from "vue";
import type {PagedType} from "@/api/pagedType.ts";
import {listNotice, type Notice} from "@/api/notice";
import {useRouter} from "vue-router";

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



<style lang="scss" scoped>

</style>