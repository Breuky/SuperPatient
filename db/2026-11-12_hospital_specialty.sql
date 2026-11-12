-- 专科类补充核对：来源均为省卫健委正式通知或医院官网自述
ALTER TABLE hospital
  ADD COLUMN hospital_type VARCHAR(20) NULL COMMENT '综合/中医/专科' AFTER level;

-- 第六批（2023-03-14）：三甲肿瘤医院、三甲精神病医院
UPDATE hospital SET hospital_type='专科', level='三甲',
  source_url='https://wsjkw.zj.gov.cn/art/2023/3/22/art_1229560650_2471410.html',
  verified_at='2026-11-12'
WHERE name='浙江省肿瘤医院' AND city='杭州';

UPDATE hospital SET hospital_type='专科', level='三甲',
  source_url='https://wsjkw.zj.gov.cn/art/2023/3/22/art_1229560650_2471410.html',
  verified_at='2026-11-12'
WHERE name='杭州市第七人民医院' AND city='杭州';

-- 第七批（浙卫发函〔2023〕352号）：三甲口腔医院、三甲皮肤病医院
UPDATE hospital SET hospital_type='专科', level='三甲',
  source_url='https://wsjkw.zj.gov.cn/art/2024/1/15/art_1229560650_2510173.html',
  verified_at='2026-11-12'
WHERE name='浙江大学医学院附属口腔医院' AND city='杭州';

UPDATE hospital SET hospital_type='专科', level='三甲', 
  source_url='https://wsjkw.zj.gov.cn/art/2024/1/15/art_1229560650_2510173.html',
  verified_at='2026-11-12'
WHERE name='杭州市第三人民医院' AND city='杭州';

-- 医院官网依据（非卫健委文件，来源类型不同，需在展示时区分）
UPDATE hospital SET hospital_type='专科', level='三甲',
  source_url='https://www.zjuch.cn/news/default/id/8243/cid/99',
  verified_at='2026-11-12'
WHERE name='浙江大学医学院附属儿童医院' AND city='杭州';

UPDATE hospital SET hospital_type='专科', level='三甲',
  source_url='https://www.womanhospital.cn/',
  verified_at='2026-11-12'
WHERE name='浙江大学医学院附属妇产科医院' AND city='杭州';

-- 已核对综合医院的类型
UPDATE hospital SET hospital_type='综合'
WHERE verified_at IS NOT NULL AND hospital_type IS NULL;
