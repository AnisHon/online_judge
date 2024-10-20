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

export default async function (emailInfo: SendEmail) {

    const {data: {message, success}} = await post("/user-api/auth/send-email-code", emailInfo)

    if (!success) {
        return await Promise.reject(message)
    }
}

export async function sendForgetEmailCode(req: ForgetPassEmailRequest) {
    const {data: {message, success}} = await post("/user-api/auth/send-forget-email-code", req)
    if (!success) {
        return await Promise.reject(message)
    }
}