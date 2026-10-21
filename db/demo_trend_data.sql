-- ============================================================
-- 演示数据（全部为合成数据，不含任何真实个人健康信息）
-- 目的: 演示"同一指标在不同医院/不同时期参考区间不同"，以及抽取溯源字段
-- 幂等: 重复执行会先清空该演示账号的记录再重建
-- 删除: 见文件末尾
-- ============================================================

SET NAMES utf8mb4;

-- 演示账号: demo_trend / demo1234
INSERT INTO `user` (username, password_hash, nickname, gender, birth_date, notes)
VALUES ('demo_trend', '$2a$10$4Y8UtJSTpAitmeigyR5uqOxlb4Ss19zTBY8gQzVsGN2E1jxpoXjje', '演示用户', 1, '1980-04-18', '合成示例数据，用于演示趋势与参考区间变化')
ON DUPLICATE KEY UPDATE nickname = '演示用户';
SET @uid := (SELECT id FROM `user` WHERE username = 'demo_trend');

DELETE FROM exam_record WHERE user_id = @uid;

INSERT INTO exam_record (user_id, category_id, exam_date, hospital) VALUES (@uid, 1, '2025-09-15 09:30:00', '市第一人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, ref_source, raw_name, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, 'report', v.raw, v.abn
FROM exam_indicator i
JOIN (      SELECT '白细胞计数' nm, 'WBC' raw, 6.8 val, 4 rmin, 10 rmax, '4.0-10.0' rtext, 0 abn
      UNION ALL SELECT '血红蛋白' nm, 'HGB' raw, 142 val, 120 rmin, 160 rmax, '120-160' rtext, 0 abn
      UNION ALL SELECT 'C反应蛋白' nm, 'CRP' raw, 5.2 val, 0 rmin, 10 rmax, '0-10' rtext, 0 abn) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (user_id, category_id, exam_date, hospital) VALUES (@uid, 1, '2025-10-20 10:00:00', '市第二人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, ref_source, raw_name, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, 'report', v.raw, v.abn
FROM exam_indicator i
JOIN (      SELECT '白细胞计数' nm, 'WBC' raw, 5.4 val, 3.5 rmin, 9.5 rmax, '3.5-9.5' rtext, 0 abn
      UNION ALL SELECT '血红蛋白' nm, 'Hb' raw, 128 val, 115 rmin, 150 rmax, '115-150' rtext, 0 abn
      UNION ALL SELECT 'C反应蛋白' nm, 'hs-CRP' raw, 12 val, 0 rmin, 8 rmax, '0-8' rtext, 1 abn) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (user_id, category_id, exam_date, hospital) VALUES (@uid, 1, '2025-11-24 09:45:00', '市第一人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, ref_source, raw_name, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, 'report', v.raw, v.abn
FROM exam_indicator i
JOIN (      SELECT '白细胞计数' nm, '白细胞' raw, 3.1 val, 4 rmin, 10 rmax, '4.0-10.0' rtext, 2 abn
      UNION ALL SELECT '血红蛋白' nm, 'HGB' raw, 116 val, 120 rmin, 160 rmax, '120-160' rtext, 2 abn
      UNION ALL SELECT 'C反应蛋白' nm, 'CRP' raw, 26.4 val, 0 rmin, 10 rmax, '0-10' rtext, 1 abn) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (user_id, category_id, exam_date, hospital) VALUES (@uid, 1, '2025-12-22 10:20:00', '市第二人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, ref_source, raw_name, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, 'report', v.raw, v.abn
FROM exam_indicator i
JOIN (      SELECT '白细胞计数' nm, 'WBC' raw, 2.8 val, 3.5 rmin, 9.5 rmax, '3.5-9.5' rtext, 2 abn
      UNION ALL SELECT '血红蛋白' nm, 'Hb' raw, 105 val, 115 rmin, 150 rmax, '115-150' rtext, 2 abn
      UNION ALL SELECT 'C反应蛋白' nm, 'hs-CRP' raw, 18.5 val, 0 rmin, 8 rmax, '0-8' rtext, 1 abn) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (user_id, category_id, exam_date, hospital) VALUES (@uid, 1, '2026-01-19 09:10:00', '市第一人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, ref_source, raw_name, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, 'report', v.raw, v.abn
FROM exam_indicator i
JOIN (      SELECT '白细胞计数' nm, 'WBC' raw, 4.6 val, 4 rmin, 10 rmax, '4.0-10.0' rtext, 0 abn
      UNION ALL SELECT '血红蛋白' nm, 'HGB' raw, 118 val, 120 rmin, 160 rmax, '120-160' rtext, 2 abn
      UNION ALL SELECT 'C反应蛋白' nm, 'CRP' raw, 8.1 val, 0 rmin, 10 rmax, '0-10' rtext, 0 abn) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (user_id, category_id, exam_date, hospital) VALUES (@uid, 1, '2026-02-23 09:50:00', '市第一人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, ref_source, raw_name, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, 'report', v.raw, v.abn
FROM exam_indicator i
JOIN (      SELECT '白细胞计数' nm, 'WBC' raw, 5.9 val, 4 rmin, 10 rmax, '4.0-10.0' rtext, 0 abn
      UNION ALL SELECT '血红蛋白' nm, 'HGB' raw, 132 val, 120 rmin, 160 rmax, '120-160' rtext, 0 abn
      UNION ALL SELECT 'C反应蛋白' nm, 'CRP' raw, 4.9 val, 0 rmin, 10 rmax, '0-10' rtext, 0 abn) v ON v.nm = i.name
WHERE i.category_id = 1;

SELECT '演示数据已写入' AS msg, @uid AS user_id,
       (SELECT COUNT(*) FROM exam_record WHERE user_id = @uid) AS records,
       (SELECT COUNT(*) FROM exam_value v JOIN exam_record r ON v.record_id = r.id WHERE r.user_id = @uid) AS values_cnt;

-- 删除演示数据（按需执行）:
-- DELETE FROM `user` WHERE username = 'demo_trend';
