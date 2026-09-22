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
      :aria-valuemin="minSize"
      :aria-valuemax="maxSize"
      :aria-valuenow="Math.round(panelSize)"
      :aria-expanded="!collapsed"
      tabindex="0"
      @pointerdown.prevent="startDragging"
      @keydown="handleKeydown"
    >
      <span />
    </div>

    <section class="activity-resizable-panel__main">
      <button
        v-if="collapsed"
        class="activity-resizable-panel__restore"
        type="button"
        title="显示题目导航"
        aria-label="显示题目导航"
        aria-expanded="true"
        @click="restore"
      >
        <el-icon><ArrowRight /></el-icon>
      </button>
      <slot />
    </section>
  </div>
</template>

<script setup lang="ts">
import {onBeforeUnmount, ref, watch} from 'vue'
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
let cleanupDragging: (() => void) | undefined

function clamp(value: number) {
  return Math.min(Math.max(value, props.minSize), props.maxSize)
}

watch(() => props.size, value => {
  panelSize.value = clamp(value)
})

function startDragging(event: PointerEvent) {
  if (props.collapsed || !rootRef.value || !handleRef.value) return
  cleanupDragging?.()

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
    window.removeEventListener('pointermove', move)
    window.removeEventListener('pointerup', stop)
    window.removeEventListener('pointercancel', stop)
    handleRef.value?.removeEventListener('lostpointercapture', stop)
    cleanupDragging = undefined
  }

  cleanupDragging = stop
  window.addEventListener('pointermove', move)
  window.addEventListener('pointerup', stop)
  window.addEventListener('pointercancel', stop)
  handleRef.value.addEventListener('lostpointercapture', stop, {once: true})
}

function handleKeydown(event: KeyboardEvent) {
  const step = event.shiftKey ? 5 : 2
  let delta = 0
  if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') delta = -step
  if (event.key === 'ArrowRight' || event.key === 'ArrowDown') delta = step
  if (event.key === 'Home') delta = props.minSize - panelSize.value
  if (event.key === 'End') delta = props.maxSize - panelSize.value
  if (!delta) return
  event.preventDefault()
  const next = clamp(panelSize.value + delta)
  panelSize.value = next
  emit('update:size', next)
}

function restore() {
  emit('update:collapsed', false)
}

onBeforeUnmount(() => cleanupDragging?.())
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
  /* 恢复按钮是绝对定位控件，不参与布局；隐藏后主区域必须完整占满宽度。 */
  padding-left: 0;
}

.activity-resizable-panel__restore {
  position: absolute;
  z-index: 2;
  top: 50%;
  left: 8px;
  display: grid;
  width: 28px;
  height: 34px;
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

@media (max-width: 760px) {
  .activity-resizable-panel {
    position: relative;
    display: block;
  }

  .activity-resizable-panel__sidebar {
    position: absolute;
    z-index: 3;
    top: 0;
    bottom: 0;
    left: 0;
    width: min(88vw, 360px);
    max-width: 100%;
    height: 100%;
    flex: none;
    box-shadow: 12px 0 30px rgb(15 23 42 / 14%);
    transform: translateX(0);
    transition: transform .22s ease;
  }

  .activity-resizable-panel.is-collapsed .activity-resizable-panel__sidebar {
    width: min(88vw, 360px);
    flex-basis: auto;
    transform: translateX(-105%);
  }

  .activity-resizable-panel__handle {
    display: none;
  }

  .activity-resizable-panel__main,
  .activity-resizable-panel.is-collapsed .activity-resizable-panel__main {
    width: 100%;
    height: 100%;
    padding-left: 0;
  }

  .activity-resizable-panel__restore {
    top: 12px;
    left: 12px;
    transform: none;
  }

  .activity-resizable-panel__restore:hover,
  .activity-resizable-panel__restore:focus-visible {
    transform: translateX(2px);
  }
}
</style>
