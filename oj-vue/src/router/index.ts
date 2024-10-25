import {createRouter, createWebHistory, type NavigationGuardNext} from 'vue-router'
import Index from "@/views/system/Index.vue";
import Layout from "@/Layout.vue";
import Forbidden from "@/views/error/Forbidden.vue";
import NotFound from "@/views/error/NotFound.vue";
import {useMenuStore} from "@/stores/useMenuStore";
import {addDynamics} from "@/router/dynamic";

import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import {useToken} from "@/stores/useToken";
import Home from "@/views/Home.vue";
import __ from "lodash";

// index不是home
// index不是home
// index不是home


// 固定公共路由
export const constRoutes =  [
  {
    path: "/auth",
    component: () => import('@/views/system/authentication/Auth.vue'),
    redirect: "/auth/login",
    children: [
      {
        path: "login",
        name: "login",
        component: () => import('@/views/system/authentication/Login.vue'),
      },
      {
        path: "sign-up",
        name: "sign-up",
        component: () => import('@/views/system/authentication/SignUp.vue'),
      },
      {
        path: "forget-password",
        name: "forget-password",
        component: () => import('@/views/system/authentication/ForgetPassword.vue'),
      }
    ]
  },
  {
    path: '',
    component: Layout,
    name: 'container',
    redirect: "home",
    children: [
      {
        path: "home",
        name: "home",
        component: Home,
      },
      {
        path: "problems",
        name: "problems",
        component: () => import('@/views/system/ProblemSet.vue'),
        meta: {
          keepAlive: true,
        }
      },
      {
        path: "problem/:id",
        name: "problem",
        component: () => import('@/views/system/problem/ProblemDetail.vue')
      },
      {
        path: "contest",
        name: "contest",
        component: () => import('@/views/contest/Contest.vue'),
      },
      {
        path: "list",
        name: "list",
        component: () => import('@/views/list/List.vue'),
      },
      {
        path: "homework",
        name: "homework",
        component: () => import('@/views/homework/Homework.vue'),
      },
    ],
    meta: {
      requireAuth: true,
      name: "主页"
    }
  },
  {
    path: "/index",
    home: "index",
    component: Index,
    meta: {
      isLoginAccess: true,
    }
  },

  {
    path: '/403',
    name: '403',
    component: Forbidden,
    meta: {
      isLoginAccess: true,
    }
  },
  {
    path: '/404',
    name: '404',
    component: NotFound,
    meta: {
      isLoginAccess: true,
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

  NProgress.start()
  const needLogin = to.matched.some((v) => v.meta.requireAuth);
  const isLoginAccess = to.matched.some(v => v.meta.isLoginAccess);
  const isMatched = !__.isEmpty(to.matched)

  const menu = useMenuStore()
  const token = useToken();

  if (token.hasToken()) {
    // 已经登陆
    if (!menu.isDynamicReady()) {
      menu.getDynamicRouters().then((data) => {
        addDynamics(data, router)
        next({ ...to, replace: true })
      });
    } else {
      loginGuard(isMatched, needLogin, isLoginAccess, next);
    }

  } else {
    if (needLogin) {
    //   需要登录
      next({name: 'login', replace: true});
    } else {
      next();
    }
  }

});

router.afterEach(() => {
  NProgress.done();
});

export default router;
