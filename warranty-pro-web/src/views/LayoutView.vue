<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close, Fold, Odometer } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { fetchMenuTree } from '@/api/menus'
import type { MenuNode } from '@/api/menus'
import NavigationMenuNode from '@/components/NavigationMenuNode.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)
const visited = ref([{ path: '/dashboard', title: '首页' }])
const fallbackMenus: MenuNode[] = [
  { id: 1, parentId: 0, name: '首页', path: '/dashboard', icon: 'House', type: 'MENU', sortOrder: 1, visible: 1, status: 1, children: [] },
  { id: 2, parentId: 0, name: '物业业务', path: '', icon: 'Setting', type: 'DIR', sortOrder: 2, visible: 1, status: 1, children: [
    ...[['工单管理', '/orders', 'Tickets'], ['排班管理', '/schedule', 'Calendar'], ['维修人员', '/workers', 'UserFilled'], ['维修评价', '/reviews', 'Star'], ['工单归档', '/archives', 'FolderOpened'], ['报修类别', '/repair-categories', 'CollectionTag'], ['业主管理', '/owners', 'User'], ['物业通知', '/notices', 'Bell'], ['轮播管理', '/banners', 'Picture']].map(([name, path, icon], index) => ({ id: 3 + index, parentId: 2, name, path, icon, type: 'MENU' as const, sortOrder: index + 1, visible: 1, status: 1, children: [] })),
  ] },
  { id: 12, parentId: 0, name: '系统管理', path: '', icon: 'Setting', type: 'DIR', sortOrder: 3, visible: 1, status: 1, children: [
    ...[['用户管理', '/users', 'User'], ['角色管理', '/roles', 'UserFilled'], ['菜单管理', '/menus', 'Menu'], ['部门管理', '/departments', 'House'], ['智能体监测', '/agent-workflow', 'MagicStick'], ['定时任务', '/scheduled-tasks', 'AlarmClock']].map(([name, path, icon], index) => ({ id: 13 + index, parentId: 12, name, path, icon, type: 'MENU' as const, sortOrder: index + 1, visible: 1, status: 1, children: [] })),
  ] },
]
const menuTree = ref<MenuNode[]>(fallbackMenus)

const activeMenu = computed(() => route.path)
const displayName = computed(() => userStore.userInfo?.realName || '未登录')
const activeTab = computed(() => route.path)

const visibleMenus = computed(() => {
  const filter = (nodes: MenuNode[]): MenuNode[] => nodes
    .filter((node) => node.status === 1 && node.visible === 1)
    .map((node) => ({ ...node, children: filter(node.children ?? []) }))
    .filter((node) => node.type !== 'DIR' || node.children.length > 0)
  return filter(menuTree.value)
})

const openMenus = computed(() => {
  const findAncestors = (nodes: MenuNode[], parents: string[] = []): string[] | null => {
    for (const node of nodes) {
      if (node.type === 'MENU' && node.path === route.path) return parents
      const found = findAncestors(node.children ?? [], [...parents, `directory-${node.id}`])
      if (found) return found
    }
    return null
  }
  return findAncestors(visibleMenus.value) ?? []
})

async function loadMenuTree() {
  try { menuTree.value = await fetchMenuTree() }
  catch { /* Non-admin roles keep their local navigation until RBAC menus are available. */ }
}

onMounted(() => {
  loadMenuTree()
  window.addEventListener('menu-config-updated', loadMenuTree)
})
onBeforeUnmount(() => window.removeEventListener('menu-config-updated', loadMenuTree))

watch(() => route.path, (path) => {
  if (!path.startsWith('/login') && !visited.value.some((item) => item.path === path)) {
    visited.value.push({ path, title: String(route.meta.title ?? '页面') })
  }
}, { immediate: true })

function closeTab(path: string) {
  if (path === '/dashboard') return
  visited.value = visited.value.filter((tab) => tab.path !== path)
  if (route.path === path) router.push(visited.value.at(-1)?.path ?? '/dashboard')
}

function onCommand(command: string | number | object) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="layout">
    <aside class="sidebar" :class="{ collapsed }">
      <div class="logo">
        <span class="logo-mark"><Odometer /></span>
        <span v-if="!collapsed" class="logo-word">数智化物业报修平台</span>
      </div>
      <el-menu :default-active="activeMenu" :default-openeds="openMenus" router class="side-menu" :collapse="collapsed">
        <NavigationMenuNode v-for="item in visibleMenus" :key="item.id" :node="item" />
      </el-menu>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <el-button text class="collapse-button" :aria-label="collapsed ? '展开菜单' : '折叠菜单'" @click="collapsed = !collapsed"><el-icon><Fold /></el-icon></el-button>
        <div class="top-right">
          <el-dropdown @command="onCommand">
            <span class="user-entry">
              {{ displayName }}
              <span class="user-caret">▾</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>
      <nav class="tabbar" aria-label="已打开页面">
        <button v-for="tab in visited" :key="tab.path" class="page-tab" :class="{ active: activeTab === tab.path }" @click="router.push(tab.path)">
          <span>{{ tab.title }}</span>
          <el-icon v-if="tab.path !== '/dashboard'" @click.stop="closeTab(tab.path)"><Close /></el-icon>
        </button>
      </nav>
      <main class="main">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  height: 100%;
}

.sidebar {
  width: 188px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-right: 1px solid #e8eaed;
  transition: width .18s ease;
}

.sidebar.collapsed { width: 64px; }

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 52px;
  padding: 0 12px;
  border-bottom: 1px solid #f0f1f2;
  overflow: hidden;
}

.logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  color: #b36bd2;
  font-size: 18px;
  font-weight: 700;
}

.logo-word {
  color: #fff;
  color: #303133;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.side-menu {
  flex: 1;
  padding: 4px 0;
  border-right: none;
  background: transparent;
  --el-menu-bg-color: #fff;
  --el-menu-text-color: #303133;
  --el-menu-hover-bg-color: #faf7fc;
  --el-menu-active-color: #b36bd2;
}

.sidebar :deep(.el-menu-item) {
  height: 40px;
  margin: 2px 8px;
  border-radius: 4px;
  color: #303133;
}

.sidebar :deep(.el-menu-item:hover) {
  background: #f7f4f9;
  color: #8e44ad;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: #f8f2fb;
  color: #ad61ca;
  box-shadow: inset 2px 0 0 #bd74d6;
}

.workspace {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 48px;
  padding: 0 14px;
  background: #fff;
  border-bottom: 1px solid var(--wp-mist);
}

.collapse-button { color: #606266; }
.tabbar { display: flex; align-items: stretch; gap: 3px; min-height: 38px; padding: 4px 12px 0; border-bottom: 1px solid #e8eaed; background: #fff; overflow-x: auto; }
.page-tab { display: inline-flex; align-items: center; gap: 8px; flex: 0 0 auto; padding: 0 12px; border: 1px solid #e8eaed; border-bottom: 0; border-radius: 6px 6px 0 0; background: #f7f7f8; color: #606266; font: inherit; font-size: 12px; cursor: pointer; }
.page-tab.active { border-color: #d7b3e4; background: #fff; color: #a45dbd; }
.page-tab :deep(.el-icon) { font-size: 11px; }

.page-title {
  font-size: 15px;
  font-weight: 600;
}

.top-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.env-tag {
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  color: var(--wp-amber);
  background: #fdf6ea;
  border: 1px solid #f3d9ad;
}

.user-entry {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  cursor: pointer;
  color: var(--wp-ink);
  font-size: 14px;
  outline: none;
}

.user-caret {
  font-size: 10px;
  color: var(--wp-muted);
}

.main {
  flex: 1;
  padding: 10px 12px;
  overflow: auto;
  background: var(--wp-paper);
}

@media (max-width: 720px) {
  .sidebar { width: 64px; }
  .sidebar:not(.collapsed) { position: absolute; inset: 0 auto 0 0; z-index: 20; width: 188px; box-shadow: 8px 0 24px rgb(20 28 35 / 12%); }
  .workspace { min-width: 0; }
}
</style>
