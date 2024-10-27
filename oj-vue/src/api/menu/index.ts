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
import {add, fetch, remove, update} from "@/utils/simpleCRUD";

interface QueryMenu extends SortedPagedType{
    menuId?: number;
    menuName?: string;
    menuType?: MenuType;
    parentId?: number;
    icon?: string;
    perms?: string;
    router?: string;
    remark?: string;
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
    await remove(id, "/user-api/menu/removeBatch", "/user-api/menu/remove");
}




const addMenu = async (form: MenuForm) => {
    await add(form, "/user-api/menu/add");
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
    await update(form, "/user-api/menu/update");
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

const getMenu = async (queryData: QueryMenu): Promise<PagedResponse<MenuView>> => {
    return await fetch(queryData, "/user-api/menu/page", "/user-api/menu/query");
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



