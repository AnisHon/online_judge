import {type TreedMenu} from "@/api/auth/menu";
import {type RouteRecordRaw} from "vue-router";
import router from "@/router/index.ts";
import {useMenuStore} from "@/stores/useMenuStore.ts";
import __ from "lodash";
import {hasPerm} from "@/utils/authUtil.ts";
import {useUserStore} from "@/stores/useUserStore.ts";

// 动态路由
export const dynamicRoute: RouteRecordRaw = {
    path: "/backend",
    name: "backend",
    redirect: () => ({name: "backend-index"}),
    component: () => import("@/layout-backend/LayoutBackEnd.vue"),
    meta: {
        name: "首页",
        isLoginAccess: true,
    },
    children: [
        {
            path: 'user-module/role-manage/role-auth/:id',
            name: 'role-auth',
            component: () => import("@/views/backend/user-module/role-manage/RoleAuth.vue"),
            meta: {
                has: ["user:role:grant", "user:role:revoke", "user:user:list"],
                name: "用户角色",
                parent: 'role-manage',
            }
        },
        {
            path: 'problem-module/problem-edit/edit-problem',
            name: 'edit-problem',
            component: () => import("@/views/backend/problem-module/problem-edit/ProblemEditView.vue"),
            meta: {
                has: ["problem:problem:list", "problem:list:add-problem", "problem:list:del-problem"],
                name: "编辑题目",
                parent: 'problem-edit',
            }
        },
        {
            path: 'problem-module/list-edit/list-problem/:id',
            name: 'list-problem',
            component: () => import("@/views/backend/problem-module/list-edit/ListProblem.vue"),
            meta: {
                has: ["problem:problem:add", "problem:problem:remove"],
                name: "列表题目编辑",
                parent: 'list-edit',
            }
        }
    ]
}


// 构建后用于递归生成menu
export const menuTree: RouteRecordRaw[] = [
    {
        path: "index",
        name: "backend-index",
        component: () => import("@/views/backend/index/Index.vue"),
        meta: {
            name: "首页",
            icon: "HomeFilled",
            path: "/backend/index",
        }
    },
]

// import对象用于加载路由
const modules = import.meta.glob('../views/**/*.vue')

// 构建RouterRaw对象
const buildRouteRaw = (treedMenu: TreedMenu, path: string): RouteRecordRaw => {

    const menu = treedMenu.menu;



    const routerRecordRaw: RouteRecordRaw = {
        path: menu.router,
        name: menu.router,
        component: modules[`../views/${menu.component}.vue`],
        meta: {
            name: menu.menuName,
            icon: menu.icon,
            path: path,

        }
    }
    if (!!treedMenu.children && treedMenu.children.length > 0) {
        // @ts-ignore
        routerRecordRaw.redirect = treedMenu.children[0].menu.router;
    }
    if (!menu.component) {
        // @ts-ignore
        routerRecordRaw.component = undefined;

    }
    return routerRecordRaw;
}


const recursiveBuildRoutes = (treedMenus: TreedMenu[], parent: string): RouteRecordRaw[] => {
    if (!treedMenus || treedMenus.length === 0) {
        return [];
    }



    // 最终结果集合
    const routers: RouteRecordRaw[] = []

    for (const treedMenu of treedMenus) {

        const currentPath = `${parent}/${treedMenu.menu.router}`

        // 将当前树节点构建成 RouterRecordRaw
        const routerRaw = buildRouteRaw(treedMenu, currentPath);

        // 递归得到子路由
        routerRaw.children = recursiveBuildRoutes(treedMenu.children, currentPath);

        // 存入
        routers.push(routerRaw);
    }

    return routers;
}

// 过滤一下上面那几个固定的动态路由
export const filterDynamic = async () => {
    await useUserStore().loadUser();
    dynamicRoute.children = __.filter(dynamicRoute.children, (data) => {
        // @ts-ignore
        return hasPerm(data.meta.has);
    })
}

// 加载最终menu
export const loadDynamicRoutes = async () => {
    const menuStore = useMenuStore();

    await filterDynamic();

    const treedMenus = await menuStore.getTree();

    menuTree.push(...recursiveBuildRoutes(treedMenus, "/backend"));

    dynamicRoute.children.push(...menuTree);

    router.addRoute(dynamicRoute);

    // console.log(menuTree);

    menuStore.setMenu(menuTree);
}

