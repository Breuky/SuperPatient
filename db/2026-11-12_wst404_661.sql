-- 来源分级 + WS/T 404.1 参考区间 + WS/T 661 采血前准备
ALTER TABLE knowledge_source ADD COLUMN origin_level CHAR(1) NULL COMMENT 'A=国标/行标 B=指南共识 C=教材 D=医院说明/项目自述' AFTER publisher;
UPDATE knowledge_source SET origin_level='A' WHERE id IN ('WST405-2012');
UPDATE knowledge_source SET origin_level='A' WHERE id='UNIT-CONV';
UPDATE knowledge_source SET origin_level='D' WHERE id='PROJECT-DRAFT';

INSERT IGNORE INTO knowledge_source (id, title, publisher, origin_level, doc_no, published_at, effective_at, url, license_note, retrieved_at, status)
VALUES
('WST404.1-2012','临床常用生化检验项目参考区间 第1部分：ALT、AST、ALP、GGT','中华人民共和国卫生部','A','WS/T 404.1-2012','2012-12-25','2013-08-01',
 'http://www.nhc.gov.cn/ewebeditor/uploadfile/2013/01/20130109170719547.pdf','卫生行业标准；仅摘录表1数值并标注出处','2026-11-12',1);

INSERT IGNORE INTO knowledge_source (id, title, publisher, origin_level, doc_no, published_at, effective_at, url, license_note, retrieved_at, status)
VALUES
('WST661-2020','静脉血液标本采集指南','中华人民共和国国家卫生健康委员会','A','WS/T 661-2020','2020-03-26','2020-10-01',
 'https://www.nhc.gov.cn/fzs/c100048/202004/b70d1212e11d4c38b6cdc5449594ade7/files/1733125608508_41277.pdf','卫生行业标准；仅摘录第4章条文并标注条款号','2026-11-12',1);

-- 404.1 表1：ALT / AST / ALP / GGT（成人 18 岁起；ALP 女性分年龄段）
INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 1, 18, NULL, 9, 50, '9-50', 'WS/T 404.1-2012', 10 FROM exam_indicator i
WHERE i.name='丙氨酸氨基转移酶' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.source='WS/T 404.1-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 0, 18, NULL, 7, 40, '7-40', 'WS/T 404.1-2012', 10 FROM exam_indicator i
WHERE i.name='丙氨酸氨基转移酶' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.gender=0 AND r.source='WS/T 404.1-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 1, 18, NULL, 15, 40, '15-40', 'WS/T 404.1-2012', 10 FROM exam_indicator i
WHERE i.name='天冬氨酸氨基转移酶' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.source='WS/T 404.1-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 0, 18, NULL, 13, 35, '13-35', 'WS/T 404.1-2012', 10 FROM exam_indicator i
WHERE i.name='天冬氨酸氨基转移酶' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.gender=0 AND r.source='WS/T 404.1-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 1, 18, NULL, 45, 125, '45-125', 'WS/T 404.1-2012', 10 FROM exam_indicator i
WHERE i.name='碱性磷酸酶' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.source='WS/T 404.1-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 0, 20, 49, 35, 100, '35-100（20-49岁）', 'WS/T 404.1-2012', 10 FROM exam_indicator i
WHERE i.name='碱性磷酸酶' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.gender=0 AND r.source='WS/T 404.1-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 0, 50, 79, 50, 135, '50-135（50-79岁）', 'WS/T 404.1-2012', 10 FROM exam_indicator i
WHERE i.name='碱性磷酸酶' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.gender=0 AND r.age_min=50 AND r.source='WS/T 404.1-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 1, 18, NULL, 10, 60, '10-60', 'WS/T 404.1-2012', 10 FROM exam_indicator i
WHERE i.name='γ-谷氨酰转肽酶' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.source='WS/T 404.1-2012');

INSERT INTO reference_range (indicator_id, gender, age_min, age_max, ref_min, ref_max, ref_text, source, priority)
SELECT i.id, 0, 18, NULL, 7, 45, '7-45', 'WS/T 404.1-2012', 10 FROM exam_indicator i
WHERE i.name='γ-谷氨酰转肽酶' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM reference_range r WHERE r.indicator_id=i.id AND r.gender=0 AND r.source='WS/T 404.1-2012');

-- WS/T 661 第4章：采血前准备（通用条目，indicator_id 为空）
INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT NULL, 'prep', '采血前不宜改变饮食习惯，24 小时内不宜饮酒。', 'WST661-2020', '4.1.1', '2026-11-12'
WHERE NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.source_id='WST661-2020' AND k.locator='4.1.1');

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT NULL, 'prep', '需要空腹采血的项目包括（不限于）：糖代谢类（空腹血糖、空腹胰岛素、空腹 C 肽等）、血脂类（总胆固醇、甘油三酯、高密度与低密度脂蛋白胆固醇、载脂蛋白等）、血液流变学、骨代谢标志物、血小板聚集率。', 'WST661-2020', '4.1.2', '2026-11-12'
WHERE NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.source_id='WST661-2020' AND k.locator='4.1.2');

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT NULL, 'prep', '空腹要求至少禁食 8 小时，以 12～14 小时为宜，但不宜超过 16 小时；宜安排在上午 7:00～9:00 采血；空腹期间可少量饮水。', 'WST661-2020', '4.1', '2026-11-12'
WHERE NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.source_id='WST661-2020' AND k.locator='4.1');

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT NULL, 'prep', '采血前 24 小时不宜剧烈运动；采血当天宜避免情绪激动；采血前宜静息至少 5 分钟。', 'WST661-2020', '4.2', '2026-11-12'
WHERE NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.source_id='WST661-2020' AND k.locator='4.2');

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT NULL, 'prep', '部分项目对采血时间有特殊要求，例如促肾上腺皮质激素与皮质醇存在昼夜节律、女性性激素随生理周期变化，采血时间需按医嘱安排。', 'WST661-2020', '4.3', '2026-11-12'
WHERE NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.source_id='WST661-2020' AND k.locator='4.3');
