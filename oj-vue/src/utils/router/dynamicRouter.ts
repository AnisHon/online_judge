import {MenuType, type MenuView, type TreedMenu} from "@/api/auth/menu";
import {type RouterType} from "@/router/dynamic";
import __ from 'lodash';

const flattenMenuTree = (treedMenu: TreedMenu[]): MenuView[] => {
    const result: MenuView[] = [];
    if (!treedMenu) {
        return [];
    }
    treedMenu.forEach((menu) => {
        result.push(menu.menu);
        result.push(...flattenMenuTree(menu.children));
    });
    return result;
}

const setDefault = (constRouter: RouterType[]) => {
    constRouter.forEach((v) => {
        if (!__.isEmpty(v.children)) {
            const array = (<RouterType[]>v.children);
            v.redirect = {name: array[0].name};
            setDefault(array);
        }
    })
}


const getDynamicRecursion = (constRouter: RouterType[], flattenMenu: MenuView[]) => {

    const result: RouterType[] = []
    if (!constRouter) {
        return [];
    }
    constRouter.forEach((router: RouterType) => {
        flattenMenu.forEach((menu: MenuView) => {
            if (menu.menuName === router.meta.name) {
                router.path = __.isEmpty(menu.router) ? router.path : menu.router;
                router.meta.icon = menu.icon;
                router.meta.type = menu.menuType;
                let children: RouterType[] | undefined = undefined;
                if (router.children && router.children.length > 0) {
                    children = getDynamicRecursion(router.children, flattenMenu)
                }
                router.children = children;
                if (router.meta.type === MenuType.MENU_ITEM || !__.isEmpty(children)) {
                    result.push(router);
                }
            }
        })

    })
    return result;
}

export {
    flattenMenuTree,
    getDynamicRecursion,
    setDefault
}