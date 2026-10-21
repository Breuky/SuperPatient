package com.superpatient.mapper;

import com.superpatient.dto.PatientDetailVO;
import com.superpatient.dto.RecordListVO;
import com.superpatient.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecordMapper {
    List<RecordListVO> page(@Param("patientId") Long patientId,
                            @Param("categoryId") Integer categoryId,
                            @Param("startDate") String startDate,
                            @Param("endDate") String endDate);
    ExamRecord findById(@Param("id") Long id);
    int insert(ExamRecord record);
    int update(ExamRecord record);
    int deleteById(@Param("id") Long id);
    int countByUserId(@Param("userId") Long userId);
    List<PatientDetailVO.CategoryStat> getCategoryStats(@Param("patientId") Long patientId);
}
