import {defineStore} from "pinia";
import {getTreedMenu, type MenuView, type TreedMenu} from "@/api/auth/menu";
import {getAuth} from "@/api/auth/menu"
import {ref} from "vue";

export const useMenuStore = defineStore('menuStore', () => {
    const auths = ref<MenuView[]>();
    const menuTrees = ref<TreedMenu[]>();

    const getAuths = async () => {
        auths.value = await getAuth();
        return auths.value;
    }

    const getMenuTrees = async () => {
        menuTrees.value = await getTreedMenu();
        return menuTrees.value;
    }

    return  {
        getAuth,
        getTreedMenu
    }

}, {persist: false})