-- B 级来源：中国血脂管理指南（2023 年），中华医学会心血管病学分会等联合发布
INSERT IGNORE INTO knowledge_source (id, title, publisher, origin_level, doc_no, published_at, url, license_note, retrieved_at, status)
VALUES ('GUIDE-LIPID-2023','中国血脂管理指南（2023 年）','中国血脂管理指南修订联合专家委员会','B',NULL,'2023-03-01',
 'https://csc.cma.org.cn/attach/0/96b4e603cee74b7fb208b278aa92bd8b.pdf',
 '指南全文公开；仅摘录要点提示与条文并标注章节，不转载原文','2026-11-12',1);

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT NULL, 'why_vary', '血脂的“合适水平”随动脉粥样硬化性心血管疾病（ASCVD）危险分层不同而异：在没有危险因素的人群中可视为“正常”的低密度脂蛋白胆固醇水平，对高危或极高危患者而言可能已属明显升高。因此指南以危险分层确定目标值，而不是用单一参考区间判断。', 'GUIDE-LIPID-2023', '摘要', '2026-11-12'
WHERE NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.source_id='GUIDE-LIPID-2023' AND k.locator='摘要');

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'definition', '低密度脂蛋白胆固醇（LDL-C）是动脉粥样硬化性心血管疾病的致病性危险因素，也是血脂干预的首要靶点。', 'GUIDE-LIPID-2023', '摘要', '2026-11-12'
FROM exam_indicator i WHERE i.name='低密度脂蛋白胆固醇' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.indicator_id=i.id AND k.source_id='GUIDE-LIPID-2023');

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'definition', '与临床密切相关的血脂成分主要包括胆固醇和甘油三酯；胆固醇又分为低密度脂蛋白胆固醇、高密度脂蛋白胆固醇等，不同成分的临床意义不同。', 'GUIDE-LIPID-2023', '二、血脂与脂蛋白', '2026-11-12'
FROM exam_indicator i WHERE i.name='总胆固醇' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.indicator_id=i.id AND k.source_id='GUIDE-LIPID-2023');

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'prep', '血脂检测通常要求空腹采血（空腹约 12 小时），因为餐后乳糜微粒会使血清外观混浊并影响甘油三酯的测定。', 'GUIDE-LIPID-2023', '二、血脂与脂蛋白', '2026-11-12'
FROM exam_indicator i WHERE i.name='甘油三酯' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.indicator_id=i.id AND k.source_id='GUIDE-LIPID-2023');
