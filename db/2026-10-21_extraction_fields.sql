-- ============================================================
-- 迁移: 抽取管线所需字段
-- 1) 参考区间下沉到"本次报告": exam_value 存快照, indicator 只保留默认值
-- 2) 保留模型抽取的原文 (raw_name/raw_unit), 便于回溯与准确率评测
-- 3) 指标别名, 用于抽取结果归一
-- 幂等: 列已存在则跳过, 别名可重复覆盖
-- ============================================================

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS sp_add_column;
DELIMITER $$
CREATE PROCEDURE sp_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_ddl TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_ddl);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL sp_add_column('exam_value', 'ref_min',
    '`ref_min` DECIMAL(12,4) DEFAULT NULL COMMENT ''本次报告印刷的参考下限'' AFTER `value_text`');
CALL sp_add_column('exam_value', 'ref_max',
    '`ref_max` DECIMAL(12,4) DEFAULT NULL COMMENT ''本次报告印刷的参考上限'' AFTER `ref_min`');
CALL sp_add_column('exam_value', 'ref_text',
    '`ref_text` VARCHAR(100) DEFAULT NULL COMMENT ''参考区间原文 (如 4.0-10.0 / 阴性)'' AFTER `ref_max`');
CALL sp_add_column('exam_value', 'raw_name',
    '`raw_name` VARCHAR(100) DEFAULT NULL COMMENT ''报告上的原始指标名 (抽取溯源)'' AFTER `ref_text`');
CALL sp_add_column('exam_value', 'raw_unit',
    '`raw_unit` VARCHAR(30) DEFAULT NULL COMMENT ''报告上的原始单位 (抽取溯源)'' AFTER `raw_name`');
CALL sp_add_column('exam_indicator', 'aliases',
    '`aliases` VARCHAR(255) DEFAULT NULL COMMENT ''别名, 逗号分隔 (用于抽取归一)'' AFTER `name`');

DROP PROCEDURE sp_add_column;

-- ---------------- 别名种子数据 ----------------
UPDATE `exam_indicator` SET `aliases` = 'HGB,Hb,HB,血红蛋白浓度'                    WHERE `name` = '血红蛋白';
UPDATE `exam_indicator` SET `aliases` = 'WBC,白细胞,白细胞数,LEU'                  WHERE `name` = '白细胞计数';
UPDATE `exam_indicator` SET `aliases` = 'NEUT%,NEUT,中性粒细胞比率,中性粒细胞占比' WHERE `name` = '中性粒细胞百分比';
UPDATE `exam_indicator` SET `aliases` = 'LYMPH%,LYM,淋巴细胞比率,淋巴细胞占比'     WHERE `name` = '淋巴细胞百分比';
UPDATE `exam_indicator` SET `aliases` = 'PLT,血小板,血小板数'                     WHERE `name` = '血小板计数';
UPDATE `exam_indicator` SET `aliases` = 'LDH,乳酸脱氢酶测定'                       WHERE `name` = '乳酸脱氢酶';
UPDATE `exam_indicator` SET `aliases` = 'ALP,AKP,碱性磷酸酶测定'                   WHERE `name` = '碱性磷酸酶';
UPDATE `exam_indicator` SET `aliases` = 'Ca,总钙,钙'                              WHERE `name` = '血钙';
UPDATE `exam_indicator` SET `aliases` = 'CRP,hs-CRP,超敏C反应蛋白,超敏C-反应蛋白'  WHERE `name` = 'C反应蛋白';
UPDATE `exam_indicator` SET `aliases` = 'ESR,红细胞沉降率,血沉率'                  WHERE `name` = '血沉';
UPDATE `exam_indicator` SET `aliases` = 'cTnI,cTnT,TnI,肌钙蛋白I,肌钙蛋白T'        WHERE `name` = '肌钙蛋白';
UPDATE `exam_indicator` SET `aliases` = 'DBIL,D-BIL,结合胆红素'                    WHERE `name` = '直接胆红素';
UPDATE `exam_indicator` SET `aliases` = 'IBIL,I-BIL,非结合胆红素'                  WHERE `name` = '间接胆红素';
UPDATE `exam_indicator` SET `aliases` = 'TBIL,T-BIL,总胆红素测定'                  WHERE `name` = '总胆红素';
UPDATE `exam_indicator` SET `aliases` = 'UA,血尿酸,尿酸测定'                       WHERE `name` = '尿酸';
UPDATE `exam_indicator` SET `aliases` = 'GLU,血糖,空腹血糖,葡萄糖测定'             WHERE `name` = '葡萄糖';
