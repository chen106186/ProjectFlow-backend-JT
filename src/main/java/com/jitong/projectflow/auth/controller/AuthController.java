package com.jitong.projectflow.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.auth.dto.ChangePasswordRequest;
import com.jitong.projectflow.auth.dto.LoginRequest;
import com.jitong.projectflow.auth.dto.LoginResponse;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.auth.security.JwtTokenService;
import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.system.dto.CurrentUserProfileResponse;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.system.service.CurrentUserPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Authentication endpoints")
public class AuthController {
    private final SystemUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final CurrentUserPermissionService currentUserPermissionService;

    public AuthController(SystemUserMapper userMapper,
                          PasswordEncoder passwordEncoder,
                          JwtTokenService tokenService,
                          CurrentUserPermissionService currentUserPermissionService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.currentUserPermissionService = currentUserPermissionService;
    }

    @Operation(summary = "修改密码", description = "当前登录用户修改自己的登录密码，需验证原密码。")
    @PatchMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Long userId = CurrentUserContext.userId();
        SystemUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "原密码错误");
        }
        if (request.getNewPassword().equals(request.getOldPassword())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码不能与原密码一致");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "Get current profile")
    @GetMapping("/profile")
    public ApiResponse<CurrentUserProfileResponse> profile() {
        return ApiResponse.success(currentUserPermissionService.getCurrentUser(), MDC.get("traceId"));
    }

    @Operation(summary = "Logout")
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success(null, MDC.get("traceId"));
    }

    @Operation(summary = "Login")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        SystemUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SystemUser>()
                        .eq(SystemUser::getUsername, request.username())
                        .eq(SystemUser::getDeleted, false));
        if (user == null || !Boolean.TRUE.equals(user.getEnabled())
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        long rememberTtlMinutes = 60L * 24 * 30;
        String token = Boolean.TRUE.equals(request.rememberMe())
                ? tokenService.createToken(user.getId(), user.getUsername(), rememberTtlMinutes)
                : tokenService.createToken(user.getId(), user.getUsername());
        return ApiResponse.success(new LoginResponse(token, user.getId(), user.getRealName()), MDC.get("traceId"));
    }
}
