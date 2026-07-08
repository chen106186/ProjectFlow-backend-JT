package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SystemUserResponse {
    @Schema(description = "主键 ID。")
    private Long id;

    @Schema(description = "部门 ID。")
    private Long departmentId;

    @Schema(description = "登录账号。")
    private String username;

    @Schema(description = "用户真实姓名。")
    private String realName;

    @Schema(description = "是否启用。")
    private Boolean enabled;
}
