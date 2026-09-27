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
| warranty-agent | AI Agent 服务层（Spring AI + Function Calling + RAG + 降级） |
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

# 3. 运行（需先建库 warranty_pro，见 docs/06 建表脚本）
java -jar warranty-bootstrap/target/warranty-bootstrap.jar
# 或 mvn spring-boot:run -pl warranty-bootstrap
```

## 实施阶段待补

- 各业务模块的实体 / Mapper / Service / Controller（表结构见 docs/06，接口契约见 docs/07）；
- warranty-auth 引入 spring-boot-starter-security 与 jjwt（版本已在父 POM dependencyManagement 锁定）；
- warranty-agent 引入 spring-ai-starter-model-openai（BOM 已锁定 1.0.0）与 Redis 向量库配置。
