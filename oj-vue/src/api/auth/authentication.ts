import {get, post, put} from '@/utils/http'
import {type LoginUser} from "@/stores/useUserStore";
import {useUserStore} from "@/stores/useUserStore";
import {useToken} from "@/stores/useToken";
import router from '@/router'
import {useMenuStore} from "@/stores/useMenuStore";

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

// 认证页面会结合表单状态显示错误，避免 HTTP 层重复弹出同一条提示。
const handledByAuthPage = () => {};

const finishLogin = async (token: string, refreshToken?: string) => {
    const tokenStore = useToken();
    const userStore = useUserStore();
    const menuStore = useMenuStore();
    try {
        // 登录前清理上一个会话的权限和用户请求，避免旧账号的菜单短暂泄漏。
        userStore.clear();
        menuStore.clear();
        tokenStore.setTokens(token, refreshToken);
        await userStore.loadUser();
        // 动态路由统一由路由守卫构建，避免登录流程和路由守卫同时请求、互相覆盖。
        await router.replace({name: "home"});
    } catch (error) {
        tokenStore.clearToken();
        useMenuStore().clear();
        throw new Error(errorMessage(error, "登录成功，但加载账户信息失败，请检查网关与用户服务配置"));
    }
}

async function login(data: LoginForm) {
    const result = await post<LoginForm, LoginResponse>('/user-api/auth/login', data, handledByAuthPage);
    if (!result.data) throw new Error(result.message || "登录响应缺少数据");
    const {message, success, token} = result.data;

    if (success) {
        await finishLogin(result.data.accessToken || token, result.data.refreshToken);
    } else {
        throw new Error(message || "用户名、密码或验证码错误");
    }
}



async function signUp(data: SignUpForm) {
    const result = await post<SignUpForm, LoginResponse>('/user-api/auth/registration', data, handledByAuthPage);
    if (!result.data) throw new Error(result.message || "注册响应缺少数据");
    const {message, success, token} = result.data;
    if (success) {
        await finishLogin(result.data.accessToken || token, result.data.refreshToken);
    } else {
        throw new Error(message || "注册失败，请检查填写内容");
    }
}

async function forgetPassword(data: ForgetPasswordForm) {
    const {data: {success, message}} = await post<ForgetPasswordForm, ForgetPasswordResponse>('/user-api/auth/forget-pass', data, handledByAuthPage);
    if (success) {
        return  message
    } else {
        throw message;
    }
}



async function resetPassword(data: {code: string, password: string}) {
    const param = {code: data.code, password: data.password};
    const {data: r} =
        await put<typeof param, Boolean>('/user-api/auth/reset-pass', param, handledByAuthPage);
    return r;
}

async function logout() {
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
