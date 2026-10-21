-- ============================================================
-- 检查指标记录系统 - MySQL 数据库 Schema
-- 版本: 1.0
-- 字符集: utf8mb4 (支持中文和 Emoji)
-- 引擎: InnoDB (支持事务和外键)
-- ============================================================

CREATE DATABASE IF NOT EXISTS `super_patient`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `super_patient`;

-- ============================================================
-- 1. 用户表 (微信小程序用户)
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `openid`      VARCHAR(64)  NOT NULL COMMENT '微信openid (唯一标识)',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '用户昵称',
    `avatar_url`  VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号 (微信授权获取)',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小程序用户表';

-- ============================================================
-- 2. 患者信息表
-- ============================================================
DROP TABLE IF EXISTS `patient`;
CREATE TABLE `patient` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`        BIGINT       NOT NULL COMMENT '所属用户ID (关联user.id)',
    `name`           VARCHAR(50)  NOT NULL COMMENT '患者姓名',
    `gender`         TINYINT      NOT NULL COMMENT '性别: 0=女 1=男',
    `birth_date`     DATE         DEFAULT NULL COMMENT '出生日期',
    `diagnosis_date` DATE         DEFAULT NULL COMMENT '确诊日期',
    `phone`          VARCHAR(20)  DEFAULT NULL COMMENT '联系电话',
    `notes`          TEXT         DEFAULT NULL COMMENT '备注信息 (如常规分型、分期等)',
    `created_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    CONSTRAINT `fk_patient_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='患者信息表';

-- ============================================================
-- 3. 检查分类表 (抽血 / CT / MRI 等)
-- ============================================================
DROP TABLE IF EXISTS `exam_category`;
CREATE TABLE `exam_category` (
    `id`          INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(50)  NOT NULL COMMENT '分类名称 (如: 抽血、CT、MRI)',
    `icon`        VARCHAR(50)  DEFAULT NULL COMMENT '图标标识 (用于前端显示)',
    `sort_order`  INT          DEFAULT 0 COMMENT '排序权重 (越小越靠前)',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查分类表';

-- ============================================================
-- 4. 检查指标定义表 (每个分类下的具体指标)
--    核心设计: 指标定义与指标值分离存储
--    新增指标只需 INSERT 一行, 不需要改表结构
-- ============================================================
DROP TABLE IF EXISTS `exam_indicator`;
CREATE TABLE `exam_indicator` (
    `id`           INT            NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `category_id`  INT            NOT NULL COMMENT '所属分类ID (关联exam_category.id)',
    `name`         VARCHAR(100)   NOT NULL COMMENT '指标名称 (如: 血红蛋白、白细胞计数)',
    `unit`         VARCHAR(20)    DEFAULT NULL COMMENT '单位 (如: g/L、10^9/L、mm)',
    `value_type`   VARCHAR(10)    DEFAULT 'number' COMMENT '值类型: number=数值型 text=文本型',
    `normal_min`   DECIMAL(12,4)  DEFAULT NULL COMMENT '正常下限 (数值型指标使用)',
    `normal_max`   DECIMAL(12,4)  DEFAULT NULL COMMENT '正常上限 (数值型指标使用)',
    `sort_order`   INT            DEFAULT 0 COMMENT '排序权重',
    `status`       TINYINT        DEFAULT 1 COMMENT '状态: 1=启用 0=禁用 (软删除)',
    `created_at`   DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_category_id` (`category_id`),
    CONSTRAINT `fk_indicator_category` FOREIGN KEY (`category_id`) REFERENCES `exam_category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查指标定义表';

-- ============================================================
-- 5. 检查记录表 (每次检查事件 = 一条记录)
--    一次抽血、一次CT扫描都是一条 exam_record
-- ============================================================
DROP TABLE IF EXISTS `exam_record`;
CREATE TABLE `exam_record` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `patient_id`  BIGINT       NOT NULL COMMENT '患者ID (关联patient.id)',
    `category_id` INT          NOT NULL COMMENT '检查分类ID (关联exam_category.id)',
    `exam_date`   DATETIME     NOT NULL COMMENT '检查日期时间',
    `hospital`    VARCHAR(100) DEFAULT NULL COMMENT '检查医院',
    `doctor`      VARCHAR(50)  DEFAULT NULL COMMENT '主治医生',
    `notes`       TEXT         DEFAULT NULL COMMENT '备注 (医嘱、特殊情况等)',
    `image_url`   VARCHAR(500) DEFAULT NULL COMMENT '报告照片URL (OSS存储路径)',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_patient_date` (`patient_id`, `exam_date`),
    KEY `idx_category` (`category_id`),
    CONSTRAINT `fk_record_patient` FOREIGN KEY (`patient_id`) REFERENCES `patient` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_record_category` FOREIGN KEY (`category_id`) REFERENCES `exam_category` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查记录表';

-- ============================================================
-- 6. 检查指标值表 (每条记录下的具体测量值)
--    一条 exam_record 可以有多个 exam_value (一对多)
--    value_decimal 存数值, value_text 存文本 (CT描述等)
--    is_abnormal 后端自动计算: 0=正常 1=偏高 2=偏低
-- ============================================================
DROP TABLE IF EXISTS `exam_value`;
CREATE TABLE `exam_value` (
    `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `record_id`      BIGINT        NOT NULL COMMENT '检查记录ID (关联exam_record.id)',
    `indicator_id`   INT           NOT NULL COMMENT '指标ID (关联exam_indicator.id)',
    `value_decimal`  DECIMAL(12,4) DEFAULT NULL COMMENT '数值型测量值 (血红蛋白、白细胞等)',
    `value_text`     TEXT          DEFAULT NULL COMMENT '文本型测量值 (CT描述、MRI描述等)',
    `is_abnormal`    TINYINT       DEFAULT 0 COMMENT '是否异常: 0=正常 1=偏高 2=偏低 (后端自动计算)',
    `created_at`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_record_id` (`record_id`),
    KEY `idx_indicator` (`indicator_id`),
    CONSTRAINT `fk_value_record` FOREIGN KEY (`record_id`) REFERENCES `exam_record` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_value_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `exam_indicator` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查指标值表';


-- ============================================================
-- 7. 上传文件归属表
--    报告图片按上传者鉴权访问, 不走公开静态目录
-- ============================================================
DROP TABLE IF EXISTS `upload_file`;
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


-- ============================================================
-- =================== 种子数据: 检查分类 ====================
-- ============================================================
INSERT INTO `exam_category` (`id`, `name`, `icon`, `sort_order`) VALUES
(1, '抽血',     'blood',  1),
(2, 'CT',      'ct',     2),
(3, 'MRI',     'mri',    3),
(4, '尿常规',  'pathology', 4),
(5, '超声',  'ultrasound',  5);

-- ============================================================
-- ============== 种子数据: 抽血指标模板 ====================
-- ============================================================
INSERT INTO `exam_indicator` (`category_id`, `name`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`) VALUES
-- 血常规
(1, '血红蛋白',         'g/L',     'number', 120.0000, 160.0000, 1,  1),
(1, '白细胞计数',       '10^9/L',  'number', 4.0000,   10.0000,  2,  1),
(1, '中性粒细胞百分比', '%',       'number', 40.0000,  75.0000,  3,  1),
(1, '淋巴细胞百分比',   '%',       'number', 20.0000,  50.0000,  4,  1),
(1, '血小板计数',       '10^9/L',  'number', 100.0000, 300.0000, 5,  1),
-- 生化指标
(1, '乳酸脱氢酶',       'U/L',     'number', 120.0000, 250.0000, 6,  1),
(1, '碱性磷酸酶',       'U/L',     'number', 40.0000,  150.0000, 7,  1),
(1, '血钙',             'mmol/L',  'number', 2.1000,   2.6000,   8,  1),
-- 炎症标志物
(1, 'C反应蛋白',        'mg/L',    'number', 0.0000,   10.0000,  9,  1),
(1, '血沉',             'mm/h',    'number', 0.0000,   15.0000,  10, 1);

-- ============================================================
-- ============== 种子数据: CT 指标模板 =====================
-- (CT 检查多为描述性文本, 使用 text 类型)
-- ============================================================
INSERT INTO `exam_indicator` (`category_id`, `name`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`) VALUES
(2, '检查部位',       'mm',  'number', NULL, NULL, 1, 1),
(2, '影像描述',         NULL,  'text',   NULL, NULL, 2, 1),
(2, '检查结论',       NULL,  'text',   NULL, NULL, 3, 1),
(2, '检查结论',       NULL,  'text',   NULL, NULL, 4, 1),
(2, '检查结论',         NULL,  'text',   NULL, NULL, 5, 1),
(2, '影像描述',         NULL,  'text',   NULL, NULL, 6, 1);

-- ============================================================
-- ============== 种子数据: MRI 指标模板 ====================
-- ============================================================
INSERT INTO `exam_indicator` (`category_id`, `name`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`) VALUES
(3, '检查部位',       'mm',  'number', NULL, NULL, 1, 1),
(3, '检查结论',         NULL,  'text',   NULL, NULL, 2, 1),
(3, '检查结论',     'mm',  'number', NULL, NULL, 3, 1),
(3, '检查结论',       NULL,  'text',   NULL, NULL, 4, 1),
(3, '检查结论',   NULL,  'text',   NULL, NULL, 5, 1),
(3, '影像描述',         NULL,  'text',   NULL, NULL, 6, 1);


-- ============================================================
-- ============== 示例数据 (开发测试用) =====================
-- ============================================================

-- 测试用户
INSERT INTO `user` (`id`, `openid`, `nickname`, `avatar_url`) VALUES
(1, 'test_openid_001', '测试医生', NULL);

-- 测试患者
INSERT INTO `patient` (`id`, `user_id`, `name`, `gender`, `birth_date`, `diagnosis_date`, `phone`, `notes`) VALUES
(1, 1, '张明', 1, '2010-05-12', '2025-03-15', '13800138000', '年度体检，无特殊病史');

-- 检查记录 1: 3月28日抽血
INSERT INTO `exam_record` (`id`, `patient_id`, `category_id`, `exam_date`, `hospital`, `doctor`, `notes`) VALUES
(1, 1, 1, '2026-03-28 09:30:00', '市第一人民医院', '陈主任', '年度体检');

INSERT INTO `exam_value` (`record_id`, `indicator_id`, `value_decimal`, `is_abnormal`) VALUES
(1, 1, 142.0000, 0),  -- 血红蛋白 142 正常
(1, 2, 6.8000,   0),  -- 白细胞 6.8 正常
(1, 5, 185.0000, 0),  -- 血小板 185 正常
(1, 9, 5.2000,   0);  -- CRP 5.2 正常

-- 检查记录 2: 5月20日抽血
INSERT INTO `exam_record` (`id`, `patient_id`, `category_id`, `exam_date`, `hospital`, `doctor`, `notes`) VALUES
(2, 1, 1, '2026-05-20 10:00:00', '市第一人民医院', '陈主任', '复查');

INSERT INTO `exam_value` (`record_id`, `indicator_id`, `value_decimal`, `is_abnormal`) VALUES
(2, 1, 125.0000, 0),  -- 血红蛋白 125 正常
(2, 2, 3.2000,   2),  -- 白细胞 3.2 偏低
(2, 5, 90.0000,  2),  -- 血小板 90 偏低
(2, 9, 18.5000,  1);  -- CRP 18.5 偏高

-- 检查记录 3: 6月15日CT
INSERT INTO `exam_record` (`id`, `patient_id`, `category_id`, `exam_date`, `hospital`, `doctor`, `notes`, `image_url`) VALUES
(3, 1, 2, '2026-06-15 14:00:00', '市第一人民医院', '陈主任', '随访两周期后疗效评估', 'https://oss.example.com/report_003.jpg');

INSERT INTO `exam_value` (`record_id`, `indicator_id`, `value_decimal`, `value_text`, `is_abnormal`) VALUES
(3, 11, 45.0000, NULL,  0),  -- 检查部位 45mm
(3, 12, NULL,     '右股骨远端干骺端', 0),  -- 影像描述
(3, 13, NULL,     '较前片未见明显变化', 0),
(3, 14, NULL,     '检查结论明显缩小', 0),
(3, 16, NULL,     '未见明显变化, 指标缩小约30%', 0);


-- ============================================================
-- ============== 实用查询示例 (开发参考) ===================
-- ============================================================

-- 查询1: 获取某患者某指标的历史趋势 (核心查询)
-- SELECT
--     r.exam_date,
--     v.value_decimal,
--     v.is_abnormal,
--     i.name AS indicator_name,
--     i.unit
-- FROM exam_value v
-- JOIN exam_record r ON v.record_id = r.id
-- JOIN exam_indicator i ON v.indicator_id = i.id
-- WHERE r.patient_id = 1
--   AND v.indicator_id = 1
--   AND v.value_decimal IS NOT NULL
-- ORDER BY r.exam_date ASC;

-- 查询2: 获取某患者最近一次检查的异常指标
-- SELECT i.name, v.value_decimal, i.unit, v.is_abnormal, r.exam_date
-- FROM exam_value v
-- JOIN exam_record r ON v.record_id = r.id
-- JOIN exam_indicator i ON v.indicator_id = i.id
-- WHERE r.patient_id = 1
--   AND v.is_abnormal != 0
--   AND r.id = (SELECT MAX(id) FROM exam_record WHERE patient_id = 1)
-- ORDER BY v.is_abnormal;

-- 查询3: 获取某患者的检查统计
-- SELECT
--     c.name AS category_name,
--     COUNT(r.id) AS record_count,
--     MAX(r.exam_date) AS last_exam_date
-- FROM exam_record r
-- JOIN exam_category c ON r.category_id = c.id
-- WHERE r.patient_id = 1
-- GROUP BY c.name;

<!-- schema.sql 结束 -->
