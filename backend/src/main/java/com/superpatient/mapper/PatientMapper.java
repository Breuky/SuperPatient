package com.superpatient.mapper;

import com.superpatient.entity.Patient;
import com.superpatient.dto.PatientListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PatientMapper {
    List<PatientListVO> page(@Param("userId") Long userId,
                             @Param("keyword") String keyword);
    Patient findById(@Param("id") Long id);
    Patient findByUserId(@Param("userId") Long userId);
    int insert(Patient patient);
    int update(Patient patient);
    int deleteById(@Param("id") Long id);
    int countByUserId(@Param("userId") Long userId);
}
