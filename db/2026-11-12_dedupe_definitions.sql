-- 清理与 C 级（TEXTBOOK-COMMON）重复的 D 级定义草稿
-- 背景：先为全部指标生成了 D 级占位草稿，后又补了 24 条 C 级释义；同一指标两条 definition 会互相冲突
DELETE d FROM indicator_knowledge d
JOIN indicator_knowledge c
  ON c.indicator_id = d.indicator_id
 AND c.field = 'definition'
 AND c.source_id = 'TEXTBOOK-COMMON'
WHERE d.source_id = 'PROJECT-DRAFT' AND d.field = 'definition';
