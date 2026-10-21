-- ============================================================
-- 迁移: 合并 patient 到 user
-- 背景: 本项目面向"用户管理自己的指标", 用户即患者本人,
--       原来 1:1 的 patient 表只是把性别/生日多存了一遍。
-- 内容:
--   1) user 增加 gender / birth_date / notes
--   2) 从 patient 回填
--   3) exam_record.patient_id -> user_id
--   4) 删除 patient 表
-- 说明: 一次性升级脚本, 执行前请备份
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS sp_add_col;
DROP PROCEDURE IF EXISTS sp_exec;
DELIMITER $$
CREATE PROCEDURE sp_exec(IN ddl TEXT)
BEGIN
    SET @s = ddl;
    PREPARE st FROM @s;
    EXECUTE st;
    DEALLOCATE PREPARE st;
END$$
CREATE PROCEDURE sp_add_col(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column) THEN
        CALL sp_exec(CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl));
    END IF;
END$$
DELIMITER ;

CALL sp_add_col('user', 'gender', '`gender` TINYINT DEFAULT NULL COMMENT ''性别: 0=女 1=男'' AFTER `phone`');
CALL sp_add_col('user', 'birth_date', '`birth_date` DATE DEFAULT NULL COMMENT ''出生日期'' AFTER `gender`');
CALL sp_add_col('user', 'notes', '`notes` TEXT DEFAULT NULL COMMENT ''备注'' AFTER `birth_date`');

-- 从 patient 回填（1:1）
UPDATE `user` u
    JOIN `patient` p ON p.user_id = u.id
SET u.gender = p.gender,
    u.birth_date = p.birth_date,
    u.notes = p.notes
WHERE u.gender IS NULL AND u.birth_date IS NULL;

-- exam_record 增加 user_id 并回填
CALL sp_add_col('exam_record', 'user_id', '`user_id` BIGINT DEFAULT NULL AFTER `id`');
UPDATE `exam_record` r
    JOIN `patient` p ON r.patient_id = p.id
SET r.user_id = p.user_id
WHERE r.user_id IS NULL;

-- 去掉旧外键与索引
SET @fk = (SELECT CONSTRAINT_NAME FROM information_schema.TABLE_CONSTRAINTS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_record'
             AND CONSTRAINT_TYPE = 'FOREIGN KEY' AND CONSTRAINT_NAME = 'fk_record_patient' LIMIT 1);
SET @ddl = IF(@fk IS NULL, 'DO 0', 'ALTER TABLE `exam_record` DROP FOREIGN KEY fk_record_patient');
CALL sp_exec(@ddl);

SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_record' AND INDEX_NAME = 'idx_patient_date');
SET @ddl = IF(@idx = 0, 'DO 0', 'ALTER TABLE `exam_record` DROP INDEX idx_patient_date');
CALL sp_exec(@ddl);

CALL sp_exec('ALTER TABLE `exam_record` MODIFY `user_id` BIGINT NOT NULL COMMENT ''用户ID (关联user.id)''');

SET @idx2 = (SELECT COUNT(*) FROM information_schema.STATISTICS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_record' AND INDEX_NAME = 'idx_user_date');
SET @ddl = IF(@idx2 = 0, 'ALTER TABLE `exam_record` ADD KEY `idx_user_date` (`user_id`, `exam_date`)', 'DO 0');
CALL sp_exec(@ddl);

CALL sp_exec('ALTER TABLE `exam_record` ADD CONSTRAINT `fk_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE');

SET @has_pid = (SELECT COUNT(*) FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'exam_record' AND COLUMN_NAME = 'patient_id');
SET @ddl = IF(@has_pid > 0, 'ALTER TABLE `exam_record` DROP COLUMN `patient_id`', 'DO 0');
CALL sp_exec(@ddl);

DROP TABLE IF EXISTS `patient`;

DROP PROCEDURE sp_add_col;
DROP PROCEDURE sp_exec;

SELECT COUNT(*) AS users, (SELECT COUNT(*) FROM exam_record) AS records FROM `user`;
