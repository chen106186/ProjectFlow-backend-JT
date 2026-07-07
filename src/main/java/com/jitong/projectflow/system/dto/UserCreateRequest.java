package com.jitong.projectflow.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserCreateRequest {
    private Long departmentId;
    @NotBlank
    private String username;
    @NotBlank
    private String password;
    @NotBlank
    private String realName;
    private String phone;
    private String email;
    private String jobNo;
    private String positionName;
    private Boolean enabled;
    private LocalDate hireDate;
}
