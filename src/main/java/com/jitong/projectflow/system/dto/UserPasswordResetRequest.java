package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserPasswordResetRequest {
    @NotBlank
    @Schema(description = "新的登录密码。")
    private String password;
}
