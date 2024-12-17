<template>
  <div>
    {{ msg }}
  </div>
</template>

<script setup lang="ts">
  import {ref} from "vue";

  const msg = ref("123")
  const update = (text: string) => {
    msg.value = text
  }

  defineExpose({"update": update})
</script>



<style scoped>

</style>