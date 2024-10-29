import type {MenuType, MenuView} from "@/api/auth/menu";
import {type Router} from "vue-router";

const Layout = () => import("@/Layout.vue")
const Teacher = () => import("@/views/teacher/Teacher.vue");
const HomeworkManage = () => import("@/views/teacher/homework-manage/HomeworkManage.vue");
const MyClass = () => import("@/views/teacher/my-class/MyClass.vue");

const UserModule = () => import("@/views/user-module/UserModule.vue");
const ClassManage = () => import("@/views/user-module/class-manage/ClassManage.vue");
const RoleManage = () => import("@/views/user-module/role-manage/RoleManage.vue");
const UserManage = () => import("@/views/user-module/user-manage/UserManage.vue");
const AuthManage = () => import("@/views/user-module/auth-manage/AuthManage.vue");

const ProblemModule = () => import("@/views/problem-module/ProblemModule.vue");
const FolderEdit = () => import("@/views/problem-module/folder-edit/FolderEdit.vue");
const ListEdit = () => import("@/views/problem-module/list-edit/ListEdit.vue");
const ProblemEdit = () => import("@/views/problem-module/problem-edit/ProblemEdit.vue");
const TagEdit = () => import("@/views/problem-module/tag-edit/TagEdit.vue");
const ProblemEditView = () => import("@/views/problem-module/problem-edit/ProblemEditView.vue")

const RoleAuth = () => import("@/views/user-module/role-manage/RoleAuth.vue")

interface MetaType {
    name: string;
    icon?: string;
    requireAuth?: boolean;
    type?: MenuType;
    parent?: string;

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
                        name: "我的班级"
                    }
                },
                {
                    path: 'homework-manage',
                    name: 'homework-manage',
                    component: HomeworkManage,
                    meta: {
                        name: "作业管理"
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
    }
]

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

