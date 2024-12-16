<template>
  <div>
    <el-form label-width="100">
      <el-form-item label="富文本样式">
        <el-select v-model="config.previewTheme" @change="configHook.setPreviewTheme">
          <el-option label="默认" value="default"/>
          <el-option label="github" value="github"/>
          <el-option label="vuepress" value="vuepress"/>
          <el-option label="清爽卡通" value="mk-cute"/>
          <el-option label="巧蓝" value="smart-blue"/>
          <el-option label="青紫" value="mk-cyanosis"/>
        </el-select>
      </el-form-item>


    </el-form>
  </div>
</template>

<script setup lang="ts">

import {reactive} from "vue";
import useConfig, {type UserConfig} from "@/stores/useConfig.ts";
import __ from "lodash";

const configHook = useConfig();

const config = reactive<UserConfig>({
  previewTheme: "",
})


// created
__.assign(config, configHook.get);



</script>

<style scoped>

</style>