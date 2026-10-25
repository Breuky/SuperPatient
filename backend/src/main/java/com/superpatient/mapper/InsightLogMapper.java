package com.superpatient.mapper;

import com.superpatient.entity.InsightLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InsightLogMapper {
    int insert(InsightLog log);

    List<InsightLog> findRecent(@Param("userId") Long userId, @Param("limit") Integer limit);

    /** 按题型聚合：条数、平均轮数/工具/token/耗时、降级次数 */
    List<com.superpatient.dto.InsightStatVO> statsByCategory(@Param("userId") Long userId);
}
