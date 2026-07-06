package com.jitong.projectflow.auth.controller;

import com.jitong.projectflow.auth.dto.LoginRequest;
import com.jitong.projectflow.auth.dto.LoginResponse;
import com.jitong.projectflow.auth.security.JwtTokenService;
import com.jitong.projectflow.common.api.ApiResponse;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final SystemUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    public AuthController(SystemUserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenService tokenService) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        SystemUser user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemUser>()
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
