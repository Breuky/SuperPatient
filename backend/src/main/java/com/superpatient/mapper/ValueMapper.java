package com.superpatient.mapper;

import com.superpatient.entity.ExamValue;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ValueMapper {
    List<ExamValue> findByRecordId(@Param("recordId") Long recordId);
    int batchInsert(@Param("list") List<ExamValue> values);
    int deleteByRecordId(@Param("recordId") Long recordId);
    List<ExamValue> findByPatientIdAndIndicatorId(@Param("patientId") Long patientId,
                                                   @Param("indicatorId") Integer indicatorId,
                                                   @Param("startDate") String startDate,
                                                   @Param("endDate") String endDate);
    int countDistinctIndicatorsByUserId(@Param("userId") Long userId);
}
