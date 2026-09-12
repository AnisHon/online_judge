<template>
  <div ref="rootRef" class="resizable-panel" :class="{ 'is-collapsed': collapsed, 'is-dragging': dragging }">
    <aside class="resizable-panel__sidebar">
      <slot name="sidebar" />
    </aside>

    <div
      v-if="!collapsed"
      class="resizable-panel__handle"
      role="separator"
      aria-label="调整侧栏宽度"
      aria-orientation="vertical"
      @pointerdown.prevent="startDragging"
    >
      <span />
    </div>

    <section class="resizable-panel__main">
      <button v-if="collapsed" class="resizable-panel__restore" type="button" title="显示题目导航" @click="restore">
        <el-icon><ArrowRight /></el-icon>
      </button>
      <slot />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  size?: number
  collapsed?: boolean
  minSize?: number
  maxSize?: number
}>(), {
  size: 25,
  collapsed: false,
  minSize: 18,
  maxSize: 42,
})

const emit = defineEmits<{
  'update:size': [value: number]
  'update:collapsed': [value: boolean]
}>()

const rootRef = ref<HTMLElement>()
const dragging = ref(false)
const size = ref(clamp(props.size))

function clamp(value: number) {
  return Math.min(Math.max(value, props.minSize), props.maxSize)
}

watch(() => props.size, value => { size.value = clamp(value) })

function startDragging(event: PointerEvent) {
  if (props.collapsed || !rootRef.value) return
  dragging.value = true
  const handle = event.currentTarget as HTMLElement
  handle.setPointerCapture?.(event.pointerId)
  const startX = event.clientX
  const startSize = size.value
  const width = rootRef.value.getBoundingClientRect().width

  const move = (moveEvent: PointerEvent) => {
    const next = clamp(startSize + ((moveEvent.clientX - startX) / width) * 100)
    size.value = next
    emit('update:size', next)
  }
  const stop = () => {
    dragging.value = false
    handle.removeEventListener('pointermove', move)
    handle.removeEventListener('pointerup', stop)
    handle.removeEventListener('pointercancel', stop)
  }
  handle.addEventListener('pointermove', move)
  handle.addEventListener('pointerup', stop)
  handle.addEventListener('pointercancel', stop)
}

function restore() {
  emit('update:collapsed', false)
}
</script>

<style scoped>
.resizable-panel { --panel-size: v-bind('size + "%"'); display: flex; width: 100%; height: 100%; min-width: 0; min-height: 0; overflow: hidden; background: var(--el-bg-color-page); }
.resizable-panel__sidebar { width: var(--panel-size); min-width: 0; flex: 0 0 var(--panel-size); overflow: hidden; transition: flex-basis .22s ease, width .22s ease; }
.resizable-panel__main { position: relative; min-width: 0; min-height: 0; flex: 1; overflow: hidden; }
.is-collapsed .resizable-panel__main { box-sizing: border-box; padding-left: 38px; }
.resizable-panel__handle { display: grid; width: 12px; flex: 0 0 12px; place-items: center; background: transparent; cursor: col-resize; touch-action: none; }
.resizable-panel__handle span { width: 4px; height: 44px; border: 1px solid var(--el-border-color); border-radius: 99px; background: var(--el-fill-color); transition: height .18s ease, background-color .18s ease, border-color .18s ease; }
.resizable-panel__handle:hover span, .is-dragging .resizable-panel__handle span { height: 72px; border-color: var(--el-color-primary-light-5); background: var(--el-color-primary-light-8); }
.is-collapsed .resizable-panel__sidebar { width: 0; flex-basis: 0; }
.resizable-panel__restore { position: absolute; z-index: 2; top: 50%; left: 8px; display: grid; width: 30px; height: 38px; place-items: center; border: 1px solid color-mix(in srgb, var(--el-border-color-light) 72%, transparent); border-radius: 9px; background: color-mix(in srgb, var(--el-bg-color) 82%, transparent); color: var(--el-color-primary); box-shadow: 0 4px 14px rgb(15 23 42 / 8%); cursor: pointer; opacity: .3; transform: translateY(-50%); transition: opacity .18s ease, transform .18s ease, background-color .18s ease; }
.resizable-panel__restore:hover, .resizable-panel__restore:focus-visible { background: var(--el-fill-color-light); opacity: 1; transform: translate(2px, -50%); }
@media (max-width: 700px) { .resizable-panel__handle { width: 8px; flex-basis: 8px; }.resizable-panel__handle span { width: 3px; }.is-collapsed .resizable-panel__main { padding-left: 34px; }.resizable-panel__restore { left: 6px; } }
</style>
