<template>
  <ContestManagement :mode="ContestType.HOMEWORK" />
</template>

<script setup lang="ts">
import ContestManagement from "@/views/backend/teacher/ContestManagement.vue";
import {ContestType} from "@/api/contest";
</script>
