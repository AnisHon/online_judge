import type {MenuForm, MenuType, MenuView, TreedMenu} from '@/api/auth/menu'
import {
    type PagedResponse,
    type SortedPagedType,
} from "@/api/pagedType";
import {get} from "@/utils/http";
import {add, fetch, remove, update} from "@/utils/simpleCRUD";
import type {IdType} from "@/api/common.ts";

interface QueryMenu extends SortedPagedType{
    menuId?: IdType;
    menuName?: string;
    menuType?: MenuType;
    parentId?: IdType;
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

const removeMenu = async (id: IdType | IdType[]) => {
    await remove(id, "/user-api/menu");
}




const addMenu = async (form: MenuForm) => {
    await add(form, "/user-api/menu");
}

const updateMenu = async (form: MenuForm) => {
    await update(form, "/user-api/menu");
}

async function getAllTreedMenu(): Promise<TreedMenu[]> {
    const {data} = await get<TreedMenu[]>("/user-api/menu/treeMenus");
    return data;
}

const getMenu = async (queryData: QueryMenu): Promise<PagedResponse<MenuView>> => {
    return await fetch(queryData, "/user-api/menu/page", "/user-api/menu/query");
}

export type {
    QueryMenu,
}

export {
    getMenu,
    removeMenu,
    addMenu,
    updateMenu,
    getAllTreedMenu,
    dict
}


