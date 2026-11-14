package com.superpatient.mapper;

import com.superpatient.dto.UserMemoryVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMemoryMapper {

    List<UserMemoryVO> findActive(@Param("userId") Long userId);

    int upsert(@Param("userId") Long userId,
               @Param("kind") String kind,
               @Param("memoryKey") String memoryKey,
               @Param("value") String value,
               @Param("source") String source,
               @Param("confidence") double confidence);

    int softDelete(@Param("userId") Long userId, @Param("id") Integer id);

    int clear(@Param("userId") Long userId);
}
