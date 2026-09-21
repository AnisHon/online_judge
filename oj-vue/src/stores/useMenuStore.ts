import {defineStore} from "pinia";
import {getTreedMenu, type TreedMenu} from "@/api/auth/menu";
import {ref} from "vue";
import __ from "lodash";
import type {RouteRecordRaw} from "vue-router";

export const useMenuStore = defineStore('menuStore', () => {
    const menuTrees = ref<TreedMenu[]>();
    const menu = ref<RouteRecordRaw[]>()
    let loadPromise: Promise<TreedMenu[]> | null = null;
    let sessionVersion = 0;

    const isDynamicReady = () => {
        return !__.isUndefined(menu.value);
    }

    const clear = () => {
        sessionVersion++;
        loadPromise = null;
        menuTrees.value = undefined;
        menu.value = undefined;
    }

    const load = async (force = false): Promise<TreedMenu[]> => {
        if (!force && menuTrees.value !== undefined) {
            return menuTrees.value;
        }
        if (loadPromise) {
            return loadPromise;
        }

        const version = sessionVersion;
        const request = getTreedMenu().then((data) => {
            const trees = data || [];
            if (version === sessionVersion) {
                menuTrees.value = trees;
            }
            return trees;
        });
        loadPromise = request;
        request.then(
            () => { if (loadPromise === request) loadPromise = null; },
            () => { if (loadPromise === request) loadPromise = null; },
        );
        return request;
    };


    const getTree = async () => {
        return load();
    };

    const setMenu = (raw: RouteRecordRaw[]) => {
        menu.value = raw;
    }

    const getMenu = (): RouteRecordRaw[] => {
        return menu.value || [];
    }

    const hasBackendAccess = (): boolean => {
        // 只有按钮权限（例如查看本人提交）不能作为进入后台的依据。
        return (menuTrees.value?.length || 0) > 0;
    }

    return  {
        isDynamicReady,
        clear,
        load,
        getTree,
        setMenu,
        getMenu,
        hasBackendAccess
    }

}, {persist: false})
