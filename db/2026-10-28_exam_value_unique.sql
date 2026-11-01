-- 一条记录里，同一个指标只允许出现一条值
-- 之前只靠应用层（前端以 indicatorId 聚合），直接调接口可绕过
ALTER TABLE exam_value
  ADD UNIQUE KEY uk_record_indicator (record_id, indicator_id);
