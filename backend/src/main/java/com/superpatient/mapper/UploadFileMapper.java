package com.superpatient.mapper;

import com.superpatient.entity.UploadFile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UploadFileMapper {
    int insert(UploadFile file);
    UploadFile findById(@Param("id") Long id);
}
