import {computed, onBeforeUnmount, ref} from 'vue'

const COOLDOWN_SECONDS = 60

/** 防止同一个认证页面重复发送邮箱验证码。 */
export function useEmailCodeCooldown() {
  const sending = ref(false)
  const remaining = ref(0)
  let timer: ReturnType<typeof setInterval> | undefined

  const label = computed(() => remaining.value > 0 ? `${remaining.value}s 后重新获取` : '获取邮箱验证码')

  const clearTimer = () => {
    if (timer !== undefined) {
      clearInterval(timer)
      timer = undefined
    }
  }

  const run = async (request: () => Promise<unknown>) => {
    if (sending.value || remaining.value > 0) return false
    sending.value = true
    try {
      await request()
      remaining.value = COOLDOWN_SECONDS
      clearTimer()
      timer = setInterval(() => {
        remaining.value -= 1
        if (remaining.value <= 0) {
          remaining.value = 0
          clearTimer()
        }
      }, 1000)
      return true
    } finally {
      sending.value = false
    }
  }

  onBeforeUnmount(clearTimer)

  return {sending, remaining, label, run}
}
