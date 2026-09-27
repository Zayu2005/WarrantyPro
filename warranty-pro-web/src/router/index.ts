import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    requiresAuth?: boolean
    hint?: string
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/views/LayoutView.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '运营看板', requiresAuth: true },
      },
      {
        path: 'orders',
        name: 'orders',
        component: () => import('@/views/PlaceholderView.vue'),
        meta: { title: '工单池', requiresAuth: true, hint: '工单受理 · 保修判定复核 · 派单确认 · 催办 —— 迭代 2 排期开发' },
      },
      {
        path: 'warranty-ledger',
        name: 'warrantyLedger',
        component: () => import('@/views/PlaceholderView.vue'),
        meta: { title: '保修台账', requiresAuth: true, hint: '设施设备台账 · 保修到期预警 —— 迭代 4 排期开发' },
      },
      {
        path: 'system',
        name: 'system',
        component: () => import('@/views/PlaceholderView.vue'),
        meta: { title: '系统管理', requiresAuth: true, hint: '用户与角色 · 保修规则 · 系统参数 —— 迭代 3 / 4 排期开发' },
      },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

const TOKEN_KEY = 'wp_token'

router.beforeEach((to) => {
  document.title = `${to.meta.title ?? 'WarrantyPro'} · WarrantyPro`
  if (to.meta.requiresAuth && !localStorage.getItem(TOKEN_KEY)) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

export default router
