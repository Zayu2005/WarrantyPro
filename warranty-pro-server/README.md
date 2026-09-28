# warranty-pro-server · 后端服务

数字化物业保修平台后端，Spring Boot 3.3（Java 21）模块化单体。模块划分与依赖关系见 [docs/04-系统架构与技术选型 §2](../docs/04-系统架构与技术选型.md)。

## 模块一览

| 模块 | 职责 |
|------|------|
| warranty-common | 统一响应体 / 异常 / 错误码 / 枚举（工单状态机等口径唯一出处） |
| warranty-auth | Spring Security + JWT 认证授权 |
| warranty-user | 用户 / 角色 / 权限、业主-房屋绑定审核 |
| warranty-estate | 小区 / 楼栋 / 房屋 / 设施台账（保修判定数据源） |
| warranty-warranty | 保修期限规则 + 判定引擎（纯规则，零 AI 依赖） |
| warranty-order | 工单 / 流转记录 / 维修记录 / 验收评价 |
| warranty-dispatch | 派单：多因子评分 + Agent 决策接入 |
| warranty-agent | LangChain4j Agent 服务层（报修分诊工作流、模型调用、规则降级与可观测日志） |
| warranty-notify | 通知中心（Push Kit / 短信 / 站内信） |
| warranty-stat | 统计分析 / 大屏指标 / NL2Query 指标域 |
| warranty-file | MinIO 文件服务 |
| warranty-bootstrap | 启动装配与全局配置（可执行 JAR） |

## 快速开始

前置：JDK 21、Maven 3.9+；本地中间件可选 Docker。

```bash
# 1. 拉起 MySQL / Redis Stack / MinIO（可选）
docker compose up -d

# 2. 构建
mvn -DskipTests package

# 3. 初始化数据库（按顺序执行；03 为工作流运行记录与节点日志）
# mysql -uroot -p warranty_pro < sql/01_schema.sql
# mysql -uroot -p warranty_pro < sql/02_seed.sql
# mysql -uroot -p warranty_pro < sql/03_agent_workflow.sql

# 4. 运行（需先建库 warranty_pro）
java -jar warranty-bootstrap/target/warranty-bootstrap.jar
# 或 mvn spring-boot:run -pl warranty-bootstrap
```

## 当前实现状态

- 已实现登录鉴权、报修与工单流转、自动 / 人工派单、排班、验收评价和运营概览；
- 到场超时自动改派、验收超时默认通过由定时任务处理；
- 已实现 LangChain4j 报修分诊工作流：模型不可用时自动使用规则降级，并记录运行与节点日志；
- PC 管理后台已提供流程图、运行记录、动态节点日志和手工启动入口；
- Agent 对话、真实向量知识库与 SSE / WebSocket 推送仍待后续迭代。
