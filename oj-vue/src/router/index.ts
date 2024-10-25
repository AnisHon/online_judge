import {createRouter, createWebHistory, type RouteLocationNormalizedGeneric} from 'vue-router'
import Index from "@/views/system/Index.vue";
import Layout from "@/Layout.vue";
import Forbidden from "@/views/error/Forbidden.vue";
import NotFound from "@/views/error/NotFound.vue";
import {useMenuStore} from "@/stores/useMenuStore";
import {addDynamics, type RouterType} from "@/router/dynamic";

import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import {useToken} from "@/stores/useToken";
import __ from  'lodash';

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
    redirect: "index",
    children: [
      {
        path: "index",
        name: "index",
        component: Index,
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
      }
    ],
    meta: {
      requireAuth: true,
      name: "主页"
    }
  },

  {
    path: '/403',
    name: '403',
    component: Forbidden
  },
  {
    path: '/*',
    name: '404',
    component: NotFound
  },


];



const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constRoutes
});


router.beforeEach((to, from, next) => {

  NProgress.start()

  const needLogin = to.matched.some((v) => v.meta.requireAuth);

  const menu = useMenuStore()
  const token = useToken();
  if (token.hasToken()) {
    // 已经登陆
    if (!menu.isDynamicReady()) {
      menu.getDynamicRouters().then((data) => {
        addDynamics(data, router)
        next(to.path)
      });
    } else {


      if (!needLogin) {

        next({name: 'index'})
      } else {
        next();
      }
    }

  } else {
    if (needLogin) {
    //   需要登录
      router.replace({name: 'login'});
    } else {
      next();
    }
  }

});

router.afterEach(() => {
  NProgress.done();
});

export default router;
