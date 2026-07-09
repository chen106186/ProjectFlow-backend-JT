package com.jitong.projectflow.auth.controller;

import com.jitong.projectflow.auth.dto.LoginRequest;
import com.jitong.projectflow.auth.dto.LoginResponse;
import com.jitong.projectflow.auth.security.JwtTokenService;
import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户登录和访问令牌签发接口")
public class AuthController {
    private final SystemUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    public AuthController(SystemUserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenService tokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Operation(summary = "用户登录",
            description = "校验用户名和密码，登录成功后返回 JWT 访问令牌和用户基础信息。")
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
