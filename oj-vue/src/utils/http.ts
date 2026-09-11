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

let refreshing: Promise<string> | null = null;

const refreshAccessToken = async (): Promise<string> => {
    const tokenStore = useToken();
    if (!tokenStore.refreshToken) throw new Error("刷新令牌不存在");
    if (!refreshing) {
        refreshing = service.post('/user-api/auth/refresh', {refreshToken: tokenStore.refreshToken}, {
            headers: {token: ''},
            _skipAuthRefresh: true
        } as any).then(response => {
            const result = response.data as AjaxResult<any>;
            const accessToken = result.data?.accessToken;
            if (result.code !== 200 || !accessToken) throw new Error(result.message || "刷新登录状态失败");
            tokenStore.setTokens(accessToken, result.data.refreshToken);
            return accessToken;
        }).finally(() => { refreshing = null; });
    }
    return refreshing;
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
        if ((result?.code === 401 || response.status === 401) && !request._skipAuthRefresh && !request._retry) {
            request._retry = true;
            return refreshAccessToken().then(accessToken => {
                request.headers.set('token', accessToken);
                return service(request);
            }).catch(() => {
                error401();
                return Promise.reject(new ApiError(result?.message || "登录已过期", 401));
            });
        }
        if (result?.code === 401 || response.status === 401) {
            error401();
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
        if (isAxiosError(error) && error.response?.status === 401 && request && !request._skipAuthRefresh && !request._retry) {
            request._retry = true;
            return refreshAccessToken().then(accessToken => {
                request.headers.set('token', accessToken);
                return service(request);
            }).catch(() => {
                error401();
                return Promise.reject(new ApiError("登录已过期", 401));
            });
        }
        const status = isAxiosError(error) ? error.response?.status : undefined;
        if (status === 401) {
            error401();
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
            if (error instanceof ApiError) {
                handle(error.message, error.code);
            }
        })
}

// 封装的 GET 和 POST 方法
const get = <R, T = any>(url: string, params: T | undefined = undefined): ResultPromise<R> => {
    if (params !== undefined && params !== null && params !== '') {
        url = url + '/' + encodeURIComponent(params.toString());
    }
    return service.get<T, AjaxResult<R>>(url);
};

export const getWithParams = <R, T>(url: string, params: T): ResultPromise<R> => {
    return service<T, AjaxResult<R>>({
        method: "GET",
        url: url,
        params: params,
        paramsSerializer: (data) => qs.stringify(data, { arrayFormat: 'repeat' })
    });
};


// 封装的 GET 方法
export const query = <R, T>(url: string, params: T): ResultPromise<PagedResponse<R>> => {
    return service.get<T, AjaxResult<PagedResponse<R>>>(url, {
        params: params,
        paramsSerializer: (data) => qs.stringify(data, { arrayFormat: 'repeat' })
    });
};

const getWithArray = <R>(url: string, params: string[]): ResultPromise<R> => {

    let param = "";
    if (params && params.length > 0) {
        param = params.join(",");
    }
    return <ResultPromise<R>>service.get<string, AjaxResult<R>>(url + "/" + param);
}

const defaultFail = (msg: string, code: number) => {
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
    return service.put<T, AjaxResult<R>>(url);
};

const del = <R, T = any>(url: string, params: T | T[] | undefined = undefined): ResultPromise<R> => {
    if (params) {
        url = url + '/' + params.toString();
    }
    return service.delete<T, AjaxResult<R>>(url);
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
