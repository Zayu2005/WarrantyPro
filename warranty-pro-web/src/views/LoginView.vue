<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { fetchSliderCaptcha, verifySliderCaptcha } from '@/api/auth'
import type { SliderCaptcha as SliderCaptchaData } from '@/api/auth'
import SliderCaptcha from '@/components/slider-captcha/SliderCaptcha.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const captchaLoading = ref(false)
const captcha = ref<SliderCaptchaData | null>(null)
const captchaDialogVisible = ref(false)
const captchaVerified = ref(false)
const verifiedChallengeId = ref('')
const captchaFailed = ref(false)
const remember = ref(false)
const form = reactive({ username: '', password: '' })

const REMEMBER_KEY = 'wp_remember_username'

onMounted(() => {
  const saved = localStorage.getItem(REMEMBER_KEY)
  if (saved) {
    form.username = saved
    remember.value = true
  }
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const stages = ['提交', '派单', '维修', '验收']

function persistRemember() {
  if (remember.value) {
    localStorage.setItem(REMEMBER_KEY, form.username)
  } else {
    localStorage.removeItem(REMEMBER_KEY)
  }
}

async function refreshCaptcha() {
  captchaLoading.value = true
  captchaFailed.value = false
  captchaVerified.value = false
  verifiedChallengeId.value = ''
  try {
    captcha.value = await fetchSliderCaptcha()
  } catch (e) {
    captcha.value = null
    ElMessage.error(e instanceof Error ? e.message : '验证码加载失败')
  } finally {
    captchaLoading.value = false
  }
}

async function openCaptcha() {
  if (!captcha.value) await refreshCaptcha()
  if (captcha.value) captchaDialogVisible.value = true
}

async function onCaptchaVerified(offset: number, trajectory: string, _durationMs: number) {
  if (!captcha.value || captchaLoading.value) return
  captchaLoading.value = true
  try {
    await verifySliderCaptcha(captcha.value.challengeId, offset, trajectory)
    verifiedChallengeId.value = captcha.value.challengeId
    captchaVerified.value = true
    captcha.value = null
    // 成功后延迟 600ms 再关闭弹框，让"拼图合拢 + ✓ 弹出"的动效完整播放
    await new Promise((r) => setTimeout(r, 600))
    captchaDialogVisible.value = false
    ElMessage.success('验证成功')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '验证失败，请重试')
    // 失败：先显示回弹/抖动 700ms，再换新图
    captchaFailed.value = true
    captchaLoading.value = false
    await new Promise((r) => setTimeout(r, 700))
    await refreshCaptcha()
    return
  }
  captchaLoading.value = false
}

async function onLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  persistRemember()
  try {
    if (!captchaVerified.value || !verifiedChallengeId.value) {
      ElMessage.warning('请先完成滑动验证')
      await openCaptcha()
      return
    }
    await userStore.login({
      username: form.username,
      password: form.password,
      challengeId: verifiedChallengeId.value,
    })
    const requestedRedirect = route.query.redirect
    const redirect = typeof requestedRedirect === 'string'
      && requestedRedirect.startsWith('/')
      && requestedRedirect !== '/login'
      ? requestedRedirect
      : '/dashboard'
    await router.replace(redirect)
    if (router.currentRoute.value.path === '/login') {
      await router.replace('/dashboard')
    }
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '登录失败')
    await refreshCaptcha()
  } finally {
    loading.value = false
  }
}

function onForgot() {
  ElMessage.info('请联系系统管理员重置密码')
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

      <div class="bp-foot">工单调度 · 师傅排班 · 数据看板</div>
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
          <button class="captcha-trigger" type="button" :disabled="captchaLoading || captchaVerified" @click="openCaptcha">
            <span v-if="captchaVerified" class="captcha-ok">✓</span>
            <span>{{ captchaVerified ? '验证成功' : '点击完成安全验证' }}</span>
            <span v-if="!captchaVerified" class="captcha-arrow">›</span>
          </button>
          <div class="form-row">
            <el-checkbox v-model="remember" label="记住用户名" size="small" @change="persistRemember" />
            <span class="forgot" @click="onForgot">忘记密码？</span>
          </div>
          <el-button type="primary" class="login-btn" :loading="loading" :disabled="!captchaVerified" @click="onLogin">
            登 录
          </el-button>
        </el-form>

        <el-dialog
          v-model="captchaDialogVisible"
          title="安全验证"
          width="min(420px, calc(100vw - 32px))"
          :close-on-click-modal="false"
          :close-on-press-escape="!captchaLoading"
          class="captcha-dialog"
        >
          <div class="captcha-instruction">拖动滑块，让拼图块与缺口对齐</div>
          <SliderCaptcha
            v-if="captcha || captchaLoading || captchaFailed"
            :model="captcha"
            :verifying="captchaLoading"
            :success="captchaVerified"
            :failed="captchaFailed"
            @verified="onCaptchaVerified"
          />
          <div v-if="!captcha && !captchaLoading && !captchaFailed" class="captcha-board captcha-placeholder">
            验证码加载失败
          </div>
          <div class="captcha-slider-row">
            <span v-if="captchaVerified">✓ 已验证，可登录</span>
            <span v-else-if="captchaFailed">未对齐，已自动刷新</span>
            <span v-else>拖动后自动校验</span>
            <el-button link type="primary" :disabled="captchaLoading || captchaFailed" @click="refreshCaptcha">换一张</el-button>
          </div>
        </el-dialog>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(340px, 42%) 1fr;
  height: 100%;
  background: var(--wp-paper);
}

/* ---- 左侧品牌板（浅色） ---- */
.brand-panel {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 36px 40px;
  background: #fff;
  border-right: 1px solid #e8eaed;
  color: var(--wp-ink);
}

.bp-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.bp-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 9px;
  background: linear-gradient(135deg, #b36bd2 0%, #8e44ad 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 700;
}

.bp-center {
  margin: auto 0;
}

.bp-pipeline {
  position: relative;
  display: flex;
  margin-bottom: 30px;
}

.bp-pipeline::before {
  content: '';
  position: absolute;
  left: 12.5%;
  right: 12.5%;
  top: 5px;
  height: 2px;
  background: linear-gradient(90deg, #b36bd2 0 66%, #e8e5ef 66% 100%);
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
  background: #b36bd2;
}

.dot.active {
  background: #fff;
  border: 2px solid #b36bd2;
  animation: wp-pulse 1.8s ease-out infinite;
}

.dot.todo {
  background: #fff;
  border: 2px solid #e8e5ef;
  width: 11px;
  height: 11px;
}

.step-label {
  font-size: 12px;
  color: #8a968f;
}

.step-label.on {
  color: #8e44ad;
  font-weight: 600;
}

@keyframes wp-pulse {
  0% { box-shadow: 0 0 0 0 rgba(179, 107, 210, 0.4); }
  70% { box-shadow: 0 0 0 8px rgba(179, 107, 210, 0); }
  100% { box-shadow: 0 0 0 0 rgba(179, 107, 210, 0); }
}

@media (prefers-reduced-motion: reduce) {
  .dot.active { animation: none; }
}

.bp-thesis {
  margin: 0 0 10px;
  font-size: 26px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: 0.5px;
  color: #303133;
}

.bp-sub {
  margin: 0;
  font-size: 13px;
  color: var(--wp-muted);
}

.bp-foot {
  font-size: 12px;
  color: #b7c4c4;
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
  font-size: 22px;
  font-weight: 700;
  color: #303133;
}

.form-sub {
  margin: 0 0 28px;
  font-size: 13px;
  color: var(--wp-muted);
}

.underline-input :deep(.el-input__wrapper) {
  box-shadow: none;
  background: transparent;
  border-radius: 0;
  padding-left: 0;
  border-bottom: 1px solid #dce7e3;
}

.underline-input :deep(.el-input__wrapper.is-focus),
.underline-input :deep(.el-input__wrapper:hover) {
  border-bottom-color: #b36bd2;
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 22px;
}

.captcha-slider-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.captcha-trigger {
  display: flex; align-items: center; gap: 10px;
  width: 100%; height: 42px; margin: 8px 0 16px;
  padding: 0 14px;
  border: 1px solid #e8e5ef; border-radius: 8px;
  background: #faf8fc;
  color: var(--wp-ink); text-align: left; cursor: pointer;
  font-size: 13px;
  transition: border-color 140ms, background 140ms;
}
.captcha-trigger:hover:not(:disabled) {
  border-color: #b36bd2;
  background: #f6f1f9;
}
.captcha-trigger:disabled { cursor: wait; }
.captcha-ok { color: #8e44ad; font-size: 16px; font-weight: 700; }
.captcha-arrow { margin-left: auto; color: var(--wp-muted); font-size: 18px; }
.captcha-instruction { margin-bottom: 12px; color: var(--wp-muted); font-size: 13px; }
.captcha-board {
  position: relative; width: 100%; aspect-ratio: 2 / 1;
  overflow: hidden; border-radius: 8px;
  background: #f7f4f9;
}
.captcha-board.is-loading { opacity: .7; }
.captcha-background { width: 100%; height: 100%; display: block; }
.captcha-piece {
  position: absolute; object-fit: contain;
  filter: drop-shadow(0 1px 2px rgba(16, 59, 58, .35));
  pointer-events: none;
}
.captcha-placeholder {
  position: absolute; inset: 0; display: grid; place-items: center;
  color: var(--wp-muted); font-size: 12px;
}
.captcha-slider-row { margin-top: 10px; color: var(--wp-muted); font-size: 12px; }
.slider-value { font-variant-numeric: tabular-nums; }
:deep(.captcha-dialog .el-dialog__body) { padding-top: 8px; }

.forgot {
  font-size: 13px;
  color: #8e44ad;
  cursor: pointer;
}

.login-btn {
  width: 100%;
  height: 44px;
  border-radius: 10px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  text-indent: 4px;
  background: linear-gradient(135deg, #b36bd2 0%, #8e44ad 100%);
  border: none;
  color: #fff;
}
.login-btn:hover, .login-btn:focus {
  background: linear-gradient(135deg, #c084d8 0%, #9d55b8 100%);
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
