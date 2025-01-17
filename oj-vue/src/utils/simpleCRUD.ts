import {del, get, getWithParams, post, put, query} from "@/utils/http";
import {
    onlyPagedData,
    type PagedResponse,
    type PagedType,
    type SortedPagedType,
    toPagedQueryData
} from "@/api/pagedType";
import {ElNotification} from "element-plus";
import type {IdType} from "@/api/common.ts";

/**
 * 通用删除
 * @param id 删除的ID可以使array或者单纯的number
 * @param url 删除的URL
 */

export const remove = async (id: IdType | IdType[] | string | string[], url: string) => {

    const {data} = await del<boolean, number | number[] | string | string[]>(url, id);

    if (!data) {
        ElNotification.warning("删除失败");
    } else {
        ElNotification.success("删除成功");
    }
};

const postedRemove =  async <T> (id: T | T[], batchUrl: string, singleUrl: string) => {

    let success;
    if (id instanceof Array) {
        const {data} = await post<T[], boolean>(batchUrl, id);
        success = data;
    } else {
        const {data} = await post <T, boolean>(singleUrl, id);
        success = data;
    }
    if (!success) {
        ElNotification.warning("删除失败");
    } else {
        ElNotification.success("删除成功");
    }
};
export const putRemove =  async <T> (id: T | T[], batchUrl: string, singleUrl: string) => {

    let success;
    if (id instanceof Array) {
        const {data} = await put<T[], boolean>(batchUrl, id);
        success = data;
    } else {
        const {data} = await put <T, boolean>(singleUrl, id);
        success = data;
    }
    if (!success) {
        ElNotification.warning("删除失败");
    } else {
        ElNotification.success("删除成功");
    }
};

/**
 * 通用添加
 * @param form 添加的表单
 * @param url 对应的URL
 */
const add = async <T> (form: T, url: string) => {
    const {data} = await post<T, boolean>(url, form);

    if (!data) {
        ElNotification.warning("添加失败");
    } else {
        ElNotification.success("添加成功");
    }
};


const batchAdd = async <T> (form: T| T[], batchUrl: string, singleUrl: string) => {
    let success;
    if (form instanceof Array) {
        const {data} = await post<T[], boolean>(batchUrl, form);
        success = data;
    } else {
        const {data} = await post <T, boolean>(singleUrl, form);
        success = data;
    }
    if (!success) {
        ElNotification.warning("添加失败");
    } else {
        ElNotification.success("添加成功");
    }
};

/**
 * 通用更新
 * @param form 更新的表单
 * @param url 更新的URL
 */
const update = async <T> (form: T, url: string) => {
    const {data} = await put<T, boolean>(url, form);
    if (!data) {
        ElNotification.warning("更改失败");
    } else {
        ElNotification.success("更改成功");
    }
};



/**
 * 批量获取
 * @param queryData 查询数据
 * @param simpleUrl 普通分页查询URL
 * @param queryUrl 复杂查询URL
 */
const fetch =  async <T extends SortedPagedType, R> (queryData: T, simpleUrl: string, queryUrl: string) => {
    if (onlyPagedData(queryData)) {
        const {data} = await query<R, PagedType>(simpleUrl, toPagedQueryData(queryData));
        return data;
    }
    const {data} = await query<R, T>(queryUrl, queryData);
    return data;
}

const pagedFetch =  async <T extends PagedType, R> (queryData: T, simpleUrl: string) => {

    const {data} = await getWithParams<PagedResponse<R>, PagedType>(simpleUrl, queryData);
    return data;

}



const simpleGet = async <T> (data: T, url: string, successMsg = "成功", fail = "失败") => {
    const {data: success} = await get<boolean, T>(url, data);
    if (success) {
        ElNotification.success(successMsg);
    } else {
        ElNotification.warning(fail);
    }

}

export {
    add,
    batchAdd,
    update,
    fetch,
    postedRemove,
    simpleGet,
    pagedFetch
}
