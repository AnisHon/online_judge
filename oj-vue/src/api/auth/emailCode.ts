import {post} from "@/utils/http";
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

    const {data: {message, success}} = await post<SendEmail, ForgetPasswordResponse>("/user-api/auth/send-email-code", emailInfo)

    if (!success) {
        throw message;
    }
}

export async function sendForgetEmailCode(req: ForgetPassEmailRequest) {
    const {data: {message, success}} = await post<ForgetPassEmailRequest, ForgetPasswordResponse>("/user-api/auth/send-forget-email-code", req)
    if (!success) {
        throw message;
    }
}
