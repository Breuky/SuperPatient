-- 导诊用的医院白名单。原则：只收可核查的公开信息；未能核对的字段留空，不用推测填充。
CREATE TABLE IF NOT EXISTS hospital (
  id           INT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(100) NOT NULL COMMENT '医院名称',
  city         VARCHAR(50)  NOT NULL COMMENT '城市',
  district     VARCHAR(50)  NULL COMMENT '行政区（可能因区划调整变化，留空表示未核对）',
  level        VARCHAR(20)  NULL COMMENT '等级，如三甲',
  address      VARCHAR(200) NULL,
  booking_url  VARCHAR(500) NULL COMMENT '官方预约入口',
  source_url   VARCHAR(500) NULL COMMENT '信息来源链接，填写后才算核对过',
  verified_at  DATE         NULL COMMENT '核对日期，为空表示未核对',
  status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
  created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_hospital (name, city)
) COMMENT '医院白名单';

CREATE TABLE IF NOT EXISTS hospital_department (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  hospital_id     INT NOT NULL,
  department      VARCHAR(50) NOT NULL COMMENT '科室名，与导诊输出的科室名一致',
  specialty_level VARCHAR(50) NULL COMMENT '国家/省级临床重点专科，查不到就留空',
  source_url      VARCHAR(500) NULL,
  verified_at     DATE NULL COMMENT '为空表示该科室资质尚未核对',
  UNIQUE KEY uk_hospital_dept (hospital_id, department),
  CONSTRAINT fk_hd_hospital FOREIGN KEY (hospital_id) REFERENCES hospital(id) ON DELETE CASCADE
) COMMENT '医院-科室（白名单的二级表）';

INSERT IGNORE INTO hospital (name, city, level) VALUES
('浙江大学医学院附属第一医院','杭州','三甲'),
('浙江大学医学院附属第二医院','杭州','三甲'),
('浙江大学医学院附属邵逸夫医院','杭州','三甲'),
('浙江大学医学院附属妇产科医院','杭州','三甲'),
('浙江大学医学院附属儿童医院','杭州','三甲'),
('浙江大学医学院附属口腔医院','杭州','三甲'),
('浙江省人民医院','杭州','三甲'),
('浙江省中医院','杭州','三甲'),
('浙江省肿瘤医院','杭州','三甲'),
('浙江省立同德医院','杭州','三甲'),
('浙江医院','杭州','三甲'),
('杭州市第一人民医院','杭州','三甲'),
('杭州师范大学附属医院','杭州','三甲'),
('杭州市第三人民医院','杭州','三甲'),
('杭州市红十字会医院','杭州','三甲'),
('杭州市中医院','杭州','三甲'),
('杭州市西溪医院','杭州','三甲'),
('杭州市肿瘤医院','杭州','三甲'),
('杭州市儿童医院','杭州','三甲'),
('杭州市第七人民医院','杭州','三甲');

-- 先只为综合医院建三个科室的占位，资质与来源留空，等人工核对后填
INSERT IGNORE INTO hospital_department (hospital_id, department)
SELECT h.id, d.name FROM hospital h
JOIN (SELECT '内分泌科' AS name UNION ALL SELECT '肾内科' UNION ALL SELECT '消化内科') d
WHERE h.city = '杭州'
  AND h.name IN ('浙江大学医学院附属第一医院','浙江大学医学院附属第二医院',
                 '浙江大学医学院附属邵逸夫医院','浙江省人民医院',
                 '杭州市第一人民医院','浙江医院');
