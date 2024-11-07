import axios from 'axios';
import {useToken} from "@/stores/useToken";
import {ElMessage} from "element-plus";
import router from "@/router"

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
    ElMessage.warning("令牌过期，请重新登录");
    router.replace({name: 'login'});

};

const service = axios.create({
    baseURL: "http://localhost:5173/api", // 设置基础 URL
    timeout: 60000, // 设置请求超时时间
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

        if (response.status === 200) {
            return response.data;
        }
        ElMessage.error(response.data.code + ":" + response.data.message);
        return Promise.reject(new Error(response.data.message));
    },
    error => {
        // 处理错误
        if (error.status == 401) {
            error401();
        } else if (error.status == 400) {
            ElMessage.error(error.message);
        } else if (error.status == 404) {
            ElMessage.error("接口404 : " + error.config.url)
        } else {
            ElMessage.error(error.message);
        }
        return error;
    }
);

const failHandler = <T>(result: ResultPromise<T>, handle: typeof defaultFail) => {
    result
        .then(result => {
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

const getWithArray = <R>(url: string, params: string[]): ResultPromise<R> => {

    let param = "";
    if (params && params.length > 0) {
        param = params.join(",");
    }
    return <ResultPromise<R>>service.get<string, AjaxResult<R>>(url + "/" + param);
}

const defaultFail = (msg: string, code: number) => {
    ElMessage.warning(msg);
}

const post = <T, R>(url: string, data: T, failCallback = defaultFail): ResultPromise<R> => {
    const promise = service.post<T, AjaxResult<R>>(url, data);
    failHandler(promise, failCallback);
    return promise;
};


// 导出封装的方法
export {
    get,
    post,
    type successCallback,
    type failCallback,
    type successPromiseCallback,
    type finallyCallback,
};
