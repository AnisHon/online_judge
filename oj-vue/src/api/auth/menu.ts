import {get} from "@/utils/http";

enum MenuType {
    MENU = 'M',
    MENU_ITEM = 'I',
    BUTTON = 'B'
}
interface MenuView {
    menuId: number;
    menuName: string;
    menuType: MenuType;
    parentId: number;
    icon: string;
    perms: string;
    router: string;
    orderNum: number;
    createTime: Date;
    remark: string;
}

interface MenuForm {
    menuId?: number;
    menuName?: string;
    menuType?: MenuType;
    parentId?: number;
    icon?: string;
    perms?: string;
    router?: string;
    orderNum?: number;
    remark?: string;
}

interface TreedMenu {
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

export {
    type MenuView,
    type TreedMenu,
    type MenuForm,
    MenuType,
    getTreedMenu,
    getAuth,
}