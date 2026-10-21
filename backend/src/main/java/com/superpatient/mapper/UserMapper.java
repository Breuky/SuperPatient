package com.superpatient.mapper;

import com.superpatient.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    User findByOpenid(String openid);
    int insert(User user);
    int update(User user);
    User findById(Long id);
}
