INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'unit_note', '总胆红素常用 μmol/L 与 mg/dL：1 mg/dL ≈ 17.1 μmol/L（μmol/L = mg/dL × 17.1）。', 'UNIT-CONV', '单位定义', '2026-11-12'
FROM exam_indicator i WHERE i.name='总胆红素' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.indicator_id=i.id AND k.field='unit_note');

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'unit_note', '尿素常用 mmol/L 与 mg/dL：1 mmol/L ≈ 6 mg/dL（以尿素氮计需另行注明，两种口径不能混用）。', 'UNIT-CONV', '单位定义', '2026-11-12'
FROM exam_indicator i WHERE i.name='尿素' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.indicator_id=i.id AND k.field='unit_note');

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'unit_note', '尿酸常用 μmol/L 与 mg/dL：1 mg/dL ≈ 59.5 μmol/L。', 'UNIT-CONV', '单位定义', '2026-11-12'
FROM exam_indicator i WHERE i.name='尿酸' AND i.owner_id=0
  AND NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.indicator_id=i.id AND k.field='unit_note');

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT NULL, 'unit_note', '电解质结果通常以 mmol/L 报告（钾、钠、氯），一般不做 mg/dL 换算；报告上的单位差异主要来自不同实验室习惯，读数值时以报告单位为准。', 'UNIT-CONV', '单位定义', '2026-11-12'
WHERE NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.source_id='UNIT-CONV' AND k.locator='单位定义' AND k.field='unit_note' AND k.indicator_id IS NULL);
