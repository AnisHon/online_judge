import type {TreedMenu} from "@/api/auth/menu";

const setTreeId = (menu: TreedMenu[]) => {
    menu.forEach((item) => {
        item.id = item.menu.menuId;
        if (item.children && item.children.length) {
            setTreeId(item.children);
        }
    })
}

export {
    setTreeId
}