-- ============================================================
-- 迁移: 参考区间规则表
-- 目的: 同一指标在不同性别/年龄段区间不同, 不能只靠指标默认值
-- 优先级: 报告印刷区间 > 本规则表(命中) > 指标默认值
-- 幂等: 可重复执行
-- ============================================================

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `reference_range` (
    `id`           INT           NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `indicator_id` INT           NOT NULL COMMENT '指标ID',
    `gender`       TINYINT       DEFAULT NULL COMMENT '性别: NULL=不限 0=女 1=男',
    `age_min`      INT           DEFAULT NULL COMMENT '年龄下限(含), NULL=不限',
    `age_max`      INT           DEFAULT NULL COMMENT '年龄上限(含), NULL=不限',
    `ref_min`      DECIMAL(12,4) DEFAULT NULL COMMENT '参考下限',
    `ref_max`      DECIMAL(12,4) DEFAULT NULL COMMENT '参考上限',
    `ref_text`     VARCHAR(100)  DEFAULT NULL COMMENT '区间原文',
    `source`       VARCHAR(50)   DEFAULT NULL COMMENT '来源说明',
    `priority`     INT           DEFAULT 0 COMMENT '优先级, 越大越优先',
    `created_at`   DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_rr_indicator` (`indicator_id`),
    CONSTRAINT `fk_rr_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `exam_indicator` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参考区间规则表';

-- exam_value 增加来源标记, 区分"报告印刷"和"规则推算"
-- (MySQL 8 不支持 ADD COLUMN IF NOT EXISTS, 用存储过程保证幂等)
DROP PROCEDURE IF EXISTS sp_add_ref_source;
DELIMITER $$
CREATE PROCEDURE sp_add_ref_source()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_value' AND COLUMN_NAME = 'ref_source') THEN
        ALTER TABLE `exam_value` ADD COLUMN `ref_source` VARCHAR(10) DEFAULT NULL
            COMMENT '参考区间来源: report=报告印刷 rule=规则推算' AFTER `ref_text`;
    END IF;
END$$
DELIMITER ;
CALL sp_add_ref_source();
DROP PROCEDURE sp_add_ref_source;

-- ---------------- 规则种子数据 ----------------
DELETE FROM `reference_range`;

INSERT INTO `reference_range` (`indicator_id`, `gender`, `age_min`, `age_max`, `ref_min`, `ref_max`, `ref_text`, `source`, `priority`)
SELECT id, 1, 18, NULL, 130, 175, '130-175', '成人男性参考区间', 10 FROM `exam_indicator` WHERE `name` = '血红蛋白';
INSERT INTO `reference_range` (`indicator_id`, `gender`, `age_min`, `age_max`, `ref_min`, `ref_max`, `ref_text`, `source`, `priority`)
SELECT id, 0, 18, NULL, 115, 150, '115-150', '成人女性参考区间', 10 FROM `exam_indicator` WHERE `name` = '血红蛋白';
INSERT INTO `reference_range` (`indicator_id`, `gender`, `age_min`, `age_max`, `ref_min`, `ref_max`, `ref_text`, `source`, `priority`)
SELECT id, NULL, 0, 17, 110, 160, '110-160', '儿童参考区间', 10 FROM `exam_indicator` WHERE `name` = '血红蛋白';

INSERT INTO `reference_range` (`indicator_id`, `gender`, `age_min`, `age_max`, `ref_min`, `ref_max`, `ref_text`, `source`, `priority`)
SELECT id, 1, 18, NULL, 208, 428, '208-428', '成人男性参考区间', 10 FROM `exam_indicator` WHERE `name` = '尿酸';
INSERT INTO `reference_range` (`indicator_id`, `gender`, `age_min`, `age_max`, `ref_min`, `ref_max`, `ref_text`, `source`, `priority`)
SELECT id, 0, 18, NULL, 155, 357, '155-357', '成人女性参考区间', 10 FROM `exam_indicator` WHERE `name` = '尿酸';

INSERT INTO `reference_range` (`indicator_id`, `gender`, `age_min`, `age_max`, `ref_min`, `ref_max`, `ref_text`, `source`, `priority`)
SELECT id, 1, 18, NULL, 57, 97, '57-97', '成人男性参考区间', 10 FROM `exam_indicator` WHERE `name` = '肌酐';
INSERT INTO `reference_range` (`indicator_id`, `gender`, `age_min`, `age_max`, `ref_min`, `ref_max`, `ref_text`, `source`, `priority`)
SELECT id, 0, 18, NULL, 41, 73, '41-73', '成人女性参考区间', 10 FROM `exam_indicator` WHERE `name` = '肌酐';

INSERT INTO `reference_range` (`indicator_id`, `gender`, `age_min`, `age_max`, `ref_min`, `ref_max`, `ref_text`, `source`, `priority`)
SELECT id, 1, NULL, NULL, 0, 15, '0-15', '男性参考区间', 5 FROM `exam_indicator` WHERE `name` = '血沉';
INSERT INTO `reference_range` (`indicator_id`, `gender`, `age_min`, `age_max`, `ref_min`, `ref_max`, `ref_text`, `source`, `priority`)
SELECT id, 0, NULL, NULL, 0, 20, '0-20', '女性参考区间', 5 FROM `exam_indicator` WHERE `name` = '血沉';

INSERT INTO `reference_range` (`indicator_id`, `gender`, `age_min`, `age_max`, `ref_min`, `ref_max`, `ref_text`, `source`, `priority`)
SELECT id, NULL, 1, 14, 100, 350, '100-350', '儿童生长期参考区间', 10 FROM `exam_indicator` WHERE `name` = '碱性磷酸酶';

INSERT INTO `reference_range` (`indicator_id`, `gender`, `age_min`, `age_max`, `ref_min`, `ref_max`, `ref_text`, `source`, `priority`)
SELECT id, NULL, 0, 14, 5, 12, '5-12', '儿童参考区间', 10 FROM `exam_indicator` WHERE `name` = '白细胞计数';
