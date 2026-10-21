-- ============================================================
-- 演示数据（全部为合成数据，不含任何真实患者信息）
-- 目的: 让趋势页能演示"同一指标在不同医院/不同时期参考区间不同"
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
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, v.abn
FROM exam_indicator i
JOIN (SELECT '白细胞计数' nm, 6.8 val, 4 rmin, 10 rmax, '4.0-10.0' rtext, 0 abn
      UNION ALL SELECT '血红蛋白', 142, 120, 160, '120-160', 0
      UNION ALL SELECT 'C反应蛋白', 5.2, 0, 10, '0-10', 0) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (user_id, category_id, exam_date, hospital) VALUES (@uid, 1, '2025-10-20 10:00:00', '市第二人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, v.abn
FROM exam_indicator i
JOIN (SELECT '白细胞计数' nm, 5.4 val, 3.5 rmin, 9.5 rmax, '3.5-9.5' rtext, 0 abn
      UNION ALL SELECT '血红蛋白', 128, 115, 150, '115-150', 0
      UNION ALL SELECT 'C反应蛋白', 12.0, 0, 8, '0-8', 1) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (user_id, category_id, exam_date, hospital) VALUES (@uid, 1, '2025-11-24 09:45:00', '市第一人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, v.abn
FROM exam_indicator i
JOIN (SELECT '白细胞计数' nm, 3.1 val, 4 rmin, 10 rmax, '4.0-10.0' rtext, 2 abn
      UNION ALL SELECT '血红蛋白', 116, 120, 160, '120-160', 2
      UNION ALL SELECT 'C反应蛋白', 26.4, 0, 10, '0-10', 1) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (user_id, category_id, exam_date, hospital) VALUES (@uid, 1, '2025-12-22 10:20:00', '市第二人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, v.abn
FROM exam_indicator i
JOIN (SELECT '白细胞计数' nm, 2.8 val, 3.5 rmin, 9.5 rmax, '3.5-9.5' rtext, 2 abn
      UNION ALL SELECT '血红蛋白', 105, 115, 150, '115-150', 2
      UNION ALL SELECT 'C反应蛋白', 18.5, 0, 8, '0-8', 1) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (user_id, category_id, exam_date, hospital) VALUES (@uid, 1, '2026-01-19 09:10:00', '市第一人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, v.abn
FROM exam_indicator i
JOIN (SELECT '白细胞计数' nm, 4.6 val, 4 rmin, 10 rmax, '4.0-10.0' rtext, 0 abn
      UNION ALL SELECT '血红蛋白', 118, 120, 160, '120-160', 2
      UNION ALL SELECT 'C反应蛋白', 8.1, 0, 10, '0-10', 0) v ON v.nm = i.name
WHERE i.category_id = 1;

INSERT INTO exam_record (user_id, category_id, exam_date, hospital) VALUES (@uid, 1, '2026-02-23 09:50:00', '市第一人民医院');
SET @r := LAST_INSERT_ID();
INSERT INTO exam_value (record_id, indicator_id, value_decimal, ref_min, ref_max, ref_text, is_abnormal)
SELECT @r, i.id, v.val, v.rmin, v.rmax, v.rtext, v.abn
FROM exam_indicator i
JOIN (SELECT '白细胞计数' nm, 5.9 val, 4 rmin, 10 rmax, '4.0-10.0' rtext, 0 abn
      UNION ALL SELECT '血红蛋白', 132, 120, 160, '120-160', 0
      UNION ALL SELECT 'C反应蛋白', 4.9, 0, 10, '0-10', 0) v ON v.nm = i.name
WHERE i.category_id = 1;

SELECT '演示数据已写入' AS msg, @uid AS user_id,
       (SELECT COUNT(*) FROM exam_record WHERE user_id = @uid) AS records,
       (SELECT COUNT(*) FROM exam_value v JOIN exam_record r ON v.record_id = r.id WHERE r.user_id = @uid) AS values_cnt;

-- 删除演示数据（按需执行）:
-- DELETE FROM `user` WHERE username = 'demo_trend';
