import {addResultNotify, baseURL, binaryService, get, getWithParams, post, put, resultNotify, service} from "@/utils/http.ts";
import {ElNotification} from "element-plus";
import __ from "lodash";
import {remove} from "@/utils/simpleCRUD.ts";
import type {IdType} from "@/api/common.ts";
import { saveAs } from 'file-saver'
import {ApiError} from "@/utils/http.ts";

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
    const {data} = await get<number>("/user-api/auth/count");
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

export const getAvatarPath = (userId: IdType, version?: number, retry = 0) => {
    const suffix = version === undefined && retry === 0
        ? ''
        : `?v=${version ?? 0}${retry > 0 ? `&r=${retry}` : ''}`;
    return `/api/avatar/${userId}${suffix}`;
}

export const uploadAvatar = async (file: File): Promise<string> => {
    const form = new FormData();
    form.append("avatar", file);
    const {data} = await service<string | null>({
        url: "/avatar",
        method: "POST",
        data: form
    });
    if (typeof data !== 'string' || !data.trim()) {
        throw new ApiError('头像上传失败，图片格式不支持或存储服务不可用', 502);
    }
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
    const {data} = await service.post<boolean>(`/content-api/file/cloud/${encodeURIComponent(md5)}/${encodeURIComponent(fileName)}/${encodeURIComponent(parentId)}`);
    addResultNotify(data);
}

const saveBinaryResponse = async (response: {data: Blob}, fileName: string) => {
    const isBlob = blobValidate(response.data);
    if (isBlob) {
        saveAs_(response.data, fileName)
        return true;
    }
    ElNotification.error('文件下载失败，请稍后重试');
    return false;
};

export const download = async (path: string, fileName: string) => {
    try {
        await binaryService.get('/file', {
        params: {
            path: path
        },
        responseType: 'blob'
        }).then((res) => saveBinaryResponse(res, fileName));
    } catch (_) {
        ElNotification.error('文件下载失败，请稍后重试');
    }
}

export const preview = async (path: string) => {
    try {
        const response = await binaryService.get('/file', {params: {path}, responseType: 'blob'});
        if (!blobValidate(response.data)) {
            ElNotification.error('文件预览失败，请稍后重试');
            return;
        }
        const url = URL.createObjectURL(response.data);
        window.open(url, '_blank', 'noopener,noreferrer');
        window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
    } catch (_) {
        ElNotification.error('文件预览失败，请稍后重试');
    }
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
    return binaryService({
        method: 'POST',
        url: `/file/${encodeURIComponent(md5)}/${encodeURIComponent(String(partNumber))}`,
        data: form
    })
}

export const mergeFile = (md5: string) => {
    return post("/file/merge/" + md5, undefined);
}

export function downloadFile(url: string, filename: string) {
    void binaryService.get('/file/download', {
        params: {fileName: url},
        responseType: 'blob'
    }).then(response => saveBinaryResponse(response, filename || 'file'))
        .catch(() => ElNotification.error('文件下载失败，请稍后重试'));
}
