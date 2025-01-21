<script setup lang="ts">
import {ref} from "vue";
import __ from "lodash";

const text = ref("复制")

const emit = defineEmits<{(e: "copy"): void}>();

const restore = __.debounce(() => {
  text.value = "复制";
}, 2000);
const handleClick = () => {
  text.value = "复制成功";
  restore();
  emit("copy");
}


</script>

<template>

  <el-link style="user-select: none" type="info" @click.stop="handleClick" v-text="text"></el-link>

</template>

<style scoped>

</style>