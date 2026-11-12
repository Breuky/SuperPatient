-- 外部来源与知识条目的存储结构。原则：只存「摘录 + 定位」，不存原文全文/PDF。
CREATE TABLE IF NOT EXISTS knowledge_source (
  id           VARCHAR(20) PRIMARY KEY COMMENT '如 WST405-2012',
  title        VARCHAR(200) NOT NULL COMMENT '文件标题',
  publisher    VARCHAR(100) NULL COMMENT '发布机构',
  doc_no       VARCHAR(50)  NULL COMMENT '标准号/文号',
  published_at DATE NULL,
  effective_at DATE NULL,
  url          VARCHAR(500) NULL COMMENT '官方页面地址（可复核）',
  license_note VARCHAR(200) NULL COMMENT '使用范围说明',
  retrieved_at DATE NULL COMMENT '抓取/核对日期',
  status       TINYINT NOT NULL DEFAULT 1
) COMMENT '外部知识来源登记';

CREATE TABLE IF NOT EXISTS indicator_knowledge (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  indicator_id INT NULL COMMENT '关联指标；为空表示通用条目（如「参考区间为什么因医院而异」）',
  field        VARCHAR(20) NOT NULL COMMENT 'definition/why_vary/interference/unit_note',
  text         VARCHAR(1000) NOT NULL COMMENT '摘录或改写后的条文',
  source_id    VARCHAR(20) NOT NULL,
  locator      VARCHAR(50) NULL COMMENT '定位：表1 / 5.3.2 / 第3章',
  verified_at  DATE NULL COMMENT '人工核对日期，为空表示未核对',
  UNIQUE KEY uk_knowledge (indicator_id, field, source_id, locator),
  CONSTRAINT fk_knowledge_source FOREIGN KEY (source_id) REFERENCES knowledge_source(id)
) COMMENT '指标知识条目（RAG 的语料）';

INSERT IGNORE INTO knowledge_source
(id, title, publisher, doc_no, published_at, effective_at, url, license_note, retrieved_at)
VALUES
('WST405-2012','血细胞分析参考区间','中华人民共和国卫生部','WS/T 405-2012','2012-12-25','2013-08-01',
 'http://www.nhc.gov.cn/ewebeditor/uploadfile/2013/01/20130109171100186.pdf',
 '卫生行业标准，公开征求意见稿/发布稿；仅摘录条文并标注出处，不转载全文','2026-11-12');

-- 通用条目：区间为什么因医院而异（标准 5.3.2 明确列出的影响因素）
INSERT IGNORE INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at) VALUES
(NULL,'why_vary','不同人群与采血方式会影响血细胞分析结果：新生儿及儿童的白细胞、红细胞计数随年龄增长明显变化；高海拔地区人群血红蛋白与红细胞计数明显高于其他地区；末梢采血与静脉采血结果存在差异；民族、生活习惯、地区差异也会造成结果变化。','WST405-2012','5.3.2','2026-11-12'),
(NULL,'why_vary','本标准参考区间基于中国成年人群多中心研究结果，适用于静脉血仪器检测方法；临床实验室应优先采用本标准区间，并在使用前做必要的验证或评估。','WST405-2012','5.1.1 / 表1注','2026-11-12'),
(NULL,'why_vary','当实验室的检测系统存在恒定系统偏倚时，会影响参考区间的适用性；而实验室内变异过大属于分析质量问题，不是参考区间适用性问题。','WST405-2012','5.1.2 注2','2026-11-12');
