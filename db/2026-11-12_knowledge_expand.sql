-- 知识库扩充：先补「可自证」的单位换算，再放「待核对」的定义草稿
INSERT IGNORE INTO knowledge_source (id, title, publisher, doc_no, url, license_note, retrieved_at, status)
VALUES
('UNIT-CONV','法定计量单位换算','国际单位制（SI）定义','—',NULL,
 '换算系数为计量单位定义，不涉及医学判断','2026-11-12',1),
('PROJECT-DRAFT','项目自述条目（待核对）','本项目','—',NULL,
 '由项目整理，尚未逐条核对权威出处；确认来源前不进入检索索引','2026-11-12',1);

-- 单位换算：数学定义，可以直接启用
INSERT IGNORE INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'unit_note', '血糖类结果常用两种单位：mmol/L 与 mg/dL，换算关系为 1 mmol/L = 18 mg/dL（mg/dL = mmol/L × 18）。', 'UNIT-CONV', '单位定义', '2026-11-12'
FROM exam_indicator i WHERE i.name = '空腹血糖' AND i.owner_id = 0;

INSERT IGNORE INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'unit_note', '肌酐常用 μmol/L 与 mg/dL 两种单位，换算关系为 1 mg/dL ≈ 88.4 μmol/L。', 'UNIT-CONV', '单位定义', '2026-11-12'
FROM exam_indicator i WHERE i.name = '肌酐' AND i.owner_id = 0;

INSERT IGNORE INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'unit_note', '胆固醇类结果 1 mmol/L ≈ 38.67 mg/dL；甘油三酯 1 mmol/L ≈ 88.5 mg/dL。两者系数不同，不能混用。', 'UNIT-CONV', '单位定义', '2026-11-12'
FROM exam_indicator i WHERE i.name = '总胆固醇' AND i.owner_id = 0;

INSERT IGNORE INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'unit_note', '血红蛋白常用 g/L 与 g/dL：1 g/dL = 10 g/L。报告写 13.5 g/dL 相当于 135 g/L。', 'UNIT-CONV', '单位定义', '2026-11-12'
FROM exam_indicator i WHERE i.name = '血红蛋白' AND i.owner_id = 0;

-- 定义类草稿：内容按「测量对象 + 单位」写，不含判断与建议；verified_at 为空，核完再启用
INSERT IGNORE INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'definition',
       CONCAT('「', i.name, '」是血液检查中的一个检测项目，结果以数值表示，报告单位见指标库。本条为项目自述，出处待补。'),
       'PROJECT-DRAFT', NULL, NULL
FROM exam_indicator i
WHERE i.owner_id = 0 AND i.status = 1 AND i.value_type = 'number'
  AND i.name IN ('血红蛋白','红细胞计数','白细胞计数','血小板计数','红细胞压积',
                 '平均红细胞体积','平均红细胞血红蛋白量','平均红细胞血红蛋白浓度',
                 '中性粒细胞百分比','淋巴细胞百分比','C反应蛋白','尿酸',
                 '空腹血糖','糖化血红蛋白','总胆固醇','甘油三酯',
                 '低密度脂蛋白胆固醇','高密度脂蛋白胆固醇',
                 '丙氨酸氨基转移酶','天冬氨酸氨基转移酶','总胆红素','肌酐','尿素','白蛋白');
