package com.jitong.projectflow.system.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateRequest {
    private Long departmentId;
    private String realName;
    private String phone;
    private String email;
    private String jobNo;
    private String positionName;
    private Boolean enabled;
    private LocalDate hireDate;
}
