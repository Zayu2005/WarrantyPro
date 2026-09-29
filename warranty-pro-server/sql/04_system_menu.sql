USE warranty_pro;

-- Execute with mysql reading this UTF-8 file directly (for example, cmd redirection).

CREATE TABLE IF NOT EXISTS sys_menu (
  id         BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
  parent_id  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '上级菜单，0 表示根节点',
  name       VARCHAR(50) NOT NULL COMMENT '菜单名称',
  path       VARCHAR(100) NOT NULL DEFAULT '' COMMENT '前端页面路径',
  icon       VARCHAR(40) NOT NULL DEFAULT '' COMMENT 'Element Plus 图标名',
  type       VARCHAR(10) NOT NULL COMMENT 'DIR 目录 / MENU 页面',
  sort_order INT NOT NULL DEFAULT 0 COMMENT '同级排序',
  visible    TINYINT NOT NULL DEFAULT 1 COMMENT '1 显示 / 0 隐藏',
  status     TINYINT NOT NULL DEFAULT 1 COMMENT '1 启用 / 0 停用',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_menu_path (path),
  KEY idx_parent_order (parent_id, sort_order)
) ENGINE = InnoDB COMMENT = '后台导航菜单';

INSERT IGNORE INTO sys_menu (id, parent_id, name, path, icon, type, sort_order, visible, status) VALUES
  (1, 0, '首页', '/dashboard', 'House', 'MENU', 1, 1, 1),
  (2, 0, '物业业务', '', 'Setting', 'DIR', 2, 1, 1),
  (3, 2, '工单管理', '/orders', 'Tickets', 'MENU', 1, 1, 1),
  (4, 2, '排班管理', '/schedule', 'Calendar', 'MENU', 2, 1, 1),
  (5, 2, '维修人员', '/workers', 'UserFilled', 'MENU', 3, 1, 1),
  (6, 2, '维修评价', '/reviews', 'Star', 'MENU', 4, 1, 1),
  (7, 2, '工单归档', '/archives', 'FolderOpened', 'MENU', 5, 1, 1),
  (8, 2, '报修类别', '/repair-categories', 'CollectionTag', 'MENU', 6, 1, 1),
  (9, 2, '业主管理', '/owners', 'User', 'MENU', 7, 1, 1),
  (10, 2, '物业通知', '/notices', 'Bell', 'MENU', 8, 1, 1),
  (11, 2, '轮播管理', '/banners', 'Picture', 'MENU', 9, 1, 1),
  (12, 0, '系统管理', '', 'Setting', 'DIR', 3, 1, 1),
  (13, 12, '用户管理', '/users', 'User', 'MENU', 1, 1, 1),
  (14, 12, '角色管理', '/roles', 'UserFilled', 'MENU', 2, 1, 1),
  (15, 12, '菜单管理', '/menus', 'Menu', 'MENU', 3, 1, 1),
  (16, 12, '部门管理', '/departments', 'House', 'MENU', 4, 1, 1),
  (17, 12, '智能体监测', '/agent-workflow', 'MagicStick', 'MENU', 5, 1, 1),
  (18, 12, '定时任务', '/scheduled-tasks', 'AlarmClock', 'MENU', 6, 1, 1);
