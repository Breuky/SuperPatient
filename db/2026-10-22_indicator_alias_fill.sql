-- ============================================================
-- 补公共库别名：都是跨医院通用的写法（真实化验单里出现过）
--   C反应蛋白 顺带恢复启用：炎症常用项，血常规单里经常一起出现，
--   高敏/全血两种写法在样本 06、07 中都出现过
-- 「大血小板比率」等指标缺失项不在本脚本内，走校对页「加入我的指标」
-- ============================================================

UPDATE `exam_indicator` SET `aliases` = 'CRP,hs-CRP,超敏C反应蛋白,高敏C反应蛋白,全血C反应蛋白', `status` = 1
WHERE `owner_id` = 0 AND `name` = 'C反应蛋白';

UPDATE `exam_indicator` SET `aliases` = 'MCV,红细胞平均体积,平均RBC体积'
WHERE `owner_id` = 0 AND `name` = '平均红细胞体积';

UPDATE `exam_indicator` SET `aliases` = 'MCH,平均红细胞血红蛋白,平均血红蛋白含量,平均红细胞血红蛋白含量,平均HGB量'
WHERE `owner_id` = 0 AND `name` = '平均红细胞血红蛋白量';

UPDATE `exam_indicator` SET `aliases` = 'MCHC,平均血红蛋白浓度,平均HGB浓度'
WHERE `owner_id` = 0 AND `name` = '平均红细胞血红蛋白浓度';

UPDATE `exam_indicator` SET `aliases` = 'RDW,红细胞体积分布宽度,红细胞分布系数'
WHERE `owner_id` = 0 AND `name` = '红细胞分布宽度';

UPDATE `exam_indicator` SET `aliases` = 'MPV,血小板平均体积,血小板体积'
WHERE `owner_id` = 0 AND `name` = '平均血小板体积';

UPDATE `exam_indicator` SET `aliases` = 'EO#,嗜酸细胞计数,嗜酸性粒细胞绝对数,嗜酸性粒细胞计数'
WHERE `owner_id` = 0 AND `name` = '嗜酸性粒细胞绝对值';

UPDATE `exam_indicator` SET `aliases` = 'BASO#,嗜碱细胞计数,嗜碱性粒细胞绝对数,嗜碱性粒细胞计数'
WHERE `owner_id` = 0 AND `name` = '嗜碱性粒细胞绝对值';

UPDATE `exam_indicator` SET `aliases` = 'PDW,血小板分布宽度,PLT分布宽度'
WHERE `owner_id` = 0 AND `name` = '血小板体积分布宽度';
