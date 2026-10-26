package com.superpatient.service;

import com.superpatient.dto.LoginRequest;
import com.superpatient.dto.LoginVO;
import com.superpatient.dto.ProfileUpdateRequest;
import com.superpatient.dto.RegisterRequest;
import com.superpatient.entity.User;
import com.superpatient.exception.BusinessException;
import com.superpatient.mapper.RecordMapper;
import com.superpatient.mapper.UserMapper;
import com.superpatient.mapper.ValueMapper;
import com.superpatient.ratelimit.RateLimiter;
import com.superpatient.util.CurrentHolder;
import com.superpatient.util.JwtUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private RecordMapper recordMapper;
    @Mock private ValueMapper valueMapper;
    @Mock private JwtUtils jwtUtils;
    @Mock private RateLimiter rateLimiter;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, recordMapper, valueMapper, jwtUtils, encoder, rateLimiter);
        // 限流器默认放行，登录用例只测业务逻辑；限流本身由 LocalWindowCounterTest 覆盖
        lenient().when(rateLimiter.check(anyString(), anyInt(), any(), anyBoolean()))
                .thenReturn(new RateLimiter.Decision(true, 0));
    }

    @AfterEach
    void tearDown() {
        CurrentHolder.remove();
    }

    @Test
    void registersUserWithHashedPasswordAndGender() {
        when(userMapper.findByUsername("zhangsan")).thenReturn(null);
        when(jwtUtils.generateToken(any())).thenReturn("jwt-token");

        LoginVO vo = authService.register(registerRequest());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("zhangsan", saved.getUsername());
        assertNotEquals("secret123", saved.getPasswordHash(), "密码必须哈希存储");
        assertTrue(encoder.matches("secret123", saved.getPasswordHash()));
        assertEquals(0, saved.getGender(), "性别用于参考区间匹配，注册时必须写入账号");

        assertEquals("jwt-token", vo.getToken());
        assertEquals("张三", vo.getUserInfo().getNickname());
    }

    @Test
    void rejectsDuplicateUsername() {
        when(userMapper.findByUsername("zhangsan")).thenReturn(new User());
        assertThrows(BusinessException.class, () -> authService.register(registerRequest()));
        verify(userMapper, never()).insert(any());
    }

    @Test
    void rejectsShortPassword() {
        RegisterRequest req = registerRequest();
        req.setPassword("123");
        assertThrows(BusinessException.class, () -> authService.register(req));
    }

    @Test
    void rejectsMissingGender() {
        RegisterRequest req = registerRequest();
        req.setGender(null);
        assertThrows(BusinessException.class, () -> authService.register(req),
                "性别用于参考区间匹配，不能缺省");
    }

    @Test
    void logsInWithCorrectPassword() {
        when(userMapper.findByUsername("zhangsan")).thenReturn(existingUser("secret123"));
        when(jwtUtils.generateToken(any())).thenReturn("jwt-token");

        LoginVO vo = authService.login(loginRequest("secret123"));

        assertEquals("jwt-token", vo.getToken());
        assertEquals(43200L, vo.getExpireIn());
        assertEquals("张三", vo.getUserInfo().getNickname());
    }

    @Test
    void rejectsWrongPasswordWithoutRevealingWhichPartIsWrong() {
        when(userMapper.findByUsername("zhangsan")).thenReturn(existingUser("secret123"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("wrong-pass")));
        assertEquals("用户名或密码错误", e.getMessage());
    }

    @Test
    void rejectsUnknownUserWithSameMessage() {
        when(userMapper.findByUsername("zhangsan")).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest("secret123")));
        assertEquals("用户名或密码错误", e.getMessage());
    }

    @Test
    void rejectsLegacyAccountWithoutPassword() {
        User user = existingUser("secret123");
        user.setPasswordHash(null);
        when(userMapper.findByUsername("zhangsan")).thenReturn(user);

        assertThrows(BusinessException.class, () -> authService.login(loginRequest("secret123")));
    }

    @Test
    void updatesProfileIncludingBirthDate() {
        CurrentHolder.setCurrentId(1L);
        User stored = existingUser("secret123");
        stored.setBirthDate(LocalDate.parse("1990-05-20"));
        when(userMapper.findById(1L)).thenReturn(stored);

        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setNickname("李四");
        req.setGender(0);
        req.setBirthDate("1990-05-20");
        req.setNotes("无过敏史");

        authService.updateProfile(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).update(captor.capture());
        User updated = captor.getValue();
        assertEquals(1L, updated.getId());
        assertEquals(0, updated.getGender());
        assertEquals(LocalDate.parse("1990-05-20"), updated.getBirthDate());
        assertEquals("无过敏史", updated.getNotes());
    }

    @Test
    void rejectsInvalidBirthDateFormat() {
        CurrentHolder.setCurrentId(1L);
        ProfileUpdateRequest req = new ProfileUpdateRequest();
        req.setBirthDate("1990/05/20");

        assertThrows(BusinessException.class, () -> authService.updateProfile(req));
        verify(userMapper, never()).update(any());
    }

    @Test
    void computesAgeFromBirthDate() {
        CurrentHolder.setCurrentId(1L);
        User user = existingUser("secret123");
        user.setBirthDate(LocalDate.now().minusYears(30));
        when(userMapper.findById(1L)).thenReturn(user);

        assertEquals(30, authService.getProfile().getAge());
    }

    private RegisterRequest registerRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("zhangsan");
        req.setPassword("secret123");
        req.setNickname("张三");
        req.setGender(0);
        return req;
    }

    private LoginRequest loginRequest(String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername("zhangsan");
        req.setPassword(password);
        return req;
    }

    private User existingUser(String rawPassword) {
        User user = new User();
        user.setId(1L);
        user.setUsername("zhangsan");
        user.setNickname("张三");
        user.setPasswordHash(encoder.encode(rawPassword));
        return user;
    }
}
