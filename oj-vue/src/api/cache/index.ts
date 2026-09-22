import type {IdType} from "@/api/common.ts";
import {del, get, getWithParams} from "@/utils/http.ts";

export interface CacheDesc {
    id: IdType;
    desc: string;
    type: string;
}

export interface CacheInfo {
    key: string;
    value: string;
    expireTime?: number;
}

export interface CacheKeyPage {
    keys: string[];
    nextCursor: string;
    hasMore: boolean;
}

export const listCacheDesc = async () => {
    const {data} = await get<CacheDesc[]>("/content-api/cache/type");
    return data;
}

export const getCacheKeys = async (prefix: string, cursor = '0', limit = 100) => {
    const {data} = await getWithParams<CacheKeyPage, {cursor: string; limit: number}>(
        `/content-api/cache/list/${encodeURIComponent(prefix)}`,
        {cursor, limit},
    );
    return data;
}

export const getCacheInfo = async (key: string) => {
    const {data} = await get<CacheInfo>("/content-api/cache", key);
    return data;
}

export const removeCache = async (key: string): Promise<boolean> => {
    const {data} = await del<boolean>("/content-api/cache", key);
    return data === true;
}

