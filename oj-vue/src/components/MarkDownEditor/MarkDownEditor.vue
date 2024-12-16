<template>
  <MdEditor v-model="text" :theme="theme" :previewTheme="previewTheme" />
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



const previewTheme = computed(() => {
  return config.get.previewTheme;
})


</script>

<style scoped>

</style>