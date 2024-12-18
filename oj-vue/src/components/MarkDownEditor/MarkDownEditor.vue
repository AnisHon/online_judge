<template>
  <MdEditor v-model="text" :theme="theme" :previewTheme="previewTheme" :codeTheme="codeTheme" />
</template>
<script setup lang="ts">
import { MdEditor } from 'md-editor-v3';
import 'md-editor-v3/lib/style.css';
import {computed} from "vue";
import {useDark} from "@vueuse/core";
import useConfig from "@/stores/useConfig.ts";

const config = useConfig();

const text = defineModel<string | undefined>();

const isDark = useDark();

const theme = computed(() => {
  return isDark.value ? "dark" : "light";
})
const codeTheme = computed(() => {
  return config.readonly.codeTheme || "atom";
})



const previewTheme = computed(() => {
  return config.readonly.previewTheme || "default";
})


</script>

<style scoped>

</style>