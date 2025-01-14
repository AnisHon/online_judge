import {getWithParams, post, service} from "@/utils/http.ts";
import {ElNotification} from "element-plus";
import __ from "lodash";
import {remove} from "@/utils/simpleCRUD.ts";

export interface CloudFile {
    cloudFileId: string;
    fileName: string;
    fileId?: string;
    fileMd5?: string;
    filePath?: string;
    fileSize?: string;
    userId: string;
    nikeName: string;
    parentId: string;
    dir: boolean;
    updateTime?: Date;
    createTime?: Date;

    edit?: boolean;
    add?: boolean;
    fileNameCopy?: string;
}

export interface QueryCloudFile {
    parentId: string;
    fileName: string;
}

export interface CloudFileForm {
    fileName: string;
    parentId: string;
}


export const uploadImages = async (files: File[]): Promise<string[]> => {
    const form = new FormData();

    files.forEach(file => form.append("images", file))
    // form.append("images", files[0]);
    const {data} = await service<string[]>({
        url: "/image",
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

    if (data) {
        ElNotification.success("添加成功")
    } else {
        ElNotification.error("添加失败")
    }
}

export const debouncedAddDir = (callback: Function) => {
    return __.debounce(async (data) => {
        await addDir(data);
        callback();
    }, 500);
}

export const deleteFile = (id: string) => {
    remove(id, "/file")
}