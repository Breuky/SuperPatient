package com.superpatient.mapper;

import com.superpatient.entity.UserIndicatorFocus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FocusIndicatorMapper {
    List<Integer> findIndicatorIdsByUserId(@Param("userId") Long userId);

    int insert(@Param("userId") Long userId, @Param("indicatorId") Integer indicatorId);

    int delete(@Param("userId") Long userId, @Param("indicatorId") Integer indicatorId);

    List<UserIndicatorFocus> findByUserId(@Param("userId") Long userId);
}
