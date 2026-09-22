import {ApiError, get} from "@/utils/http";


interface CaptchaCode {
    image: string;
    token: string;
}

export default async function () {
    const {data} = await get<CaptchaCode | null>("/user-api/auth/captcha-code")
    if (!data?.image || !data.token) {
        throw new ApiError("验证码服务暂不可用，请稍后重试", 502)
    }
    return {image: data.image, token: data.token}
}
