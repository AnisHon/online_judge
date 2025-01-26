import {createRouter, createWebHistory, type NavigationGuardNext, type RouteLocationNormalizedGeneric} from 'vue-router'
import Index from "@/views/Index.vue";
import Layout from "@/layout/Layout.vue";
import Forbidden from "@/views/error/Forbidden.vue";
import NotFound from "@/views/error/NotFound.vue";
import {useMenuStore} from "@/stores/useMenuStore";
import {loadDynamicRoutes} from "@/router/dynamic";

import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import {useToken} from "@/stores/useToken";
import Home from "@/views/Home.vue";
import __ from "lodash";

// index不是home
// index不是home
// index不是home
export const constMenu = [
  {
    path: "home",
    name: "home",
    component: Home,
    meta: {
      name: "首页",
      icon: "HomeFilled",
      path: "/home"
    }
  },
  {
    path: "problems",
    name: "problems",
    component: () => import('@/views/problem/ProblemSet.vue'),
    meta: {
      name: "题库",
      icon: "Document",
      path: "/problems"
    }
  },
  {
    path: "contest",
    name: "contest",
    component: () => import('@/views/contest/Contest.vue'),
    meta: {
      name: "比赛",
      icon: "DataBoard",
      path: "/contest"
    }
  },
  {
    path: "list",
    name: "list",
    component: () => import('@/views/list/List.vue'),
    meta: {
      name: "题单",
      icon: "List",
      path: "/list"
    }
  },
  {
    path: "homework",
    name: "homework",
    component: () => import('@/views/homework/Homework.vue'),
    meta: {
      name: "作业",
      icon: "Notebook",
      path: "/homework"
    }
  },
  {
    path: "solutions",
    name: "solutions",
    component: () => import('@/views/solutions/solutions.vue'),
    meta: {
      name: "题解",
      icon: "EditPen",
      path: "/solutions"
    }
  },
  {
    path: "check-in",
    name: "check-in",
    component: () => import('@/views/check-in/CheckIn.vue'),
    meta: {
      name: "签到",
      icon: "CircleCheckFilled",
      path: "/check-in"
    }
  },
  {
    path: "notification",
    name: "notification",
    component: () => import('@/views/notification/Notification.vue'),
    meta: {
      name: "公告",
      icon: "Notification",
      path: "/notification"
    }
  },
  {
    path: "materials",
    name: "materials",
    component: () => import('@/views/materials/Materials.vue'),
    meta: {
      name: "资料",
      icon: "FolderOpened",
      path: "/materials"
    }
  }
]

// 固定公共路由
export const constRoutes =  [
  {
    path: "/auth",
    component: () => import('@/views/authentication/Auth.vue'),
    redirect: "/auth/login",
    children: [
      {
        path: "login",
        name: "login",
        component: () => import('@/views/authentication/Login.vue'),
        meta: {
          name: "登录",
        }
      },
      {
        path: "sign-up",
        name: "sign-up",
        component: () => import('@/views/authentication/SignUp.vue'),
        meta: {
          name: "注册",
        }
      },
      {
        path: "forget-password",
        name: "forget-password",
        component: () => import('@/views/authentication/ForgetPassword.vue'),
        meta: {
          name: "找回密码",
        }
      }
    ]
  },
  {
    path: '',
    component: Layout,
    name: 'container',
    redirect: "home",
    meta: {
      requireAuth: true,
      name: "主页"
    },
    children: [
      {
        path: "problem/:id",
        name: "problem",
        component: () => import('@/views/problem/Problem.vue'),
        meta: {
          name: "题目详情",
        }
      },
      {
        path: "setting",
        name: "setting",
        component: () => import('@/views/setting/Setting.vue'),
        meta: {
          name: "设置"
        }
      },
      {
        path: "contest/problems/:id",
        name: "contest-problems",
        component: () => import('@/views/contest/ContestProblems/ContestProblems.vue'),
        meta: {
          name: "比赛中"
        }
      },
      {
        path: "solution/:id",
        name: "solution",
        component: () => import('@/views/solutions/solution/solution.vue'),
        meta: {
          name: "题解详情"
        }
      },
      {
        path: "solution/edit",
        name: "solution_edit",
        component: () => import('@/views/solutions/edit-solution/EditSolution.vue'),
        meta: {
          name: "题解编辑"
        }
      },
      ...constMenu
    ]
  },
  {
    path: "/index",
    name: "index",
    component: Index,
    meta: {
      isLoginAccess: true,
      name: "在线教育"
    }
  },
  {
    path: '/403',
    name: '403',
    component: Forbidden,
    meta: {
      isLoginAccess: true,
      name: "拒绝访问"
    }
  },
  {
    path: '/404',
    name: '404',
    component: NotFound,
    meta: {
      isLoginAccess: true,
      name: "404"
    }
  },
];



const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constRoutes
});



const loginGuard = (isMatched: boolean, needLogin: boolean, isLoginAccess: boolean, next: NavigationGuardNext, to: RouteLocationNormalizedGeneric) => {
  if (!isMatched) {
    next({name: '404', replace: true})
  } else if (!needLogin && !isLoginAccess) {
    next({name: '403', replace: true})
  } else {
    next();
  }
}

router.beforeEach((to, from, next) => {

  try {
    NProgress.start()
  } catch (ignore) {}
  const needLogin = to.matched.some((v) => v.meta.requireAuth);
  const isLoginAccess = to.matched.some(v => v.meta.isLoginAccess);
  const isMatched = !__.isEmpty(to.matched)

  const menu = useMenuStore()
  const token = useToken();

  if (token.hasToken()) {
    // 已经登陆
    if (!menu.isDynamicReady()) {

      // 动态路由没有加载成功，加载路由
      loadDynamicRoutes().then(() => {
        next({ ...to, replace: true })
      })

    } else {
      loginGuard(isMatched, needLogin, isLoginAccess, next, to);
    }

  } else {
    if (needLogin || !isMatched) {
    //   需要登录
      next({name: 'login', replace: true});
    } else {
      next();
    }
  }

});

router.afterEach((to) => {

  document.title = <string>to.meta?.name || "OJ平台"

  try {
    NProgress.done();
  } catch (ignore) {}
});

export default router;
