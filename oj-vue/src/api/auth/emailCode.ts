import {ApiError, post} from "@/utils/http";
export interface SendEmail {
    captchaCode: string;
    captchaToken: string;
    email: string;
}

export interface ForgetPassEmailRequest {
    captchaCode: string;
    captchaToken: string;
    username: string;
}

export interface ForgetPasswordResponse {
    success: boolean;
    message: string;
}

export async function sendEmailCodePromise(emailInfo: SendEmail) {
    const {data} = await post<SendEmail, ForgetPasswordResponse | null>("/user-api/auth/send-email-code", emailInfo)
    if (!data) throw new ApiError("邮箱验证码服务暂不可用，请稍后重试", 502)
    if (!data.success) throw new Error(data.message || "邮箱验证码发送失败")
}

export async function sendForgetEmailCode(req: ForgetPassEmailRequest) {
    const {data} = await post<ForgetPassEmailRequest, ForgetPasswordResponse | null>("/user-api/auth/send-forget-email-code", req)
    if (!data) throw new ApiError("邮箱验证码服务暂不可用，请稍后重试", 502)
    if (!data.success) throw new Error(data.message || "邮箱验证码发送失败")
}
