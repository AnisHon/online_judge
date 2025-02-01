import {addResultNotify, baseURL, get, getWithParams, post, put, resultNotify, service} from "@/utils/http.ts";
import {ElNotification} from "element-plus";
import __ from "lodash";
import {remove} from "@/utils/simpleCRUD.ts";
import {useUserStore} from "@/stores/useUserStore.ts";
import {computed} from "vue";
import type {IdType} from "@/api/common.ts";
import axios from "axios";
import {useToken} from "@/stores/useToken.ts";
import { saveAs } from 'file-saver'

export interface CloudFile {
    cloudFileId: string;
    fileName: string;
    fileId?: IdType;
    fileMd5?: string;
    filePath?: string;
    fileSize?: string;
    userId: IdType;
    nikeName: string;
    parentId: IdType;
    dir: boolean;
    updateTime?: Date;
    createTime?: Date;

    edit?: boolean;
    add?: boolean;
    fileNameCopy?: string;
}

export interface QueryCloudFile {
    parentId: IdType;
    fileName: string;
}

export interface CloudFileForm {
    fileName: string;
    parentId: IdType;
}

/**
 * 分片上传初始化参数
 */
export interface SpliceChunk {
    md5: string;
    chunk: File;
    chunkSize: number;
    fileName: string;
    index: number;
    totalSize: number;
}

/**
 * 初始化分片的结果
 */
export interface ChunkInfo {
    chunkId: IdType;
    chunkNum: number;
    chunkSize: number;
    fileMd5: string;
    fileName: string;
    filePath: string;
    fileSize: number;
}

/**
 * 每次分片上传返回的结果
 */
export interface Splice extends ChunkInfo {
    finished: boolean;
    path: string;
    uploadId: string;
    partHashes: {chuckIndex: number, partHash: string}[];

}

export const countOnline = async (): Promise<number> => {
    const {data} = await get<number>("/content-api/info/online");
    return data;
}

export const freeDisk = async (): Promise<{free: number, total: number}> => {
    const {data} = await get<{free: number, total: number}>("/content-api/info/free");
    return data;
}

export const uploadImages = async (files: File[]): Promise<string[]> => {
    const form = new FormData();

    files.forEach(file => form.append("images", file))

    const {data} = await service<string[]>({
        url: "/image",
        method: "POST",
        data: form
    });
    return data;
}

export const myAvatarPath = computed(() => {
    const userStore = useUserStore();
    const userId = userStore?.user?.userId || ""
    return getAvatarPath(userId);
})

export const getAvatarPath = (userId: IdType) => {


    return `/api/avatar/${userId}`;
}

export const uploadAvatar = async (file: File): Promise<string> => {
    const form = new FormData();
    form.append("avatar", file);
    const {data} = await service<string>({
        url: "/avatar",
        method: "POST",
        data: form
    });
    return data;
}


export const listFiles = async (query: QueryCloudFile): Promise<CloudFile[]> => {
    const {data} = await getWithParams<CloudFile[], QueryCloudFile>("/file/list", query);
    return data;
}

const addDir = async (file: CloudFileForm): Promise<void> => {
    const {data} = await post<CloudFileForm, boolean>("/file/dir", file);
    resultNotify(data, "添加成功", "添加失败");
}
// 验证是否为blob格式
export function blobValidate(data: any) {
    return data.type !== 'application/json'
}

export const debouncedAddDir = (callback: Function) => {
    return __.debounce(async (data) => {
        await addDir(data);
        callback();
    }, 500);
}

export const updateFile = async (form: CloudFileForm) => {
    const {data} = await put<CloudFileForm, boolean>("/file", form);
    resultNotify(data, "修改成功", "修改失败");
}

export const deleteFile = (id: IdType) => {
    return remove(id, "/file")
}

export const addFile = async (md5: string, fileName: string, parentId: string) => {
    const {data} = await service.post<boolean>(`/content-api/file/cloud/${md5}/${fileName}/${parentId}`);
    addResultNotify(data);
}

export const download = (path: string, fileName: string) => {

    axios.get('/file', {
        baseURL: baseURL,
        params: {
            path: path
        },
        headers: {
            token: useToken().token
        },
        responseType: 'blob'
    }).then((res) => {
        console.log(res)
        const isBlob = blobValidate(res.data);
        if (isBlob) {
            const blob = new Blob([res.data])
            saveAs_(blob, fileName)
        } else {
            ElNotification.error(res.data.message)
        }
    })
}

export const preview = (path: string) => {
    window.open(`${baseURL}/file?path=${path}`);
}

export const saveAs_ = (text: any, name: string, opts?: any) => {
    saveAs(text, name, opts);
}

export const initSlice = async (spliceChunk: SpliceChunk) => {
    const {data} = await post<SpliceChunk, Splice>("/file/init", spliceChunk);
    return data;
}

export const getProgress = async (md5: string) => {
    const {data} = await get<Splice>(`/file/progress/${md5}`);
    return data;
}

export const upload = async (md5: string, partNumber: number, blob: Blob) => {
    const form = new FormData();
    form.append("file", blob);
    return axios({
        method: 'POST',
        url: baseURL + `/file/${md5}/${partNumber}`,
        data: form
    })
}

export const mergeFile = (md5: string) => {
    return post("/file/merge/" + md5, undefined);
}

export function downloadFile(url: string, filename: string) {
    const link = document.createElement('a');
    link.href = `${baseURL}/file/download?fileName=${url}`;
    link.download = filename || 'file'; // 设置下载文件的默认名称
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}