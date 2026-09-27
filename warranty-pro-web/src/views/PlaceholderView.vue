<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const title = computed(() => String(route.meta.title ?? '页面'))
const hint = computed(() => String(route.meta.hint ?? '按需求排期开发中'))
const mark = computed(() => title.value.slice(0, 1))
</script>

<template>
  <div class="wp-card placeholder">
    <span class="ph-mark">{{ mark }}</span>
    <h3 class="ph-title">{{ title }}</h3>
    <p class="ph-hint">{{ hint }}</p>
    <p class="ph-doc">功能设计见 docs/02 需求分析 · 接口契约见 docs/07</p>
  </div>
</template>

<style scoped>
.placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 64px 20px;
  text-align: center;
}

.ph-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  background: rgba(232, 147, 12, 0.12);
  color: var(--wp-amber);
  font-size: 24px;
  font-weight: 700;
}

.ph-title {
  margin: 18px 0 0;
  font-size: 17px;
  font-weight: 600;
}

.ph-hint {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--wp-ink);
}

.ph-doc {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--wp-muted);
}
</style>
