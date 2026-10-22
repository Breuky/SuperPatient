-- ============================================================
-- 迁移: 长期关注指标
-- 用户把自己长期关心的指标标星，录入页与趋势页默认只看这些
-- 幂等: 可重复执行
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `user_indicator_focus` (
    `id`           BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`      BIGINT   NOT NULL COMMENT '用户ID',
    `indicator_id` INT      NOT NULL COMMENT '指标ID',
    `created_at`   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_indicator` (`user_id`, `indicator_id`),
    CONSTRAINT `fk_focus_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_focus_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `exam_indicator` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户关注的指标';

SELECT COUNT(*) AS focus_rows FROM `user_indicator_focus`;
