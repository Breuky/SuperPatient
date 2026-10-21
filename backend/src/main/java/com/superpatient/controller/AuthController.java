package com.superpatient.controller;

import com.superpatient.dto.*;
import com.superpatient.pojo.Result;
import com.superpatient.service.AuthService;
import com.superpatient.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证模块", description = "账号密码登录、注册与用户信息")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PatientService patientService;

    @Operation(summary = "登录", description = "用户名 + 密码换取 JWT Token")
    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest request) {
        LoginVO vo = authService.login(request);
        return Result.success(vo);
    }

    @Operation(summary = "注册", description = "创建账号并自动生成患者档案，注册成功即返回 Token")
    @PostMapping("/register")
    public Result register(@RequestBody RegisterRequest request) {
        LoginVO vo = authService.register(request);
        return Result.success(vo);
    }

    @Operation(summary = "获取用户信息")
    @GetMapping("/profile")
    public Result getProfile() {
        UserVO vo = authService.getProfile();
        return Result.success(vo);
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody ProfileUpdateRequest request) {
        UserVO vo = authService.updateProfile(request);
        return Result.success(vo);
    }

    @Operation(summary = "获取我的患者信息", description = "返回当前用户绑定的患者详细信息（含统计数据）")
    @GetMapping("/my-patient")
    public Result getMyPatient() {
        PatientDetailVO vo = patientService.getMyPatient();
        return Result.success(vo);
    }

    @Operation(summary = "更新我的患者信息", description = "更新当前用户的姓名、性别、出生日期、确诊日期等")
    @PutMapping("/my-patient")
    public Result updateMyPatient(@RequestBody PatientUpdateRequest request) {
        patientService.updateMyPatient(request);
        return Result.success(null);
    }

    @Operation(summary = "登出")
    @PostMapping("/logout")
    public Result logout() {
        return Result.success(null);
    }

    @Operation(summary = "获取用户统计信息")
    @GetMapping("/statistics")
    public Result getStatistics() {
        StatisticsVO vo = authService.getStatistics();
        return Result.success(vo);
    }
}
