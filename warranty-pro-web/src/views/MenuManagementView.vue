<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createMenu, deleteMenu, fetchMenuTree, updateMenu } from '@/api/menus'
import type { MenuInput, MenuNode } from '@/api/menus'

const menus = ref<MenuNode[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
const form = reactive<MenuInput>({ parentId: 0, name: '', path: '', icon: '', type: 'DIR', sortOrder: 0, visible: 1, status: 1 })

const routeOptions = [
  ['/dashboard', '首页'], ['/orders', '工单管理'], ['/schedule', '排班管理'], ['/workers', '维修人员'],
  ['/reviews', '维修评价'], ['/archives', '工单归档'], ['/repair-categories', '报修类别'], ['/owners', '业主管理'],
  ['/notices', '物业通知'], ['/banners', '轮播管理'], ['/users', '用户管理'], ['/roles', '角色管理'],
  ['/menus', '菜单管理'], ['/departments', '部门管理'], ['/agent-workflow', '智能体监测'], ['/scheduled-tasks', '定时任务'],
  ['/warranty-ledger', '设施台账'], ['/system', '系统管理'],
] as const
const iconOptions = ['House', 'Setting', 'Tickets', 'Calendar', 'UserFilled', 'Star', 'FolderOpened', 'CollectionTag', 'User', 'Bell', 'Picture', 'Menu', 'MagicStick', 'AlarmClock', 'Odometer']
const flatMenus = computed(() => {
  const rows: MenuNode[] = []
  const visit = (nodes: MenuNode[]) => nodes.forEach((node) => { rows.push(node); visit(node.children ?? []) })
  visit(menus.value)
  return rows
})
const directoryOptions = computed(() => flatMenus.value.filter((item) => item.type === 'DIR'))
const menuCount = computed(() => flatMenus.value.filter((item) => item.type === 'MENU').length)
const hiddenCount = computed(() => flatMenus.value.filter((item) => item.visible === 0 || item.status === 0).length)

async function load() {
  loading.value = true
  try { menus.value = await fetchMenuTree() }
  catch (error) { ElMessage.error(error instanceof Error ? error.message : '菜单加载失败') }
  finally { loading.value = false }
}

function resetForm() {
  Object.assign(form, { parentId: 0, name: '', path: '', icon: '', type: 'DIR', sortOrder: 0, visible: 1, status: 1 })
  editingId.value = null
}

function addMenu(parentId = 0, type: 'DIR' | 'MENU' = 'DIR') {
  resetForm()
  form.parentId = parentId
  form.type = type
  if (type === 'MENU' && parentId === 0 && directoryOptions.value.length) form.parentId = directoryOptions.value[0].id
  dialogVisible.value = true
}

function editMenu(node: MenuNode) {
  Object.assign(form, {
    parentId: node.parentId, name: node.name, path: node.path, icon: node.icon,
    type: node.type, sortOrder: node.sortOrder, visible: node.visible, status: node.status,
  })
  editingId.value = node.id
  dialogVisible.value = true
}

function onTypeChange(type: 'DIR' | 'MENU') {
  form.path = ''
  form.parentId = type === 'MENU' ? (directoryOptions.value[0]?.id ?? 0) : 0
  if (type === 'DIR') form.icon ||= 'Setting'
}

async function save() {
  if (!formRef.value) return
  try { await formRef.value.validate() }
  catch { return }
  try {
    if (editingId.value) await updateMenu(editingId.value, { ...form })
    else await createMenu({ ...form })
    ElMessage.success(editingId.value ? '菜单已更新' : '菜单已创建')
    dialogVisible.value = false
    await load()
    window.dispatchEvent(new CustomEvent('menu-config-updated'))
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '保存失败') }
}

async function removeMenu(node: MenuNode) {
  try { await ElMessageBox.confirm(`确定删除「${node.name}」？`, '删除菜单', { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }) }
  catch { return }
  try {
    await deleteMenu(node.id)
    ElMessage.success('菜单已删除')
    await load()
    window.dispatchEvent(new CustomEvent('menu-config-updated'))
  } catch (error) { ElMessage.error(error instanceof Error ? error.message : '删除失败') }
}

const rules = {
  name: [{ required: true, message: '请填写菜单名称', trigger: 'blur' }, { max: 50, message: '名称不能超过 50 个字符', trigger: 'blur' }],
  parentId: [{ required: true, message: '请选择上级目录', trigger: 'change' }],
  path: [{ required: true, message: '请选择页面路径', trigger: 'change' }],
}

onMounted(load)
</script>

<template>
  <section class="menus-page">
    <header class="page-head">
      <div><h1>菜单管理</h1><p>维护后台导航名称、层级、可见状态和排列顺序。</p></div>
      <div class="head-actions">
        <el-button aria-label="刷新菜单" title="刷新菜单" :loading="loading" @click="load">刷新</el-button>
        <el-button type="primary" @click="addMenu()">新增目录</el-button>
      </div>
    </header>

    <div class="summary-row">
      <div><span class="summary-label">页面菜单</span><strong>{{ menuCount }}</strong></div>
      <div><span class="summary-label">隐藏或停用</span><strong>{{ hiddenCount }}</strong></div>
      <span class="summary-note">排序值越小，同级菜单越靠前</span>
    </div>

    <el-table :data="menus" row-key="id" default-expand-all v-loading="loading" :tree-props="{ children: 'children' }" class="menu-table" empty-text="暂无菜单配置">
      <el-table-column label="菜单名称" min-width="220">
        <template #default="{ row }"><span class="menu-name">{{ row.name }}</span><el-tag size="small" effect="plain" class="type-tag">{{ row.type === 'DIR' ? '目录' : '页面' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="path" label="页面路径" min-width="190"><template #default="{ row }">{{ row.path || '—' }}</template></el-table-column>
      <el-table-column prop="icon" label="图标" width="100"><template #default="{ row }">{{ row.icon || '—' }}</template></el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" align="center" />
      <el-table-column label="可见" width="90" align="center"><template #default="{ row }"><el-tag :type="row.visible ? 'success' : 'info'" size="small">{{ row.visible ? '显示' : '隐藏' }}</el-tag></template></el-table-column>
      <el-table-column label="状态" width="90" align="center"><template #default="{ row }"><el-tag :type="row.status ? 'success' : 'danger'" size="small">{{ row.status ? '启用' : '停用' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.type === 'DIR'" link type="primary" @click="addMenu(row.id, 'MENU')">添加页面</el-button>
          <el-button link type="primary" @click="editMenu(row)">编辑</el-button>
          <el-button link type="danger" @click="removeMenu(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑菜单' : form.type === 'DIR' ? '新增目录' : '新增页面菜单'" width="520px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="92px">
        <el-form-item label="菜单类型"><el-radio-group v-model="form.type" :disabled="Boolean(editingId)" @change="onTypeChange"><el-radio-button value="DIR">目录</el-radio-button><el-radio-button value="MENU">页面</el-radio-button></el-radio-group></el-form-item>
        <el-form-item label="菜单名称" prop="name"><el-input v-model="form.name" maxlength="50" show-word-limit /></el-form-item>
        <el-form-item v-if="form.type === 'MENU'" label="上级目录" prop="parentId"><el-select v-model="form.parentId" style="width: 100%"><el-option v-for="item in directoryOptions" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item>
        <el-form-item v-if="form.type === 'MENU'" label="页面路径" prop="path"><el-select v-model="form.path" filterable style="width: 100%"><el-option v-for="[path, label] in routeOptions" :key="path" :label="`${label} · ${path}`" :value="path" /></el-select></el-form-item>
        <el-form-item label="图标"><el-select v-model="form.icon" filterable style="width: 100%"><el-option v-for="icon in iconOptions" :key="icon" :label="icon" :value="icon" /></el-select></el-form-item>
        <el-form-item label="同级排序"><el-input-number v-model="form.sortOrder" :min="0" :max="999" /></el-form-item>
        <el-form-item label="可见状态"><el-switch v-model="form.visible" :active-value="1" :inactive-value="0" active-text="显示" inactive-text="隐藏" /></el-form-item>
        <el-form-item label="启用状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </section>
</template>

<style scoped>
.menus-page { min-width: 0; }
.page-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin: 2px 0 14px; }
h1 { margin: 0; color: #27343a; font-size: 19px; font-weight: 650; }
.page-head p { margin: 5px 0 0; color: #77838a; font-size: 13px; }
.head-actions { display: flex; gap: 8px; }
.summary-row { display: flex; align-items: center; gap: 26px; min-height: 54px; padding: 0 4px; border-top: 1px solid #e7ecee; border-bottom: 1px solid #e7ecee; }
.summary-row > div { display: flex; align-items: center; gap: 9px; }
.summary-label,.summary-note { color: #77838a; font-size: 12px; }
.summary-row strong { color: #29363b; font-size: 16px; font-weight: 600; }
.summary-note { margin-left: auto; }
.menu-table { width: 100%; margin-top: 12px; }
.menu-name { color: #2c383d; font-weight: 550; }
.type-tag { margin-left: 8px; }
@media (max-width: 650px) { .page-head { flex-direction: column; } .summary-row { gap: 12px; flex-wrap: wrap; padding: 10px 0; } .summary-note { width: 100%; margin: 0; } }
</style>
