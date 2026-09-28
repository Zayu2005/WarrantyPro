<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const remember = ref(true)
const form = reactive({ username: '', password: '' })

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const stages = ['提交', '派单', '维修', '验收']

function fill(demoUsername: string) {
  form.username = demoUsername
  form.password = '123456'
}

async function onLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    const redirect = (route.query.redirect as string | undefined) ?? '/dashboard'
    router.push(redirect)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 品牌板：与鸿蒙端同一签名的职场版 -->
    <aside class="brand-panel">
      <div class="bp-logo">
        <span class="bp-mark">W</span>
        <span>WarrantyPro</span>
      </div>

      <div class="bp-center">
        <div class="bp-pipeline">
          <div v-for="(s, i) in stages" :key="s" class="bp-step">
            <span class="dot" :class="i === 2 ? 'active' : i < 2 ? 'done' : 'todo'"></span>
            <span class="step-label" :class="i === 2 ? 'on' : ''">{{ s }}</span>
          </div>
        </div>
        <h1 class="bp-thesis">报修有进度，<br />处理有结果</h1>
        <p class="bp-sub">数字化物业报修平台 · 管理后台</p>
      </div>

      <div class="bp-foot">客服调度 · 设施台账 · 数据看板</div>
    </aside>

    <!-- 表单区 -->
    <div class="form-panel">
      <div class="form-box">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-sub">登录 WarrantyPro 管理后台</p>

        <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onLogin">
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              placeholder="用户名"
              maxlength="50"
              class="underline-input"
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="密码"
              show-password
              class="underline-input"
            />
          </el-form-item>
          <div class="form-row">
            <el-checkbox v-model="remember" label="记住用户名" size="small" />
            <span class="forgot">忘记密码？</span>
          </div>
          <el-button type="primary" class="login-btn" :loading="loading" @click="onLogin">
            登 录
          </el-button>
        </el-form>

        <div class="chips">
          <span class="chip" @click="fill('owner')">业主 · owner</span>
          <span class="chip" @click="fill('kefu')">客服 · kefu</span>
          <span class="chip" @click="fill('shifu')">师傅 · shifu</span>
        </div>
        <p class="form-foot">点一点填入演示账号（密码 123456）· 账号由系统管理员开通</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(380px, 44%) 1fr;
  height: 100%;
}

/* ---- 左侧品牌板 ---- */
.brand-panel {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 40px 48px;
  background: linear-gradient(160deg, #12403f 0%, #0b2b2a 100%);
  color: #fff;
}

.bp-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.bp-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 9px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 15px;
  font-weight: 700;
}

.bp-center {
  margin: auto 0;
}

.bp-pipeline {
  position: relative;
  display: flex;
  margin-bottom: 34px;
}

/* 连接线：节点圆心在 1/8 ~ 7/8，前 2/3 已达成 */
.bp-pipeline::before {
  content: '';
  position: absolute;
  left: 12.5%;
  right: 12.5%;
  top: 5px;
  height: 2px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.75) 0 66%, rgba(255, 255, 255, 0.25) 66% 100%);
}

.bp-step {
  position: relative;
  z-index: 1;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 6px;
}

.dot.done {
  background: rgba(255, 255, 255, 0.85);
}

.dot.active {
  background: var(--wp-amber);
  animation: wp-pulse 1.6s ease-out infinite;
}

.dot.todo {
  background: transparent;
  border: 2px solid rgba(255, 255, 255, 0.3);
  width: 11px;
  height: 11px;
}

.step-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
}

.step-label.on {
  color: var(--wp-amber);
  font-weight: 600;
}

@keyframes wp-pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(232, 147, 12, 0.45);
  }
  70% {
    box-shadow: 0 0 0 9px rgba(232, 147, 12, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(232, 147, 12, 0);
  }
}

@media (prefers-reduced-motion: reduce) {
  .dot.active {
    animation: none;
  }
}

.bp-thesis {
  margin: 0 0 12px;
  font-size: 30px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 1px;
}

.bp-sub {
  margin: 0;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

.bp-foot {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.45);
}

/* ---- 右侧表单 ---- */
.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.form-box {
  width: 360px;
}

.form-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 700;
}

.form-sub {
  margin: 0 0 30px;
  font-size: 13px;
  color: var(--wp-muted);
}

.underline-input :deep(.el-input__wrapper) {
  box-shadow: none;
  background: transparent;
  border-radius: 0;
  padding-left: 0;
  border-bottom: 1px solid var(--wp-mist);
}

.underline-input :deep(.el-input__wrapper.is-focus),
.underline-input :deep(.el-input__wrapper:hover) {
  border-bottom-color: var(--wp-moss);
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.forgot {
  font-size: 13px;
  color: var(--wp-moss);
  cursor: pointer;
}

.login-btn {
  width: 100%;
  height: 46px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 6px;
}

.chips {
  display: flex;
  gap: 10px;
  margin-top: 26px;
}

.chip {
  padding: 7px 14px;
  border-radius: 16px;
  border: 1px solid var(--wp-mist);
  background: #fff;
  font-size: 13px;
  color: var(--wp-moss);
  cursor: pointer;
  transition: border-color 0.15s;
}

.chip:hover {
  border-color: var(--wp-moss);
}

.form-foot {
  margin: 14px 0 0;
  font-size: 12px;
  color: var(--wp-muted);
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .brand-panel {
    display: none;
  }
}
</style>
