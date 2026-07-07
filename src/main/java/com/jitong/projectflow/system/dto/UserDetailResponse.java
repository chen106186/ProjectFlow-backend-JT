package com.jitong.projectflow.system.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class UserDetailResponse {
    private Long id;
    private Long departmentId;
    private String username;
    private String realName;
    private String phone;
    private String email;
    private String jobNo;
    private String positionName;
    private Boolean enabled;
    private LocalDate hireDate;
    private List<Long> roleIds;
}
