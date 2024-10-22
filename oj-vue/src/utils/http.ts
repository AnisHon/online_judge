import axios from 'axios';
import {useToken} from "@/stores/useToken";
import {useRouter} from "vue-router";
import {ElMessage} from "element-plus";

export interface AjaxResult<T> {
    code: number;
    message: string;
    data: T;
}

type ResultPromise<T> = Promise<AjaxResult<T>>

const token = useToken()

const error401 = () => {
    const router = useRouter()
    token.clearToken()
    ElMessage.warning("令牌过期，请重新登录")
    router.replace('/auth/login')

}

const service = axios.create({
    baseURL: "http://localhost:5173/api", // 设置基础 URL
    timeout: 10000, // 设置请求超时时间
});

// 请求拦截器
service.interceptors.request.use(
    config => {
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

        if (response.data.code == 401) {
            error401();
        } else if (response.data.code == 400) {
            ElMessage.error(response.data.message);
        } else {
            ElMessage.error(response.data.code + ":" + response.data.message);
        }
        return Promise.reject(new Error(response.data.message));
    },
    error => {
        // 处理错误
        let message: string;
        if (error.response) {
            message = error.response.data.message || '请求失败';
        } else {
            message = '网络错误，请稍后再试';
        }
        // 可以在这里添加全局错误提示
        console.error(message);
        return Promise.reject(new Error(message));
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
const get = <R, T>(url: string, params: T | undefined = undefined): ResultPromise<R> => {
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
};
