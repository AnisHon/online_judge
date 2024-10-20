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

export interface ForgotPasswordForm {
    username: string;
    password: string;
    code: string;
}



async function checkAvailableEmail(email: string) {
    const {data} = await get("/user-api/user/email", email);
    return data;
}

async function checkAvailableUsername(username: string) {
    const {data} = await get("/user-api/user/username", username);
    return data;
}

async function getMe(): LoginUser {
    const {data} = await get("/user-api/auth/me");
    return data;
}

async function login(data: LoginForm) {
    const {data: {message, success, token}} = await post('/user-api/auth/login', data);
    const tokenStore = useToken();
    if (success) {
        tokenStore.setToken(token);
    } else {
        return await Promise.reject(message);
    }
}

async function signUp(data: SignUpForm) {
    const {data: {message, success, token}} = await post('/user-api/auth/registration', data);
    const tokenStore = useToken();
    if (success) {
        tokenStore.setToken(token);
    } else {
        return await Promise.reject(message);
    }
}

async function forgetPassword(data: ForgotPasswordForm) {
    const {data: {success, message}} = await post('/user-api/auth/forget-pass', data);
    if (success) {
        return  message
    } else {
        return await Promise.reject(message);
    }
}

async function logout() {
    //to do
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


