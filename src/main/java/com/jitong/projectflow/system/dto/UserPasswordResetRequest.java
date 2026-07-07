package com.jitong.projectflow.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserPasswordResetRequest {
    @NotBlank
    private String password;
}
