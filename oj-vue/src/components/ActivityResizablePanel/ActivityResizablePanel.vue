<template>
  <div
    ref="rootRef"
    class="activity-resizable-panel"
    :class="{ 'is-collapsed': collapsed, 'is-dragging': dragging }"
    :style="{ '--activity-sidebar-size': `${panelSize}%` }"
  >
    <aside class="activity-resizable-panel__sidebar">
      <slot name="sidebar" />
    </aside>

    <div
      v-if="!collapsed"
      ref="handleRef"
      class="activity-resizable-panel__handle"
      role="separator"
      aria-label="调整题目导航宽度"
      aria-orientation="vertical"
      @pointerdown.prevent="startDragging"
    >
      <span />
    </div>

    <section class="activity-resizable-panel__main">
      <button
        v-if="collapsed"
        class="activity-resizable-panel__restore"
        type="button"
        title="显示题目导航"
        @click="restore"
      >
        <el-icon><ArrowRight /></el-icon>
      </button>
      <slot />
    </section>
  </div>
</template>

<script setup lang="ts">
import {ref, watch} from 'vue'
import {ArrowRight} from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  size?: number
  collapsed?: boolean
  minSize?: number
  maxSize?: number
}>(), {
  size: 25,
  collapsed: false,
  minSize: 20,
  maxSize: 36,
})

const emit = defineEmits<{
  'update:size': [value: number]
  'update:collapsed': [value: boolean]
}>()

const rootRef = ref<HTMLElement>()
const handleRef = ref<HTMLElement>()
const dragging = ref(false)
const panelSize = ref(clamp(props.size))

function clamp(value: number) {
  return Math.min(Math.max(value, props.minSize), props.maxSize)
}

watch(() => props.size, value => {
  panelSize.value = clamp(value)
})

function startDragging(event: PointerEvent) {
  if (props.collapsed || !rootRef.value || !handleRef.value) return

  const bounds = rootRef.value.getBoundingClientRect()
  if (bounds.width <= 0) return

  dragging.value = true
  const startX = event.clientX
  const startSize = panelSize.value
  handleRef.value.setPointerCapture?.(event.pointerId)

  const move = (moveEvent: PointerEvent) => {
    const nextSize = clamp(startSize + ((moveEvent.clientX - startX) / bounds.width) * 100)
    panelSize.value = nextSize
    emit('update:size', nextSize)
  }

  const stop = () => {
    dragging.value = false
    handleRef.value?.removeEventListener('pointermove', move)
    handleRef.value?.removeEventListener('pointerup', stop)
    handleRef.value?.removeEventListener('pointercancel', stop)
  }

  handleRef.value.addEventListener('pointermove', move)
  handleRef.value.addEventListener('pointerup', stop)
  handleRef.value.addEventListener('pointercancel', stop)
}

function restore() {
  emit('update:collapsed', false)
}
</script>

<style scoped>
.activity-resizable-panel {
  display: flex;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  min-width: 0;
  min-height: 0;
  max-height: 100%;
  flex-direction: row;
  overflow: hidden;
  background: var(--el-bg-color-page);
}

.activity-resizable-panel__sidebar {
  width: var(--activity-sidebar-size);
  height: 100%;
  min-width: 0;
  min-height: 0;
  flex: 0 0 var(--activity-sidebar-size);
  overflow: hidden;
  transition: width .22s ease, flex-basis .22s ease;
}

.activity-resizable-panel__handle {
  display: flex;
  width: 14px;
  height: 100%;
  min-height: 0;
  flex: 0 0 14px;
  align-items: center;
  justify-content: center;
  cursor: col-resize;
  touch-action: none;
}

.activity-resizable-panel__handle span {
  display: block;
  width: 4px;
  height: 54px;
  border: 1px solid var(--el-border-color);
  border-radius: 99px;
  background: var(--el-fill-color);
  transition: height .18s ease, background-color .18s ease, border-color .18s ease;
}

.activity-resizable-panel__handle:hover span,
.activity-resizable-panel.is-dragging .activity-resizable-panel__handle span {
  height: 78px;
  border-color: var(--el-color-primary-light-5);
  background: var(--el-color-primary-light-8);
}

.activity-resizable-panel__main {
  position: relative;
  width: auto;
  height: 100%;
  min-width: 0;
  min-height: 0;
  flex: 1 1 auto;
  overflow: hidden;
}

.activity-resizable-panel.is-collapsed .activity-resizable-panel__sidebar {
  width: 0;
  flex-basis: 0;
}

.activity-resizable-panel.is-collapsed .activity-resizable-panel__main {
  padding-left: 40px;
}

.activity-resizable-panel__restore {
  position: absolute;
  z-index: 2;
  top: 50%;
  left: 8px;
  display: grid;
  width: 30px;
  height: 38px;
  place-items: center;
  border: 1px solid color-mix(in srgb, var(--el-border-color-light) 72%, transparent);
  border-radius: 9px;
  background: color-mix(in srgb, var(--el-bg-color) 84%, transparent);
  color: var(--el-color-primary);
  box-shadow: 0 4px 14px rgb(15 23 42 / 8%);
  cursor: pointer;
  opacity: .34;
  transform: translateY(-50%);
  transition: opacity .18s ease, transform .18s ease, background-color .18s ease;
}

.activity-resizable-panel__restore:hover,
.activity-resizable-panel__restore:focus-visible {
  background: var(--el-fill-color-light);
  opacity: 1;
  transform: translate(2px, -50%);
}

@media (max-width: 700px) {
  .activity-resizable-panel__handle {
    width: 12px;
    flex-basis: 12px;
  }

  .activity-resizable-panel__handle span {
    width: 3px;
  }
}
</style>
