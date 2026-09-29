<script setup lang="ts">
import { categoryLabel } from '@/utils/labels'

const categories = ['WATER_ELECTRICITY', 'CIVIL_WATERPROOF', 'DOOR_WINDOW', 'HVAC', 'ELEVATOR', 'PUBLIC_FACILITY', 'OTHER']
</script>

<template>
  <section class="categories-page">
    <header><div><h1>报修类别</h1><p>工单使用的类别与维修人员技能标签保持一致。</p></div><span class="count">{{ categories.length }} 个类别</span></header>
    <el-table :data="categories.map((code) => ({ code, name: categoryLabel(code) }))" row-key="code">
      <el-table-column prop="code" label="类别代码" width="240" />
      <el-table-column prop="name" label="显示名称" />
      <el-table-column label="用于" width="240"><template #default>报修工单 / 维修人员技能</template></el-table-column>
    </el-table>
    <div class="note">类别当前由平台统一定义，新增类别前需要同步更新工单和派单规则。</div>
  </section>
</template>

<style scoped>
.categories-page { min-width: 0; }
header { display: flex; justify-content: space-between; align-items: flex-start; margin: 2px 0 16px; }
h1 { margin: 0; color: #27343a; font-size: 19px; font-weight: 650; }
p { margin: 5px 0 0; color: #77838a; font-size: 13px; }
.count { color: #56656b; font-size: 13px; }
.note { margin-top: 14px; color: #77838a; font-size: 12px; }
</style>
