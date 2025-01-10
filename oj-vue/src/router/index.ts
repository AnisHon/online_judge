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
import {useTabStore} from "@/stores/useTabStore.ts";

// index不是home
// index不是home
// index不是home


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
        path: "home",
        name: "home",
        component: Home,
        meta: {
          name: "首页",
        }
      },
      {
        path: "problems",
        name: "problems",
        component: () => import('@/views/problem/ProblemSet.vue'),
        meta: {
          name: "题库"
        }
      },
      {
        path: "problem/:id",
        name: "problem",
        component: () => import('@/views/problem/Problem.vue'),
        meta: {
          name: "题目详情"
        }
      },
      {
        path: "contest",
        name: "contest",
        component: () => import('@/views/contest/Contest.vue'),
        meta: {
          name: "比赛"
        }
      },
      {
        path: "contest-problems/:id",
        name: "contest-problems",
        component: () => import('@/views/contest/ContestProblems.vue'),
        meta: {
          name: "进入"
        }
      },
      {
        path: "list",
        name: "list",
        component: () => import('@/views/list/List.vue'),
        meta: {
          name: "列表"
        }
      },
      {
        path: "homework",
        name: "homework",
        component: () => import('@/views/homework/Homework.vue'),
        meta: {
          name: "作业"
        }
      },
      {
        path: "check-in",
        name: "check-in",
        component: () => import('@/views/check-in/CheckIn.vue'),
        meta: {
          name: "签到"
        }
      },
      {
        path: "setting",
        name: "setting",
        component: () => import('@/views/setting/Setting.vue'),
        meta: {
          name: "设置"
        }
      }
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



const loginGuard = (isMatched: boolean, needLogin: boolean, isLoginAccess: boolean, next: NavigationGuardNext) => {
  if (!isMatched) {
    next({name: '404', replace: true})
  } else if (!needLogin && !isLoginAccess) {
    next({name: '403', replace: true})
  } else {
    next()
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
      loginGuard(isMatched, needLogin, isLoginAccess, next);
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

router.afterEach(() => {
  try {
    NProgress.done();
  } catch (ignore) {}
});

export default router;
