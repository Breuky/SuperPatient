package com.superpatient.mapper;

import com.superpatient.entity.ExamIndicator;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface IndicatorMapper {
    List<ExamIndicator> findByCategoryId(@Param("categoryId") Integer categoryId);
    List<ExamIndicator> findAllEnabled();
    ExamIndicator findById(@Param("id") Integer id);
    int insert(ExamIndicator indicator);
    int update(ExamIndicator indicator);
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);
    int deleteById(@Param("id") Integer id);
    int countEnabled();
    boolean existsValuesByIndicatorId(@Param("indicatorId") Integer indicatorId);
}
