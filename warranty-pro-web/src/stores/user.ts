import { ref } from 'vue'
import { defineStore } from 'pinia'
import { loginApi } from '@/api/auth'
import type { LoginPayload, UserInfo } from '@/api/auth'

const TOKEN_KEY = 'wp_token'
const USER_KEY = 'wp_user'

function loadSavedUser(): UserInfo | null {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) ?? 'null') as UserInfo | null
  } catch {
    return null
  }
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  // 用户信息随 token 持久化，刷新页面后头部仍显示登录人
  const userInfo = ref<UserInfo | null>(loadSavedUser())

  function setToken(value: string) {
    token.value = value
    if (value) {
      localStorage.setItem(TOKEN_KEY, value)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
  }

  async function login(payload: LoginPayload) {
    const result = await loginApi(payload)
    setToken(result.accessToken)
    userInfo.value = result.user
    localStorage.setItem(USER_KEY, JSON.stringify(result.user))
  }

  function logout() {
    setToken('')
    userInfo.value = null
    localStorage.removeItem(USER_KEY)
  }

  return { token, userInfo, login, logout }
})
