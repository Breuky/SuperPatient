-- ============================================================
-- 指标库分两层：owner_id = 0 为系统公共指标，其他值为该用户的私有指标
-- 已有数据全部落在 0，行为不变；私有指标只对 owner 自己可见、可改
-- ============================================================

ALTER TABLE `exam_indicator`
    ADD COLUMN `owner_id` INT NOT NULL DEFAULT 0 COMMENT '归属: 0=系统公共, 其他=用户ID (私有指标)' AFTER `category_id`;

-- 同一归属下按分类唯一：「检查结论」这类通用名在 CT/MRI/超声 各有一行是正常的
ALTER TABLE `exam_indicator`
    ADD UNIQUE KEY `uk_owner_name` (`owner_id`, `category_id`, `name`);
