package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class CurrentUserProfileResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "部门 ID。")
    private Long departmentId;
    @Schema(description = "部门名称。")
    private String departmentName;
    @Schema(description = "登录账号。")
    private String username;
    @Schema(description = "用户真实姓名。")
    private String realName;
    @Schema(description = "工号。")
    private String jobNo;
    @Schema(description = "岗位名称。")
    private String positionName;
    @Schema(description = "手机号。")
    private String phone;
    @Schema(description = "邮箱。")
    private String email;
    @Schema(description = "入职日期，格式 yyyy-MM-dd。")
    private LocalDate hireDate;
    @Schema(description = "当前用户拥有的角色列表。")
    private List<RoleResponse> roles;
    @Schema(description = "当前用户可访问的菜单列表。")
    private List<MenuResponse> menus;
    @Schema(description = "当前用户拥有的权限码列表。")
    private List<String> permissions;
}
