import {get, post, put} from '@/utils/http'
import {type LoginUser} from "@/stores/useUserStore";
import {useUserStore} from "@/stores/useUserStore";
import {useToken} from "@/stores/useToken";
import router from '@/router'
import {useMenuStore} from "@/stores/useMenuStore";
import {closeSse, initSSE, SSE_URL} from "@/utils/sse";

export interface LoginForm {
    captchaCode: string;
    password: string;
    token: string;
    username: string;
}

export interface SignUpForm {
    userName: string;
    nikeName: string;
    password: string;
    code: string
    email: string;
}

export interface ForgetPasswordForm {
    username: string;
    password: string;
    code: string;
}

export interface LoginResponse {
    message: string;
    success: boolean;
    token: string;
    accessToken?: string;
    refreshToken?: string;
    expiresIn?: number;
}

export interface ForgetPasswordResponse {
    message: string;
    success: boolean;
}


async function checkAvailableEmail(email: string) {
    const {data} = await get("/user-api/user/email", email);
    return data;
}

async function checkAvailableUsername(username: string) {
    const {data} = await get("/user-api/user/username", username);
    return data;
}

async function getMe(): Promise<LoginUser> {
    const {data} = await get<LoginUser, any>("/user-api/auth/me");
    return data;
}

const errorMessage = (error: unknown, fallback: string) => {
    if (error instanceof Error && error.message) return error.message;
    if (typeof error === "string" && error) return error;
    if (typeof error === "object" && error && "message" in error) {
        const message = (error as {message?: unknown}).message;
        if (typeof message === "string" && message) return message;
    }
    return fallback;
}

const finishLogin = async (token: string, refreshToken?: string) => {
    const tokenStore = useToken();
    try {
        tokenStore.setTokens(token, refreshToken);
        await useUserStore().loadUser();
        await useMenuStore().getTree();
        initSSE();
        await router.replace({name: "home"});
    } catch (error) {
        tokenStore.clearToken();
        useMenuStore().clear();
        throw new Error(errorMessage(error, "登录成功，但加载账户信息失败，请检查网关与用户服务配置"));
    }
}

async function login(data: LoginForm) {
    const result = await post<LoginForm, LoginResponse>('/user-api/auth/login', data);
    if (!result.data) throw new Error(result.message || "登录响应缺少数据");
    const {message, success, token} = result.data;

    if (success) {
        await finishLogin(result.data.accessToken || token, result.data.refreshToken);
    } else {
        throw new Error(message || "用户名、密码或验证码错误");
    }
}



async function signUp(data: SignUpForm) {
    const result = await post<SignUpForm, LoginResponse>('/user-api/auth/registration', data);
    if (!result.data) throw new Error(result.message || "注册响应缺少数据");
    const {message, success, token} = result.data;
    if (success) {
        await finishLogin(result.data.accessToken || token, result.data.refreshToken);
    } else {
        throw new Error(message || "注册失败，请检查填写内容");
    }
}

async function forgetPassword(data: ForgetPasswordForm) {
    const {data: {success, message}} = await post<ForgetPasswordForm, ForgetPasswordResponse>('/user-api/auth/forget-pass', data);
    if (success) {
        return  message
    } else {
        throw message;
    }
}



async function resetPassword(data: {code: string, password: string}) {
    const param = {code: data.code, password: data.password};
    const {data: r} =
        await put<typeof param, Boolean>('/user-api/auth/reset-pass', param);
    return r;
}

async function logout() {
    closeSse(SSE_URL);
    try {
        await get("/user-api/auth/logout");
    } finally {
        useToken().clearToken();
        useMenuStore().clear();
    }
    router.replace({name: "login"});
}

async function refreshLogin(refreshToken: string) {
    const result = await post<{refreshToken: string}, LoginResponse>('/user-api/auth/refresh', {refreshToken});
    if (!result.data?.accessToken) throw new Error(result.message || '刷新登录状态失败');
    useToken().setTokens(result.data.accessToken, result.data.refreshToken);
    return result.data.accessToken;
}

export {
    login,
    getMe,
    checkAvailableEmail,
    checkAvailableUsername,
    signUp,
    logout,
    forgetPassword,
    resetPassword,
    refreshLogin
}
