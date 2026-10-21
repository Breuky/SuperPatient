package com.superpatient.service;

import com.superpatient.auth.WechatAuthClient;
import com.superpatient.dto.*;
import com.superpatient.entity.Patient;
import com.superpatient.entity.User;
import com.superpatient.mapper.IndicatorMapper;
import com.superpatient.mapper.PatientMapper;
import com.superpatient.mapper.RecordMapper;
import com.superpatient.mapper.UserMapper;
import com.superpatient.mapper.ValueMapper;
import com.superpatient.util.CurrentHolder;
import com.superpatient.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PatientMapper patientMapper;
    private final RecordMapper recordMapper;
    private final ValueMapper valueMapper;
    private final JwtUtils jwtUtils;
    private final WechatAuthClient wechatAuthClient;

    /**
     * 微信登录：用 wx.login() 的 code 换取 openid，再按 openid 查找或创建用户。
     * 每个用户自动创建一个对应的 patient 记录（1:1 关系）。
     */
    public LoginVO login(LoginRequest request) {
        String openid = wechatAuthClient.resolveOpenid(request.getCode());
        User user = userMapper.findByOpenid(openid);
        if (user == null) {
            user = new User();
            user.setOpenid(openid);
            user.setNickname(request.getNickname());
            user.setAvatarUrl(request.getAvatarUrl());
            userMapper.insert(user);

            // 为新用户自动创建 patient 记录
            Patient patient = new Patient();
            patient.setUserId(user.getId());
            patient.setName(request.getNickname() != null ? request.getNickname() : "患者");
            patient.setGender(1);
            patientMapper.insert(patient);
        } else if (request.getNickname() != null || request.getAvatarUrl() != null) {
            if (request.getNickname() != null) user.setNickname(request.getNickname());
            if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
            userMapper.update(user);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        String token = jwtUtils.generateToken(claims);

        UserVO userVO = toUserVO(user);
        return new LoginVO(token, 43200L, userVO);
    }

    public UserVO getProfile() {
        Long userId = CurrentHolder.getCurrentId();
        User user = userMapper.findById(userId);
        return toUserVO(user);
    }

    public UserVO updateProfile(ProfileUpdateRequest request) {
        Long userId = CurrentHolder.getCurrentId();
        User user = new User();
        user.setId(userId);
        user.setNickname(request.getNickname());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setPhone(request.getPhone());
        userMapper.update(user);
        return toUserVO(userMapper.findById(userId));
    }

    public StatisticsVO getStatistics() {
        Long userId = CurrentHolder.getCurrentId();
        int recordCount = recordMapper.countByUserId(userId);
        int indicatorCount = valueMapper.countDistinctIndicatorsByUserId(userId);
        return new StatisticsVO(1, recordCount, indicatorCount);
    }

    private UserVO toUserVO(User user) {
        return new UserVO(user.getId(), user.getNickname(), user.getAvatarUrl(), user.getPhone());
    }
}
