<template>
  <button
    class="radio"
    type="button"
    :role="role"
    :aria-checked="isSelected"
    :aria-label="`${option} ${content}`"
    @click="handleSelect"
    @keydown.enter.prevent="handleSelect"
    @keydown.space.prevent="handleSelect"
  >
    <span class="circle" :class="{'active': isSelected}" aria-hidden="true">{{ option }}</span>
    <span class="radio-content"><markdown-preview variant="embedded" :text="content" /></span>
  </button>
</template>

<script setup lang="ts">
import MarkdownPreview from '@/components/MarkdownPreview.vue';
import {letterToNumber} from '@/utils/stringUtils';
import type {Answer} from '@/api/problem/judge';
import {computed} from 'vue';

const props = withDefaults(defineProps<{option?: string; content?: string; answers: Answer[]; isMulti?: boolean}>(), {
  option: 'A',
  content: '',
  isMulti: false,
});
const role = computed(() => props.isMulti ? 'checkbox' : 'radio');
const isSelected = computed(() => props.answers.some(item => item.index === letterToNumber(props.option)));
const emit = defineEmits<{(event: 'select', index: number, selected: boolean): void}>();
const handleSelect = () => emit('select', letterToNumber(props.option), isSelected.value);
</script>

<style scoped>
.radio { display: flex; width: 100%; align-items: center; gap: 10px; padding: 10px 12px; border: 1px solid var(--el-border-color-lighter); border-radius: 12px; background: transparent; color: var(--el-text-color-primary); cursor: pointer; text-align: left; transition: border-color .18s, background-color .18s, transform .18s; }
.radio:hover, .radio:focus-visible { border-color: var(--el-color-primary-light-5); background: var(--el-color-primary-light-9); outline: none; }
.radio:active { transform: translateY(1px); }
.circle { display: grid; width: 30px; height: 30px; flex: 0 0 auto; place-items: center; border: 1px solid var(--el-border-color); border-radius: 50%; color: var(--el-text-color-secondary); font-weight: 700; transition: background-color .18s, border-color .18s, color .18s; }
.circle.active { border-color: var(--el-color-primary); background: var(--el-color-primary); color: #fff; }
.radio-content { min-width: 0; flex: 1; }
</style>
