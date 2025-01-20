import type {IdType} from "@/api/common.ts";
import {del, get, resultNotify} from "@/utils/http.ts";

export interface CacheDesc {
    id: IdType;
    desc: string;
    type: string;
}

export interface CacheInfo {
    key: string;
    value: string;
    expireTime?: Date;
}

export const listCacheDesc = async () => {
    const {data} = await get<CacheDesc[]>("/content-api/cache/type");
    return data;
}

export const getCacheKeys = async (prefix: string) => {
    const {data} = await get<string[]>("/content-api/cache/list", prefix);
    return data;
}

export const getCacheInfo = async (key: string) => {
    const {data} = await get<CacheInfo>("/content-api/cache", key);
    return data;
}

export const removeCache = async (key: string) => {
    const {data} = await del<boolean>("/content-api/cache", key);
    resultNotify(data, "缓存清楚成功", "删除失败");
}


