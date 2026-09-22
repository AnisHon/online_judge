<template>
  <div class="app-container">
    <el-tabs
        v-model="tabStore.currentTab"
        type="card"
        class="tabs"
        @tab-remove="handleTabRemove"
        @tab-change="navigateToTab"
    >
      <el-tab-pane
          v-for="item in tabStore.tabs"
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

const syncRouteToTab = () => {
  if (!route.fullPath.startsWith("/backend") || !route.name) return;
  const routeName = String(route.name);
  // 同一个路由名可能承载不同的 :id/:contestId，详情页必须各自保留标签。
  const hasParams = route.matched.some(record => record.path.includes(':'));
  const tabIdentity = hasParams ? `${routeName}:${route.fullPath}` : routeName;
  tabStore.open(tabIdentity, String(route.meta?.name || ""), route.fullPath, routeName);
};

const handleTabRemove = async (targetName: string) => {
  const wasActive = tabStore.currentTab === targetName;
  const activeName = tabStore.removeTab(targetName);
  if (wasActive) await navigateToTab(activeName);
};

const navigateToTab = async (targetName: string) => {
  const path = tabStore.getUrl(targetName);
  if (path && path !== route.fullPath) await router.push({path});
};

watch(() => route.fullPath, syncRouteToTab, { immediate: true });

</script>

<style scoped lang="scss">
.app-container {
  height: var(--tab-height);
  width: 100%;
  flex: 0 0 var(--tab-height);
  box-sizing: border-box;
  overflow: hidden;
  padding: 3px 14px 0;
  background: var(--el-bg-color-page);
}
.app-container ::v-deep(.tabs),
.app-container ::v-deep(.tabs) .el-tabs__header,
.app-container ::v-deep(.tabs) .el-tabs__nav-scroll {
  height: var(--tab-height);
  margin: 0;
}

.app-container ::v-deep(.tabs) .el-tabs__content {
  display: none;
}

.app-container ::v-deep(.tabs) .el-tabs__header .el-tabs__nav-scroll .el-tabs__nav {
  border: none ;
}
.app-container ::v-deep(.tabs) .el-tabs__nav-wrap { overflow: visible; }
.app-container ::v-deep(.tabs) .el-tabs__nav-wrap::after { background: var(--el-border-color-lighter); }

.app-container ::v-deep(.tabs) .el-tabs__item {
  height: 30px;
  margin: 0 5px 0 0;
  padding: 0 14px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px 8px 0 0;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-lighter);
  line-height: 30px;
}
.app-container ::v-deep(.tabs) .el-tabs__item.is-active {
  color: var(--el-color-primary);
  background: var(--el-bg-color);
  border-bottom-color: var(--el-bg-color);
}
.app-container ::v-deep(.tabs) .el-tabs__item:hover { color: var(--el-color-primary); }
@media (max-width: 700px) {
  .app-container { padding: 3px 8px 0; }
  .app-container ::v-deep(.tabs) .el-tabs__item { padding: 0 10px; }
}
</style>
