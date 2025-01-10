<template>
  <div class="app-container">
    <el-container style="height: 100%;" >
      <el-aside class="menu vertical-menu-animation" width="auto" >
        <backend-menu :collapse="collapse"/>
      </el-aside>

      <el-container>
        <el-header class="header" height="var(--backend-header-height)">
          <header-bar v-model:collapse="collapse"/>
          <custom-tab/>
        </el-header>

        <el-main ref="elMainRef" id="main-box" class="main">

          <router-view>
            <template v-slot="{Component}">
              <transition name="fade-transform" mode="out-in">
                <keep-alive :max="10">
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
import BackendMenu from "@/components/BackendMenu/BackendMenu.vue";
import {provide, ref} from "vue";
import HeaderBar from "@/components/HeaderBar/HeaderBar.vue";
import type {ElMain} from "element-plus";
import CustomTab from "@/components/CustomTab/CustomTab.vue";

const collapse = ref(true);

const elMainRef = ref<InstanceType<typeof ElMain>>();
provide('elMain', {elMainRef: elMainRef});


</script>


<style scoped>
.menu {
  background: var(--vertical-menu-color);
}

.header {
  padding: 0;
}

.main {

  overflow-x: hidden;

}

</style>