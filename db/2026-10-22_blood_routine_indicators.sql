-- ============================================================
-- 迁移: 补全血常规指标
-- 背景: 真实化验单的血常规有 20+ 项，指标库原来只有 5 项，
--       评测时大量行落进 unmatched。这里补齐常见项。
-- 说明: normal_min/max 留空 —— 区间多为性别/年龄相关，走 reference_range 或报告自带区间
-- 幂等: 已存在同名指标则跳过
-- ============================================================

SET NAMES utf8mb4;

INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '红细胞计数', 'RBC,红细胞,红细胞数', '10^12/L', 'number', NULL, NULL, 20, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '红细胞计数' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '红细胞压积', 'HCT,红细胞比容,压积', 'L/L', 'number', NULL, NULL, 21, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '红细胞压积' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '平均红细胞体积', 'MCV,红细胞平均体积', 'fL', 'number', NULL, NULL, 22, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '平均红细胞体积' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '平均红细胞血红蛋白量', 'MCH,平均红细胞血红蛋白', 'pg', 'number', NULL, NULL, 23, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '平均红细胞血红蛋白量' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '平均红细胞血红蛋白浓度', 'MCHC', 'g/L', 'number', NULL, NULL, 24, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '平均红细胞血红蛋白浓度' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '红细胞分布宽度', 'RDW,红细胞体积分布宽度', '%', 'number', NULL, NULL, 25, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '红细胞分布宽度' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '平均血小板体积', 'MPV,血小板平均体积', 'fL', 'number', NULL, NULL, 26, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '平均血小板体积' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '血小板压积', 'PCT,血小板比积', '%', 'number', NULL, NULL, 27, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '血小板压积' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '中性粒细胞绝对值', 'NEUT#,中性粒细胞计数,中性粒细胞数', '10^9/L', 'number', NULL, NULL, 28, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '中性粒细胞绝对值' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '淋巴细胞绝对值', 'LYMPH#,淋巴细胞计数,淋巴细胞数', '10^9/L', 'number', NULL, NULL, 29, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '淋巴细胞绝对值' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '单核细胞绝对值', 'MONO#,单核细胞计数,单核细胞数', '10^9/L', 'number', NULL, NULL, 30, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '单核细胞绝对值' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '嗜酸性粒细胞绝对值', 'EO#,嗜酸细胞计数', '10^9/L', 'number', NULL, NULL, 31, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '嗜酸性粒细胞绝对值' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '嗜碱性粒细胞绝对值', 'BASO#,嗜碱细胞计数', '10^9/L', 'number', NULL, NULL, 32, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '嗜碱性粒细胞绝对值' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '单核细胞百分比', 'MONO%,单核细胞比率', '%', 'number', NULL, NULL, 33, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '单核细胞百分比' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '嗜酸性粒细胞百分比', 'EO%,嗜酸细胞百分比,嗜酸性粒细胞比率', '%', 'number', NULL, NULL, 34, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '嗜酸性粒细胞百分比' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '嗜碱性粒细胞百分比', 'BASO%,嗜碱细胞百分比,嗜碱性粒细胞比率', '%', 'number', NULL, NULL, 35, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '嗜碱性粒细胞百分比' AND `category_id` = 1);

SELECT COUNT(*) AS blood_indicators FROM `exam_indicator` WHERE category_id = 1;
