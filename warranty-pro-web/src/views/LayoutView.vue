<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Odometer, Tickets, Notebook, Setting } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => route.path)
const displayName = computed(() => userStore.userInfo?.realName || '未登录')

function onCommand(command: string | number | object) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="logo">
        <span class="logo-mark">W</span>
        <span class="logo-word">WarrantyPro</span>
      </div>
      <el-menu :default-active="activeMenu" router class="side-menu">
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>运营看板</span>
        </el-menu-item>
        <el-menu-item index="/orders">
          <el-icon><Tickets /></el-icon>
          <span>工单池</span>
        </el-menu-item>
        <el-menu-item index="/warranty-ledger">
          <el-icon><Notebook /></el-icon>
          <span>保修台账</span>
        </el-menu-item>
        <el-menu-item index="/system">
          <el-icon><Setting /></el-icon>
          <span>系统管理</span>
        </el-menu-item>
      </el-menu>
      <div class="side-foot">演示环境 · v0.1</div>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <span class="page-title">{{ route.meta.title }}</span>
        <div class="top-right">
          <span class="env-tag">演示环境</span>
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
  width: 224px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #103b3a 0%, #0b2b2a 100%);
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 60px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.logo-word {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.side-menu {
  flex: 1;
  padding: 10px 0;
  border-right: none;
  background: transparent;
  --el-menu-bg-color: transparent;
}

.sidebar :deep(.el-menu-item) {
  height: 44px;
  margin: 4px 12px;
  border-radius: 9px;
  color: #a9bfb9;
}

.sidebar :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.08);
  color: #fff;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.13);
  color: #fff;
  box-shadow: inset 3px 0 0 var(--wp-amber);
}

.side-foot {
  padding: 14px 18px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
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
  height: 56px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid var(--wp-mist);
}

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
  padding: 18px;
  overflow: auto;
  background: var(--wp-paper);
}
</style>
