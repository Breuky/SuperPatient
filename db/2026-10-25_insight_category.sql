-- 题型分类：用于统计「用户到底在问什么、哪类最贵」
ALTER TABLE `insight_log`
    ADD COLUMN `category` VARCHAR(20) DEFAULT 'other' COMMENT 'abnormal/trend/cross_record/value/summary/boundary/other' AFTER `question`,
    ADD KEY `idx_insight_category` (`user_id`, `category`);
