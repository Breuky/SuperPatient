-- B 级来源：中国慢性肾脏病早期评价与管理指南（中华内科杂志 2023;62(8):902-930）
INSERT IGNORE INTO knowledge_source (id, title, publisher, origin_level, doc_no, published_at, url, license_note, retrieved_at, status)
VALUES ('GUIDE-CKD-2023','中国慢性肾脏病早期评价与管理指南','中华预防医学会肾脏病预防与控制专业委员会','B','中华内科杂志 2023;62(8):902-930','2023-08-01',
 'http://mp.weixin.qq.com/s?__biz=MzI4MzAxOTMwMQ==&mid=2650114501&idx=6&sn=3b2b70e2619c626e188dd878f1ebd28e',
 '期刊原文的推荐意见与图表转载；仅摘录表2、表3 的分期定义','2026-11-12',1);

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT NULL, 'definition', '慢性肾脏病按肾小球滤过率（eGFR）分为 G1（≥90）、G2（60~89）、G3a（45~59）、G3b（30~44）、G4（15~29）、G5（肾衰竭）期，单位 ml·min⁻¹·1.73m⁻²。', 'GUIDE-CKD-2023', '表2', '2026-11-12'
WHERE NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.source_id='GUIDE-CKD-2023' AND k.locator='表2');

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT NULL, 'definition', '尿白蛋白/肌酐比值（UACR）用于分级：A1 为正常至轻度增加，A2 为 30~300 mg/g（中度增加），A3 为 >300 mg/g（显著增加）。', 'GUIDE-CKD-2023', '表3', '2026-11-12'
WHERE NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.source_id='GUIDE-CKD-2023' AND k.locator='表3');

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'definition', '血肌酐是估算肾小球滤过率（eGFR）的基础指标之一，单独看肌酐难以反映早期肾功能变化，临床上常结合 eGFR 与尿白蛋白分级评估。', 'GUIDE-CKD-2023', '表2、表3', '2026-11-12'
FROM exam_indicator i WHERE i.name='肌酐' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.indicator_id=i.id AND k.source_id='GUIDE-CKD-2023');
