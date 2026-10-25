-- Agent 调用留痕：用于观察「用户都在问什么、模型都查了什么」
CREATE TABLE IF NOT EXISTS `insight_log` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`    BIGINT       NOT NULL,
    `record_id`  BIGINT       DEFAULT NULL,
    `mode`       VARCHAR(16)  NOT NULL COMMENT 'simple/agent/rule',
    `question`   VARCHAR(500) DEFAULT NULL,
    `turns`      INT          DEFAULT NULL COMMENT '对话轮数',
    `tool_calls` INT          DEFAULT 0 COMMENT '工具调用次数',
    `tokens`     INT          DEFAULT 0,
    `elapsed_ms` INT          DEFAULT 0,
    `degraded`   TINYINT      DEFAULT 0,
    `summary`    VARCHAR(1000) DEFAULT NULL,
    `trace_json` TEXT         DEFAULT NULL COMMENT '每次工具调用的参数与耗时',
    `created_at` DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_insight_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 调用留痕';
