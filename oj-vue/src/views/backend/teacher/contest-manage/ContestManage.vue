<template>
  <ContestManagement :mode="ContestType.CONTEST" />
</template>

<script setup lang="ts">
import ContestManagement from "@/views/backend/teacher/ContestManagement.vue";
import {ContestType} from "@/api/contest";
</script>
