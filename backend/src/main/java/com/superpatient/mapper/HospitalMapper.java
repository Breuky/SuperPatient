package com.superpatient.mapper;

import com.superpatient.dto.HospitalVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface HospitalMapper {

    List<HospitalVO> findByCityAndDepartment(@Param("city") String city,
                                             @Param("department") String department,
                                             @Param("limit") int limit);
}
