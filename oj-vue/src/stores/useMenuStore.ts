import {defineStore} from "pinia";
import {getTreedMenu, type TreedMenu} from "@/api/auth/menu";
import {ref} from "vue";
import __ from "lodash";
import type {RouteRecordRaw} from "vue-router";

export type MenuLoadStatus = 'idle' | 'loading' | 'ready' | 'error';

export const useMenuStore = defineStore('menuStore', () => {
    const menuTrees = ref<TreedMenu[]>();
    const menu = ref<RouteRecordRaw[]>()
    const status = ref<MenuLoadStatus>('idle');
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
        status.value = 'idle';
    }

    const load = async (force = false): Promise<TreedMenu[]> => {
        if (!force && menuTrees.value !== undefined) {
            return menuTrees.value;
        }
        if (loadPromise) {
            return loadPromise;
        }

        const version = sessionVersion;
        status.value = 'loading';
        const request = getTreedMenu().then((data) => {
            const trees = data || [];
            if (version === sessionVersion) {
                menuTrees.value = trees;
                status.value = 'ready';
            }
            return trees;
        }).catch(error => {
            if (version === sessionVersion) status.value = 'error';
            throw error;
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

    return  {
        isDynamicReady,
        clear,
        load,
        getTree,
        setMenu,
        getMenu,
        getStatus: () => status.value
    }

}, {persist: false})
