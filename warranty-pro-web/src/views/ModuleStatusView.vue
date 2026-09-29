<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { InfoFilled } from '@element-plus/icons-vue'

const route = useRoute()
const title = computed(() => String(route.meta.title ?? '业务模块'))
const details = computed(() => String(route.meta.hint ?? '该模块尚未接入数据模型和服务接口。'))
</script>

<template>
  <section class="module-status">
    <div class="status-mark"><el-icon><InfoFilled /></el-icon></div>
    <div><h1>{{ title }}</h1><p>{{ details }}</p></div>
    <el-tag type="info" effect="plain">未接入</el-tag>
  </section>
</template>

<style scoped>
.module-status { display: flex; align-items: center; gap: 14px; min-height: 108px; padding: 20px; border-top: 1px solid #e6ebed; border-bottom: 1px solid #e6ebed; }
.status-mark { display: grid; place-items: center; width: 36px; height: 36px; flex: 0 0 auto; color: #718087; background: #f1f4f5; border-radius: 6px; font-size: 18px; }
h1 { margin: 0; color: #27343a; font-size: 16px; font-weight: 600; }
p { margin: 5px 0 0; color: #77838a; font-size: 13px; }
.module-status :deep(.el-tag) { margin-left: auto; }
</style>
