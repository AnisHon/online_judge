import {type TreedMenu} from "@/api/auth/menu";
import {RouterView, type RouteRecordRaw} from "vue-router";
import router from "@/router/index.ts";
import {useMenuStore} from "@/stores/useMenuStore.ts";
import {hasAnyPerm, hasPerm} from "@/utils/authUtil.ts";
import {useUserStore} from "@/stores/useUserStore.ts";
import {MenuType} from "@/api/auth/menu.ts";

// 动态路由
export const dynamicRoute: RouteRecordRaw = {
    path: "/backend",
    name: "backend",
    redirect: () => ({name: "backend-index"}),
    component: () => import("@/layout-backend/LayoutBackEnd.vue"),
    meta: {
        name: "首页",
        requireAuth: true,
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
                component: 'RoleAuth',
                noKeepAlive: true
            }
        },
        {
            path: 'user-module/class-manage/student/:classId',
            name: 'student-manage',
            component: () => import("@/views/backend/user-module/class-manage/user/StudentManage.vue"),
            meta: {
                has: ['user:class:edit'],
                name: "学生管理",
                component: 'StudentManage',
                noKeepAlive: true,
            }
        },
        {
            path: 'problem-module/problem-edit/edit-problem',
            name: 'edit-problem',
            strict: true,
            sensitive: true,
            component: () => import("@/views/backend/problem-module/problem-edit/ProblemEditView.vue"),
            meta: {
                has: ["problem:problem:list"],
                name: "编辑题目",
                component: 'ProblemEditView',
                noKeepAlive: true,
            }
        },
        {
            path: 'problem-module/list-edit/list-problem/:id',
            name: 'list-problem',
            component: () => import("@/views/backend/problem-module/list-edit/ListProblem.vue"),
            meta: {
                hasAny: ["problem:list:add-problem", "problem:list:del-problem", "problem:problem:list"],
                name: "列表题目编辑",
                component: 'ListProblem',
                noKeepAlive: true,
            }
        },
        {
            path: 'teacher/contest-manage/user/joined/:contestId',
            name: 'user-joined',
            component: () => import("@/views/backend/teacher/contest-manage/user-joined/UserJoined.vue"),
            meta: {
                hasAny: ['problem:contest:list', 'user:user:list'],
                name: "参加管理",
                component: 'UserJoined',
                noKeepAlive: true,
            }
        },
        {
            path: 'teacher/contest-manage/problem-statistic/:contestId',
            name: 'problem-statistic',
            component: () => import("@/views/backend/teacher/contest-manage/problem-statistic/ProblemStatistic.vue"),
            meta: {
                has: ['problem:contest:statistic'],
                name: "题目统计",
                component: 'ProblemStatistic',
                noKeepAlive: true,
            }
        },
        {
            path: 'teacher/contest-manage/problem-scores/:contestId/:problemId',
            name: 'problem-scores',
            component: () => import("@/views/backend/teacher/contest-manage/problem-scores/ProblemScores.vue"),
            meta: {
                has: ['problem:contest:statistic'],
                name: "题目分数",
                component: 'ProblemScores',
                noKeepAlive: true,
            }
        },
        {
            path: 'teacher/contest-manage/user-statistic/:contestId',
            name: 'user-statistic',
            component: () => import("@/views/backend/teacher/contest-manage/user-statistic/UserStatistic.vue"),
            meta: {
                has: ['problem:contest:statistic'],
                name: "用户统计",
                component: 'UserStatistic',
                noKeepAlive: true,
            }
        },
        {
            path: 'teacher/contest-manage/user-scores/:contestId/:userId',
            name: 'user-scores',
            component: () => import("@/views/backend/teacher/contest-manage/user-scores/UserScores.vue"),
            meta: {
                has: ['problem:contest:statistic'],
                name: "用户分数",
                component: 'UserScores',
                noKeepAlive: true,
            }
        },
        {
            path: 'teacher/contest-manage/user-answer/:contestId/:userId/:problemId',
            name: 'user-answer',
            component: () => import("@/views/backend/teacher/contest-manage/user-answer/ProblemAnswer.vue"),
            meta: {
                has: ['problem:contest:statistic'],
                name: "用户答案",
                component: 'ProblemAnswer',
                noKeepAlive: true,
            }
        },
        {
            path: 'teacher/contest-manage/supplement/:contestId',
            name: 'supplement',
            component: () => import("@/views/backend/teacher/contest-manage/supplement/Supplement.vue"),
            meta: {
                has: ['problem:contest:edit'],
                name: "设置迟交",
                component: 'Supplement',
                noKeepAlive: true,
            }
        },
        {
            path: 'problem-module/problem-edit/case-edit/:problemId',
            name: 'case-edit',
            component: () => import("@/views/backend/problem-module/problem-edit/case-edit/CaseEdit.vue"),
            meta: {
                has: ['problem:problem:edit', 'problem:problem:list'],
                name: "题例编辑",
                component: 'CaseEdit',
                noKeepAlive: true,
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

const staticMenuTree = [...menuTree];
const fixedDynamicChildren = [...dynamicRoute.children];
let routeLoadPromise: Promise<void> | null = null;
let routeGeneration = 0;

// import对象用于加载路由
const modules = import.meta.glob('../views/**/*.vue')

const normalizeSegment = (value: string | undefined) => String(value || '').replace(/^\/+|\/+$/g, '');

const createRouteName = (routerPath: string, menuId: unknown, usedNames: Set<string>) => {
    const base = normalizeSegment(routerPath).replace(/[^a-zA-Z0-9_-]+/g, '-') || `menu-${String(menuId || 'unknown')}`;
    let name = base;
    if (usedNames.has(name)) name = `${base}-${String(menuId || usedNames.size)}`;
    let suffix = 2;
    while (usedNames.has(name)) name = `${base}-${String(menuId || 'unknown')}-${suffix++}`;
    usedNames.add(name);
    return name;
};

// 构建 RouterRecordRaw。只有菜单/菜单项进入路由，按钮权限仍由 v-has 控制。
const buildRouteRaw = (
    treedMenu: TreedMenu,
    path: string,
    children: RouteRecordRaw[],
    usedNames: Set<string>,
): RouteRecordRaw | null => {
    const menu = treedMenu.menu;
    const componentPath = menu.component ? `../views/${menu.component}.vue` : '';
    const component = componentPath ? modules[componentPath] : undefined;
    if (menu.component && !component) {
        console.warn(`[router] 忽略不存在的动态组件: ${componentPath}`);
        return null;
    }
    if (!menu.component && children.length === 0) return null;
    const routeName = createRouteName(menu.router, menu.menuId, usedNames);
    const routerRecordRaw: RouteRecordRaw = {
        path: normalizeSegment(menu.router),
        name: routeName,
        component: component || RouterView,
        children,
        meta: {
            name: menu.menuName,
            icon: menu.icon,
            path: path,
            menuType: menu.menuType,
            permission: menu.perms,
        }
    }
    if (children.length > 0) {
        routerRecordRaw.redirect = children[0].path;
    }
    if (menu.component) {
        const pattens = menu.component.split("/");
        routerRecordRaw.meta!.component = pattens[pattens.length - 1];
    }
    return routerRecordRaw;
}


const recursiveBuildRoutes = (treedMenus: TreedMenu[], parent: string, usedNames: Set<string>): RouteRecordRaw[] => {
    if (!treedMenus || treedMenus.length === 0) {
        return [];
    }



    // 最终结果集合
    const routers: RouteRecordRaw[] = []

    for (const treedMenu of treedMenus) {
        const menu = treedMenu.menu;
        if (!menu || menu.menuType === MenuType.BUTTON) continue;
        const segment = normalizeSegment(menu.router);
        if (!segment) continue;
        const currentPath = `${parent}/${segment}`;
        const children = recursiveBuildRoutes(treedMenu.children || [], currentPath, usedNames);
        const routerRaw = buildRouteRaw(treedMenu, currentPath, children, usedNames);
        if (routerRaw) routers.push(routerRaw);
    }

    return routers;
}

// 过滤一下上面那几个固定的动态路由
export const filterDynamic = async (): Promise<RouteRecordRaw[]> => {
    const userStore = useUserStore();
    await userStore.loadUser();
    return fixedDynamicChildren.filter((data) => {
        const meta = data.meta as {has?: string | string[]; hasAny?: string | string[] } | undefined;
        return hasPerm(meta?.has) && hasAnyPerm(meta?.hasAny);
    });
}

export const resetDynamicRoutes = () => {
    routeGeneration++;
    routeLoadPromise = null;
    if (router.hasRoute('backend')) {
        router.removeRoute('backend');
    }
    menuTree.splice(0, menuTree.length, ...staticMenuTree);
    dynamicRoute.children = [...fixedDynamicChildren];
};

/**
 * 用于退出登录时中断“正在加载动态路由”的旧会话请求。
 * 仅检查 backend 是否已注册不够，因为请求可能还停留在接口等待阶段。
 */
export const isDynamicLoading = () => routeLoadPromise !== null;

// 加载最终menu
export const loadDynamicRoutes = async () => {
    const menuStore = useMenuStore();
    if (menuStore.isDynamicReady() && router.hasRoute('backend')) return;
    if (routeLoadPromise) return routeLoadPromise;

    const generation = routeGeneration;
    const request = (async () => {
        const permittedFixedChildren = await filterDynamic();
        const treedMenus = await menuStore.getTree();
        if (generation !== routeGeneration) return;

        // 权限可能在不同账号之间切换，不能复用上一个账号已经拼接过的全局路由。
        if (router.hasRoute('backend')) {
            router.removeRoute('backend');
        }
        menuTree.splice(0, menuTree.length, ...staticMenuTree);
        const usedNames = new Set<string>(['backend', ...permittedFixedChildren.map(route => String(route.name))]);
        const dynamicChildren = recursiveBuildRoutes(treedMenus, "/backend", usedNames);
        menuTree.push(...dynamicChildren);
        dynamicRoute.children = [...permittedFixedChildren, ...menuTree];
        router.addRoute(dynamicRoute);
        menuStore.setMenu(menuTree);
    })();
    routeLoadPromise = request;
    request.then(
        () => { if (routeLoadPromise === request) routeLoadPromise = null; },
        () => { if (routeLoadPromise === request) routeLoadPromise = null; },
    );
    return request;
}
