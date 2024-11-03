<template>
  <div style="margin: auto; max-width: var(--page-max-width);">


    <el-row :gutter="20">
      <el-col :span="16">
        <el-space direction="vertical" alignment="stretch" style="width: 100%">

          <custom-card icon="Notification" title="公告">
            <p v-for="i in 4" :key="i"><el-link type="primary">item {{i}}</el-link> </p>
          </custom-card>

          <custom-card icon="Notification" title="公告">
            <p v-for="i in 4" :key="i">item {{i}}</p>
          </custom-card>

          <custom-card icon="Notification" title="公告">
            <p v-for="i in 4" :key="i">item {{i}}</p>
          </custom-card>

        </el-space>

      </el-col>
      <el-col :span="8">
        <custom-card icon="Histogram" title="排名" style="height: 100%">
          <el-table :data="ranks">
            <el-table-column type="index" label="#" />
            <el-table-column label="分数" align="center" prop="points" />
            <el-table-column label="用户名"  prop="nikeName" />
          </el-table>
        </custom-card>
      </el-col>
    </el-row>

  </div>

</template>

<script setup lang="ts">
import CustomCard from "@/components/CustomCard/CustomCard.vue";
import {rank, type UserView} from "@/api/user";
import {ref} from "vue";
import {useDark} from "@vueuse/core";

const ranks = ref<UserView[]>([])

// created
rank(20)
    .then((data) => {
      ranks.value = data;
    })

</script>

<style scoped>

</style>