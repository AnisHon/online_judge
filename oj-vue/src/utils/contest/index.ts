import {ContestAuth} from "@/api/contest";

const authText = (auth: ContestAuth) => {
    const results = ['公开赛', '私有赛', '白名单']
    return results[auth]
}

const authTagType = (auth: ContestAuth) => {
    const results = ['success', 'danger', 'info']
    return results[auth]
}

export {
    authText,
    authTagType
}