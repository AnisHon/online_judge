<template>
  <div class="app-container">
    <el-container class="backend-container">
      <el-aside class="backend-aside" :width="collapse ? 'var(--vertical-menu-collapse-width)' : 'var(--vertical-menu-expand-width)'" >
        <backend-menu :collapse="collapse"/>
      </el-aside>

      <el-container class="backend-body">
        <el-header class="header" height="var(--backend-header-height)">
          <header-bar v-model:collapse="collapse"/>
          <custom-tab/>
        </el-header>

        <el-main ref="elMainRef" id="main-box" class="main">

          <router-view>
            <template v-slot="{Component}">
              <transition name="fade-transform" mode="out-in">
                <keep-alive :max="10" :include="includeRoutes" :exclude="excludeRoutes">
                  <component :is="Component" />
                </keep-alive>
              </transition>
            </template>
          </router-view>

        </el-main>
      </el-container>


    </el-container>



  </div>
</template>

<script setup lang="ts">
import BackendMenu from "@/layout-backend/component/BackendMenu/BackendMenu.vue";
import {computed, onBeforeUnmount, provide, ref} from "vue";
import HeaderBar from "@/layout-backend/component/HeaderBar/HeaderBar.vue";
import type {ElMain} from "element-plus";
import CustomTab from "@/layout-backend/component/CustomTab/CustomTab.vue";
import {useTabStore} from "@/stores/useTabStore.ts";
import {useRouter} from "vue-router";

const router = useRouter();

const tabStore = useTabStore();

const collapse = ref(true);

const includeRoutes = computed(() => {
  return tabStore.tabs.map(tab => tab.component).filter(tab => tab);
});

const excludeRoutes = computed((): string[] => {
  return <string[]>router
      .getRoutes()
      .filter(route => !!route.meta?.noKeepAlive)
      .map(route => route.meta.component);
})

// 后台布局销毁即代表退出后台会话，不能把上一个账号的标签页留给下一个账号。
onBeforeUnmount(() => tabStore.reset());

const elMainRef = ref<InstanceType<typeof ElMain>>();

provide('elMain', {elMainRef: elMainRef});


</script>


<style scoped>
.menu {
  background: var(--vertical-menu-color);
}

.app-container { height: 100%; overflow: hidden; background: var(--el-bg-color-page); }
.backend-container, .backend-body { height: 100%; min-width: 0; }
.backend-aside { overflow: hidden; background: var(--vertical-menu-color); transition: width .25s ease; }

.header {
  padding: 0;
  display: flex;
  flex-direction: column;
  flex: 0 0 var(--backend-header-height);
  height: var(--backend-header-height);
  position: relative;
  /* 不抢占 Element Plus Drawer/Dialog 的全局弹层层级。 */
  z-index: 10;
  overflow: visible;
}

.main {
  min-width: 0;
  width: 100%;
  box-sizing: border-box;
  margin: 0;
  padding: 20px 24px 28px;
  overflow: auto;
  background: var(--el-bg-color-page);
}

@media (max-width: 700px) {
  .main { padding: 14px 12px 24px; }
}

</style>
