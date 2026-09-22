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
        return status.value === 'ready' && !__.isUndefined(menu.value);
    }

    const clear = () => {
        sessionVersion++;
        loadPromise = null;
        menuTrees.value = undefined;
        menu.value = undefined;
        status.value = 'idle';
    }

    // 权限资源 CRUD 后使当前账号的菜单快照失效，避免动态路由继续使用旧树。
    const invalidate = () => {
        sessionVersion++;
        loadPromise = null;
        menuTrees.value = undefined;
        status.value = 'idle';
    }

    const load = async (force = false): Promise<TreedMenu[]> => {
        if (!force && menuTrees.value !== undefined) {
            return menuTrees.value;
        }
        if (loadPromise && !force) {
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
        // 路由构建器会继续复用自己的临时数组，菜单组件不能持有那个可变引用。
        menu.value = raw.slice();
    }

    const getMenu = (): RouteRecordRaw[] => {
        return menu.value ? menu.value.slice() : [];
    }

    return  {
        isDynamicReady,
        clear,
        invalidate,
        load,
        getTree,
        setMenu,
        getMenu,
        getStatus: () => status.value
    }

}, {persist: false})
