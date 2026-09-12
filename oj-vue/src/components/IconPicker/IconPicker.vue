<template>
  <el-popover v-model:visible="visible" placement="bottom-start" :width="430" trigger="click" popper-class="resource-icon-picker-popper">
    <template #reference>
      <button type="button" class="icon-picker-trigger" :disabled="props.disabled">
        <span class="icon-picker-trigger__visual"><el-icon v-if="selectedIcon"><component :is="selectedIcon" /></el-icon><span v-else>—</span></span>
        <span class="icon-picker-trigger__copy"><strong>{{ selectedName }}</strong><small>点击选择图标</small></span>
        <el-icon class="icon-picker-trigger__arrow"><ArrowDown /></el-icon>
      </button>
    </template>

    <div class="icon-picker-popover">
      <div class="icon-picker-popover__heading"><div><strong>选择菜单图标</strong><small>从当前项目已支持的 Element Plus 图标中选择</small></div><span>{{ filteredIcons.length }} / {{ iconCatalog.length }}</span></div>
      <el-input v-model="keyword" clearable :prefix-icon="Search" placeholder="搜索图标名称，例如 User、Setting" />
      <div class="icon-picker-popover__grid">
        <button type="button" class="icon-picker-tile" :class="{ 'is-active': props.modelValue === '#' }" @click="selectIcon('#')"><span class="icon-picker-tile__visual">—</span><span class="icon-picker-tile__name">无图标</span></button>
        <button v-for="item in filteredIcons" :key="item.name" type="button" class="icon-picker-tile" :class="{ 'is-active': props.modelValue === item.name }" :title="item.name" @click="selectIcon(item.name)"><span class="icon-picker-tile__visual"><el-icon><component :is="item.component" /></el-icon></span><span class="icon-picker-tile__name">{{ item.name }}</span></button>
        <div v-if="!filteredIcons.length" class="icon-picker-popover__empty">没有匹配的图标</div>
      </div>
    </div>
  </el-popover>
</template>

<script setup lang="ts">
import { computed, markRaw, ref } from 'vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { ArrowDown, Search } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{ modelValue?: string; disabled?: boolean }>(), { modelValue: '#' })
const emit = defineEmits<{ (event: 'update:modelValue', value: string): void }>()

const visible = ref(false)
const keyword = ref('')
const iconCatalog = Object.entries(ElementPlusIconsVue)
  .filter(([name]) => name !== 'default')
  .sort(([a], [b]) => a.localeCompare(b))
  .map(([name, component]) => ({ name, component: markRaw(component) }))
const iconMap = new Map(iconCatalog.map(item => [item.name, item.component]))
const filteredIcons = computed(() => {
  const value = keyword.value.trim().toLowerCase()
  return value ? iconCatalog.filter(item => item.name.toLowerCase().includes(value)) : iconCatalog
})
const selectedIcon = computed(() => props.modelValue && props.modelValue !== '#' ? iconMap.get(props.modelValue) : undefined)
const selectedName = computed(() => props.modelValue && props.modelValue !== '#' ? props.modelValue : '无图标')

function selectIcon(name: string) {
  emit('update:modelValue', name)
  visible.value = false
  keyword.value = ''
}
</script>

<style lang="scss" scoped>
.icon-picker-trigger { display: flex; width: 100%; min-height: 52px; align-items: center; gap: 10px; padding: 7px 10px; border: 1px solid var(--el-border-color); border-radius: 10px; background: var(--el-bg-color); color: var(--el-text-color-primary); cursor: pointer; text-align: left; transition: border-color .18s ease, background-color .18s ease; }.icon-picker-trigger:hover:not(:disabled) { border-color: #8b5cf6; background: var(--el-fill-color-light); }.icon-picker-trigger:disabled { cursor: not-allowed; opacity: .6; }.icon-picker-trigger__visual { display: grid; width: 34px; height: 34px; flex: 0 0 auto; place-items: center; border-radius: 9px; background: rgb(139 92 246 / 12%); color: #8b5cf6; font-size: 18px; }.icon-picker-trigger__copy { min-width: 0; flex: 1; }.icon-picker-trigger__copy strong, .icon-picker-trigger__copy small { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.icon-picker-trigger__copy strong { font-size: 12px; }.icon-picker-trigger__copy small { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 10px; }.icon-picker-trigger__arrow { color: var(--el-text-color-placeholder); font-size: 13px; }
.icon-picker-popover__heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 11px; }.icon-picker-popover__heading strong, .icon-picker-popover__heading small { display: block; }.icon-picker-popover__heading strong { font-size: 13px; }.icon-picker-popover__heading small { margin-top: 3px; color: var(--el-text-color-secondary); font-size: 10px; }.icon-picker-popover__heading > span { color: var(--el-text-color-secondary); font: 11px var(--code-font-family, monospace); white-space: nowrap; }.icon-picker-popover__grid { display: grid; max-height: 286px; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 7px; overflow-y: auto; margin-top: 10px; padding: 2px; }.icon-picker-tile { display: flex; min-height: 68px; align-items: center; justify-content: center; padding: 7px 4px; border: 1px solid var(--el-border-color-lighter); border-radius: 9px; background: var(--el-bg-color); color: var(--el-text-color-secondary); cursor: pointer; flex-direction: column; gap: 5px; transition: all .16s ease; }.icon-picker-tile:hover { border-color: rgb(139 92 246 / 55%); color: #8b5cf6; transform: translateY(-1px); }.icon-picker-tile.is-active { border-color: #8b5cf6; background: rgb(139 92 246 / 11%); box-shadow: 0 0 0 2px rgb(139 92 246 / 12%); color: #8b5cf6; }.icon-picker-tile__visual { display: grid; height: 23px; place-items: center; font-size: 19px; }.icon-picker-tile__name { max-width: 100%; overflow: hidden; font-size: 10px; line-height: 1.2; text-overflow: ellipsis; white-space: nowrap; }.icon-picker-popover__empty { grid-column: 1 / -1; padding: 28px 0; color: var(--el-text-color-secondary); font-size: 12px; text-align: center; }
</style>
