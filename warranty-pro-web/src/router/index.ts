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
        component: () => import('@/views/OrdersPoolView.vue'),
        meta: { title: '工单池', requiresAuth: true },
      },
      {
        path: 'schedule',
        name: 'schedule',
        component: () => import('@/views/ScheduleView.vue'),
        meta: { title: '师傅排班', requiresAuth: true },
      },
      {
        path: 'agent-workflow',
        name: 'agentWorkflow',
        component: () => import('@/views/WorkflowView.vue'),
        meta: { title: '智能体工作流', requiresAuth: true },
      },
      {
        path: 'warranty-ledger',
        name: 'warrantyLedger',
        component: () => import('@/views/ModuleStatusView.vue'),
        meta: { title: '设施台账', requiresAuth: true, hint: '设施台账数据接口尚未接入。' },
      },
      {
        path: 'system',
        name: 'system',
        component: () => import('@/views/ModuleStatusView.vue'),
        meta: { title: '系统管理', requiresAuth: true, hint: '请使用左侧的用户管理和角色管理查看现有系统数据。' },
      },
      ...[
        ['workers', '维修人员'],
        ['reviews', '维修评价'],
        ['archives', '工单归档'],
        ['repair-categories', '报修类别'],
        ['owners', '业主管理'],
        ['notices', '物业通知'],
        ['banners', '轮播管理'],
        ['users', '用户管理'],
        ['roles', '角色管理'],
        ['menus', '菜单管理'],
        ['departments', '部门管理'],
        ['scheduled-tasks', '定时任务'],
      ].map(([path, title]) => ({
        path,
        name: path,
        component: path === 'menus'
          ? () => import('@/views/MenuManagementView.vue')
          : ['workers', 'reviews', 'archives', 'owners', 'notices', 'users', 'roles'].includes(path)
          ? () => import('@/views/AdminDirectoryView.vue')
          : path === 'repair-categories'
            ? () => import('@/views/RepairCategoriesView.vue')
            : () => import('@/views/ModuleStatusView.vue'),
        meta: {
          title,
          requiresAuth: true,
          hint: `${title}的数据模型或管理接口尚未接入。`,
        },
      })),
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
