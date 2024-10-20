<template>
  <div>
    <RouterView />
  </div>
</template>
<script setup lang="ts">
import axios from "axios";

const a = await axios.get("/api/user-api/test")
console.log(a.data)
</script>