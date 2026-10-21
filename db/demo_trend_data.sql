-- ============================================================
-- 演示数据（全部为合成数据，不含任何真实患者信息）
-- 目的: 让趋势页能演示"同一指标在不同医院/不同时期参考区间不同"
-- 幂等: 重复执行会先清空该演示患者的记录再重建
-- 删除: 见文件末尾
-- ============================================================

SET NAMES utf8mb4;

-- 演示账号: demo_trend / demo1234
INSERT INTO `user` (username, password_hash, nickname)
VALUES ('demo_trend', '$2a$10$4Y8UtJSTpAitmeigyR5uqOxlb4Ss19zTBY8gQzVsGN2E1jxpoXjje', '演示数据')
ON DUPLICATE KEY UPDATE nickname = '演示数据';
SET @uid := (SELECT id FROM `user` WHERE username = 'demo_trend');

INSERT INTO patient (user_id, name, gender, birth_date, diagnosis_date, notes)
SELECT @uid, '演示患者（合成数据）', 1, '2010-05-12', '2025-03-15',
       '合成示例数据，用于演示趋势与参考区间变化'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM patient WHERE user_id = @uid);
SET @pid := (SELECT id FROM patient WHERE user_id = @uid ORDER BY id LIMIT 1);

DELETE FROM exam_record WHERE patient_id = @pid;

INSERT INTO exam_record (patient_id, category_id, exam_date, hospital) VALUES (@pid, 1, '2025-09-15 09:30:00', '市第一人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, v.abn
FROM exam_indicator i
JOIN (      SELECT '白细胞计数' nm, 6.8 val, 4 rmin, 10 rmax, '4.0-10.0' rtext, 0 abn
      UNION ALL SELECT '血红蛋白' nm, 142 val, 120 rmin, 160 rmax, '120-160' rtext, 0 abn
      UNION ALL SELECT 'C反应蛋白' nm, 5.2 val, 0 rmin, 10 rmax, '0-10' rtext, 0 abn) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (patient_id, category_id, exam_date, hospital) VALUES (@pid, 1, '2025-10-20 10:00:00', '浙江省人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, v.abn
FROM exam_indicator i
JOIN (      SELECT '白细胞计数' nm, 5.4 val, 3.5 rmin, 9.5 rmax, '3.5-9.5' rtext, 0 abn
      UNION ALL SELECT '血红蛋白' nm, 128 val, 115 rmin, 150 rmax, '115-150' rtext, 0 abn
      UNION ALL SELECT 'C反应蛋白' nm, 12 val, 0 rmin, 8 rmax, '0-8' rtext, 1 abn) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (patient_id, category_id, exam_date, hospital) VALUES (@pid, 1, '2025-11-24 09:45:00', '市第一人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, v.abn
FROM exam_indicator i
JOIN (      SELECT '白细胞计数' nm, 3.1 val, 4 rmin, 10 rmax, '4.0-10.0' rtext, 2 abn
      UNION ALL SELECT '血红蛋白' nm, 116 val, 120 rmin, 160 rmax, '120-160' rtext, 2 abn
      UNION ALL SELECT 'C反应蛋白' nm, 26.4 val, 0 rmin, 10 rmax, '0-10' rtext, 1 abn) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (patient_id, category_id, exam_date, hospital) VALUES (@pid, 1, '2025-12-22 10:20:00', '浙江省人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, v.abn
FROM exam_indicator i
JOIN (      SELECT '白细胞计数' nm, 2.8 val, 3.5 rmin, 9.5 rmax, '3.5-9.5' rtext, 2 abn
      UNION ALL SELECT '血红蛋白' nm, 105 val, 115 rmin, 150 rmax, '115-150' rtext, 2 abn
      UNION ALL SELECT 'C反应蛋白' nm, 18.5 val, 0 rmin, 8 rmax, '0-8' rtext, 1 abn) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (patient_id, category_id, exam_date, hospital) VALUES (@pid, 1, '2026-01-19 09:10:00', '市第一人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, v.abn
FROM exam_indicator i
JOIN (      SELECT '白细胞计数' nm, 4.6 val, 4 rmin, 10 rmax, '4.0-10.0' rtext, 0 abn
      UNION ALL SELECT '血红蛋白' nm, 118 val, 120 rmin, 160 rmax, '120-160' rtext, 2 abn
      UNION ALL SELECT 'C反应蛋白' nm, 8.1 val, 0 rmin, 10 rmax, '0-10' rtext, 0 abn) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (patient_id, category_id, exam_date, hospital) VALUES (@pid, 1, '2026-02-23 09:50:00', '市第一人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, v.abn
FROM exam_indicator i
JOIN (      SELECT '白细胞计数' nm, 5.9 val, 4 rmin, 10 rmax, '4.0-10.0' rtext, 0 abn
      UNION ALL SELECT '血红蛋白' nm, 132 val, 120 rmin, 160 rmax, '120-160' rtext, 0 abn
      UNION ALL SELECT 'C反应蛋白' nm, 4.9 val, 0 rmin, 10 rmax, '0-10' rtext, 0 abn) v ON v.nm = i.name
WHERE i.category_id = 1;

SELECT '演示数据已写入' AS msg, @pid AS patient_id,
       (SELECT COUNT(*) FROM exam_record WHERE patient_id = @pid) AS records,
       (SELECT COUNT(*) FROM exam_value v JOIN exam_record r ON v.record_id = r.id WHERE r.patient_id = @pid) AS values_cnt;

-- 删除演示数据（按需执行）:
-- DELETE FROM `user` WHERE username = 'demo_trend';  -- 级联删除 patient / record / value
