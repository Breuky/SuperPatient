-- 医院科室资质：只填官网明确写出的，逐条带来源
-- 浙大一院：特色科室页列出「国家临床重点专科」，含肾脏病科、消化内科
UPDATE hospital_department hd
JOIN hospital h ON h.id = hd.hospital_id
SET hd.specialty_level = '国家临床重点专科',
    hd.source_url = 'https://www.zy91.com/server/characteristic_dept/209',
    hd.verified_at = '2026-11-12'
WHERE h.name = '浙江大学医学院附属第一医院' AND hd.department IN ('肾内科', '消化内科');

-- 浙江省人民医院：官网概况列 11 个省级临床重点专科，含消化内科
UPDATE hospital_department hd
JOIN hospital h ON h.id = hd.hospital_id
SET hd.specialty_level = '省级临床重点专科',
    hd.source_url = 'https://www.hospitalstar.com/#/intro/detail?id=201',
    hd.verified_at = '2026-11-12'
WHERE h.name = '浙江省人民医院' AND hd.department = '消化内科';

-- 邵逸夫医院：肾内科页面自述「浙江省医学重点专科」
UPDATE hospital_department hd
JOIN hospital h ON h.id = hd.hospital_id
SET hd.specialty_level = '省级医学重点专科',
    hd.source_url = 'https://www.srrsh.com/departmentInfo/20',
    hd.verified_at = '2026-11-12'
WHERE h.name = '浙江大学医学院附属邵逸夫医院' AND hd.department = '肾内科';
