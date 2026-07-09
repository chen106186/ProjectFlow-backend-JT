package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SystemUserResponse {
    @Schema(description = "Primary key ID")
    private Long id;

    @Schema(description = "Department ID")
    private Long departmentId;

    @Schema(description = "Department name")
    private String departmentName;

    @Schema(description = "Login username")
    private String username;

    @Schema(description = "Real name")
    private String realName;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Whether the user is enabled")
    private Boolean enabled;

    @Schema(description = "Assigned role ID list")
    private List<Long> roleIds;

    @Schema(description = "Assigned role name list")
    private List<String> roleNames;
}