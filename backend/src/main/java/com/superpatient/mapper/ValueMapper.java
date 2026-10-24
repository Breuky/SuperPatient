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
    List<ExamValue> findByUserIdAndIndicatorId(@Param("userId") Long userId,
                                               @Param("indicatorId") Integer indicatorId,
                                                   @Param("startDate") String startDate,
                                                   @Param("endDate") String endDate);
    int countDistinctIndicatorsByUserId(@Param("userId") Long userId);

    /** 当前用户有数值记录的指标（不看启用状态，指标被禁用后历史趋势仍要能查） */
    List<com.superpatient.dto.TrendableIndicatorVO> findTrendableIndicators(@Param("userId") Long userId);
}
