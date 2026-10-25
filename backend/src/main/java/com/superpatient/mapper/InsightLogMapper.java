package com.superpatient.mapper;

import com.superpatient.entity.InsightLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InsightLogMapper {
    int insert(InsightLog log);

    List<InsightLog> findRecent(@Param("userId") Long userId, @Param("limit") Integer limit);
}
