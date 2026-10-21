-- ============================================================
-- 迁移: 登录改为 Web 端账号密码
-- 1) user 表新增 username / password_hash
-- 2) 兼容历史账号: 用 openid 回填 username
-- 3) 删除 openid（本项目不再对接小程序）
-- 演示账号: demo / demo1234, demo_trend / demo1234
-- 幂等: 可重复执行
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

CALL sp_add_col('user', 'username', '`username` VARCHAR(50) NULL AFTER `id`');
CALL sp_add_col('user', 'password_hash', '`password_hash` VARCHAR(100) NULL AFTER `username`');

-- 历史账号回填：先按 openid 映射成可读用户名（openid 已删时自动跳过）
SET @has_openid_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
                       WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'openid');
SET @ddl = IF(@has_openid_col > 0,
    'UPDATE `user` SET `username` = ''demo'' WHERE `openid` = ''test_openid_001''',
    'DO 0');
CALL sp_exec(@ddl);
SET @ddl = IF(@has_openid_col > 0,
    'UPDATE `user` SET `username` = ''demo_trend'' WHERE `openid` = ''demo_superpatient''',
    'DO 0');
CALL sp_exec(@ddl);

-- 剩下的账号给一个占位用户名，避免 NOT NULL 失败
UPDATE `user` SET `username` = CONCAT('user_', `id`) WHERE `username` IS NULL OR `username` = '';
UPDATE `user` SET `password_hash` = '$2a$10$4Y8UtJSTpAitmeigyR5uqOxlb4Ss19zTBY8gQzVsGN2E1jxpoXjje'
WHERE `username` IN ('demo', 'demo_trend') AND (`password_hash` IS NULL OR `password_hash` = '');

-- 约束
CALL sp_exec('ALTER TABLE `user` MODIFY `username` VARCHAR(50) NOT NULL');

SET @has_uk = (SELECT COUNT(*) FROM information_schema.STATISTICS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND INDEX_NAME = 'uk_username');
SET @ddl = IF(@has_uk = 0, 'ALTER TABLE `user` ADD UNIQUE KEY `uk_username` (`username`)', 'DO 0');
CALL sp_exec(@ddl);

-- 小程序相关字段下线
SET @has_openid = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'openid');
SET @ddl = IF(@has_openid > 0, 'ALTER TABLE `user` DROP COLUMN `openid`', 'DO 0');
CALL sp_exec(@ddl);

DROP PROCEDURE sp_add_col;
DROP PROCEDURE sp_exec;

SELECT id, username, nickname FROM `user`;
