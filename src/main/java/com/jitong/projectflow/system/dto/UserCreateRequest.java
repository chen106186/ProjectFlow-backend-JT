package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class UserCreateRequest {
    @Schema(description = "部门 ID。")
    private Long departmentId;

    @NotBlank
    @Schema(description = "登录账号。")
    private String username;

    @NotBlank
    @Schema(description = "登录密码。")
    private String password;

    @NotBlank
    @Schema(description = "用户真实姓名。")
    private String realName;

    @Schema(description = "手机号码。")
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

    @Schema(description = "初始角色 ID 列表，不传或为空时创建无角色用户。")
    private List<Long> roleIds = new ArrayList<>();
}
