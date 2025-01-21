<template>
  <div class="layout">
    <el-container style="height: 100%;">
      <el-header class="header" >
        <MenuBar/>
      </el-header>
      <el-scrollbar height="var(--content-height)">
        <el-main ref="elMainRef" id="main-box" style="box-sizing: border-box;">
            <div>
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
}


.header {
  padding: 0;
  height: var(--menu-height);
  display: flex;
}

</style>

<style>
</style>