-- =====================================================================
-- WarrantyPro 数字化物业保修平台 · 建表脚本（01_schema.sql）
-- 目标库：MySQL 8.0+ / InnoDB / utf8mb4
-- 设计依据：docs/06-数据库设计.md（与本脚本同步维护）
-- 运行：mysql -uroot -p < 01_schema.sql && mysql -uroot -p < 02_seed.sql
-- 约定：
--   1. 不建物理外键，表间为逻辑外键，参照完整性由应用层保证；
--   2. 业务主表含逻辑删除字段 deleted；日志类表仅保留 created_at；
--   3. 枚举一律 VARCHAR 存代码值，代码唯一出处为 warranty-common 枚举类。
-- =====================================================================

CREATE DATABASE IF NOT EXISTS warranty_pro
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE warranty_pro;

-- =====================================================================
-- 1. 用户与权限组
-- =====================================================================

CREATE TABLE sys_user (
  id            BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(50)  NOT NULL COMMENT '用户名（登录账号，全角色统一）',
  phone         VARCHAR(20)  NULL COMMENT '手机号（联系方式，非登录账号）',
  password_hash VARCHAR(100) NULL COMMENT 'BCrypt 哈希（验证码登录用户可空）',
  real_name     VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '姓名',
  avatar        VARCHAR(255) NULL COMMENT '头像 URL',
  status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 启用 / 0 停用',
  last_login_at DATETIME     NULL COMMENT '最后登录时间',
  deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 / 1 是',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  UNIQUE KEY uk_phone (phone)
) ENGINE = InnoDB COMMENT = '用户表';

CREATE TABLE sys_role (
  id         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  code       VARCHAR(20) NOT NULL COMMENT '角色代码：OWNER/WORKER/DISPATCHER/MANAGER/ADMIN',
  name       VARCHAR(50) NOT NULL COMMENT '角色名称',
  created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (code)
) ENGINE = InnoDB COMMENT = '角色表';

CREATE TABLE sys_user_role (
  id         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID（逻辑外键 sys_user.id）',
  role_id    BIGINT UNSIGNED NOT NULL COMMENT '角色 ID（逻辑外键 sys_role.id）',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_role (role_id)
) ENGINE = InnoDB COMMENT = '用户角色关联表（业主与师傅可为同一人）';

-- =====================================================================
-- 2. 房产与设施组
-- =====================================================================

CREATE TABLE community (
  id             BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  name           VARCHAR(100) NOT NULL COMMENT '小区名称',
  address        VARCHAR(200) NOT NULL DEFAULT '' COMMENT '地址',
  developer_name VARCHAR(100) NULL COMMENT '开发商名称（保修责任对接方）',
  contact        VARCHAR(100) NULL COMMENT '开发商联系方式',
  deleted        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 / 1 是',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id)
) ENGINE = InnoDB COMMENT = '小区表';

CREATE TABLE building (
  id              BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  community_id    BIGINT UNSIGNED NOT NULL COMMENT '小区 ID（逻辑外键 community.id）',
  name            VARCHAR(50)     NOT NULL COMMENT '楼栋名称，如 3 栋',
  completion_date DATE            NULL COMMENT '竣工日期（户内保修判定起算日，docs/03 §4）',
  deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 / 1 是',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_community (community_id)
) ENGINE = InnoDB COMMENT = '楼栋表';

CREATE TABLE house (
  id          BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  building_id BIGINT UNSIGNED NOT NULL COMMENT '楼栋 ID（逻辑外键 building.id）',
  unit        VARCHAR(20)     NOT NULL DEFAULT '' COMMENT '单元',
  room_no     VARCHAR(20)     NOT NULL COMMENT '房号',
  area        DECIMAL(8, 2)   NULL COMMENT '建筑面积（㎡）',
  deleted     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 / 1 是',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_room (building_id, unit, room_no)
) ENGINE = InnoDB COMMENT = '房屋表';

CREATE TABLE user_house (
  id         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  user_id    BIGINT UNSIGNED NOT NULL COMMENT '业主用户 ID（逻辑外键 sys_user.id）',
  house_id   BIGINT UNSIGNED NOT NULL COMMENT '房屋 ID（逻辑外键 house.id）',
  relation   VARCHAR(20)     NOT NULL DEFAULT 'OWNER' COMMENT '关系：OWNER 业主 / TENANT 租户 / FAMILY 家属',
  status     VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '审核状态：PENDING / APPROVED / REJECTED',
  audited_by BIGINT UNSIGNED NULL COMMENT '审核管理员（sys_user.id）',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_house (user_id, house_id),
  KEY idx_house (house_id)
) ENGINE = InnoDB COMMENT = '业主-房屋绑定表（绑定需管理员审核，FR-A-03）';

CREATE TABLE facility (
  id             BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  community_id   BIGINT UNSIGNED NOT NULL COMMENT '小区 ID（逻辑外键 community.id）',
  name           VARCHAR(100)    NOT NULL COMMENT '设施名称，如 1 号楼电梯',
  type           VARCHAR(30)     NOT NULL COMMENT '类型：ELEVATOR/FIRE/PUMP/FACADE/HVAC/OTHER',
  location       VARCHAR(100)    NOT NULL DEFAULT '' COMMENT '安装位置',
  supplier_name  VARCHAR(100)    NULL COMMENT '承建（供应）方名称',
  contact_name   VARCHAR(50)     NULL COMMENT '对接人',
  contact_phone  VARCHAR(20)     NULL COMMENT '对接人电话',
  install_date   DATE            NULL COMMENT '安装验收日',
  warranty_start DATE            NULL COMMENT '保修起始日（判定起算日）',
  warranty_end   DATE            NULL COMMENT '保修到期日（按规则推算后落库存档）',
  deleted        TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 / 1 是',
  created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_community_type (community_id, type),
  KEY idx_warranty_end (warranty_end)
) ENGINE = InnoDB COMMENT = '设施设备台账表';

CREATE TABLE warranty_rule (
  id             BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  scope          VARCHAR(20) NOT NULL COMMENT '适用范围：LEGAL 法定默认 / CONTRACT 合同覆盖',
  part_category  VARCHAR(30) NOT NULL COMMENT '工程部位：MAIN_STRUCTURE/WATERPROOF/HEATING_COOLING/ME_INSTALLATION/INSULATION',
  duration_value INT         NULL COMMENT '期限数值（DESIGN_LIFE 时为空）',
  duration_unit  VARCHAR(20) NOT NULL COMMENT '单位：YEAR / HEATING_SEASON 采暖供冷期 / DESIGN_LIFE 设计使用年限',
  community_id   BIGINT UNSIGNED NULL COMMENT '生效小区（NULL = 全局；合同规则按小区覆盖）',
  source         VARCHAR(200) NOT NULL DEFAULT '' COMMENT '条款依据，如 条例第四十条 / 合同名',
  priority       INT         NOT NULL DEFAULT 0 COMMENT '优先级：合同 > 法定，同 scope 内数值大者优先',
  deleted        TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 / 1 是',
  created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_category_community (part_category, community_id)
) ENGINE = InnoDB COMMENT = '保修期限规则表（判定引擎数据源，docs/03 §4）';

CREATE TABLE warranty_alert (
  id           BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  alert_target VARCHAR(20)     NOT NULL DEFAULT 'FACILITY' COMMENT '预警对象：FACILITY 设施 / BUILDING 楼栋（G6）',
  target_id    BIGINT UNSIGNED NOT NULL COMMENT '对象 ID（设施或楼栋）',
  warranty_end DATE            NOT NULL COMMENT '到期日',
  days_left    INT             NOT NULL COMMENT '剩余天数',
  status       VARCHAR(20)     NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING / PROCESSED',
  handled_by   BIGINT UNSIGNED NULL COMMENT '处理人（sys_user.id）',
  handled_at   DATETIME        NULL COMMENT '处理时间',
  remark       VARCHAR(200)    NULL COMMENT '处理备注（如续保交涉进展）',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_target_end (alert_target, target_id, warranty_end),
  KEY idx_status (status)
) ENGINE = InnoDB COMMENT = '保修到期预警表（每日定时任务生成，uk 保证幂等去重，G9）';

-- =====================================================================
-- 3. 工单组（核心）
-- =====================================================================

CREATE TABLE repair_order (
  id                  BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  order_no            VARCHAR(32)     NOT NULL COMMENT '工单号：WO + yyyyMMdd + 4 位序列',
  community_id        BIGINT UNSIGNED NOT NULL COMMENT '小区 ID（冗余，加速筛选）',
  house_id            BIGINT UNSIGNED NULL COMMENT '报修房屋（户内报修必填）',
  facility_id         BIGINT UNSIGNED NULL COMMENT '报修设施（公共设施报修必填）',
  owner_id            BIGINT UNSIGNED NOT NULL COMMENT '提交业主（sys_user.id）',
  object_type         VARCHAR(20)     NOT NULL COMMENT '对象：INDOOR 户内 / PUBLIC_FACILITY 公共设施',
  category            VARCHAR(30)     NOT NULL COMMENT '故障类别（FaultCategory 枚举）',
  location_detail     VARCHAR(100)    NOT NULL DEFAULT '' COMMENT '具体位置',
  phenomenon          VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '故障现象描述',
  urgency             VARCHAR(10)     NOT NULL DEFAULT 'NORMAL' COMMENT '紧急程度：URGENT / NORMAL',
  source              VARCHAR(20)     NOT NULL DEFAULT 'FORM' COMMENT '来源：AI_CHAT 对话式 / FORM 表单',
  source_session_id   BIGINT UNSIGNED NULL COMMENT 'AI 报修来源会话（agent_session.id，追溯对话，G5）',
  status              VARCHAR(20)     NOT NULL DEFAULT 'SUBMITTED' COMMENT '状态机 8 态（docs/03 §1）',
  verdict             VARCHAR(30)     NULL COMMENT '判定快照：IN_WARRANTY/OUT_OF_WARRANTY/OWNER_RESPONSIBLE',
  responsible_party   VARCHAR(20)     NULL COMMENT '判定快照：DEVELOPER/PROPERTY/OWNER',
  verdict_basis       VARCHAR(300)    NULL COMMENT '判定快照：依据条款',
  warranty_start      DATE            NULL COMMENT '判定快照：起算日',
  warranty_expire     DATE            NULL COMMENT '判定快照：到期日',
  verdict_adjusted_by BIGINT UNSIGNED NULL COMMENT '客服纠正判定时的操作人（留痕）',
  paid_status         VARCHAR(20)     NOT NULL DEFAULT 'NOT_REQUIRED' COMMENT '收费状态：NOT_REQUIRED 无需 / UNPAID 待收 / PAID 已收（G3）',
  fee_amount          DECIMAL(10, 2)  NULL COMMENT '有偿维修金额（线下收费，系统登记）',
  current_worker_id   BIGINT UNSIGNED NULL COMMENT '当前维修师傅（sys_user.id）',
  dispatch_mode       VARCHAR(20)     NULL COMMENT '本单派单模式：RECOMMEND/AUTO/MANUAL',
  accept_deadline     DATETIME        NULL COMMENT '接单截止时间（超时自动改派）',
  submitted_at        DATETIME        NULL COMMENT '提交时间',
  accepted_at         DATETIME        NULL COMMENT '客服受理时间',
  dispatched_at       DATETIME        NULL COMMENT '派单时间',
  started_at          DATETIME        NULL COMMENT '师傅到场/开工时间',
  completed_at        DATETIME        NULL COMMENT '完工提交时间',
  confirmed_at        DATETIME        NULL COMMENT '验收时间（含超时默认通过）',
  cancel_reason       VARCHAR(200)    NULL COMMENT '取消 / 关单原因',
  deleted             TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 / 1 是',
  created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_owner_time (owner_id, created_at),
  KEY idx_worker_status (current_worker_id, status),
  KEY idx_status_community_time (status, community_id, created_at),
  KEY idx_accepted (accepted_at)
) ENGINE = InnoDB COMMENT = '报修工单表';

CREATE TABLE order_flow_record (
  id            BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  order_id      BIGINT UNSIGNED NOT NULL COMMENT '工单 ID（逻辑外键 repair_order.id）',
  from_status   VARCHAR(20)     NULL COMMENT '流转前状态（首节点为空）',
  to_status     VARCHAR(20)     NOT NULL COMMENT '流转后状态',
  operator_id   BIGINT UNSIGNED NULL COMMENT '操作人（系统动作为空）',
  operator_role VARCHAR(20)     NULL COMMENT '操作角色（RoleCode）',
  action        VARCHAR(30)     NOT NULL COMMENT '动作（OrderAction 枚举，含 EXTERNAL_FOLLOWUP 外部跟进）',
  remark        VARCHAR(300)    NULL COMMENT '备注',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_order_time (order_id, created_at)
) ENGINE = InnoDB COMMENT = '工单流转记录表（业主端时间轴与催办依据）';

CREATE TABLE dispatch_record (
  id            BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  order_id      BIGINT UNSIGNED NOT NULL COMMENT '工单 ID（逻辑外键 repair_order.id）',
  worker_id     BIGINT UNSIGNED NOT NULL COMMENT '承接师傅（sys_user.id）',
  mode          VARCHAR(20)     NOT NULL COMMENT '派单模式：AUTO / RECOMMEND / MANUAL',
  score         DECIMAL(4, 3)   NULL COMMENT '多因子综合得分',
  factors       JSON            NULL COMMENT '四因子得分明细（skill/load/location/rating）',
  reason        VARCHAR(500)    NULL COMMENT 'Agent 推荐理由（自然语言）',
  status        VARCHAR(20)     NOT NULL DEFAULT 'DISPATCHED' COMMENT 'DISPATCHED 待接 / ACCEPTED 已接 / REJECTED 待接期拒单 / PENDING_REASSIGN 维修中申请改派待审 / TIMEOUT_REASSIGNED 接单超时改派（G1）',
  round_no      INT             NOT NULL DEFAULT 1 COMMENT '第几轮派单（自动改派轮次，上限 3）',
  dispatched_by BIGINT UNSIGNED NULL COMMENT '人工派单时的操作客服',
  reject_reason VARCHAR(200)    NULL COMMENT '拒单 / 改派申请原因（G1）',
  created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_order_round (order_id, round_no),
  KEY idx_worker_status (worker_id, status)
) ENGINE = InnoDB COMMENT = '派单记录表（一次派单一行，含改派历史）';

CREATE TABLE worker_profile (
  id              BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  user_id         BIGINT UNSIGNED NOT NULL COMMENT '师傅用户 ID（sys_user.id，一对一）',
  skill_tags      JSON            NULL COMMENT '技能标签数组（FaultCategory 子集）',
  community_id    BIGINT UNSIGNED NULL COMMENT '常驻小区（位置就近因子）',
  max_concurrent  INT             NOT NULL DEFAULT 3 COMMENT '并发单量上限',
  on_duty         TINYINT         NOT NULL DEFAULT 1 COMMENT '在岗开关：1 在岗 / 0 请假',
  rating_avg      DECIMAL(3, 2)   NULL COMMENT '全期平均评分（评价写入时增量维护；近 90 天口径由统计查询聚合，G7）',
  rating_count    INT             NOT NULL DEFAULT 0 COMMENT '评分条数',
  order_total     INT             NOT NULL DEFAULT 0 COMMENT '累计派单数',
  order_completed INT             NOT NULL DEFAULT 0 COMMENT '累计完结数',
  deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 / 1 是',
  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user (user_id)
) ENGINE = InnoDB COMMENT = '维修师傅画像表';

CREATE TABLE repair_report (
  id          BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  order_id    BIGINT UNSIGNED NOT NULL COMMENT '工单 ID（一对一）',
  worker_id   BIGINT UNSIGNED NOT NULL COMMENT '维修师傅（sys_user.id）',
  fault_cause VARCHAR(300)    NOT NULL DEFAULT '' COMMENT '故障原因',
  measures    VARCHAR(500)    NOT NULL DEFAULT '' COMMENT '处理措施',
  materials   JSON            NULL COMMENT '更换材料清单',
  work_hours  DECIMAL(4, 1)   NULL COMMENT '工时（小时）',
  ai_assisted TINYINT         NOT NULL DEFAULT 0 COMMENT '是否 AI 辅助生成：0 否 / 1 是',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order (order_id)
) ENGINE = InnoDB COMMENT = '维修记录表';

CREATE TABLE evaluation (
  id         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  order_id   BIGINT UNSIGNED NOT NULL COMMENT '工单 ID（一对一）',
  owner_id   BIGINT UNSIGNED NOT NULL COMMENT '评价业主（sys_user.id）',
  worker_id  BIGINT UNSIGNED NOT NULL COMMENT '被评师傅（sys_user.id）',
  stars      TINYINT         NOT NULL COMMENT '星级 1-5',
  tags       JSON            NULL COMMENT '评价标签：态度好/速度快/一次修复…',
  comment    VARCHAR(500)    NULL COMMENT '文字评价',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_order (order_id),
  KEY idx_worker (worker_id),
  CONSTRAINT chk_stars CHECK (stars BETWEEN 1 AND 5)
) ENGINE = InnoDB COMMENT = '验收评价表（验收结果走 order_flow_record，本表存服务评价）';

-- =====================================================================
-- 4. Agent 组
-- =====================================================================

CREATE TABLE agent_session (
  id          BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT UNSIGNED NULL COMMENT '发起用户（派单 Agent 事件触发时为空）',
  agent_type  VARCHAR(30)     NOT NULL COMMENT '类型：XIAOBAO/DISPATCH/REPAIR_ASSIST/ANALYSIS',
  biz_ref_id  BIGINT UNSIGNED NULL COMMENT '关联业务 ID（如工单）',
  status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / CLOSED / DEGRADED',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_user_time (user_id, created_at)
) ENGINE = InnoDB COMMENT = 'Agent 会话表';

CREATE TABLE agent_message (
  id          BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  session_id  BIGINT UNSIGNED NOT NULL COMMENT '会话 ID（逻辑外键 agent_session.id）',
  role        VARCHAR(20)     NOT NULL COMMENT '角色：USER / ASSISTANT / TOOL',
  content     TEXT            NULL COMMENT '消息内容',
  tool_name   VARCHAR(50)     NULL COMMENT '工具名（role = TOOL 时）',
  token_count INT             NULL COMMENT 'token 数',
  created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_session_time (session_id, created_at)
) ENGINE = InnoDB COMMENT = 'Agent 会话消息表';

CREATE TABLE agent_decision_log (
  id            BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  agent_type    VARCHAR(30)  NOT NULL COMMENT '类型：XIAOBAO/DISPATCH/REPAIR_ASSIST/ANALYSIS',
  order_id      BIGINT UNSIGNED NULL COMMENT '关联工单（派单 Agent）',
  mode          VARCHAR(20)  NULL COMMENT '模式：RECOMMEND / AUTO（非派单类为空）',
  input_summary VARCHAR(500) NOT NULL DEFAULT '' COMMENT '输入摘要（脱敏后）',
  output        JSON         NULL COMMENT '决策输出（docs/05 §4.2 结构）',
  model         VARCHAR(50)  NULL COMMENT '使用的模型标识',
  latency_ms    INT          NULL COMMENT '耗时（毫秒）',
  status        VARCHAR(20)  NOT NULL DEFAULT 'OK' COMMENT '状态：OK / SCHEMA_FAIL / DEGRADED',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_order (order_id),
  KEY idx_type_time (agent_type, created_at)
) ENGINE = InnoDB COMMENT = 'Agent 决策日志表（可审计可回放）';

CREATE TABLE kb_document (
  id              BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  doc_type        VARCHAR(20)  NOT NULL COMMENT '语料类型：KB 知识 / ORDER 历史工单',
  category        VARCHAR(30)  NULL COMMENT '所属类别（FaultCategory）',
  title           VARCHAR(200) NOT NULL DEFAULT '' COMMENT '标题',
  content         TEXT         NOT NULL COMMENT '正文（切片前原文）',
  source_order_id BIGINT UNSIGNED NULL COMMENT '来源工单（doc_type = ORDER 时）',
  vector_id       VARCHAR(64)  NULL COMMENT 'Redis 向量库主键',
  status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / EMBEDDING / DELETED',
  created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_type_category_status (doc_type, category, status)
) ENGINE = InnoDB COMMENT = '知识库文档表（向量本体存 Redis Stack，docs/05 §7）';

-- =====================================================================
-- 5. 支撑组
-- =====================================================================

CREATE TABLE notice (
  id           BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  community_id BIGINT UNSIGNED NULL COMMENT '生效小区（NULL = 全局）',
  title        VARCHAR(200)    NOT NULL COMMENT '标题',
  content      TEXT            NOT NULL COMMENT '正文',
  type         VARCHAR(30)     NULL COMMENT '类型：停水 / 停电 / 其他',
  status       VARCHAR(20)     NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：PUBLISHED / WITHDRAWN',
  deleted      TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 否 / 1 是',
  created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_community (community_id)
) ENGINE = InnoDB COMMENT = '物业公告表';

CREATE TABLE attachment (
  id           BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  biz_type     VARCHAR(30)  NOT NULL COMMENT '业务类型：ORDER_PHOTO / REPAIR_BEFORE / REPAIR_AFTER / AVATAR…',
  biz_id       BIGINT UNSIGNED NOT NULL COMMENT '业务 ID（多态关联，G8）',
  file_key     VARCHAR(200) NOT NULL COMMENT 'MinIO 对象键（UUID 防路径穿越）',
  content_type VARCHAR(100) NULL COMMENT 'MIME 类型（白名单校验）',
  size         BIGINT       NULL COMMENT '文件大小（字节）',
  uploader_id  BIGINT UNSIGNED NULL COMMENT '上传人（sys_user.id，G8）',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_biz (biz_type, biz_id)
) ENGINE = InnoDB COMMENT = '通用附件表（MinIO 对象存储）';

CREATE TABLE notify_record (
  id         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  user_id    BIGINT UNSIGNED NOT NULL COMMENT '接收用户（sys_user.id）',
  channel    VARCHAR(10)     NOT NULL COMMENT '通道：PUSH / SMS / INBOX',
  event      VARCHAR(50)     NOT NULL COMMENT '事件类型（如 ORDER_DISPATCHED）',
  biz_id     BIGINT UNSIGNED NULL COMMENT '关联业务 ID',
  status     VARCHAR(20)     NOT NULL DEFAULT 'SENT' COMMENT '发送状态：SENT / FAILED',
  created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user_time (user_id, created_at)
) ENGINE = InnoDB COMMENT = '通知发送记录表';

CREATE TABLE operation_log (
  id           BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  user_id      BIGINT UNSIGNED NULL COMMENT '操作人（sys_user.id）',
  module       VARCHAR(30)  NOT NULL COMMENT '模块：ORDER / WARRANTY_RULE / DISPATCH…',
  action       VARCHAR(30)  NOT NULL COMMENT '动作：判定纠正 / 人工派单 / 关单 / 规则变更…',
  target       VARCHAR(50)  NOT NULL DEFAULT '' COMMENT '对象标识（如工单号）',
  before_value TEXT         NULL COMMENT '变更前值（JSON）',
  after_value  TEXT         NULL COMMENT '变更后值（JSON）',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_module_time (module, created_at),
  KEY idx_user (user_id)
) ENGINE = InnoDB COMMENT = '操作留痕表（FR-S-06）';

CREATE TABLE sys_config (
  id           BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  config_key   VARCHAR(100) NOT NULL COMMENT '参数键，如 dispatch.weights.skill',
  config_value VARCHAR(500) NOT NULL DEFAULT '' COMMENT '参数值',
  remark       VARCHAR(200) NULL COMMENT '说明',
  updated_by   BIGINT UNSIGNED NULL COMMENT '最后修改人（sys_user.id）',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_key (config_key)
) ENGINE = InnoDB COMMENT = '系统参数表（运行时可变配置，FR-A-08，G2；yml 中 warranty.* 为缺省值）';
