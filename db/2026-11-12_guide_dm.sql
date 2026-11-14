-- B 级来源：中国糖尿病防治指南（2024 版）
-- 说明：该指南 PDF 直链禁止脚本下载（403），本条仅录入检索结果中可见的原文句子
INSERT IGNORE INTO knowledge_source (id, title, publisher, origin_level, doc_no, published_at, url, license_note, retrieved_at, status)
VALUES ('GUIDE-DM-2024','中国糖尿病防治指南（2024 版）','中华医学会糖尿病学分会','B',NULL,'2024-12-01',
 'https://www.yantai.gov.cn:444/cms_files/jcms1/web1/site/attach/0/96e88fd3c75d4c8b91577814a7c86930.pdf',
 '政府网站公开全文；仅摘录表3注释中的一句原文','2026-11-12',1);

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT NULL, 'prep', '空腹状态指至少 8 小时没有进食热量。', 'GUIDE-DM-2024', '表3 注释', '2026-11-12'
WHERE NOT EXISTS (SELECT 1 FROM indicator_knowledge k WHERE k.source_id='GUIDE-DM-2024');
