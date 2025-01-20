import axios from 'axios';
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

type successCallback<T> =  (value: T) => void;
type successPromiseCallback<T> =  (value: Promise<T>) => void;
type failCallback = (msg: string, code: number) => void;
type finallyCallback = () => void;
type ResultPromise<T> = Promise<AjaxResult<T>>;



const error401 = () => {
    const token = useToken();
    token.clearToken();
    ElNotification.warning("令牌过期，请重新登录");
    router.replace({name: 'login'});
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

// 请求拦截器
service.interceptors.request.use(
    config => {
        const token = useToken();
        config.headers.set('token', token.token)
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 响应拦截器
service.interceptors.response.use(
    response => {

        const code = response?.data?.code || response.status;

        if (code === 200) {
            return response.data;
        } if (code == 400) {
            ElNotification.error(response.data.message);
        } else if (code === 403) {
            error403();
        } else {
            ElNotification.error(response.data.code + ":" + response.data.message);
        }
        return Promise;
    },
    error => {
        // 处理错误
        console.log(error)
        if (error.status == 401) {
            error401();
        } else if (error.status == 400) {
            ElNotification.error(error.response?.data?.message);
        } else if (error.status == 404) {
            ElNotification.error("接口404 : " + error.config.url)
        } else {
            ElNotification.error(error?.response?.data?.message);
        }
        return error;
    }
);

const failHandler = <T>(result: ResultPromise<T>, handle: typeof defaultFail) => {
    result
        .catch(result => {
            if (result.code != 200) {
                handle(result.message, result.code);
            }
        })
}

// 封装的 GET 和 POST 方法
const get = <R, T = any>(url: string, params: T | undefined = undefined): ResultPromise<R> => {
    if (params) {
        url = url + '/' + params.toString();
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
