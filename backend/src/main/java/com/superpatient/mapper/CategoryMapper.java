package com.superpatient.mapper;

import com.superpatient.entity.ExamCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CategoryMapper {
    List<ExamCategory> findAll();
    ExamCategory findById(@Param("id") Integer id);
    int insert(ExamCategory category);
    int update(ExamCategory category);
    int deleteById(@Param("id") Integer id);
}
