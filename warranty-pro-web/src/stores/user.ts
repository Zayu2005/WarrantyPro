import { ref } from 'vue'
import { defineStore } from 'pinia'
import { loginApi } from '@/api/auth'
import type { UserInfo } from '@/api/auth'

const TOKEN_KEY = 'wp_token'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  const userInfo = ref<UserInfo | null>(null)

  function setToken(value: string) {
    token.value = value
    if (value) {
      localStorage.setItem(TOKEN_KEY, value)
    } else {
      localStorage.removeItem(TOKEN_KEY)
    }
  }

  async function login(username: string, password: string) {
    const result = await loginApi({ username, password })
    setToken(result.accessToken)
    userInfo.value = result.user
  }

  function logout() {
    setToken('')
    userInfo.value = null
  }

  return { token, userInfo, login, logout }
})
