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
                component: 'RoleAuth',
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
                has: ["problem:problem:list", "problem:list:add-problem", "problem:list:del-problem"],
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
                has: ["problem:problem:add", "problem:problem:remove"],
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
                has: ['problem:contest:edit'],
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

const len = menuTree.length;

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
    } else {
        const pattens = menu.component.split("/");
        routerRecordRaw.meta!.component = pattens[pattens.length - 1];
    }
    // console.log(menu.component, modules[`../views/${menu.component}.vue`],)
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

    if (len == menuTree.length) {
        menuTree.push(...recursiveBuildRoutes(treedMenus, "/backend"));
        dynamicRoute.children.push(...menuTree);
        router.addRoute(dynamicRoute);
    }
    menuStore.setMenu(menuTree);
}

