import axios, {isAxiosError, type AxiosRequestConfig} from 'axios';
import {useToken} from "@/stores/useToken";
import {ElNotification} from "element-plus";
import router from "@/router"
import type {PagedResponse} from "@/api/pagedType.ts";
import {refreshAccessToken, SessionChangedError} from '@/utils/authSession'
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
let isRedirectingToForbidden = false;

const error401 = (requestGeneration?: number) => {
    const token = useToken();
    // 旧请求不能清除新账号的会话，也不能把新账号带回登录页。
    if (requestGeneration !== undefined && requestGeneration !== token.getSessionVersion()) return;
    if (isRedirectingToLogin) return;
    isRedirectingToLogin = true;
    token.clearToken();
    ElNotification.warning("令牌过期，请重新登录");
    router.replace({name: 'login'}).finally(() => { isRedirectingToLogin = false });
};

const error403 = () => {
    if (isRedirectingToForbidden || router.currentRoute.value.name === '403') return;
    isRedirectingToForbidden = true;
    ElNotification.error("拒绝访问");
    router.replace({name: '403'}).finally(() => { isRedirectingToForbidden = false });
};

export const service = axios.create({
    baseURL: baseURL, // 设置基础 URL
    timeout: 60000, // 设置请求超时时间
    withCredentials: true // 携带cookie
});

export const binaryService = axios.create({
    baseURL,
    timeout: 60000,
    withCredentials: true
});

binaryService.interceptors.request.use(config => {
    const token = useToken();
    setAccessTokenHeader(config.headers, token.token);
    const request = config as typeof config & {__sessionVersion?: number};
    request.__sessionVersion = token.getSessionVersion();
    return config;
});

const setHeader = (headers: AxiosRequestConfig['headers'] | undefined, name: string, value: string) => {
    if (!headers) return;
    if (typeof (headers as {set?: unknown}).set === 'function') {
        (headers as {set: (key: string, value: string) => void}).set(name, value);
        return;
    }
    (headers as Record<string, string>)[name] = value;
};

const setAccessTokenHeader = (headers: AxiosRequestConfig['headers'] | undefined, accessToken: string) => {
    if (!headers) return;
    if (!accessToken) {
        if (typeof (headers as {delete?: unknown}).delete === 'function') {
            (headers as {delete: (key: string) => void}).delete('Authorization');
        } else {
            delete (headers as Record<string, string>).Authorization;
        }
        return;
    }
    setHeader(headers, 'Authorization', `Bearer ${accessToken}`);
};

const requestPath = (request: {url?: string; baseURL?: string} | undefined) => {
    if (!request?.url) return '';
    try {
        return new URL(request.url, request.baseURL || window.location.origin).pathname;
    } catch (_) {
        return request.url.split('?')[0];
    }
};

const isRefreshRequest = (request: {url?: string; baseURL?: string} | undefined) =>
    requestPath(request).endsWith('/user-api/auth/refresh');

const noRefreshPaths = [
    '/user-api/auth/login',
    '/user-api/auth/registration',
    '/user-api/auth/refresh',
    '/user-api/auth/captcha-code',
    '/user-api/auth/send-email-code',
    '/user-api/auth/send-forget-email-code',
    '/user-api/auth/forget-pass'
];

const isNoRefreshPath = (request: any) => {
    const path = requestPath(request);
    return noRefreshPaths.some(item => path.endsWith(item));
};

const canRefreshRequest = (request: any) => {
    const tokenStore = useToken();
    const generation = request?.__sessionVersion;
    return !!request
        && !request._skipAuthRefresh
        && !request._retry
        && generation === tokenStore.getSessionVersion()
        && !!tokenStore.token
        && !isNoRefreshPath(request);
};

const retryWithFreshAccessToken = async (request: any, client = service) => {
    const tokenStore = useToken();
    const generation = request.__sessionVersion as number;
    if (generation !== tokenStore.getSessionVersion()) throw new SessionChangedError();
    request._retry = true;
    const accessToken = await refreshAccessToken(generation);
    if (generation !== tokenStore.getSessionVersion()) throw new SessionChangedError();
    setAccessTokenHeader(request.headers, accessToken);
    return client(request);
};

// 二进制响应不能走业务 AjaxResult 解包，但认证失败仍必须复用同一套会话恢复策略。
binaryService.interceptors.response.use(
    response => response,
    error => {
        const request = isAxiosError(error) ? error.config as any : undefined;
        const status = isAxiosError(error) ? error.response?.status : undefined;
        if (status === 401 && canRefreshRequest(request)) {
            return retryWithFreshAccessToken(request, binaryService).catch(refreshError => {
                if (!(refreshError instanceof SessionChangedError)) error401(request.__sessionVersion);
                return Promise.reject(new ApiError('登录状态已过期，请重新登录', 401));
            });
        }
        if (status === 401) {
            if (useToken().hasToken()) error401(request?.__sessionVersion);
        } else if (status === 403) {
            error403();
        }
        return Promise.reject(new ApiError(safeErrorMessage(undefined, status || 0), status || 0));
    }
);

// 请求拦截器
service.interceptors.request.use(
    config => {
        const token = useToken();
        if (!isRefreshRequest(config)) {
            setAccessTokenHeader(config.headers, token.token);
        }
        const request = config as typeof config & {_sessionVersion?: number; __sessionVersion?: number};
        request.__sessionVersion = token.getSessionVersion();
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
            return retryWithFreshAccessToken(request).catch(error => {
                if (!(error instanceof SessionChangedError)) error401(request.__sessionVersion);
                return Promise.reject(new ApiError(result?.message || "登录已过期", 401));
            });
        }
        if (result?.code === 401 || response.status === 401) {
            if (useToken().hasToken()) error401(request.__sessionVersion);
            return Promise.reject(new ApiError(result?.message || "登录已过期", 401));
        }
        if (result?.code === 403 || response.status === 403) {
            error403();
            return Promise.reject(new ApiError(result?.message || "拒绝访问", 403));
        }
        if (result.code !== 200) {
            return Promise.reject(new ApiError(safeErrorMessage(result.message || `请求失败（${result.code}）`, result.code), result.code));
        }
        return result as any;
    },
    error => {
        const request = isAxiosError(error) ? error.config as any : undefined;
        if (isAxiosError(error) && error.response?.status === 401 && canRefreshRequest(request)) {
            return retryWithFreshAccessToken(request).catch(refreshError => {
                if (!(refreshError instanceof SessionChangedError)) error401(request.__sessionVersion);
                return Promise.reject(new ApiError("登录已过期", 401));
            });
        }
        const status = isAxiosError(error) ? error.response?.status : undefined;
        if (status === 401) {
            if (useToken().hasToken()) error401(request?.__sessionVersion);
        } else if (status === 403) {
            error403();
        }
        const result = isAxiosError(error) ? error.response?.data as Partial<AjaxResult<unknown>> | undefined : undefined;
        return Promise.reject(new ApiError(safeErrorMessage(result?.message || error.message, status || 0), status || 0));
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
    const param = (params || []).map(value => encodeURIComponent(String(value))).join(",");
    return withFailHandler(<ResultPromise<R>>service.get<string, AjaxResult<R>>(url + "/" + param));
}

const safeErrorMessage = (message: unknown, status: number) => {
    const raw = typeof message === 'string' ? message.trim() : '';
    if (status === 401) return '登录状态已过期，请重新登录';
    if (status === 403) return '当前账号没有执行此操作的权限';
    if (!raw) return status === 0 ? '网络连接失败，请检查网络后重试' : '请求失败，请稍后重试';
    // 开发环境保留后端业务提示；生产环境隔离 5xx/网关异常，避免把堆栈直接展示给用户。
    if (import.meta.env.PROD && status >= 500) return '服务暂时不可用，请稍后重试';
    return raw;
};

const defaultFail = (msg: string, code: number) => {
    if (code === 401 || code === 403) return;
    ElNotification.error({title: "请求失败", message: safeErrorMessage(msg, code)});
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
        url = url + '/' + encodeURIComponent(params.toString());
    }
    return withFailHandler(service.put<T, AjaxResult<R>>(url));
};

const del = <R, T = any>(url: string, params: T | T[] | undefined = undefined): ResultPromise<R> => {
    if (params) {
        const value = Array.isArray(params)
            ? params.map(item => encodeURIComponent(String(item))).join(',')
            : encodeURIComponent(params.toString());
        url = url + '/' + value;
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
