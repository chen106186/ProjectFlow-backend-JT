package com.jitong.projectflow.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.auth.dto.LoginRequest;
import com.jitong.projectflow.auth.dto.LoginResponse;
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
        String token = tokenService.createToken(user.getId(), user.getUsername());
        return ApiResponse.success(new LoginResponse(token, user.getId(), user.getRealName()), MDC.get("traceId"));
    }
}
