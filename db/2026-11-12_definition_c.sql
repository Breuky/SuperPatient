-- C 级来源：教材通用表述（未逐条核对页码），用于覆盖高频指标释义
INSERT IGNORE INTO knowledge_source (id, title, publisher, origin_level, doc_no, url, license_note, retrieved_at, status)
VALUES ('TEXTBOOK-COMMON','常见医学教材通用表述','项目整理','C',NULL,NULL,
 '教材级通用定义，尚未逐条标注页码；回答中会明示来源等级为 C，不作为诊断依据','2026-11-12',1);

INSERT INTO indicator_knowledge (indicator_id, field, text, source_id, locator, verified_at)
SELECT i.id, 'definition', d.txt, 'TEXTBOOK-COMMON', NULL, '2026-11-12'
FROM exam_indicator i
JOIN (
  SELECT '血红蛋白' AS name, '血红蛋白是红细胞内负责运输氧的蛋白质，血液中的血红蛋白浓度反映红细胞的携氧能力，单位为 g/L。' AS txt
  UNION ALL SELECT '白细胞计数','白细胞计数是单位体积血液中白细胞的总数，白细胞参与机体免疫防御，单位为 ×10⁹/L。'
  UNION ALL SELECT '血小板计数','血小板计数是单位体积血液中血小板的数量，血小板参与止血与凝血过程，单位为 ×10⁹/L。'
  UNION ALL SELECT '红细胞计数','红细胞计数是单位体积血液中红细胞的数量，单位为 ×10¹²/L。'
  UNION ALL SELECT '红细胞压积','红细胞压积是红细胞体积占全血体积的比例，又称血细胞比容。'
  UNION ALL SELECT '平均红细胞体积','平均红细胞体积（MCV）是单个红细胞的平均体积，常用于贫血的形态学分类，单位为 fL。'
  UNION ALL SELECT '平均红细胞血红蛋白量','平均红细胞血红蛋白量（MCH）是单个红细胞内血红蛋白的平均含量，单位为 pg。'
  UNION ALL SELECT '平均红细胞血红蛋白浓度','平均红细胞血红蛋白浓度（MCHC）是红细胞内血红蛋白的平均浓度，单位为 g/L。'
  UNION ALL SELECT '中性粒细胞百分比','中性粒细胞百分比是中性粒细胞占白细胞总数的比例。'
  UNION ALL SELECT '淋巴细胞百分比','淋巴细胞百分比是淋巴细胞占白细胞总数的比例。'
  UNION ALL SELECT 'C反应蛋白','C 反应蛋白（CRP）是一种急性时相反应蛋白，机体存在炎症或感染时其浓度可升高，单位为 mg/L。'
  UNION ALL SELECT '空腹血糖','空腹血糖是至少空腹 8 小时后测得的血液葡萄糖浓度，单位为 mmol/L。'
  UNION ALL SELECT '糖化血红蛋白','糖化血红蛋白是红细胞内血红蛋白与葡萄糖结合的产物，其比例反映近 2～3 个月的平均血糖水平。'
  UNION ALL SELECT '总胆固醇','总胆固醇是血液中各类脂蛋白所含胆固醇的总量，单位为 mmol/L。'
  UNION ALL SELECT '甘油三酯','甘油三酯是血液中的一种脂类，主要来源于饮食摄入与肝脏合成，单位为 mmol/L。'
  UNION ALL SELECT '低密度脂蛋白胆固醇','低密度脂蛋白胆固醇（LDL-C）是主要将胆固醇运送至外周组织的脂蛋白，单位为 mmol/L。'
  UNION ALL SELECT '高密度脂蛋白胆固醇','高密度脂蛋白胆固醇（HDL-C）参与胆固醇的逆向转运，单位为 mmol/L。'
  UNION ALL SELECT '丙氨酸氨基转移酶','丙氨酸氨基转移酶（ALT）是主要存在于肝细胞胞浆中的酶，单位为 U/L。'
  UNION ALL SELECT '天冬氨酸氨基转移酶','天冬氨酸氨基转移酶（AST）是存在于肝细胞线粒体及心肌、骨骼肌中的酶，单位为 U/L。'
  UNION ALL SELECT '肌酐','肌酐是肌肉中磷酸肌酸的代谢产物，主要经肾小球滤过排出，单位为 μmol/L。'
  UNION ALL SELECT '尿素','尿素是蛋白质代谢的终产物，主要经肾脏排出，单位为 mmol/L。'
  UNION ALL SELECT '总胆红素','总胆红素是血红蛋白分解代谢的产物，经肝脏处理后随胆汁排泄，单位为 μmol/L。'
  UNION ALL SELECT '白蛋白','白蛋白是血浆中含量最高的蛋白质，参与维持血浆胶体渗透压并运输多种物质，单位为 g/L。'
  UNION ALL SELECT '尿酸','尿酸是嘌呤代谢的终产物，单位为 μmol/L。'
) d ON d.name = i.name
WHERE i.owner_id = 0
  AND NOT EXISTS (SELECT 1 FROM indicator_knowledge k
                  WHERE k.indicator_id = i.id AND k.field='definition' AND k.source_id='TEXTBOOK-COMMON');
