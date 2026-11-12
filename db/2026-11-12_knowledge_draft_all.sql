-- 给全部公共指标补定义草稿（含未启用的），统一标记为待核对
INSERT IGNORE INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'definition',
       CONCAT('「', i.name, '」是', c.name, '检查中的一个检测项目，结果以',
              CASE WHEN i.value_type = 'text' THEN '文字描述' ELSE '数值' END,
              '表示，报告单位见指标库。本条为项目自述，出处待补。'),
       'PROJECT-DRAFT', NULL, NULL
FROM exam_indicator i
JOIN exam_category c ON c.id = i.category_id
WHERE i.owner_id = 0
  AND NOT EXISTS (
      SELECT 1 FROM indicator_knowledge k
      WHERE k.indicator_id = i.id AND k.field = 'definition'
  );
