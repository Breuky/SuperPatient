package com.superpatient.mapper;

import com.superpatient.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    int insert(User user);
    int update(User user);
    User findById(@Param("id") Long id);
}
