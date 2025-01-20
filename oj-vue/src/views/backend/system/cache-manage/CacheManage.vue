<template>
  <div class="app-container">
    <el-row :gutter="20" style="height: 100%">
      <el-col :span="8">
        <el-card class="card" body-class="card" header="类别">
          <el-table :data="cacheList" :show-header="false" @row-click="handleDescClickRow" highlight-current-row	>
            <el-table-column label="ID" prop="id" align="center" width="50"/>
            <el-table-column label="前缀" prop="desc" show-overflow-tooltip/>
            <el-table-column label="描述" prop="type" show-overflow-tooltip/>
            <template #empty>
              <el-empty description="空"/>
            </template>
          </el-table>

        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="card" header="缓存键">
          <el-table :data="keyList" :show-header="false" @row-click="handleKeyClickRow" fit highlight-current-row>
            <el-table-column show-overflow-tooltip>
              <template v-slot="scope">
                {{ scope.row }}
              </template>
            </el-table-column>
            <el-table-column type="default" align="right">
              <template v-slot="scope">
                <el-button type="danger" @click="handleDelete(scope.row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="没有缓存键"/>
            </template>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="card" header="内容">
          <el-form label-position="top" >
            <el-form-item label="缓存键">
              <el-input  v-model="cacheInfo.key" />
            </el-form-item>
            <el-form-item label="过期时间">
              <el-input  v-model="cacheInfo.expireTime" />
            </el-form-item>
            <el-form-item label="缓存值">
              <el-input v-model="cacheInfo.value" type="textarea" :rows="10"/>
            </el-form-item>
          </el-form>

        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">

import {ref} from "vue";
import {type CacheDesc, type CacheInfo, getCacheInfo, getCacheKeys, listCacheDesc, removeCache} from "@/api/cache";
import {ElMessageBox} from "element-plus";

const cacheList = ref<CacheDesc[]>([]);

const keyList = ref<string[]>([]);

const prefix = ref<string>("")

const cacheInfo = ref<CacheInfo>({
  key: "",
  value: "",
  expireTime: undefined,
});

const handleDelete = async (row: string) => {
  ElMessageBox.confirm("你确定要删除缓存吗", {confirmButtonText: "确定", cancelButtonText: "取消"})
      .then(async () => {
        await removeCache(row);
        await getKeyList();
      }).catch(() => {})
}

const getCacheList = async () => {
  cacheList.value = await listCacheDesc();
}

const handleDescClickRow = async (row: CacheDesc) => {
  prefix.value = row.type;
  await getKeyList();
}

const getKeyList = async () => {
  keyList.value = await getCacheKeys(prefix.value);
}

const handleKeyClickRow = async (row: string) => {
  cacheInfo.value = await getCacheInfo(row);
}



getCacheList();
</script>

<style lang="scss" scoped>
.app-container {
  height: 100%;
}

.app-container .card {
  height: 100%;
}

</style>
