package com.superpatient.service;

import com.superpatient.dto.HospitalVO;
import com.superpatient.mapper.HospitalMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 医院白名单查询：只读，不做推荐排序，排序规则固定为「有重点专科标注的优先」 */
@Service
@RequiredArgsConstructor
public class HospitalService {

    private static final int MAX_LIMIT = 10;

    private final HospitalMapper hospitalMapper;

    public List<HospitalVO> find(String city, String department, Integer limit) {
        int size = limit == null || limit <= 0 ? 5 : Math.min(limit, MAX_LIMIT);
        return hospitalMapper.findByCityAndDepartment(city, department, size);
    }
}
