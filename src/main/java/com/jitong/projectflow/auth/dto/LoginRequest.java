package com.jitong.projectflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "登录请求参数。")
public record LoginRequest(
        @Schema(description = "登录账号。")
        @NotBlank
        String username,

        @Schema(description = "登录密码。")
        @NotBlank
        String password,

        @Schema(description = "是否记住登录状态。")
        Boolean rememberMe) {
}
