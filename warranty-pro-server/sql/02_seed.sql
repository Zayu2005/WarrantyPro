-- =====================================================================
-- WarrantyPro · 种子数据（02_seed.sql）
-- 前置：先执行 01_schema.sql
-- 内容：五角色 / 法定保修期限规则（《建设工程质量管理条例》第四十条，docs/03 §4）
--       / 系统参数缺省值（与 application.yml warranty.* 对齐）
-- 说明：管理员账号不在此预置（密码 BCrypt 须由应用生成），首次通过
--       注册接口 + 手动授角色，或启动类 CommandLineRunner 初始化。
-- =====================================================================

USE warranty_pro;

-- ---------------- 1. 角色（docs/02 §1） ----------------
INSERT INTO sys_role (id, code, name) VALUES
  (1, 'OWNER',      '业主 / 住户'),
  (2, 'WORKER',     '维修师傅'),
  (3, 'DISPATCHER', '物业客服（调度员）'),
  (4, 'MANAGER',    '物业管理层'),
  (5, 'ADMIN',      '系统管理员');

-- ---------------- 2. 法定保修期限规则（scope = LEGAL，全局生效） ----------------
INSERT INTO warranty_rule (scope, part_category, duration_value, duration_unit, community_id, source, priority) VALUES
  ('LEGAL', 'MAIN_STRUCTURE',  NULL, 'DESIGN_LIFE',    NULL, '地基基础与主体结构：设计文件规定的合理使用年限（条例第四十条）', 100),
  ('LEGAL', 'WATERPROOF',      5,    'YEAR',           NULL, '屋面防水、有防水要求的卫生间/房间/外墙面防渗漏：5 年（条例第四十条）', 100),
  ('LEGAL', 'HEATING_COOLING', 2,    'HEATING_SEASON', NULL, '供热与供冷系统：2 个采暖期/供冷期（条例第四十条）', 100),
  ('LEGAL', 'ME_INSTALLATION', 2,    'YEAR',           NULL, '电气管线、给排水管道、设备安装与装修工程：2 年（条例第四十条）', 100),
  ('LEGAL', 'INSULATION',      5,    'YEAR',           NULL, '保温工程：5 年（条例第四十条）', 100);

-- ---------------- 3. 系统参数缺省值（FR-A-08，管理员可在 PC 端修改） ----------------
INSERT INTO sys_config (config_key, config_value, remark) VALUES
  ('dispatch.weights.skill',                    '0.40', '派单权重：技能匹配度'),
  ('dispatch.weights.load',                     '0.25', '派单权重：负载空闲度'),
  ('dispatch.weights.location',                 '0.20', '派单权重：位置就近度'),
  ('dispatch.weights.rating',                   '0.15', '派单权重：历史评分'),
  ('dispatch.accept-timeout-minutes',           '15',   '师傅接单超时（分钟），超时自动改派'),
  ('dispatch.max-reassign-rounds',              '3',    '自动改派最大轮次，超限告警客服人工介入'),
  ('dispatch.auto-dispatch-timeout-minutes',    '15',   '客服超时未处理（分钟）转自动派单'),
  ('dispatch.max-concurrent',                   '3',    '师傅并发在办单量上限'),
  ('accept-timeout.dispatcher-normal-minutes',  '30',   '客服受理超时（普通工单，分钟）'),
  ('accept-timeout.dispatcher-urgent-minutes',  '10',   '客服受理超时（紧急工单，分钟）'),
  ('arrive-timeout.normal-hours',               '4',    '到场超时（普通工单，小时），催办师傅'),
  ('arrive-timeout.urgent-hours',               '2',    '到场超时（紧急工单，小时），催办师傅'),
  ('confirm.auto-pass-hours',                   '48',   '业主验收超时（小时）默认通过'),
  ('warranty.alert.ahead-days',                 '90',   '保修到期预警提前天数'),
  ('ai.enabled',                                'true', 'AI Agent 总开关（降级控制，docs/05 §8）');
