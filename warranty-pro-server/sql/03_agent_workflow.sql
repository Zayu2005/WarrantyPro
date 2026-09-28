CREATE TABLE agent_workflow_run (
  id            BIGINT UNSIGNED AUTO_INCREMENT COMMENT '运行 ID',
  workflow_key  VARCHAR(60)  NOT NULL COMMENT '工作流标识',
  workflow_name VARCHAR(120) NOT NULL COMMENT '工作流名称',
  caller_id     BIGINT UNSIGNED NULL COMMENT '调用者用户 ID',
  input_summary VARCHAR(500) NOT NULL DEFAULT '' COMMENT '脱敏后的输入摘要',
  status        VARCHAR(20)  NOT NULL COMMENT 'RUNNING / COMPLETED / DEGRADED / FAILED',
  result        MEDIUMTEXT   NULL COMMENT '最终工作流输出',
  latency_ms    INT          NULL COMMENT '执行耗时',
  started_at    DATETIME     NOT NULL,
  finished_at   DATETIME     NULL,
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_workflow_time (workflow_key, created_at),
  KEY idx_status_time (status, created_at)
) ENGINE = InnoDB COMMENT = 'LangChain4j 工作流运行记录';

CREATE TABLE agent_workflow_node_log (
  id             BIGINT UNSIGNED AUTO_INCREMENT COMMENT '节点日志 ID',
  run_id         BIGINT UNSIGNED NOT NULL,
  node_key       VARCHAR(60)  NOT NULL,
  node_name      VARCHAR(120) NOT NULL,
  sequence_no    INT          NOT NULL,
  status         VARCHAR(20)  NOT NULL COMMENT 'RUNNING / COMPLETED / DEGRADED / FAILED',
  input_summary  VARCHAR(1000) NOT NULL DEFAULT '',
  output_summary MEDIUMTEXT NULL,
  model          VARCHAR(100) NULL,
  latency_ms     INT          NULL,
  error_message  VARCHAR(1000) NULL,
  started_at     DATETIME     NOT NULL,
  finished_at    DATETIME     NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_run_sequence (run_id, sequence_no),
  KEY idx_created_at (created_at)
) ENGINE = InnoDB COMMENT = 'LangChain4j 工作流节点动态调用日志';
