-- ============================================================
-- 迁移: 上传文件归属表
-- 目的: 报告图片不再通过 /uploads/** 公开静态目录暴露，
--       改为按上传者鉴权的 /api/files/{id} 读取。
-- 幂等: 可重复执行
-- ============================================================
CREATE TABLE IF NOT EXISTS `upload_file` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`       BIGINT       NOT NULL COMMENT '上传者ID (关联user.id)',
    `file_name`     VARCHAR(128) NOT NULL COMMENT '磁盘文件名',
    `original_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    `content_type`  VARCHAR(100) DEFAULT NULL COMMENT 'MIME 类型',
    `size_bytes`    BIGINT       DEFAULT NULL COMMENT '文件大小',
    `biz_type`      VARCHAR(20)  DEFAULT 'report' COMMENT '用途: report=报告 avatar=头像',
    `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_file_name` (`file_name`),
    KEY `idx_upload_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='上传文件归属表';
