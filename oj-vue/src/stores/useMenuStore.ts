import {defineStore} from "pinia";
import {getTreedMenu, type MenuView, type TreedMenu} from "@/api/auth/menu";
import {getAuth} from "@/api/auth/menu"
import {ref} from "vue";
import __ from "lodash";
import type {RouteRecordRaw} from "vue-router";

export const useMenuStore = defineStore('menuStore', () => {
    const auths = ref<MenuView[]>();
    const menuTrees = ref<TreedMenu[]>();
    const menu = ref<RouteRecordRaw[]>()

    const exist = (): boolean => {
        return __.has(auths, 'value');
    }

    const loadAuths = async () => {
        auths.value = await getAuth();
    }

    const getAuths = () => {
        return auths.value || [];
    }

    const getMenuTrees = async () => {
        if (!exist()) {
            menuTrees.value = await getTreedMenu();
        }
        return menuTrees.value;
    }

    const isDynamicReady = () => {
        return !__.isUndefined(menu.value);
    }

    const clear = () => {
        auths.value = undefined;
        menuTrees.value = undefined;
        menu.value = undefined;
    }


    const getTree = async () => {
        if (isDynamicReady()) {
            return <TreedMenu[]>menuTrees.value;
        }
        const data = await getMenuTrees();
        await loadAuths();
        return data || [];
    };

    const setMenu = (raw: RouteRecordRaw[]) => {
        menu.value = raw;
    }

    const getMenu = (): RouteRecordRaw[] => {
        return menu.value as RouteRecordRaw[];
    }

    return  {
        getAuths,
        isDynamicReady,
        clear,
        getTree,
        setMenu,
        getMenu
    }

}, {persist: false})