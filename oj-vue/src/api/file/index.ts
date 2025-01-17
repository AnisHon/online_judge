import {get, getWithParams, post, service} from "@/utils/http.ts";
import {ElNotification} from "element-plus";
import __ from "lodash";
import {remove} from "@/utils/simpleCRUD.ts";
import {useUserStore} from "@/stores/useUserStore.ts";
import {computed} from "vue";
import type {IdType} from "@/api/common.ts";

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
    return remove(id, "/file")
}