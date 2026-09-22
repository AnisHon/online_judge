import {get, type successCallback} from "@/utils/http";
import {add, putRemove} from "@/utils/simpleCRUD";
import useLoading from "@/hooks/useLoading";
import {debounce} from "lodash";
import type {IdType} from "@/api/common.ts";

enum MenuType {
    MENU = 'M',
    MENU_ITEM = 'I',
    BUTTON = 'B'
}
interface MenuRoleRelation {
    menuId?: IdType;
    roleId?: IdType;
}


interface MenuView {
    menuId: IdType;
    menuName: string;
    menuType: MenuType;
    parentId: IdType;
    icon: string;
    perms: string;                  // 权限子段
    router: string;                 // 路由路径
    component: string;              // 组件路径
    orderNum: number;
    createTime: Date;
    remark: string;
}

interface MenuForm {
    menuId?: IdType;
    menuName?: string;
    menuType?: MenuType;
    parentId?: IdType;
    icon?: string;
    perms?: string;
    router?: string;
    component?: string;
    orderNum?: number;
    remark?: string;
}

interface TreedMenu {
    id?: IdType;
    menu: MenuView;
    children: TreedMenu[];
}

async function getTreedMenu(): Promise<TreedMenu[]> {
    const {data} = await get<TreedMenu[]>("/user-api/auth/menus");
    return data;
}

async function getAuth() : Promise<MenuView[]> {
    const {data} = await get<MenuView[]>("/user-api/auth/auths");
    return data;
}

async function grant(relations: MenuRoleRelation[]): Promise<void> {
    await add(relations, "/user-api/menu/grant");
}

const debouncedGrant = (relations: MenuRoleRelation[], success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading()
    const add = debounce(() => {
        grant(relations)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}


async function revoke(relations: MenuRoleRelation[]): Promise<void> {
    await putRemove(relations, "/user-api/menu/batchRevoke", "/user-api/menu/batchRevoke");
}
const debouncedRevoke = (relations: MenuRoleRelation[], success: successCallback<void>) => {
    const {loading, isLoading, finish} = useLoading();
    const add = debounce(() => {
        revoke(relations)
            .then(success)
            .finally(finish);
    }, 1000);
    return {loading, isLoading, add};
}

const listRoleMenu = async (id: IdType) => {
    const {data} = await get<MenuView[], IdType>("/user-api/menu/listRoleMenu", id);
    return data;
}

export {
    type MenuView,
    type TreedMenu,
    type MenuForm,
    type MenuRoleRelation,
    MenuType,
    getTreedMenu,
    getAuth,
    grant,
    revoke,
    debouncedGrant,
    debouncedRevoke,
    listRoleMenu
}
