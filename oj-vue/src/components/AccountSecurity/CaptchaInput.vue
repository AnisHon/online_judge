<template>
  <div class="captcha-field">
    <div class="captcha-field__row">
      <el-input v-model="code" placeholder="图形验证码" autocomplete="off" aria-label="图形验证码" />
      <button type="button" class="captcha-field__image" :disabled="loading" aria-label="刷新图形验证码" @click="void refresh()">
        <img v-if="image" :src="image" alt="图形验证码" />
        <span v-else>{{ loading ? '加载中' : '点击刷新' }}</span>
      </button>
    </div>
    <span v-if="error" class="captcha-field__error" role="alert">{{ error }}</span>
  </div>
</template>

<script setup lang="ts">
import {onMounted, reactive} from 'vue'
import {useCaptchaCode} from '@/composables/auth/useCaptchaCode'
const code = defineModel<string>('code', {default: ''})
const token = defineModel<string>('token', {default: ''})
const captcha = reactive({get token() { return token.value }, set token(value: string) { token.value = value }})
const {image, loading, error, refresh: reload} = useCaptchaCode(captcha)
const refresh = async () => { code.value = ''; return reload() }
onMounted(() => { void refresh() })
defineExpose({refresh})
</script>

<style scoped>
.captcha-field { width: 100%; min-width: 0; }
.captcha-field__row { display: flex; align-items: stretch; gap: 12px; min-width: 0; }
.captcha-field__row > .el-input { flex: 1; min-width: 0; }
.captcha-field__image { flex: 0 0 112px; height: 40px; padding: 0; overflow: hidden; border: 1px solid var(--el-border-color); border-radius: 8px; background: var(--el-fill-color-light); color: var(--el-text-color-secondary); cursor: pointer; }
.captcha-field__image img { width: 100%; height: 100%; object-fit: contain; }
.captcha-field__error { display: block; margin-top: 6px; color: var(--el-color-danger); font-size: 12px; }
</style>
