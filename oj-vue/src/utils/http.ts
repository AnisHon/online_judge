import axios, {isAxiosError} from 'axios';
import {useToken} from "@/stores/useToken";
import {ElNotification} from "element-plus";
import router from "@/router"
import type {PagedResponse} from "@/api/pagedType.ts";
import qs from "qs"

export const baseURL = "/api";



export interface AjaxResult<T> {
    code: number;
    message: string;
    data: T;
}

export class ApiError extends Error {
    notified = false;

    constructor(message: string, readonly code: number) {
        super(message);
        this.name = "ApiError";
    }
}

type successCallback<T> =  (value: T) => void;
type successPromiseCallback<T> =  (value: Promise<T>) => void;
type failCallback = (msg: string, code: number) => void;
type finallyCallback = () => void;
type ResultPromise<T> = Promise<AjaxResult<T>>;



let isRedirectingToLogin = false;

const error401 = () => {
    if (isRedirectingToLogin) return;
    isRedirectingToLogin = true;
    const token = useToken();
    token.clearToken();
    ElNotification.warning("令牌过期，请重新登录");
    router.replace({name: 'login'}).finally(() => { isRedirectingToLogin = false });
};

const error403 = () => {
    const token = useToken();
    ElNotification.error("拒绝访问");
    router.replace({name: '403'});
};

export const service = axios.create({
    baseURL: baseURL, // 设置基础 URL
    timeout: 60000, // 设置请求超时时间
    withCredentials: true // 携带cookie
});

// 刷新令牌必须绕开业务响应拦截器，否则已经解包的 AjaxResult 会被二次解包。
const refreshService = axios.create({
    baseURL,
    timeout: 60000,
    withCredentials: true
});

let refreshing: Promise<string> | null = null;

interface RefreshTokenResult {
    accessToken?: string;
    refreshToken?: string;
}

const refreshAccessToken = async (): Promise<string> => {
    const tokenStore = useToken();
    if (!tokenStore.refreshToken) throw new Error("刷新令牌不存在");
    if (!refreshing) {
        const refreshToken = tokenStore.refreshToken;
        refreshing = refreshService.post<AjaxResult<RefreshTokenResult>>(
            '/user-api/auth/refresh',
            {refreshToken},
            {headers: {token: ''}}
        ).then(response => {
            const result = response.data;
            const accessToken = result.data?.accessToken;
            if (result.code !== 200 || !accessToken) throw new Error(result.message || "刷新登录状态失败");
            tokenStore.setTokens(accessToken, result.data.refreshToken);
            return accessToken;
        }).finally(() => { refreshing = null; });
    }
    return refreshing;
};

const noRefreshPaths = [
    '/auth/login',
    '/auth/registration',
    '/auth/refresh',
    '/auth/captcha-code',
    '/auth/send-email-code',
    '/auth/send-forget-email-code',
    '/auth/forget-pass'
];

const canRefreshRequest = (request: any) => {
    const tokenStore = useToken();
    return !!request
        && !request._skipAuthRefresh
        && !request._retry
        && !!tokenStore.token
        && !!tokenStore.refreshToken
        && !noRefreshPaths.some(path => request.url?.includes(path));
};

const retryWithFreshAccessToken = async (request: any) => {
    request._retry = true;
    const accessToken = await refreshAccessToken();
    request.headers.set('token', accessToken);
    return service(request);
};

// 请求拦截器
service.interceptors.request.use(
    config => {
        if (!config.url?.endsWith('/auth/refresh')) {
            const token = useToken();
            config.headers.set('token', token.token)
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 响应拦截器
service.interceptors.response.use(
    response => {
        const result = response.data as AjaxResult<unknown>;
        if (!result || typeof result.code !== "number") {
            return Promise.reject(new ApiError("服务返回了无法识别的数据", response.status));
        }
        const request = response.config as any;
        if ((result?.code === 401 || response.status === 401) && canRefreshRequest(request)) {
            return retryWithFreshAccessToken(request).catch(() => {
                error401();
                return Promise.reject(new ApiError(result?.message || "登录已过期", 401));
            });
        }
        if (result?.code === 401 || response.status === 401) {
            if (useToken().hasToken()) error401();
            return Promise.reject(new ApiError(result?.message || "登录已过期", 401));
        }
        if (result?.code === 403 || response.status === 403) {
            error403();
            return Promise.reject(new ApiError(result?.message || "拒绝访问", 403));
        }
        if (result.code !== 200) {
            return Promise.reject(new ApiError(result.message || `请求失败（${result.code}）`, result.code));
        }
        return result as any;
    },
    error => {
        const request = isAxiosError(error) ? error.config as any : undefined;
        if (isAxiosError(error) && error.response?.status === 401 && canRefreshRequest(request)) {
            return retryWithFreshAccessToken(request).catch(() => {
                error401();
                return Promise.reject(new ApiError("登录已过期", 401));
            });
        }
        const status = isAxiosError(error) ? error.response?.status : undefined;
        if (status === 401) {
            if (useToken().hasToken()) error401();
        } else if (status === 403) {
            error403();
        }
        const result = isAxiosError(error) ? error.response?.data as Partial<AjaxResult<unknown>> | undefined : undefined;
        return Promise.reject(new ApiError(result?.message || error.message || "网络请求失败", status || 0));
    }
);

const failHandler = <T>(result: ResultPromise<T>, handle: typeof defaultFail) => {
    result
        .catch(error => {
            if (error instanceof ApiError && !error.notified) {
                handle(error.message, error.code);
                error.notified = true;
            }
        })
}

const withFailHandler = <T>(result: ResultPromise<T>, handle: typeof defaultFail = defaultFail) => {
    failHandler(result, handle);
    return result;
};

// 封装的 GET 和 POST 方法
const get = <R, T = any>(url: string, params: T | undefined = undefined): ResultPromise<R> => {
    if (params !== undefined && params !== null && params !== '') {
        url = url + '/' + encodeURIComponent(params.toString());
    }
    return withFailHandler(service.get<T, AjaxResult<R>>(url));
};

export const getWithParams = <R, T>(url: string, params: T): ResultPromise<R> => {
    return withFailHandler(service<T, AjaxResult<R>>({
        method: "GET",
        url: url,
        params: params,
        paramsSerializer: (data) => qs.stringify(data, { arrayFormat: 'repeat' })
    }));
};


// 封装的 GET 方法
export const query = <R, T>(url: string, params: T): ResultPromise<PagedResponse<R>> => {
    return withFailHandler(service.get<T, AjaxResult<PagedResponse<R>>>(url, {
        params: params,
        paramsSerializer: (data) => qs.stringify(data, { arrayFormat: 'repeat' })
    }));
};

const getWithArray = <R>(url: string, params: string[]): ResultPromise<R> => {

    let param = "";
    if (params && params.length > 0) {
        param = params.join(",");
    }
    return withFailHandler(<ResultPromise<R>>service.get<string, AjaxResult<R>>(url + "/" + param));
}

const defaultFail = (msg: string, code: number) => {
    if (code === 401 || code === 403) return;
    ElNotification.error({title: "请求失败", message: msg || "服务暂时不可用，请稍后重试"});
}

const post = <T, R>(url: string, data: T, failCallback = defaultFail): ResultPromise<R> => {
    const promise = service.post<T, AjaxResult<R>>(url, data);
    failHandler(promise, failCallback);
    return promise;
};

const put = <T, R>(url: string, data: T, failCallback = defaultFail): ResultPromise<R> => {
    const promise = service.put<T, AjaxResult<R>>(url, data);
    failHandler(promise, failCallback);
    return promise;
};

const pathPut = <R, T = any>(url: string, params: T | undefined = undefined): ResultPromise<R> => {
    if (params) {
        url = url + '/' + params.toString();
    }
    return withFailHandler(service.put<T, AjaxResult<R>>(url));
};

const del = <R, T = any>(url: string, params: T | T[] | undefined = undefined): ResultPromise<R> => {
    if (params) {
        url = url + '/' + params.toString();
    }
    return withFailHandler(service.delete<T, AjaxResult<R>>(url));
};

export const resultNotify = (result: boolean | undefined, successMsg: string, errorMsg: string) => {
    if (result === true) {
        ElNotification.success(successMsg);
    } else if (result === false) {
        ElNotification.error(errorMsg);
    }
}


export const addResultNotify = (result: boolean | undefined, successMsg = "添加成功", errorMsg = "添加失败") => {
    resultNotify(result, successMsg, errorMsg);
}

export const updateResultNotify = (result: boolean | undefined, successMsg = "修改成功", errorMsg = "修改失败") => {
    resultNotify(result, successMsg, errorMsg);
}

export const removeResultNotify = (result: boolean | undefined, successMsg = "删除成功", errorMsg = "删除失败") => {
    resultNotify(result, successMsg, errorMsg);
}

export const getFormData = <T> (object: T): FormData => {
    const formData = new FormData();
    Object.keys(object as object).forEach((key) => {

        const value = object[key as keyof object];

        if (value === undefined || value === null) {
            return;
        }else if(Array.isArray(value)) {
            (<Array<any>>value).forEach((subValue, i) => {
                formData.append(key + `[${i}]`, subValue)
            })
        } else {
            formData.append(key, value);
        }
    })
    return formData;
}


// 导出封装的方法
export {
    get,
    post,
    put,
    pathPut,
    del,
    type successCallback,
    type failCallback,
    type successPromiseCallback,
    type finallyCallback,
};
