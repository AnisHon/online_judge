<template>
  <section class="avatar-cutter" aria-label="头像裁剪">
    <input ref="inputRef" class="avatar-cutter__input" type="file" accept="image/gif,image/jpeg,image/png" @change="handleFileChange" />

    <template v-if="imageUrl">
      <div ref="stageRef" class="avatar-cutter__stage">
        <img
          ref="imageRef"
          class="avatar-cutter__image"
          :src="imageUrl"
          :style="imageStyle"
          alt="头像裁剪预览"
          draggable="false"
          @load="handleImageLoad"
          @pointerdown="startPan"
          @pointermove="movePan"
          @pointerup="endPan"
          @pointercancel="endPan"
        />
        <div class="avatar-cutter__frame" aria-hidden="true" />
        <div v-if="!ready" class="avatar-cutter__loading">正在准备图片…</div>
      </div>

      <div v-if="ready" class="avatar-cutter__toolbar">
        <span class="avatar-cutter__zoom-label">缩放</span>
        <input v-model.number="zoom" class="avatar-cutter__range" type="range" min="1" max="3" step="0.01" aria-label="图片缩放" @input="handleZoom" />
        <span class="avatar-cutter__zoom-value">{{ Math.round(zoom * 100) }}%</span>
        <button class="avatar-cutter__icon-button" type="button" title="居中图片" @click="centerImage">
          <el-icon><RefreshLeft /></el-icon>
        </button>
      </div>

      <p class="avatar-cutter__filename" :title="fileName">{{ fileName }}</p>
      <p v-if="errorMessage" class="avatar-cutter__error">{{ errorMessage }}</p>

      <div class="avatar-cutter__actions">
        <button class="avatar-cutter__button avatar-cutter__button--ghost" type="button" @click="chooseAnother">重新选择</button>
        <div class="avatar-cutter__actions-right">
          <button class="avatar-cutter__button avatar-cutter__button--ghost" type="button" @click="cancel">取消</button>
          <button class="avatar-cutter__button avatar-cutter__button--primary" type="button" :disabled="!ready || isCropping" @click="confirmCrop">
            {{ isCropping ? '处理中…' : '使用此头像' }}
          </button>
        </div>
      </div>
    </template>

    <div v-else class="avatar-cutter__empty">
      <div class="avatar-cutter__empty-icon"><el-icon><Picture /></el-icon></div>
      <strong>上传一张头像</strong>
      <span>支持 JPG、PNG、GIF，建议使用清晰的正方形图片</span>
      <button class="avatar-cutter__button avatar-cutter__button--primary" type="button" @click="openFilePicker">选择本地图片</button>
      <p v-if="errorMessage" class="avatar-cutter__error">{{ errorMessage }}</p>
    </div>
  </section>
</template>

<script setup lang="ts">
import {computed, nextTick, onMounted, onUnmounted, ref, watch} from 'vue'
import {Picture, RefreshLeft} from '@element-plus/icons-vue'

const emit = defineEmits<{
  (event: 'cutDown', file: File): void
  (event: 'cancel'): void
}>()

const CROP_OUTPUT_SIZE = 512
const inputRef = ref<HTMLInputElement>()
const imageRef = ref<HTMLImageElement>()
const stageRef = ref<HTMLElement>()
const imageUrl = ref('')
const fileName = ref('')
const errorMessage = ref('')
const ready = ref(false)
const isCropping = ref(false)
const cropSize = ref(320)
const naturalWidth = ref(0)
const naturalHeight = ref(0)
const baseScale = ref(1)
const zoom = ref(1)
const offsetX = ref(0)
const offsetY = ref(0)
const isDragging = ref(false)
const dragStartX = ref(0)
const dragStartY = ref(0)
const dragOriginX = ref(0)
const dragOriginY = ref(0)
let resizeObserver: ResizeObserver | undefined

const scale = computed(() => baseScale.value * zoom.value)
const displayWidth = computed(() => naturalWidth.value * scale.value)
const displayHeight = computed(() => naturalHeight.value * scale.value)
const imageStyle = computed(() => ({
  width: `${displayWidth.value}px`,
  height: `${displayHeight.value}px`,
  transform: `translate3d(${offsetX.value}px, ${offsetY.value}px, 0)`,
}))

const revokeImageUrl = () => {
  if (imageUrl.value) URL.revokeObjectURL(imageUrl.value)
}

const resetCropState = () => {
  revokeImageUrl()
  imageUrl.value = ''
  fileName.value = ''
  errorMessage.value = ''
  ready.value = false
  naturalWidth.value = 0
  naturalHeight.value = 0
  baseScale.value = 1
  zoom.value = 1
  offsetX.value = 0
  offsetY.value = 0
  if (inputRef.value) inputRef.value.value = ''
}

const openFilePicker = () => inputRef.value?.click()

const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  if (!['image/gif', 'image/jpeg', 'image/png'].includes(file.type)) {
    errorMessage.value = '请选择 JPG、PNG 或 GIF 图片。'
    input.value = ''
    return
  }
  if (file.size > 10 * 1024 * 1024) {
    errorMessage.value = '图片不能超过 10MB，请重新选择。'
    input.value = ''
    return
  }

  revokeImageUrl()
  imageUrl.value = URL.createObjectURL(file)
  fileName.value = file.name
  errorMessage.value = ''
  ready.value = false
  isCropping.value = false
  naturalWidth.value = 0
  naturalHeight.value = 0
  zoom.value = 1
  offsetX.value = 0
  offsetY.value = 0
}

const clampPosition = (x: number, y: number) => ({
  x: Math.min(0, Math.max(cropSize.value - displayWidth.value, x)),
  y: Math.min(0, Math.max(cropSize.value - displayHeight.value, y)),
})

const centerImage = () => {
  if (!naturalWidth.value || !naturalHeight.value) return
  const position = clampPosition(
    (cropSize.value - displayWidth.value) / 2,
    (cropSize.value - displayHeight.value) / 2,
  )
  offsetX.value = position.x
  offsetY.value = position.y
}

const handleImageLoad = () => {
  const image = imageRef.value
  if (!image?.naturalWidth || !image.naturalHeight) return
  naturalWidth.value = image.naturalWidth
  naturalHeight.value = image.naturalHeight
  baseScale.value = Math.max(cropSize.value / image.naturalWidth, cropSize.value / image.naturalHeight)
  zoom.value = 1
  centerImage()
  ready.value = true
}

const handleStageResize = () => {
  const width = stageRef.value?.clientWidth
  if (!width || Math.abs(width - cropSize.value) < 1) return
  cropSize.value = width
  if (ready.value) {
    baseScale.value = Math.max(cropSize.value / naturalWidth.value, cropSize.value / naturalHeight.value)
    centerImage()
  }
}

const handleZoom = () => {
  if (!ready.value) return
  const oldScale = scale.value
  const focalX = (cropSize.value / 2 - offsetX.value) / oldScale
  const focalY = (cropSize.value / 2 - offsetY.value) / oldScale
  const nextScale = baseScale.value * zoom.value
  const position = clampPosition(
    cropSize.value / 2 - focalX * nextScale,
    cropSize.value / 2 - focalY * nextScale,
  )
  offsetX.value = position.x
  offsetY.value = position.y
}

const startPan = (event: PointerEvent) => {
  if (!ready.value) return
  event.preventDefault()
  isDragging.value = true
  dragStartX.value = event.clientX
  dragStartY.value = event.clientY
  dragOriginX.value = offsetX.value
  dragOriginY.value = offsetY.value
  imageRef.value?.setPointerCapture(event.pointerId)
}

const movePan = (event: PointerEvent) => {
  if (!isDragging.value) return
  const position = clampPosition(
    dragOriginX.value + event.clientX - dragStartX.value,
    dragOriginY.value + event.clientY - dragStartY.value,
  )
  offsetX.value = position.x
  offsetY.value = position.y
}

const endPan = (event: PointerEvent) => {
  if (!isDragging.value) return
  isDragging.value = false
  if (imageRef.value?.hasPointerCapture(event.pointerId)) imageRef.value.releasePointerCapture(event.pointerId)
}

const confirmCrop = () => {
  const image = imageRef.value
  if (!ready.value || !image || isCropping.value) return
  isCropping.value = true

  const canvas = document.createElement('canvas')
  canvas.width = CROP_OUTPUT_SIZE
  canvas.height = CROP_OUTPUT_SIZE
  const context = canvas.getContext('2d')
  if (!context) {
    isCropping.value = false
    errorMessage.value = '当前浏览器无法处理图片，请重试。'
    return
  }

  const sourceSize = Math.min(naturalWidth.value, naturalHeight.value, cropSize.value / scale.value)
  const sourceX = Math.max(0, Math.min(naturalWidth.value - sourceSize, -offsetX.value / scale.value))
  const sourceY = Math.max(0, Math.min(naturalHeight.value - sourceSize, -offsetY.value / scale.value))
  context.imageSmoothingEnabled = true
  context.imageSmoothingQuality = 'high'
  context.drawImage(image, sourceX, sourceY, sourceSize, sourceSize, 0, 0, CROP_OUTPUT_SIZE, CROP_OUTPUT_SIZE)

  canvas.toBlob((blob) => {
    isCropping.value = false
    if (!blob) {
      errorMessage.value = '图片裁剪失败，请重试。'
      return
    }
    const outputName = fileName.value.replace(/\.[^.]+$/, '') || 'avatar'
    emit('cutDown', new File([blob], `${outputName}.png`, {type: 'image/png'}))
  }, 'image/png', 0.92)
}

const chooseAnother = () => {
  resetCropState()
  openFilePicker()
}

const cancel = () => {
  resetCropState()
  emit('cancel')
}

const observeStage = async () => {
  await nextTick()
  if (!stageRef.value || !resizeObserver) return
  resizeObserver.observe(stageRef.value)
  handleStageResize()
}

watch(imageUrl, (value) => {
  if (value) observeStage()
})

onMounted(() => {
  resizeObserver = new ResizeObserver(handleStageResize)
  if (stageRef.value) resizeObserver.observe(stageRef.value)
})

onUnmounted(() => {
  resizeObserver?.disconnect()
  revokeImageUrl()
})
</script>

<style scoped>
.avatar-cutter {
  --cutter-surface: var(--el-bg-color, #fff);
  --cutter-page: var(--el-bg-color-page, #f7f8fa);
  --cutter-border: var(--el-border-color, #dcdfe6);
  --cutter-text: var(--el-text-color-primary, #303133);
  --cutter-muted: var(--el-text-color-secondary, #909399);
  --cutter-primary: var(--el-color-primary, #409eff);
  width: 100%;
  color: var(--cutter-text);
}
.avatar-cutter__input { display: none; }
.avatar-cutter__empty { display: flex; min-height: 210px; align-items: center; justify-content: center; gap: 8px; padding: 24px 16px; border: 1px dashed var(--cutter-border); border-radius: 14px; background: var(--cutter-page); text-align: center; flex-direction: column; }
.avatar-cutter__empty strong { font-size: 14px; }
.avatar-cutter__empty span { max-width: 290px; color: var(--cutter-muted); font-size: 12px; line-height: 1.6; }
.avatar-cutter__empty-icon { display: grid; width: 48px; height: 48px; margin-bottom: 4px; place-items: center; border-radius: 14px; color: var(--cutter-primary); background: color-mix(in srgb, var(--cutter-primary) 12%, transparent); font-size: 22px; }
.avatar-cutter__stage { position: relative; width: min(100%, 320px); aspect-ratio: 1; margin: 0 auto; overflow: hidden; border-radius: 14px; background: #16181d; box-shadow: inset 0 0 0 1px rgb(255 255 255 / 10%); touch-action: none; user-select: none; }
.avatar-cutter__image { position: absolute; top: 0; left: 0; max-width: none; cursor: grab; object-fit: fill; transform-origin: 0 0; user-select: none; will-change: transform; }
.avatar-cutter__image:active { cursor: grabbing; }
.avatar-cutter__frame { position: absolute; inset: 0; pointer-events: none; box-shadow: inset 0 0 0 1px rgb(255 255 255 / 70%), inset 0 0 0 999px rgb(0 0 0 / 12%); }
.avatar-cutter__frame::after { position: absolute; inset: 10px; border: 1px solid rgb(255 255 255 / 35%); border-radius: 10px; content: ''; }
.avatar-cutter__loading { position: absolute; inset: 0; display: grid; place-items: center; color: #fff; background: rgb(0 0 0 / 35%); font-size: 12px; }
.avatar-cutter__toolbar { display: flex; align-items: center; gap: 10px; margin-top: 14px; }
.avatar-cutter__zoom-label, .avatar-cutter__zoom-value { color: var(--cutter-muted); font-size: 12px; white-space: nowrap; }
.avatar-cutter__range { min-width: 0; flex: 1; accent-color: var(--cutter-primary); }
.avatar-cutter__icon-button { display: inline-grid; width: 30px; height: 30px; padding: 0; place-items: center; border: 1px solid var(--cutter-border); border-radius: 8px; color: var(--cutter-muted); background: var(--cutter-surface); cursor: pointer; }
.avatar-cutter__icon-button:hover { color: var(--cutter-primary); border-color: var(--cutter-primary); }
.avatar-cutter__filename { overflow: hidden; margin: 9px 0 0; color: var(--cutter-muted); font-size: 11px; text-align: center; text-overflow: ellipsis; white-space: nowrap; }
.avatar-cutter__error { margin: 8px 0 0; color: var(--el-color-danger, #f56c6c); font-size: 12px; text-align: center; }
.avatar-cutter__actions { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-top: 16px; }
.avatar-cutter__actions-right { display: flex; gap: 8px; }
.avatar-cutter__button { min-height: 34px; padding: 0 14px; border: 1px solid transparent; border-radius: 9px; font-size: 12px; font-weight: 500; cursor: pointer; transition: border-color .2s, background-color .2s, color .2s, opacity .2s; }
.avatar-cutter__button:disabled { cursor: not-allowed; opacity: .55; }
.avatar-cutter__button--ghost { border-color: var(--cutter-border); color: var(--cutter-text); background: var(--cutter-surface); }
.avatar-cutter__button--ghost:hover { color: var(--cutter-primary); border-color: var(--cutter-primary); }
.avatar-cutter__button--primary { color: #fff; background: var(--cutter-primary); box-shadow: 0 5px 12px color-mix(in srgb, var(--cutter-primary) 22%, transparent); }
.avatar-cutter__button--primary:hover:not(:disabled) { filter: brightness(1.05); }
@media (max-width: 420px) {
  .avatar-cutter__actions { align-items: stretch; flex-direction: column; }
  .avatar-cutter__actions-right { justify-content: flex-end; }
}
</style>
