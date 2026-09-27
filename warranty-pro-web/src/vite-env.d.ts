/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** API 基础路径，默认 /api/v1（开发环境经 vite 代理到 8080） */
  readonly VITE_API_BASE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
