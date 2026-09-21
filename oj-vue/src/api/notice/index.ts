import {add, remove, update} from "@/utils/simpleCRUD.ts";
import type {PagedResponse, PagedType} from "@/api/pagedType.ts";
import {get, getWithParams, service} from "@/utils/http.ts";
import type {IdType} from "@/api/common.ts";

export interface NoticeDto {
    noticeId: IdType;
    title: string;
    content: string;
    topUp: boolean;
}

export interface Notice extends NoticeDto {
    userId: IdType
    updateTime: Date;
    createTime: Date;
}


export const updateNotice = async (notice: NoticeDto) => {
    await update(notice, "/content-api/notice");
}

export const listNotice = async (page: PagedType) => {
    const {data} = await getWithParams<PagedResponse<Notice>, PagedType>("/content-api/notice/list", page);
    return data;
}

export const addNotice = async (notice: NoticeDto) => {
    await add(notice, "/content-api/notice");
}

export const removeNotice = async (id: IdType[] | IdType) => {
    await remove(id, "/content-api/notice");
}

export const getNotice = async (id: IdType) => {
    const {data} = await get<Notice>("/content-api/notice", id);
    return data;
}

export const recentNotices = async () => {
    const {data} = await service.get("/content-api/notice/recent");
    return data;
}
