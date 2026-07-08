package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

import java.time.LocalDate;

@Data
public class UserUpdateRequest {
    @Schema(description = "部门 ID。")
    private Long departmentId;
    @Schema(description = "用户真实姓名。")
    private String realName;
    @Schema(description = "手机号。")
    private String phone;
    @Schema(description = "邮箱地址。")
    private String email;
    @Schema(description = "工号。")
    private String jobNo;
    @Schema(description = "岗位名称。")
    private String positionName;
    @Schema(description = "是否启用。")
    private Boolean enabled;
    @Schema(description = "入职日期，格式 yyyy-MM-dd。")
    private LocalDate hireDate;
}
