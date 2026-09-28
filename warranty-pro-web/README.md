# warranty-pro-web · PC 管理后台

数字化物业保修平台的 PC 端（Vue3 + TypeScript + Vite + Element Plus + Pinia + ECharts）。

## 技术栈与文档

- 选型论证：[docs/04-系统架构与技术选型](../docs/04-系统架构与技术选型.md) §3
- 接口契约：[docs/07-接口设计概要](../docs/07-接口设计概要.md)（基础路径 `/api/v1`，统一响应体 `{ code, message, data }`）

## 快速开始

```bash
pnpm install
pnpm dev        # http://localhost:5173，/api 与 /ws 已代理到 localhost:8080
pnpm build      # 类型检查（vue-tsc）+ 产物构建
```

## 目录结构

```
src/
├── api/           # axios 实例与接口定义（token 注入 / Result 解包 / 401 处理）
├── router/        # 路由与登录守卫
├── stores/        # Pinia（user 会话）
├── views/         # LoginView / LayoutView / DashboardView / PlaceholderView
├── style.css
├── App.vue
└── main.ts        # Element Plus（中文 locale）+ Pinia + Router 装配
```

## 当前状态

已就绪：登录页、主布局（侧边导航 + 用户菜单）、真实统计运营看板（/stats/overview）、
工单池、智能派单、人工改派、师傅排班日历和 axios 鉴权拦截器；保修台账 / 系统管理仍在后续迭代。

开发计划按 docs/02 功能需求推进：FR-D（客服调度）→ FR-M（管理层）→ FR-A（管理员）。
