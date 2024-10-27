import {defineStore} from "pinia";
import {getTreedMenu, type MenuView, type TreedMenu} from "@/api/auth/menu";
import {getAuth} from "@/api/auth/menu"
import {ref} from "vue";
import __ from "lodash";
import {flattenMenuTree, getDynamicRecursion, setDefault} from "@/utils/router/dynamicRouter";
import {dynamicConst, type RouterType} from "@/router/dynamic";

export const useMenuStore = defineStore('menuStore', () => {
    const auths = ref<MenuView[]>();
    const menuTrees = ref<TreedMenu[]>();
    const dynamicRouters = ref<RouterType[]>()

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

    const getFlatten = async () => {
        if (!exist()) {
            await getMenuTrees()
        }
        return flattenMenuTree(menuTrees.value as TreedMenu[])
    }

    const isDynamicReady = () => {
        return !__.isUndefined(dynamicRouters.value);
    }

    const getDynamicRouters = async () => {
        const routers = dynamicConst.children;
        const flatten = await getFlatten();

        if (!isDynamicReady()) {
            const dynamicRecursion = getDynamicRecursion(<RouterType[]>routers, flatten);
            setDefault(dynamicRecursion);
            dynamicRouters.value = dynamicRecursion;
        }
        await loadAuths()
        return <RouterType[]>dynamicRouters.value;
    }

    const clear = () => {
        auths.value = undefined;
        menuTrees.value = undefined;
        dynamicRouters.value = undefined;
    }

    return  {
        getAuths,
        getTreedMenu,
        getFlatten,
        getDynamicRouters,
        isDynamicReady,
        clear
    }

}, {persist: false})