package com.superpatient.mapper;

import com.superpatient.entity.ReferenceRange;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ReferenceRangeMapper {
    List<ReferenceRange> findByIndicatorId(@Param("indicatorId") Integer indicatorId);
    List<ReferenceRange> findByIndicatorIds(@Param("ids") Collection<Integer> ids);
    ReferenceRange findById(@Param("id") Integer id);
    int insert(ReferenceRange range);
    int update(ReferenceRange range);
    int deleteById(@Param("id") Integer id);
}
