import {get, post, type successCallback} from "@/utils/http";
import type {MenuForm, MenuView} from "@/api/auth/menu";
import {
    onlyPagedData,
    type PagedResponse,
    type PagedType,
    type SortedPagedType,
    toPagedQueryData
} from "@/api/pagedType";

/**
 * 通用删除
 * @param id 删除的ID可以使array或者单纯的number
 * @param batchUrl 批量删除的URL
 * @param singleUrl 单独删除的URL
 */

const remove = async (id: number | number[], batchUrl: string, singleUrl: string) => {

    let success = false;
    if (id instanceof Array) {
        const {data} = await get<boolean, number[]>(batchUrl, id);
        success = data;
    } else {
        const {data} = await get<boolean, number>(singleUrl, id);
        success = data;
    }
    if (!success) {
        ElMessage.warning("删除失败");
    } else {
        ElMessage.success("删除成功");
    }
};

const postedRemove =  async <T> (id: T | T[], batchUrl: string, singleUrl: string) => {

    let success = false;
    if (id instanceof Array) {
        const {data} = await post<T[], boolean>(batchUrl, id);
        success = data;
    } else {
        const {data} = await post <T, boolean>(singleUrl, id);
        success = data;
    }
    if (!success) {
        ElMessage.warning("删除失败");
    } else {
        ElMessage.success("删除成功");
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
        ElMessage.warning("添加失败");
    } else {
        ElMessage.success("添加成功");
    }
};


const batchAdd = async <T> (form: T| T[], batchUrl: string, singleUrl: string) => {
    let success = false;
    if (form instanceof Array) {
        const {data} = await post<T[], boolean>(batchUrl, form);
        success = data;
    } else {
        const {data} = await post <T, boolean>(singleUrl, form);
        success = data;
    }
    if (!success) {
        ElMessage.warning("添加失败");
    } else {
        ElMessage.success("添加成功");
    }
};

/**
 * 通用更新
 * @param form 更新的表单
 * @param url 更新的URL
 */
const update = async <T> (form: T, url: string) => {
    const {data} = await post<T, boolean>(url, form);
    if (!data) {
        ElMessage.warning("更改失败");
    } else {
        ElMessage.success("更改成功");
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
        const {data} = await post<PagedType, PagedResponse<R>>(simpleUrl, toPagedQueryData(queryData));
        return data;
    }
    const {data} = await post<T, PagedResponse<R>>(queryUrl, queryData);
    return data;
}

const pagedFetch =  async <T extends PagedType, R> (queryData: T, simpleUrl: string) => {

    const {data} = await post<PagedType, PagedResponse<R>>(simpleUrl, toPagedQueryData(queryData));
    return data;

}



const simpleGet = async <T> (data: T, url: string, successMsg = "成功", fail = "失败") => {
    const {data: success} = await get<boolean, T>(url, data);
    if (success) {
        ElMessage.success(successMsg);
    } else {
        ElMessage.warning(fail);
    }

}

export {
    add,
    batchAdd,
    remove,
    update,
    fetch,
    postedRemove,
    simpleGet,
    pagedFetch
}
