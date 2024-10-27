import type {MenuForm, MenuType, MenuView} from '@/api/auth/menu'
import {
    onlyPagedData,
    type PagedResponse,
    type PagedType,
    type SortedPagedType,
    toPagedQueryData
} from "@/api/pagedType";
import {get, post, type successCallback} from "@/utils/http";
import {debounce} from "lodash";
import useLoading from "@/hooks/useLoading";

interface roleState

interface QueryRole extends SortedPagedType{
    roleId: number;
    roleName: string;
    status: number;
    remark: string;
}

const dict = {
    menuType: [
        {
            value: "M",
            label: "菜单栏"
        }, {
            value: "I",
            label: "菜单项"
        }, {
            value: "B",
            label: "按钮"
        }
    ],
}

const removeMenu = async (id: number | number[]) => {

    let success = false;
    if (id instanceof Array) {
        const {data} = await get<boolean, number[]>("/user-api/menu/removeBatch", id);
        success = data;
    } else {
        const {data} = await get<boolean, number>("/user-api/menu/remove", id);
        success = data;
    }
    if (!success) {
        ElMessage.warning("删除失败");
    } else {
        ElMessage.success("删除成功");
    }
}




const addMenu = async (form: MenuForm) => {
    const {data} = await post<MenuForm, boolean>("/user-api/menu/add", form);
    if (!data) {
        ElMessage.warning("添加失败");
    } else {
        ElMessage.success("删除成功");
    }
}

const debouncedAddMenu = (form: MenuForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        addMenu(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}



const updateMenu = async (form: MenuForm) => {
    const {data} = await post<MenuForm, boolean>("/user-api/menu/update", form);
    if (!data) {
        ElMessage.warning("更改失败");
    } else {
        ElMessage.success("更改成功");
    }
}

const debouncedUpdateMenu = (form: MenuForm, success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const update = debounce(() => {
        updateMenu(form)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, update};
}

const getMenu = async (queryData: QueryMenu) => {
    if (onlyPagedData(queryData)) {
        const {data} = await post<PagedType, PagedResponse<MenuView>>("/user-api/menu/page", toPagedQueryData(queryData))
        return data;
    }
    const {data} = await post<QueryMenu, PagedResponse<MenuView>>("/user-api/menu/query", queryData)
    return data;
}

const debouncedGetMenu = (queryData: QueryMenu, success: successCallback<PagedResponse<MenuView>>) => {
    const {loading, isLoading, finish} = useLoading()
    const get = debounce(() => {
        getMenu(queryData)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, get};
}

export type {
    QueryMenu,
}

export {
    getMenu,
    debouncedGetMenu,
    removeMenu,
    addMenu,
    debouncedAddMenu,
    updateMenu,
    debouncedUpdateMenu,
    dict
}



