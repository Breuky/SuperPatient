package com.superpatient.service;

import com.superpatient.dto.*;
import com.superpatient.entity.User;
import com.superpatient.exception.BusinessException;
import com.superpatient.mapper.RecordMapper;
import com.superpatient.mapper.UserMapper;
import com.superpatient.mapper.ValueMapper;
import com.superpatient.ratelimit.RateLimitExceededException;
import com.superpatient.ratelimit.RateLimiter;
import com.superpatient.util.CurrentHolder;
import com.superpatient.util.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Web 端账号体系：用户名 + 密码。
 * 用户即患者本人，性别/出生日期直接挂在账号上（参考区间需要）。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserMapper userMapper;
    private final RecordMapper recordMapper;
    private final ValueMapper valueMapper;
    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final RateLimiter rateLimiter;

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
        user.setGender(request.getGender());
        userMapper.insert(user);

        return issueToken(user);
    }

    /**
     * 用户名不存在与密码错误返回同一句提示，避免暴露账号是否存在。
     */
    public LoginVO login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (isBlank(request.getPassword())) {
            throw new BusinessException("请输入密码");
        }

        // 同一账号 10 分钟内最多 5 次尝试；登录成功清零。
        // 提示语与「密码错误」保持一致，不泄露账号是否存在。
        String accountKey = "rl:login-account:" + username;
        RateLimiter.Decision decision = rateLimiter.check(accountKey, 5, Duration.ofMinutes(10), true);
        if (!decision.allowed()) {
            throw new RateLimitExceededException(decision.retryAfterSeconds());
        }

        User user = userMapper.findByUsername(username);
        if (user == null || user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误");
        }
        rateLimiter.reset(accountKey);
        return issueToken(user);
    }

    public UserVO getProfile() {
        return toUserVO(userMapper.findById(CurrentHolder.getCurrentId()));
    }

    @Transactional
    public UserVO updateProfile(ProfileUpdateRequest request) {
        Long userId = CurrentHolder.getCurrentId();
        User user = new User();
        user.setId(userId);
        user.setNickname(request.getNickname());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setPhone(request.getPhone());
        user.setGender(request.getGender());
        user.setBirthDate(parseBirthDate(request.getBirthDate()));
        user.setNotes(request.getNotes());
        userMapper.update(user);
        return toUserVO(userMapper.findById(userId));
    }

    public StatisticsVO getStatistics() {
        Long userId = CurrentHolder.getCurrentId();
        int recordCount = recordMapper.countByUserId(userId);
        int indicatorCount = valueMapper.countDistinctIndicatorsByUserId(userId);
        return new StatisticsVO(recordCount, indicatorCount);
    }

    private LoginVO issueToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        String token = jwtUtils.generateToken(claims);
        return new LoginVO(token, 43200L, toUserVO(user));
    }

    private LocalDate parseBirthDate(String raw) {
        if (isBlank(raw)) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException("出生日期格式应为 yyyy-MM-dd");
        }
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
        if (user == null) return null;
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setPhone(user.getPhone());
        vo.setGender(user.getGender());
        vo.setGenderText(genderText(user.getGender()));
        vo.setNotes(user.getNotes());
        if (user.getBirthDate() != null) {
            vo.setBirthDate(user.getBirthDate().toString());
            vo.setAge((int) ChronoUnit.YEARS.between(user.getBirthDate(), LocalDate.now()));
        }
        return vo;
    }

    private String genderText(Integer gender) {
        if (gender == null) return "未设置";
        return gender == 0 ? "女" : "男";
    }
}
