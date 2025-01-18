<template>
  <div>
    <el-form label-width="100">
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

</style>