<template>
  <custom-card :icon="icon" :title="title" :more="more" @show-more="$emit('showMore')">
    <slot />
  </custom-card>
</template>

<script setup lang="ts">
import CustomCard from "@/components/CustomCard/CustomCard.vue";
defineProps<{ title: string; icon: string; more?: boolean }>();
defineEmits<{ (event: "showMore"): void }>();
</script>
