import axios from 'axios'

/**
 * 统一请求实例（契约见 docs/07：基础路径 /api/v1，响应体 { code, message, data }）。
 * 拦截器完成：token 注入、Result 解包、401 跳登录、错误码透出。
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE ?? '/api/v1',
  timeout: 15000,
})

const TOKEN_KEY = 'wp_token'

request.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data as { code?: number; message?: string }
    if (typeof body.code === 'number' && body.code !== 0) {
      if (body.code === 40101) {
        localStorage.removeItem(TOKEN_KEY)
        window.location.href = '/login'
      }
      return Promise.reject(new Error(body.message ?? '请求失败'))
    }
    // 后端统一响应体解包：直接返回 data 字段
    return (body as { data?: unknown }).data as never
  },
  (error) => {
    const status = error.response?.status
    const message =
      (error.response?.data?.message as string | undefined) ??
      (status === 404 ? '接口不存在' : status ? `请求失败（HTTP ${status}）` : '网络异常，请检查连接')
    return Promise.reject(new Error(message))
  },
)

export default request
