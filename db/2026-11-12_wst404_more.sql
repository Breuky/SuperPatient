-- WS/T 404.2 / 404.3 / 404.4 / 404.5 / 404.9-2026 参考区间
INSERT IGNORE INTO knowledge_source (id, title, publisher, origin_level, doc_no, published_at, effective_at, url, license_note, retrieved_at, status)
VALUES
('WST404.2-2012','临床常用生化检验项目参考区间 第2部分：血清总蛋白、白蛋白','中华人民共和国卫生部','A','WS/T 404.2-2012','2012-12-25','2013-08-01','https://www.nhc.gov.cn/ewebeditor/uploadfile/2013/01/20130109170840325.pdf','卫生行业标准；仅摘录表1数值','2026-11-12',1),
('WST404.3-2012','临床常用生化检验项目参考区间 第3部分：血清钾、钠、氯','中华人民共和国卫生部','A','WS/T 404.3-2012','2012-12-25','2013-08-01','https://www.nhc.gov.cn/ewebeditor/uploadfile/2013/01/20130109170954741.pdf','卫生行业标准；仅摘录表1数值','2026-11-12',1),
('WST404.4-2012','临床常用生化检验项目参考区间 第4部分：血清总胆红素、直接胆红素','中华人民共和国卫生部','A','WS/T 404.4-2012','2018-05-01','2018-11-01','https://www.nhc.gov.cn/wjw/s9492/201805/67aa364588f74ec69a1de85f893ed2c4/files/1739781483928_13416.pdf','卫生行业标准；仅摘录表1数值','2026-11-12',1),
('WST404.5-2012','临床常用生化检验项目参考区间 第5部分：血清尿素、肌酐','中华人民共和国卫生部','A','WS/T 404.5-2012','2015-05-01','2015-11-01','https://www.nhc.gov.cn/ewebeditor/uploadfile/2015/05/20150504151554530.pdf','卫生行业标准；仅摘录表1数值','2026-11-12',1),
('WST404.9-2026','临床常用生化检验项目参考区间 第9部分：血清C-反应蛋白、前白蛋白、转铁蛋白、β2-微球蛋白','中华人民共和国国家卫生健康委员会','A','WS/T 404.9-2026','2026-05-25','2026-11-01','https://www.nhc.gov.cn/wjw/s9492/202606/7659f697fc954a188cf7f53c62501cf6/files/WST 404.9—2026-20260612152247997.pdf','卫生行业标准；仅摘录表1数值','2026-11-12',1);

-- 404.2 总蛋白/白蛋白/球蛋白（男女同）
INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, NULL, 18, NULL, 65, 85, '65-85', 'WS/T 404.2-2012', 10 FROM exam_indicator i
WHERE i.name='总蛋白' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.source='WS/T 404.2-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, NULL, 18, NULL, 40, 55, '40-55', 'WS/T 404.2-2012', 10 FROM exam_indicator i
WHERE i.name='白蛋白' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.source='WS/T 404.2-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, NULL, 18, NULL, 20, 40, '20-40', 'WS/T 404.2-2012', 10 FROM exam_indicator i
WHERE i.name='球蛋白' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.source='WS/T 404.2-2012');

-- 404.3 钾钠氯
INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, NULL, 18, NULL, 3.5, 5.3, '3.5-5.3', 'WS/T 404.3-2012', 10 FROM exam_indicator i
WHERE i.name='钾' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.source='WS/T 404.3-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, NULL, 18, NULL, 137, 147, '137-147', 'WS/T 404.3-2012', 10 FROM exam_indicator i
WHERE i.name='钠' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.source='WS/T 404.3-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, NULL, 18, NULL, 99, 110, '99-110', 'WS/T 404.3-2012', 10 FROM exam_indicator i
WHERE i.name='氯' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.source='WS/T 404.3-2012');

-- 404.4 总胆红素（单侧上限；直接胆红素因分析系统不同暂不录入）
INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 1, 20, 79, NULL, 26.0, '≤26.0', 'WS/T 404.4-2012', 10 FROM exam_indicator i
WHERE i.name='总胆红素' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.source='WS/T 404.4-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 0, 20, 79, NULL, 21.0, '≤21.0', 'WS/T 404.4-2012', 10 FROM exam_indicator i
WHERE i.name='总胆红素' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.gender=0 AND r.source='WS/T 404.4-2012');

-- 404.5 尿素 / 肌酐（性别 × 年龄段）
INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, g.gender, g.age_min, g.age_max, g.lo, g.hi, g.txt, 'WS/T 404.5-2012', 10
FROM exam_indicator i
JOIN (SELECT 1 AS gender, 20 AS age_min, 59 AS age_max, 3.1 AS lo, 8.0 AS hi, '3.1-8.0（男 20-59岁）' AS txt
      UNION ALL SELECT 1, 60, 79, 3.6, 9.5, '3.6-9.5（男 60-79岁）'
      UNION ALL SELECT 0, 20, 59, 2.6, 7.5, '2.6-7.5（女 20-59岁）'
      UNION ALL SELECT 0, 60, 79, 3.1, 8.8, '3.1-8.8（女 60-79岁）') g
WHERE i.name='尿素' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.gender=g.gender AND r.age_min=g.age_min AND r.source='WS/T 404.5-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, g.gender, g.age_min, g.age_max, g.lo, g.hi, g.txt, 'WS/T 404.5-2012', 10
FROM exam_indicator i
JOIN (SELECT 1 AS gender, 20 AS age_min, 59 AS age_max, 57 AS lo, 97 AS hi, '57-97（男 20-59岁）' AS txt
      UNION ALL SELECT 1, 60, 79, 57, 111, '57-111（男 60-79岁）'
      UNION ALL SELECT 0, 20, 59, 41, 73, '41-73（女 20-59岁）'
      UNION ALL SELECT 0, 60, 79, 41, 81, '41-81（女 60-79岁）') g
WHERE i.name='肌酐' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.gender=g.gender AND r.age_min=g.age_min AND r.source='WS/T 404.5-2012');

-- 404.9-2026 C 反应蛋白（单侧上限）
INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, NULL, 18, 79, NULL, 6.0, '≤6.0', 'WS/T 404.9-2026', 10 FROM exam_indicator i
WHERE i.name='C反应蛋白' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.source='WS/T 404.9-2026');
