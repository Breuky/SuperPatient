package com.superpatient.controller;

import com.superpatient.dto.*;
import com.superpatient.pojo.Result;
import com.superpatient.ratelimit.Dimension;
import com.superpatient.ratelimit.RateLimit;
import com.superpatient.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "认证模块", description = "账号密码登录、注册与个人信息")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录", description = "用户名 + 密码换取 JWT Token；同 IP 每分钟 10 次")
    @RateLimit(scope = "login-ip", limit = 10, window = 1, unit = java.util.concurrent.TimeUnit.MINUTES, dimension = Dimension.IP)
    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @Operation(summary = "注册", description = "创建账号并直接返回 Token；同 IP 每小时 3 次")
    @RateLimit(scope = "register-ip", limit = 3, window = 1, unit = java.util.concurrent.TimeUnit.HOURS, dimension = Dimension.IP)
    @PostMapping("/register")
    public Result register(@RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    @Operation(summary = "获取个人信息", description = "含性别、出生日期、年龄，用于参考区间匹配")
    @GetMapping("/profile")
    public Result getProfile() {
        return Result.success(authService.getProfile());
    }

    @Operation(summary = "更新个人信息")
    @PutMapping("/profile")
    public Result updateProfile(@RequestBody ProfileUpdateRequest request) {
        return Result.success(authService.updateProfile(request));
    }

    @Operation(summary = "登出", description = "JWT 无状态，服务端不维护会话，前端删除本地 Token 即可")
    @PostMapping("/logout")
    public Result logout() {
        return Result.success(null);
    }

    @Operation(summary = "统计信息", description = "记录数与使用过的指标数")
    @GetMapping("/statistics")
    public Result getStatistics() {
        return Result.success(authService.getStatistics());
    }
}
