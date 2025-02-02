import {get, post, put} from '@/utils/http'
import {type LoginUser} from "@/stores/useUserStore";
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

const toHome = () => {
    router.replace({name: "home"});
}

async function login(data: LoginForm) {
    const {data: {message, success, token}} = await post<LoginForm, LoginResponse>('/user-api/auth/login', data);

    const tokenStore = useToken();
    if (success) {
        await tokenStore.setToken(token);
        await useMenuStore().getTree()
        initSSE();
        toHome();
    } else {
        throw message;
    }
}



async function signUp(data: SignUpForm) {
    const {data: {message, success, token}} = await post<SignUpForm, LoginResponse>('/user-api/auth/registration', data);
    const tokenStore = useToken();
    if (success) {
        tokenStore.setToken(token);
        toHome()
    } else {
        throw message;
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
    await get("/user-api/auth/logout");
    // const router = useRouter();
    useToken().clearToken();
    useMenuStore().clear();
    router.replace({name: "login"});
}

export {
    login,
    getMe,
    checkAvailableEmail,
    checkAvailableUsername,
    signUp,
    logout,
    forgetPassword,
    resetPassword
}


