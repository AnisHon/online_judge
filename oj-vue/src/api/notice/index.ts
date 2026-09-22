import type {PagedResponse, PagedType} from "@/api/pagedType.ts";
import {del, get, getWithParams, post, put, service} from "@/utils/http.ts";
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

export interface NoticeQuery extends PagedType {
    keyword?: string;
    topUp?: boolean;
}


export const updateNotice = async (notice: NoticeDto): Promise<boolean> => {
    const {data} = await put<NoticeDto, boolean>("/content-api/notice", notice);
    return data === true;
}

export const listNotice = async (page: NoticeQuery) => {
    const {data} = await getWithParams<PagedResponse<Notice>, NoticeQuery>("/content-api/notice/list", page);
    return data;
}

/** 后台管理列表，与前台公开公告列表分离，权限由后端校验。 */
export const listAdminNotice = async (page: NoticeQuery) => {
    const {data} = await getWithParams<PagedResponse<Notice>, NoticeQuery>("/content-api/notice/admin/list", page);
    return data;
}

export const addNotice = async (notice: NoticeDto): Promise<boolean> => {
    const {data} = await post<NoticeDto, boolean>("/content-api/notice", notice);
    return data === true;
}

export const removeNotice = async (id: IdType[] | IdType): Promise<boolean> => {
    const {data} = await del<boolean>("/content-api/notice", id);
    return data === true;
}

export const getNotice = async (id: IdType) => {
    const {data} = await get<Notice>("/content-api/notice", id);
    return data;
}

export const recentNotices = async () => {
    const {data} = await service.get("/content-api/notice/recent");
    return data;
}
