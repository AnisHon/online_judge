<template>
  <div class="layout">
    <el-container class="layout-container">
      <el-header class="header" >
        <MenuBar/>
      </el-header>
      <el-scrollbar ref="contentScrollRef" class="content-scroll" height="var(--content-height)">
        <el-main ref="elMainRef" id="main-box" class="main-content">
            <div class="main-content__inner">
              <router-view v-slot="{Component, route}">

                <transition :name="route.meta.disableLayoutTransition ? '' : 'el-zoom-in-top'" mode="out-in">
                  <keep-alive include="Home,ProblemSet,Contest,List,Homework,Solution">
                    <component :is="Component"
                               :key="route.meta.disableLayoutTransition ? route.path : route.fullPath" />
                  </keep-alive>

                </transition>

              </router-view>
            </div>
        </el-main>
      </el-scrollbar>
    </el-container>

    <floating-ball/>
  </div>
</template>

<script setup lang="ts">

import MenuBar from "@/layout/component/menu/Menu.vue";
import {nextTick, onMounted, provide, ref, watch} from "vue";
import {type ElMain, type ScrollbarInstance} from "element-plus"
import {useRoute} from "vue-router";
//@ts-ignore
import FloatingBall from "@/layout/component/FloatingBall/index.vue";
const elMainRef = ref<InstanceType<typeof ElMain>>();
const contentScrollRef = ref<ScrollbarInstance>();
const route = useRoute();

/**
 * 页面滚动实际属于 Layout 里的 ElScrollbar，而不是 window。
 * 路由只替换 scrollbar 内部的内容，因此如果不主动处理，进入下一个
 * 页面时会自然沿用上一个页面的 scrollTop。
 */
const resetContentScroll = async () => {
  await nextTick();
  contentScrollRef.value?.setScrollTop(0);
  contentScrollRef.value?.setScrollLeft(0);
};

watch(() => route.fullPath, (nextPath, previousPath) => {
  if (nextPath !== previousPath) {
    resetContentScroll();
  }
}, {flush: 'post'});

onMounted(resetContentScroll);
provide('elMain', {elMainRef: elMainRef});

</script>

<style scoped>
.layout {
  height: 100%;
  overflow: hidden;
}
.layout-container { height: 100%; }

.header {
  padding: 0;
  height: var(--menu-height);
  display: flex;
  position: relative;
  /* 不创建高层级 stacking context，避免遮住 teleport 到 body 的菜单、抽屉和对话框。 */
  z-index: var(--oj-z-content);
}
.content-scroll { width: 100%; }
.main-content { box-sizing: border-box; min-height: var(--content-height); padding: var(--main-padding); }
.main-content__inner { width: 100%; }
@media (max-width: 650px) {
  .main-content { padding: 12px; }
}

</style>

<style>
</style>
