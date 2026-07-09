package com.jitong.projectflow.auth.controller;

import com.jitong.projectflow.auth.dto.LoginRequest;
import com.jitong.projectflow.auth.security.JwtTokenService;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthControllerTest {
    @Test
    void loginReturnsChineseErrorWhenCredentialsAreInvalid() {
        SystemUserMapper userMapper = mock(SystemUserMapper.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtTokenService tokenService = mock(JwtTokenService.class);
        AuthController controller = new AuthController(userMapper, passwordEncoder, tokenService);

        SystemUser user = new SystemUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setPasswordHash("encoded");
        user.setEnabled(true);
        user.setDeleted(false);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> controller.login(new LoginRequest("admin", "wrong-password")))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException businessException = (BusinessException) ex;
                    assertThat(businessException.errorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
                    assertThat(businessException.getMessage()).isEqualTo("用户名或密码错误");
                });
    }
}
