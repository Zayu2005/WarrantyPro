<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => String(route.meta.title ?? '页面'))
</script>

<template>
  <el-card shadow="never">
    <el-empty :description="`${title} —— 页面开发中（功能设计见 docs/02 需求分析）`" />
  </el-card>
</template>
