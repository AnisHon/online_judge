import { createRouter, createWebHistory } from 'vue-router'
import Index from "@/views/system/Index.vue";
import Layout from "@/Layout.vue";
import Forbidden from "@/views/system/error/Forbidden.vue";
import NotFound from "@/views/system/error/NotFound.vue";
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
    mate: {
      requireAuth: true,
      name: "主页"
    }
  },

  {
    path: '/403',
    component: Forbidden
  },
  {
    path: '/*',
    component: NotFound
  },


]

// 动态路由
export const dynamicRoute = [
  {
    path: '',
    component: Layout,
    name: 'container',
    redirect: "/index",
    mate: {
      requireAuth: true,
      name: "主页"
    }
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: constRoutes
})

export default router
