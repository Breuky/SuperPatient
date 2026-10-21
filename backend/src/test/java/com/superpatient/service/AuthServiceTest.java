package com.superpatient.service;

import com.superpatient.dto.LoginRequest;
import com.superpatient.dto.LoginVO;
import com.superpatient.dto.RegisterRequest;
import com.superpatient.entity.Patient;
import com.superpatient.entity.User;
import com.superpatient.exception.BusinessException;
import com.superpatient.mapper.PatientMapper;
import com.superpatient.mapper.RecordMapper;
import com.superpatient.mapper.UserMapper;
import com.superpatient.mapper.ValueMapper;
import com.superpatient.util.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private PatientMapper patientMapper;
    @Mock private RecordMapper recordMapper;
    @Mock private ValueMapper valueMapper;
    @Mock private JwtUtils jwtUtils;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, patientMapper, recordMapper, valueMapper,
                jwtUtils, encoder);
    }

    @Test
    void registersUserAndPatientWithHashedPassword() {
        when(userMapper.findByUsername("zhangsan")).thenReturn(null);
        when(jwtUtils.generateToken(any())).thenReturn("jwt-token");

        LoginVO vo = authService.register(registerRequest());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("zhangsan", saved.getUsername());
        assertNotEquals("secret123", saved.getPasswordHash(), "密码必须哈希存储");
        assertTrue(encoder.matches("secret123", saved.getPasswordHash()));

        ArgumentCaptor<Patient> patientCaptor = ArgumentCaptor.forClass(Patient.class);
        verify(patientMapper).insert(patientCaptor.capture());
        assertEquals(0, patientCaptor.getValue().getGender(), "注册时选择的性别要写进患者档案");

        assertEquals("jwt-token", vo.getToken());
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
        User user = existingUser("secret123");
        when(userMapper.findByUsername("zhangsan")).thenReturn(user);
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
