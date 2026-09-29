<script setup lang="ts">
import { computed } from 'vue'
import { AlarmClock, Bell, Calendar, CollectionTag, FolderOpened, House, MagicStick, Menu as MenuIcon, Odometer, Picture, Setting, Star, Tickets, User, UserFilled } from '@element-plus/icons-vue'
import type { MenuNode } from '@/api/menus'

defineOptions({ name: 'NavigationMenuNode' })
const props = defineProps<{ node: MenuNode }>()

const icons: Record<string, object> = {
  AlarmClock, Bell, Calendar, CollectionTag, FolderOpened, House, MagicStick,
  Menu: MenuIcon, Odometer, Picture, Setting, Star, Tickets, User, UserFilled,
}
const iconComponent = computed(() => icons[props.node.icon] ?? (props.node.type === 'DIR' ? Setting : Odometer))
</script>

<template>
  <el-sub-menu v-if="node.type === 'DIR'" :index="`directory-${node.id}`">
    <template #title><el-icon><component :is="iconComponent" /></el-icon><span>{{ node.name }}</span></template>
    <NavigationMenuNode v-for="child in node.children ?? []" :key="child.id" :node="child" />
  </el-sub-menu>
  <el-menu-item v-else :index="node.path">
    <el-icon><component :is="iconComponent" /></el-icon>
    <template #title>{{ node.name }}</template>
  </el-menu-item>
</template>
