import {onBeforeUnmount, ref} from 'vue'
import getCaptcha from '@/api/auth/captchaCode'

interface CaptchaForm {
  token: string
}

/**
 * 图形验证码状态机：刷新期间废弃旧 token，失败时不会把旧 token 留给下一次请求。
 */
export function useCaptchaCode(form: CaptchaForm) {
  const image = ref('')
  const loading = ref(false)
  const error = ref('')
  let requestId = 0

  const refresh = async () => {
    const currentRequestId = ++requestId
    loading.value = true
    error.value = ''
    image.value = ''
    form.token = ''

    try {
      const result = await getCaptcha()
      if (currentRequestId !== requestId) return false
      image.value = result.image
      form.token = result.token
      return true
    } catch {
      if (currentRequestId !== requestId) return false
      error.value = '验证码加载失败，请点击图片重试'
      return false
    } finally {
      if (currentRequestId === requestId) loading.value = false
    }
  }

  onBeforeUnmount(() => {
    requestId++
  })

  return {image, loading, error, refresh}
}
