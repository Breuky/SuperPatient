-- 规则来源要能记住具体出处（如 WS/T 405-2012），varchar(10) 放不下
ALTER TABLE exam_value MODIFY COLUMN ref_source VARCHAR(50) NULL COMMENT '参考区间来源: report / WS/T 405-2012 / 成人男性参考区间 / default';
