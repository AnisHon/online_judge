<template>
  <div class="layout">
    <el-container class="layout-container">
      <el-header class="header" >
        <MenuBar/>
      </el-header>
      <el-scrollbar class="content-scroll" height="var(--content-height)">
        <el-main ref="elMainRef" id="main-box" class="main-content">
            <div class="main-content__inner">
              <router-view v-slot="{Component}">

                <transition name="el-zoom-in-top" mode="out-in">
                  <keep-alive include="Home,ProblemSet,Contest,List,Homework,Solution">
                    <component :is="Component" />
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
import {provide, ref} from "vue";
import {type ElMain} from "element-plus"
//@ts-ignore
import FloatingBall from "@/layout/component/FloatingBall/index.vue";
const elMainRef = ref<InstanceType<typeof ElMain>>();
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
  z-index: 2000;
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
