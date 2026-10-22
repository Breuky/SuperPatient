-- ============================================================
-- 个人检查检验指标记录系统 - MySQL 数据库 Schema
-- 版本: 1.0
-- 字符集: utf8mb4 (支持中文和 Emoji)
-- 引擎: InnoDB (支持事务和外键)
-- ============================================================

CREATE DATABASE IF NOT EXISTS `super_patient`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `super_patient`;

-- 允许在已有数据的库上重复执行（会重建全部表）
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 1. 用户表
-- ============================================================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`      VARCHAR(50)  NOT NULL COMMENT '登录用户名 (唯一)',
    `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    `nickname`      VARCHAR(50)  DEFAULT NULL COMMENT '用户昵称',
    `avatar_url`    VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    `phone`         VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `gender`        TINYINT      DEFAULT NULL COMMENT '性别: 0=女 1=男 (参考区间匹配用)',
    `birth_date`    DATE         DEFAULT NULL COMMENT '出生日期 (参考区间匹配用)',
    `notes`         TEXT         DEFAULT NULL COMMENT '备注 (既往病史、过敏史等)',
    `created_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 检查分类表 (抽血 / CT / MRI 等)
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
    `aliases`      VARCHAR(255)   DEFAULT NULL COMMENT '别名, 逗号分隔 (用于抽取归一)',
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
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID (关联user.id)',
    `category_id` INT          NOT NULL COMMENT '检查分类ID (关联exam_category.id)',
    `exam_date`   DATETIME     NOT NULL COMMENT '检查日期时间',
    `hospital`    VARCHAR(100) DEFAULT NULL COMMENT '检查医院',
    `doctor`      VARCHAR(50)  DEFAULT NULL COMMENT '主治医生',
    `notes`       TEXT         DEFAULT NULL COMMENT '备注 (医嘱、特殊情况等)',
    `image_url`   VARCHAR(500) DEFAULT NULL COMMENT '报告照片URL (OSS存储路径)',
    `created_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_date` (`user_id`, `exam_date`),
    KEY `idx_category` (`category_id`),
    CONSTRAINT `fk_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE,
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
    `ref_min`        DECIMAL(12,4) DEFAULT NULL COMMENT '本次报告印刷的参考下限 (优先于指标默认值)',
    `ref_max`        DECIMAL(12,4) DEFAULT NULL COMMENT '本次报告印刷的参考上限',
    `ref_text`       VARCHAR(100)  DEFAULT NULL COMMENT '参考区间原文 (如 4.0-10.0 / 阴性)',
    `ref_source`     VARCHAR(10)   DEFAULT NULL COMMENT '参考区间来源: report=报告印刷 rule=规则推算',
    `raw_name`       VARCHAR(100)  DEFAULT NULL COMMENT '报告上的原始指标名 (抽取溯源)',
    `raw_unit`       VARCHAR(30)   DEFAULT NULL COMMENT '报告上的原始单位 (抽取溯源)',
    `is_abnormal`    TINYINT       DEFAULT 0 COMMENT '是否异常: 0=正常 1=偏高 2=偏低 (后端自动计算)',
    `created_at`     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_record_id` (`record_id`),
    KEY `idx_indicator` (`indicator_id`),
    CONSTRAINT `fk_value_record` FOREIGN KEY (`record_id`) REFERENCES `exam_record` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_value_indicator` FOREIGN KEY (`indicator_id`) REFERENCES `exam_indicator` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='检查指标值表';


-- ============================================================
-- 7. 参考区间规则表
--    同一指标在不同性别/年龄段区间不同。
--    优先级: 报告印刷区间 > 本表命中规则 > 指标默认值
-- ============================================================
DROP TABLE IF EXISTS `reference_range`;
CREATE TABLE `reference_range` (
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

-- ============================================================
-- 8. 上传文件归属表
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
(1, '抽血',    'blood',      1),
(2, '尿常规',  'urine',      2),
(3, 'CT',     'ct',         3),
(4, 'MRI',    'mri',        4),
(5, '超声',    'ultrasound', 5);

-- ============================================================
-- ============== 种子数据: 抽血指标 (通用健康体检常见项) ======
-- ============================================================
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`) VALUES
-- 血常规
(1, '血红蛋白',           'HGB,Hb,HB,血红蛋白浓度',            'g/L',    'number', 120.0000, 160.0000, 1,  1),
(1, '白细胞计数',         'WBC,白细胞,白细胞数,LEU',          '10^9/L', 'number', 4.0000,   10.0000,  2,  1),
(1, '中性粒细胞百分比',   'NEUT%,NEUT,中性粒细胞比率',        '%',      'number', 40.0000,  75.0000,  3,  1),
(1, '淋巴细胞百分比',     'LYMPH%,LYM,淋巴细胞比率',          '%',      'number', 20.0000,  50.0000,  4,  1),
(1, '血小板计数',         'PLT,血小板,血小板数',              '10^9/L', 'number', 100.0000, 300.0000, 5,  1),
-- 血糖与血脂
(1, '空腹血糖',           'GLU,血糖,空腹葡萄糖,葡萄糖',       'mmol/L', 'number', 3.9000,   6.1000,   6,  1),
(1, '糖化血红蛋白',       'HbA1c,HbA1C,GHb,糖化血红蛋白A1c',   '%',      'number', 4.0000,   6.0000,   7,  1),
(1, '总胆固醇',           'TC,CHOL,胆固醇',                    'mmol/L', 'number', 2.8000,   5.2000,   8,  1),
(1, '甘油三酯',           'TG,三酰甘油',                       'mmol/L', 'number', 0.4000,   1.7000,   9,  1),
(1, '低密度脂蛋白胆固醇', 'LDL-C,LDL,低密度脂蛋白',            'mmol/L', 'number', 0.0000,   3.4000,   10, 1),
-- 肝肾功能
(1, '丙氨酸氨基转移酶',   'ALT,GPT,谷丙转氨酶',                'U/L',    'number', 9.0000,   50.0000,  11, 1),
(1, '总胆红素',           'TBIL,T-BIL,总胆红素测定',           'umol/L', 'number', 0.0000,   26.0000,  12, 1),
(1, '肌酐',               'Cr,CREA,血肌酐,肌酐测定',           'umol/L', 'number', 44.0000,  106.0000, 13, 1),
(1, '尿酸',               'UA,血尿酸,尿酸测定',                'umol/L', 'number', 208.0000, 428.0000, 14, 1),
-- 其他生化
(1, '乳酸脱氢酶',         'LDH,乳酸脱氢酶测定',                'U/L',    'number', 120.0000, 250.0000, 15, 1),
(1, '碱性磷酸酶',         'ALP,AKP,碱性磷酸酶测定',            'U/L',    'number', 40.0000,  150.0000, 16, 1),
(1, '血钙',               'Ca,总钙,钙',                        'mmol/L', 'number', 2.1000,   2.6000,   17, 1),
-- 炎症标志物
(1, 'C反应蛋白',          'CRP,hs-CRP,超敏C反应蛋白',          'mg/L',   'number', 0.0000,   10.0000,  18, 1),
(1, '血沉',               'ESR,红细胞沉降率,血沉率',           'mm/h',   'number', 0.0000,   15.0000,  19, 1),
(1, '红细胞计数', 'RBC,红细胞,红细胞数', '10^12/L', 'number', NULL, NULL, 20, 1),
(1, '红细胞压积', 'HCT,红细胞比容,压积', 'L/L', 'number', NULL, NULL, 21, 1),
(1, '平均红细胞体积', 'MCV,红细胞平均体积', 'fL', 'number', NULL, NULL, 22, 1),
(1, '平均红细胞血红蛋白量', 'MCH,平均红细胞血红蛋白', 'pg', 'number', NULL, NULL, 23, 1),
(1, '平均红细胞血红蛋白浓度', 'MCHC', 'g/L', 'number', NULL, NULL, 24, 1),
(1, '红细胞分布宽度', 'RDW,红细胞体积分布宽度', '%', 'number', NULL, NULL, 25, 1),
(1, '平均血小板体积', 'MPV,血小板平均体积', 'fL', 'number', NULL, NULL, 26, 1),
(1, '血小板压积', 'PCT,血小板比积', '%', 'number', NULL, NULL, 27, 1),
(1, '中性粒细胞绝对值', 'NEUT#,中性粒细胞计数,中性粒细胞数', '10^9/L', 'number', NULL, NULL, 28, 1),
(1, '淋巴细胞绝对值', 'LYMPH#,淋巴细胞计数,淋巴细胞数', '10^9/L', 'number', NULL, NULL, 29, 1),
(1, '单核细胞绝对值', 'MONO#,单核细胞计数,单核细胞数', '10^9/L', 'number', NULL, NULL, 30, 1),
(1, '嗜酸性粒细胞绝对值', 'EO#,嗜酸细胞计数', '10^9/L', 'number', NULL, NULL, 31, 1),
(1, '嗜碱性粒细胞绝对值', 'BASO#,嗜碱细胞计数', '10^9/L', 'number', NULL, NULL, 32, 1),
(1, '单核细胞百分比', 'MONO%,单核细胞比率', '%', 'number', NULL, NULL, 33, 1),
(1, '嗜酸性粒细胞百分比', 'EO%,嗜酸细胞百分比,嗜酸性粒细胞比率', '%', 'number', NULL, NULL, 34, 1),
(1, '嗜碱性粒细胞百分比', 'BASO%,嗜碱细胞百分比,嗜碱性粒细胞比率', '%', 'number', NULL, NULL, 35, 1);

-- ============================================================
-- ============== 种子数据: 尿常规指标 ========================
-- ============================================================
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`) VALUES
(2, '尿蛋白',  'PRO,URO-PRO,蛋白质',   NULL, 'text', NULL, NULL, 1, 1),
(2, '尿糖',    'GLU-U,尿葡萄糖',       NULL, 'text', NULL, NULL, 2, 1),
(2, '尿隐血',  'BLD,隐血,潜血',        NULL, 'text', NULL, NULL, 3, 1);

-- ============================================================
-- ============== 种子数据: 影像类指标模板 ====================
-- (影像报告以描述性文本为主, 统一用 text 类型)
-- ============================================================
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`) VALUES
(3, '检查部位', '部位,扫描部位', NULL, 'text', NULL, NULL, 1, 1),
(3, '影像描述', '描述,影像所见', NULL, 'text', NULL, NULL, 2, 1),
(3, '检查结论', '结论,诊断意见', NULL, 'text', NULL, NULL, 3, 1),
(4, '检查部位', '部位,扫描部位', NULL, 'text', NULL, NULL, 1, 1),
(4, '影像描述', '描述,影像所见', NULL, 'text', NULL, NULL, 2, 1),
(4, '检查结论', '结论,诊断意见', NULL, 'text', NULL, NULL, 3, 1),
(5, '检查部位', '部位,扫查部位', NULL, 'text', NULL, NULL, 1, 1),
(5, '影像描述', '描述,超声所见', NULL, 'text', NULL, NULL, 2, 1),
(5, '检查结论', '结论,超声提示', NULL, 'text', NULL, NULL, 3, 1);

-- ============================================================
-- ============== 种子数据: 参考区间规则 ======================
-- 只覆盖差异明确的几项, 其余走指标默认值
-- ============================================================
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

-- ============================================================
-- ============== 示例数据 (合成, 开发测试用) =================
-- ============================================================

-- 示例账号: demo / demo1234
INSERT INTO `user` (`id`, `username`, `password_hash`, `nickname`, `gender`, `birth_date`, `notes`) VALUES
(1, 'demo', '$2a$10$4Y8UtJSTpAitmeigyR5uqOxlb4Ss19zTBY8gQzVsGN2E1jxpoXjje', '示例用户', 1, '1985-06-12', '合成示例数据，用于本地开发调试');


-- 记录 1: 年度体检（市第一人民医院）
INSERT INTO `exam_record` (`id`, `user_id`, `category_id`, `exam_date`, `hospital`, `doctor`, `notes`) VALUES
(1, 1, 1, '2026-03-28 09:30:00', '市第一人民医院', '王医生', '年度体检，空腹');

INSERT INTO `exam_value` (`record_id`, `indicator_id`, `value_decimal`, `ref_min`, `ref_max`, `is_abnormal`)
SELECT 1, i.id, v.val, v.rmin, v.rmax, v.abn
FROM `exam_indicator` i
JOIN (SELECT '血红蛋白' nm,   142 val, 120 rmin, 160 rmax, 0 abn
      UNION ALL SELECT '白细胞计数', 6.8, 4, 10, 0
      UNION ALL SELECT '血小板计数', 185, 100, 300, 0
      UNION ALL SELECT '空腹血糖',   6.4, 3.9, 6.1, 1
      UNION ALL SELECT '总胆固醇',   5.6, 2.8, 5.2, 1
      UNION ALL SELECT 'C反应蛋白',  5.2, 0, 10, 0) v ON v.nm = i.name
WHERE i.category_id = 1;

-- 记录 2: 复查（市第二人民医院，参考区间与上一家不同）
INSERT INTO `exam_record` (`id`, `user_id`, `category_id`, `exam_date`, `hospital`, `doctor`, `notes`) VALUES
(2, 1, 1, '2026-05-20 10:00:00', '市第二人民医院', '李医生', '复查');

INSERT INTO `exam_value` (`record_id`, `indicator_id`, `value_decimal`, `ref_min`, `ref_max`, `is_abnormal`)
SELECT 2, i.id, v.val, v.rmin, v.rmax, v.abn
FROM `exam_indicator` i
JOIN (SELECT '血红蛋白' nm,   138 val, 115 rmin, 150 rmax, 0 abn
      UNION ALL SELECT '白细胞计数', 5.4, 3.5, 9.5, 0
      UNION ALL SELECT '血小板计数', 210, 125, 350, 0
      UNION ALL SELECT '空腹血糖',   5.8, 4.0, 6.0, 0
      UNION ALL SELECT '总胆固醇',   5.1, 3.0, 5.7, 0
      UNION ALL SELECT 'C反应蛋白',  3.1, 0, 8, 0) v ON v.nm = i.name
WHERE i.category_id = 1;

-- 记录 3: 影像检查（文本型指标）
INSERT INTO `exam_record` (`id`, `user_id`, `category_id`, `exam_date`, `hospital`, `doctor`, `notes`) VALUES
(3, 1, 3, '2026-06-15 14:00:00', '市第一人民医院', '王医生', '常规复查');

INSERT INTO `exam_value` (`record_id`, `indicator_id`, `value_text`, `is_abnormal`)
SELECT 3, i.id, v.txt, 0
FROM `exam_indicator` i
JOIN (SELECT '检查部位' nm, '胸部' txt
      UNION ALL SELECT '影像描述', '双肺纹理清晰，未见明显异常密度影'
      UNION ALL SELECT '检查结论', '胸部 CT 未见明显异常') v ON v.nm = i.name
WHERE i.category_id = 3;


-- ============================================================
-- ============== 实用查询示例 (开发参考) ===================
-- ============================================================

SET FOREIGN_KEY_CHECKS = 1;

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
