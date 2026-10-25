
-- ============================================================
-- 随访提醒：只存事实与计划，不存医疗建议
--   last_exam_date / interval_days 来自用户自己的历史，
--   due_date = last_exam_date + interval_days
-- ============================================================
CREATE TABLE IF NOT EXISTS `follow_up` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        BIGINT       NOT NULL COMMENT '所属用户',
    `indicator_id`   INT          NOT NULL COMMENT '指标ID',
    `last_exam_date` DATE         NOT NULL COMMENT '推出这条提醒的那次检查日期',
    `interval_days`  INT          NOT NULL COMMENT '该用户此项的既往复查间隔中位数(天)',
    `due_date`       DATE         NOT NULL COMMENT '到期复查日',
    `priority`       TINYINT      NOT NULL DEFAULT 0 COMMENT '0=普通 1=连续异常',
    `reason`         VARCHAR(255) DEFAULT NULL COMMENT '规则生成的依据，例如「近5次有3次偏高」',
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'pending' COMMENT 'pending/done/dismissed',
    `created_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_follow_up` (`user_id`, `indicator_id`, `due_date`),
    KEY `idx_follow_up_user` (`user_id`, `status`),
    CONSTRAINT `fk_follow_up_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_follow_up_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `exam_indicator` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='随访提醒(基于用户自身历史间隔)';
