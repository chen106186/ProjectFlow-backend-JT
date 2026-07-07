package com.jitong.projectflow.system.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CurrentUserProfileResponse {
    private Long id;
    private Long departmentId;
    private String username;
    private String realName;
    private List<RoleResponse> roles;
    private List<MenuResponse> menus;
    private List<String> permissions;
}
