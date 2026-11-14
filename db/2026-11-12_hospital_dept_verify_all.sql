-- 15 条科室资质全部核对：verified_at 非空 = 查过官网；specialty_level 为空 = 官网未标注重点专科
-- 浙大一院（特色科室页）
UPDATE hospital_department hd JOIN hospital h ON h.id = hd.hospital_id
SET hd.verified_at='2026-11-12', hd.source_url='https://www.zy91.com/server/characteristic_dept/209',
    hd.specialty_level = CASE WHEN hd.department IN ('肾内科','消化内科') THEN '国家临床重点专科' ELSE NULL END
WHERE h.name='浙江大学医学院附属第一医院';

-- 浙大二院（医院简介列出重点专科/学科，不含内分泌、肾内、消化）
UPDATE hospital_department hd JOIN hospital h ON h.id = hd.hospital_id
SET hd.verified_at='2026-11-12', hd.source_url='https://www.z2hospital.com/Jxgl/News/Detail/13692',
    hd.specialty_level = NULL
WHERE h.name='浙江大学医学院附属第二医院';

-- 邵逸夫医院（各科室页自述）
UPDATE hospital_department hd JOIN hospital h ON h.id = hd.hospital_id
SET hd.verified_at='2026-11-12', hd.specialty_level=NULL,
    hd.source_url='https://www.srrsh.com/departmentInfo/15'
WHERE h.name='浙江大学医学院附属邵逸夫医院' AND hd.department='消化内科';

UPDATE hospital_department hd JOIN hospital h ON h.id = hd.hospital_id
SET hd.verified_at='2026-11-12', hd.specialty_level='省级医学重点学科',
    hd.source_url='https://www.srrsh.com/departmentInfo/24'
WHERE h.name='浙江大学医学院附属邵逸夫医院' AND hd.department='内分泌科';

-- 浙江省人民医院（官网概况：国家 4 个、省级 11 个）
UPDATE hospital_department hd JOIN hospital h ON h.id = hd.hospital_id
SET hd.verified_at='2026-11-12', hd.source_url='https://www.hospitalstar.com/#/intro/detail?id=201',
    hd.specialty_level = CASE WHEN hd.department='消化内科' THEN '省级临床重点专科' ELSE NULL END
WHERE h.name='浙江省人民医院';

-- 杭州市第一人民医院（学科筑峰页：国家级建设项目含消化内科，内分泌/肾内未列）
UPDATE hospital_department hd JOIN hospital h ON h.id = hd.hospital_id
SET hd.verified_at='2026-11-12', hd.source_url='https://www.hz-hospital.com/feature/zlcenter',
    hd.specialty_level = CASE WHEN hd.department='消化内科' THEN '国家临床重点专科' ELSE NULL END
WHERE h.name='杭州市第一人民医院';
