package com.superpatient.mapper;

import com.superpatient.dto.RecordListVO;
import com.superpatient.entity.ExamRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecordMapper {
    List<RecordListVO> page(@Param("userId") Long userId,
                            @Param("categoryId") Integer categoryId,
                            @Param("startDate") String startDate,
                            @Param("endDate") String endDate);
    ExamRecord findById(@Param("id") Long id);
    int insert(ExamRecord record);
    int update(ExamRecord record);
    int deleteById(@Param("id") Long id);
    int countByUserId(@Param("userId") Long userId);
}
