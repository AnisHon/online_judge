import type {IdType} from "@/api/common.ts";
import type {PagedResponse, PagedType} from "@/api/pagedType.ts";
import {del, getWithParams} from "@/utils/http.ts";

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

export interface FileInfoQuery extends PagedType {
    keyword?: string;
}


export const listFileInfo = async (page: FileInfoQuery) => {
    const {data} = await getWithParams<PagedResponse<FileInfo>, FileInfoQuery>("/content-api/fileInfo/list", page);
    return data;
}

export const removeFileInfo = async (ids: IdType[] | IdType): Promise<boolean> => {
    const {data} = await del<boolean>("/content-api/fileInfo", ids);
    return data === true;
}
