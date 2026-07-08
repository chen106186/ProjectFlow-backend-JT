package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

@Data
public class SystemUserQueryRequest extends PageQuery {
    @Schema(description = "关键字，支持按名称、标题或内容模糊查询。")
    private String keyword;
    @Schema(description = "部门 ID。")
    private Long departmentId;
    @Schema(description = "是否启用。")
    private Boolean enabled;
}
