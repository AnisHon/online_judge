import axios from 'axios';
import {useToken} from "@/stores/useToken";
import {useRouter} from "vue-router";
import {ElMessage} from "element-plus";

export interface AjaxResult {
    code: number;
    message: string;
    data: object;
}

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
        let message = '';
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

// 封装的 GET 和 POST 方法
const get = (url: string, params: any): Promise<AjaxResult> => {
    if (params) {
        url = url + '/' + params.toString();
    }
    return service.get(url);
};

const getWithArray = (url: string, params: string[]): Promise<AjaxResult> => {

    let param = "";
    if (params && params.length > 0) {
        param = params.join(",");
    }
    return service.get(url + "/" + params);
}

const post = (url: string, data: object) => {
    return service.post(url, data);
};


// 导出封装的方法
export {
    get,
    post,
};
