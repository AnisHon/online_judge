import {getTreedMenu, type MenuType, type TreedMenu} from "@/api/auth/menu";
import {type Router, type RouteRecordRaw} from "vue-router";
import router from "@/router/index.ts";
import LayoutBackEnd from "@/layout-backend/LayoutBackEnd.vue";

const Layout = import("@/layout/Layout.vue")
const Teacher = () => import("@/views/teacher/Teacher.vue");
const HomeworkManage = () => import("@/views/teacher/homework-manage/HomeworkManage.vue");
const ContestManage = () => import("@/views/teacher/contest-manage/ContestManage.vue");
const MyClass = () => import("@/views/teacher/my-class/MyClass.vue");

const UserModule = () => import("@/views/user-module/UserModule.vue");
const ClassManage = () => import("@/views/user-module/class-manage/ClassManage.vue");
const RoleManage = () => import("@/views/user-module/role-manage/RoleManage.vue");
const UserManage = () => import("@/views/user-module/user-manage/UserManage.vue");
const AuthManage = () => import("@/views/user-module/auth-manage/AuthManage.vue");

const ProblemModule = () => import("@/views/problem-module/ProblemModule.vue");
const FolderEdit = () => import("@/views/problem-module/folder-edit/FolderEdit.vue");
const ListEdit = () => import("@/views/problem-module/list-edit/ListEdit.vue");
const ListProblem = () => import("@/views/problem-module/list-edit/ListProblem.vue");
const ProblemEdit = () => import("@/views/problem-module/problem-edit/ProblemEdit.vue");
const TagEdit = () => import("@/views/problem-module/tag-edit/TagEdit.vue");
const ProblemEditView = () => import("@/views/problem-module/problem-edit/ProblemEditView.vue")

const RoleAuth = () => import("@/views/user-module/role-manage/RoleAuth.vue")

const Index = () => import("@/views/backend/index/Index.vue")

interface MetaType {
    name: string;
    icon?: string;
    requireAuth?: boolean;
    type?: MenuType;
    parent?: string;
    has?: string | string[];

}

interface RouterType {
    path: string;
    name: string;
    component: any;
    redirect?: string | object;
    meta: MetaType;
    children?: RouterType[];
}

const dynamicConst: RouterType = {
    path: '',
    component: Layout,
    name: 'container',
    redirect: "/index",
    meta: {
        requireAuth: true,
        name: "主页"
    },
    children: [
        {
            path: 'teacher',
            name: 'teacher',
            component: Teacher,
            meta: {
                name: "教师功能"
            },
            children: [
                {
                    path: 'my-class',
                    name: 'my-class',
                    component: MyClass,
                    meta: {
                        name: "用户组"
                    }
                },
                {
                    path: 'homework-manage',
                    name: 'homework-manage',
                    component: HomeworkManage,
                    meta: {
                        name: "作业管理"
                    }
                },
                {
                    path: 'contest-manage',
                    name: 'contest-manage',
                    component: ContestManage,
                    meta: {
                        name: "竞赛管理"
                    }
                }
            ]
        },
        {
            path: 'problem-module',
            name: 'problem-module',
            component: ProblemModule,
            meta: {
                name: "题目模块"
            },
            children: [
                {
                    path: 'problem-edit',
                    name: 'problem-edit',
                    component: ProblemEdit,
                    meta: {
                        name: "题目编辑"
                    }
                },
                {
                    path: 'tag-edit',
                    name: 'tag-edit',
                    component: TagEdit,
                    meta: {
                        name: "标签编辑"
                    }
                },
                {
                    path: 'list-edit',
                    name: 'list-edit',
                    component: ListEdit,
                    meta: {
                        name: "题单编辑"
                    }
                },
                {
                    path: 'folder-edit',
                    name: 'folder-edit',
                    component: FolderEdit,
                    meta: {
                        name: "目录编辑"
                    }
                },

            ]
        },
        {
            path: 'user-module',
            name: 'user-module',
            component: UserModule,
            meta: {
                name: "用户模块"
            },
            children: [
                {
                    path: 'user-manage',
                    name: 'user-manage',
                    component: UserManage,
                    meta: {
                        name: "用户管理"
                    }
                },
                {
                    path: 'class-manage',
                    name: 'class-manage',
                    component: ClassManage,
                    meta: {
                        name: "班级管理"
                    }
                },
                {
                    path: 'auth-manage',
                    name: 'auth-manage',
                    component: AuthManage,
                    meta: {
                        name: '权限管理'
                    }
                },
                {
                    path: 'role-manage',
                    name: 'role-manage',
                    component: RoleManage,
                    meta: {
                        name: "角色管理"
                    }
                }
            ]
        }
    ]
};


// 动态路由
export const dynamicRouter: RouteRecordRaw = {
    path: "/backend",
    name: "backend",
    redirect: "index",
    component: LayoutBackEnd,
    meta: {
        name: "首页"
    },
    children: [
        {
            path: 'user-module/role-manage/role-auth/:id',
            name: 'role-auth',
            component: RoleAuth,
            meta: {
                has: ["user:role:grant", "user:role:revoke", "user:user:list"],
                name: "用户角色",
                parent: 'role-manage',
            }
        },
        {
            path: 'problem-module/problem-edit/edit-problem',
            name: 'edit-problem',
            component: ProblemEditView,
            meta: {
                has: ["problem:problem:list", "problem:list:add-problem", "problem:list:del-problem"],
                name: "编辑题目",
                parent: 'problem-edit',
            }
        },
        {
            path: 'problem-module/problem-edit/list-problem/:id',
            name: 'list-problem',
            component: ListProblem,
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
        path: "",
        name: "backend-index",
        component: Index,
        meta: {
            name: "首页",
            icon: ""
        }
    },
]


const buildRouterRaw = (treedMenu: TreedMenu): RouteRecordRaw => {
    const menu = treedMenu.menu;
    const routerRecordRaw: RouteRecordRaw = {
        path: menu.router,
        name: menu.router,
        component: () => import(menu.component),
        meta: {
            name: menu.menuName,
            icon: menu.icon,
        }
    }
    if (treedMenu.children && treedMenu.children.length > 0) {
        // @ts-ignore
        routerRecordRaw.redirect = treedMenu.children[0].menu.router;
    }
    return routerRecordRaw;
}


const recursiveSetRouters = (treedMenus: TreedMenu[]): RouteRecordRaw[] => {
    if (!treedMenus || treedMenus.length === 0) {
        return [];
    }

    // 最终结果集合
    const routers: RouteRecordRaw[] = []

    for (const treedMenu of treedMenus) {

        // 将当前树节点构建成 RouterRecordRaw
        const routerRaw = buildRouterRaw(treedMenu);

        // 递归得到子路由
        routerRaw.children = recursiveSetRouters(treedMenu.children);

        // 存入
        routers.push(routerRaw);
    }

    return routers;
}


// 加载最终menu
export const loadDynamicRouters = async () => {
    const treedMenus = await getTreedMenu();

    menuTree.push(...recursiveSetRouters(treedMenus));

    dynamicRouter.children.push(...menuTree);

    router.addRoute(dynamicRouter);
}



const additional: RouterType[] = [
    {
        path: 'role-auth/:id',
        name: 'role-auth',
        component: RoleAuth,
        meta: {
            name: "用户角色",
            parent: 'role-manage',
        }
    },
    {
        path: 'edit-problem',
        name: 'edit-problem',
        component: ProblemEditView,
        meta: {
            name: "编辑题目",
            parent: 'problem-edit',
        }
    },
    {
        path: 'list-problem/:id',
        name: 'list-problem',
        component: ListProblem,
        meta: {
            has: ["problem:problem:add", "problem:problem:add"],
            name: "列表题目编辑",
            parent: 'list-edit',
        }
    }
]
// list-problem
const addDynamics = (dynamicRouters: RouterType[], router: Router) => {
    dynamicRouters.forEach((dynamicRouter: RouterType) => {
        // @ts-ignore
        router.addRoute('container', dynamicRouter);
    })
}

const addAdditional =  (router: Router) => {
    additional.forEach((v) => {
        // @ts-ignore
        if (router.hasRoute(v.meta.parent)) {
            // @ts-ignore
            router.addRoute(v.meta.parent, v)
        }
    })
}













export {
    type RouterType,
    dynamicConst,
    addDynamics,
    addAdditional
}

