-- 迁移: 补全生化面板 + 修正别名
SET NAMES utf8mb4;

-- 1) 更新已有指标的别名
UPDATE `exam_indicator` SET `aliases` = 'MCH,平均红细胞血红蛋白,平均血红蛋白含量' WHERE `name` = '平均红细胞血红蛋白量' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'MCHC,平均血红蛋白浓度' WHERE `name` = '平均红细胞血红蛋白浓度' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'NEUT%,NEUT,中性粒细胞比率,中性粒细胞占比,中性粒细胞%,中性粒%' WHERE `name` = '中性粒细胞百分比' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'LYMPH%,LYM,淋巴细胞比率,淋巴细胞占比,淋巴细胞%' WHERE `name` = '淋巴细胞百分比' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'MONO%,单核细胞比率,单核细胞%' WHERE `name` = '单核细胞百分比' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'EO%,嗜酸细胞百分比,嗜酸性粒细胞比率,嗜酸性粒细胞%,嗜酸细胞%' WHERE `name` = '嗜酸性粒细胞百分比' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'BASO%,嗜碱细胞百分比,嗜碱性粒细胞比率,嗜碱性粒细胞%' WHERE `name` = '嗜碱性粒细胞百分比' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'NEUT#,中性粒细胞计数,中性粒细胞数,中性粒细胞绝对数' WHERE `name` = '中性粒细胞绝对值' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'LYMPH#,淋巴细胞计数,淋巴细胞数,淋巴细胞绝对数' WHERE `name` = '淋巴细胞绝对值' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'MONO#,单核细胞计数,单核细胞数,单核细胞绝对数' WHERE `name` = '单核细胞绝对值' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'EO#,嗜酸细胞计数,嗜酸性粒细胞绝对数' WHERE `name` = '嗜酸性粒细胞绝对值' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'BASO#,嗜碱细胞计数,嗜碱性粒细胞绝对数' WHERE `name` = '嗜碱性粒细胞绝对值' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'HCT,红细胞比容,压积,血细胞比容' WHERE `name` = '红细胞压积' AND `category_id` = 1;
UPDATE `exam_indicator` SET `aliases` = 'PCT,血小板比积,血小板比容' WHERE `name` = '血小板压积' AND `category_id` = 1;

-- 2) 新增生化指标
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '总蛋白', 'TP,总蛋白测定', 'g/L', 'number', NULL, NULL, 36, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '总蛋白' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '白蛋白', 'ALB,清蛋白', 'g/L', 'number', NULL, NULL, 37, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '白蛋白' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '球蛋白', 'GLB,球蛋白测定', 'g/L', 'number', NULL, NULL, 38, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '球蛋白' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '白蛋白球蛋白比值', 'A/G,白球比,白蛋白/球蛋白比值', NULL, 'number', NULL, NULL, 39, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '白蛋白球蛋白比值' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '直接胆红素', 'DBIL,D-BIL,结合胆红素', 'umol/L', 'number', NULL, NULL, 40, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '直接胆红素' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '间接胆红素', 'IBIL,I-BIL,非结合胆红素', 'umol/L', 'number', NULL, NULL, 41, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '间接胆红素' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '天冬氨酸氨基转移酶', 'AST,GOT,谷草转氨酶', 'U/L', 'number', NULL, NULL, 42, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '天冬氨酸氨基转移酶' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, 'γ-谷氨酰转肽酶', 'GGT,γ-GT,谷氨酰转肽酶,r-GT', 'U/L', 'number', NULL, NULL, 43, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = 'γ-谷氨酰转肽酶' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '尿素', 'UREA,尿素氮,BUN', 'mmol/L', 'number', NULL, NULL, 44, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '尿素' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '钾', 'K,血钾', 'mmol/L', 'number', NULL, NULL, 45, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '钾' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '钠', 'Na,血钠', 'mmol/L', 'number', NULL, NULL, 46, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '钠' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '氯', 'Cl,血氯', 'mmol/L', 'number', NULL, NULL, 47, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '氯' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '磷', 'P,血磷,无机磷', 'mmol/L', 'number', NULL, NULL, 48, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '磷' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '镁', 'Mg,血镁', 'mmol/L', 'number', NULL, NULL, 49, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '镁' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '高密度脂蛋白胆固醇', 'HDL-C,HDL,高密度脂蛋白', 'mmol/L', 'number', NULL, NULL, 50, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '高密度脂蛋白胆固醇' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '载脂蛋白A1', 'ApoA1,APOA1,载脂蛋白A', 'g/L', 'number', NULL, NULL, 51, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '载脂蛋白A1' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '载脂蛋白B', 'ApoB,APOB', 'g/L', 'number', NULL, NULL, 52, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '载脂蛋白B' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '肌酸激酶', 'CK,CPK,肌酸磷酸激酶', 'U/L', 'number', NULL, NULL, 53, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '肌酸激酶' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '肌酸激酶同工酶', 'CK-MB,CKMB,肌酸激酶MB同工酶', 'U/L', 'number', NULL, NULL, 54, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '肌酸激酶同工酶' AND `category_id` = 1);
INSERT INTO `exam_indicator` (`category_id`, `name`, `aliases`, `unit`, `value_type`, `normal_min`, `normal_max`, `sort_order`, `status`)
SELECT 1, '血小板体积分布宽度', 'PDW,血小板分布宽度', '%', 'number', NULL, NULL, 55, 1
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM `exam_indicator` WHERE `name` = '血小板体积分布宽度' AND `category_id` = 1);

SELECT COUNT(*) AS blood_indicators FROM `exam_indicator` WHERE category_id = 1;