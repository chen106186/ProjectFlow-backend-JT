package com.jitong.projectflow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "登录响应信息。")
public record LoginResponse(
        @Schema(description = "JWT 访问令牌，请在后续请求 Authorization 头中携带。")
        String token,

        @Schema(description = "用户 ID。")
        Long userId,

        @Schema(description = "用户真实姓名。")
        String realName) {
}
