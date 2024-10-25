import {get} from "@/utils/http";

enum MenuType {
    MENU = 'M',
    MENU_ITEM = 'I',
    BUTTON = 'B'
}
interface MenuView {
    menuId: number;
    parentId: number;
    menuName: string;
    icon: string;
    router: string;
    menuType: MenuType;
    orderNum: number;
    perms: string;
    createTime: Date;
    remark: string;
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
    MenuType,
    getTreedMenu,
    getAuth
}