import type {IdType} from "@/api/common.ts";
import type {PagedResponse, PagedType} from "@/api/pagedType.ts";
import {del, getWithParams, resultNotify} from "@/utils/http.ts";

export interface FileInfo {
    fileId: IdType;
    fileMd5: string;
    fileName: string;
    filePath: string;
    fileSize: number;
    fileType: string;
    reference: number;
    uploadTime: Date;
}


export const listFileInfo = async (page: PagedType) => {
    const {data} = await getWithParams<PagedResponse<FileInfo>, PagedType>("/content-api/fileInfo/list", page);
    return data;
}

export const removeFileInfo = async (ids: IdType[] | IdType) => {
    const {data} = await del<boolean>("/content-api/fileInfo", ids);
    resultNotify(data, "删除成功", "删除失败")
}