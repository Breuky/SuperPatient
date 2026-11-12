-- 依据 WS/T 405-2012《血细胞分析参考区间》表 1 补全参考区间规则
-- 来源：国家卫生行业标准，2013-08-01 实施；适用于静脉血仪器检测
-- 说明：成人（>=18 岁）；性别无关项 gender 为 NULL，性别相关项分男女两条

-- 与国标不一致的默认值：先按国标修正（这两项性别无关，标准明确）
UPDATE exam_indicator SET normal_min = 3.5, normal_max = 9.5
WHERE name = '白细胞计数' AND owner_id = 0;

UPDATE exam_indicator SET normal_min = 125, normal_max = 350
WHERE name = '血小板计数' AND owner_id = 0;

-- 血红蛋白的默认值保留 120~160 作为「性别未知时的折中」；
-- 成人男女分别由 reference_range 的规则行给出（已存在 130~175 / 115~150，与国标一致）

-- 补规则：性别无关项
INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, NULL, 18, NULL, v.lo, v.hi, CONCAT(v.lo, '-', v.hi), 'WS/T 405-2012', 10
FROM exam_indicator i
JOIN (
    SELECT '白细胞计数' AS name, 3.5 AS lo, 9.5 AS hi
    UNION ALL SELECT '中性粒细胞百分比', 40, 75
    UNION ALL SELECT '淋巴细胞百分比', 20, 50
    UNION ALL SELECT '嗜酸性粒细胞百分比', 0.4, 8.0
    UNION ALL SELECT '嗜碱性粒细胞百分比', 0, 1
    UNION ALL SELECT '单核细胞百分比', 3, 10
    UNION ALL SELECT '中性粒细胞绝对值', 1.8, 6.3
    UNION ALL SELECT '淋巴细胞绝对值', 1.1, 3.2
    UNION ALL SELECT '嗜酸性粒细胞绝对值', 0.02, 0.52
    UNION ALL SELECT '嗜碱性粒细胞绝对值', 0, 0.06
    UNION ALL SELECT '单核细胞绝对值', 0.1, 0.6
    UNION ALL SELECT '平均红细胞体积', 82, 100
    UNION ALL SELECT '平均红细胞血红蛋白量', 27, 34
    UNION ALL SELECT '平均红细胞血红蛋白浓度', 316, 354
    UNION ALL SELECT '血小板计数', 125, 350
) v ON v.name = i.name
WHERE i.owner_id = 0
  AND NOT EXISTS (
      SELECT 1 FROM reference_range r
      WHERE r.indicator_id = i.id AND r.source = 'WS/T 405-2012'
  );

-- 补规则：性别相关项（红细胞计数、红细胞比容）
INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 1, 18, NULL, 4.3, 5.8, '4.3-5.8', 'WS/T 405-2012', 10
FROM exam_indicator i WHERE i.name = '红细胞计数' AND i.owner_id = 0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id = i.id AND r.gender = 1 AND r.source = 'WS/T 405-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 0, 18, NULL, 3.8, 5.1, '3.8-5.1', 'WS/T 405-2012', 10
FROM exam_indicator i WHERE i.name = '红细胞计数' AND i.owner_id = 0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id = i.id AND r.gender = 0 AND r.source = 'WS/T 405-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 1, 18, NULL, 0.40, 0.50, '0.40-0.50', 'WS/T 405-2012', 10
FROM exam_indicator i WHERE i.name = '红细胞压积' AND i.owner_id = 0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id = i.id AND r.gender = 1 AND r.source = 'WS/T 405-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 0, 18, NULL, 0.35, 0.45, '0.35-0.45', 'WS/T 405-2012', 10
FROM exam_indicator i WHERE i.name = '红细胞压积' AND i.owner_id = 0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id = i.id AND r.gender = 0 AND r.source = 'WS/T 405-2012');
