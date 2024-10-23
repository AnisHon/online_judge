import {get, post} from '@/utils/http'
import {type LoginUser} from "@/stores/useUserStore";
import {useToken} from "@/stores/useToken";


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

async function login(data: LoginForm) {
    const {data: {message, success, token}} = await post<LoginForm, LoginResponse>('/user-api/auth/login', data);
    const tokenStore = useToken();
    if (success) {
        tokenStore.setToken(token);
    } else {
        throw message;
    }
}

async function signUp(data: SignUpForm) {
    const {data: {message, success, token}} = await post<SignUpForm, LoginResponse>('/user-api/auth/registration', data);
    const tokenStore = useToken();
    if (success) {
        tokenStore.setToken(token);
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

async function logout() {
    // todo
}

export {
    login,
    getMe,
    checkAvailableEmail,
    checkAvailableUsername,
    signUp,
    logout,
    forgetPassword
}


