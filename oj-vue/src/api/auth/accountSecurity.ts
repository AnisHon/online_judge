import {post, put} from '@/utils/http'

export type SensitiveAction = 'PASSWORD' | 'EMAIL'
export type VerificationMethod = 'PASSWORD' | 'EMAIL'
interface CaptchaInput { captchaToken: string; captchaCode: string }
export interface StepUpGrant { stepUpToken: string; expiresIn: number }
const handledByForm = () => {}

export async function verifySensitiveAction(input: {
  action: SensitiveAction; method: VerificationMethod; password?: string; code?: string
}): Promise<StepUpGrant> {
  const {data} = await post<typeof input, StepUpGrant>('/user-api/auth/step-up/verify', input, handledByForm)
  if (!data?.stepUpToken || !data.expiresIn) throw new Error('身份验证失败，请重试')
  return data
}

export async function sendIdentityCode(input: CaptchaInput & {action: SensitiveAction}) {
  await post('/user-api/auth/step-up/email-code', input, handledByForm)
}

export async function sendNewEmailCode(input: CaptchaInput & {stepUpToken: string; newEmail: string}) {
  await post('/user-api/auth/step-up/new-email-code', input, handledByForm)
}

interface SecurityResult { success: boolean; message: string }
export async function changePassword(input: {stepUpToken: string; password: string}) {
  const {data} = await put<typeof input, SecurityResult>('/user-api/auth/reset-pass', input, handledByForm)
  if (!data?.success) throw new Error(data?.message || '密码更新失败')
}

export async function changeEmail(input: {stepUpToken: string; newEmail: string; newEmailCode: string}) {
  const {data} = await put<typeof input, SecurityResult>('/user-api/auth/reset-email', input, handledByForm)
  if (!data?.success) throw new Error(data?.message || '邮箱更新失败')
}
