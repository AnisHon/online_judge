import {get} from "@/utils/http";


interface CaptchaCode {
    image: string;
    token: string;
}

export default async function () {

    const {data: {image, token}} = await get<CaptchaCode>("/user-api/auth/captcha-code")
    return {image, token}
}