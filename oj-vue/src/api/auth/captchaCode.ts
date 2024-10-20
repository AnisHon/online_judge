import {get} from "@/utils/http";

export default async function () {

    const {data: {image, token}} = await get("/user-api/auth/captcha-code")
    return {image, token}
}