package com.superpatient.service;

import com.superpatient.dto.*;
import com.superpatient.entity.Patient;
import com.superpatient.entity.User;
import com.superpatient.exception.BusinessException;
import com.superpatient.mapper.PatientMapper;
import com.superpatient.mapper.RecordMapper;
import com.superpatient.mapper.UserMapper;
import com.superpatient.mapper.ValueMapper;
import com.superpatient.util.CurrentHolder;
import com.superpatient.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Web 端账号体系：用户名 + 密码，登录后用 JWT 换身份。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserMapper userMapper;
    private final PatientMapper patientMapper;
    private final RecordMapper recordMapper;
    private final ValueMapper valueMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    /**
     * 注册：创建账号，并自动创建一个与账号 1:1 的患者档案。
     */
    @Transactional
    public LoginVO register(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        validatePassword(request.getPassword());

        if (request.getGender() == null || (request.getGender() != 0 && request.getGender() != 1)) {
            throw new BusinessException("请选择性别（用于匹配参考区间）");
        }
        if (userMapper.findByUsername(username) != null) {
            throw new BusinessException("该用户名已被注册");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(isBlank(request.getNickname()) ? username : request.getNickname().trim());
        userMapper.insert(user);

        Patient patient = new Patient();
        patient.setUserId(user.getId());
        patient.setName(user.getNickname());
        patient.setGender(request.getGender());
        patientMapper.insert(patient);

        return issueToken(user);
    }

    /**
     * 登录：用户名不存在与密码错误返回同一句提示，避免暴露账号是否存在。
     */
    public LoginVO login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (isBlank(request.getPassword())) {
            throw new BusinessException("请输入密码");
        }

        User user = userMapper.findByUsername(username);
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        return issueToken(user);
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

    private LoginVO issueToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        String token = jwtUtils.generateToken(claims);
        return new LoginVO(token, 43200L, toUserVO(user));
    }

    private String normalizeUsername(String raw) {
        if (isBlank(raw)) {
            throw new BusinessException("请输入用户名");
        }
        String username = raw.trim();
        if (username.length() < 3 || username.length() > 32) {
            throw new BusinessException("用户名长度需为 3-32 个字符");
        }
        if (!username.matches("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$")) {
            throw new BusinessException("用户名只能包含字母、数字、下划线或中文");
        }
        return username;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new BusinessException("密码至少 " + MIN_PASSWORD_LENGTH + " 位");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private UserVO toUserVO(User user) {
        return new UserVO(user.getId(), user.getNickname(), user.getAvatarUrl(), user.getPhone());
    }
}
