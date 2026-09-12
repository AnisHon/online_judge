<template>
  <div class="drawer-content">
    <div class="drawer-intro"><span class="intro-icon">Aa</span><div><strong>阅读与代码</strong><p>选择适合你的内容呈现方式</p></div></div>
    <el-form label-position="top">
      <el-form-item label="富文本样式">
        <el-select v-model="config.previewTheme">
          <el-option label="默认" value="default"/>
          <el-option label="GitHub" value="github"/>
          <el-option label="VuePress" value="vuepress"/>
          <el-option label="可爱风" value="mk-cute"/>
          <el-option label="智能蓝" value="smart-blue"/>
          <el-option label="蓝调风" value="mk-cyanosis"/>
        </el-select>
      </el-form-item>
      <el-form-item label="代码样式">
        <el-select v-model="config.codeTheme">
          <el-option label="原子" value="atom"/>
          <el-option label="无障碍" value="a11y"/>
          <el-option label="GitHub" value="github"/>
          <el-option label="渐变" value="gradient"/>
          <el-option label="金比" value="kimbie"/>
          <el-option label="天堂" value="paraiso"/>
          <el-option label="QT Creator" value="qtcreator"/>
          <el-option label="stackoverflow论坛" value="stackoverflow"/>
        </el-select>
      </el-form-item>

    </el-form>
  </div>
</template>

<script setup lang="ts">

import {computed, } from "vue";
import useConfig from "@/stores/useConfig.ts";
import __ from "lodash";

const configHook = useConfig();

const config = computed({
  set(config) {
    console.log(config)
    __.assign(configHook.get, config);
  },
  get() {
    return configHook.get;
  }
})

// created
__.assign(config, configHook.get);



</script>

<style scoped>
.drawer-content { color: var(--el-text-color-primary); }
.drawer-intro { display: flex; align-items: center; gap: 12px; margin-bottom: 24px; padding: 15px; border: 1px solid var(--el-border-color-light); border-radius: 14px; background: var(--el-fill-color-lighter); }
.intro-icon { display: grid; width: 38px; height: 38px; place-items: center; border-radius: 11px; color: #fff; background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-success)); font-size: 16px; font-weight: 800; }
.drawer-intro strong, .drawer-intro p { display: block; }
.drawer-intro p { margin: 4px 0 0; color: var(--el-text-color-secondary); font-size: 12px; }
.drawer-content :deep(.el-form-item__label) { color: var(--el-text-color-regular); font-weight: 600; }
.drawer-content :deep(.el-select__wrapper) { min-height: 44px; border-radius: 11px; }
</style>
