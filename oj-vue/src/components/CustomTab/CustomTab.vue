<template>
  <div class="app-container">
    <el-tabs
        v-model="editableTabsValue"
        type="card"
        class="tabs"
        @tab-remove="onTabRemove"
        @tab-change="onTabChange"
    >
      <el-tab-pane
          v-for="item in editableTabs"
          :key="item.name"
          :label="item.title"
          :name="item.name"
          :closable="item.closable"
          lazy
      />


    </el-tabs>
  </div>

</template>

<script lang="ts" setup>
import {useTabStore} from "@/stores/useTabStore.ts";
import {useRoute, useRouter} from "vue-router";
import {watch} from "vue";

const tabStore = useTabStore();
const route = useRoute();
const router = useRouter();

const editableTabsValue = tabStore.getCurrentTab();
const editableTabs = tabStore.getTabs();

const setTabs = () => {
  if (!route.fullPath.startsWith("/backend")) {
    return;
  }

  const name = <string>route.name
  const title = <string>route?.meta?.name || "";
  tabStore.open(name, title);
}

const onTabRemove = (targetName: string) => {
  const active = tabStore.removeTab(targetName);
  onTabChange(active);
}

const onTabChange = (targetName: string) => {
  router.push({name: targetName})
}

watch(route, setTabs, { immediate: true });

</script>

<style scoped>
.app-container {
  height: var(--tab-height);
}
</style>

<style>
.tabs,
.el-tabs__header,
.el-tabs__nav-scroll {
  height: var(--tab-height);
  margin: 0;
}

.el-tabs__content {
  display: none;
}

.el-tabs__header .el-tabs__nav-scroll .el-tabs__nav {
  border: none ;
}

.el-tabs__item {
  height: 80%;
}

</style>