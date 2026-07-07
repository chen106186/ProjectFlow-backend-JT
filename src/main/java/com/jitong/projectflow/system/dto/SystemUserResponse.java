package com.jitong.projectflow.system.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemUserResponse {
    private Long id;
    private Long departmentId;
    private String username;
    private String realName;
    private Boolean enabled;
}
