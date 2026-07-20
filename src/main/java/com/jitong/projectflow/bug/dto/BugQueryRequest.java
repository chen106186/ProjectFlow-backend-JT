package com.jitong.projectflow.bug.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

@Data
public class BugQueryRequest extends PageQuery {
    @Schema(description = "状态。")
    private String status;
    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "关联任务 ID。")
    private Long taskId;
    @Schema(description = "关键字，支持按名称、标题或内容模糊查询。")
    private String keyword;
    @Schema(description = "指定处理人用户 ID。")
    private Long assigneeId;
    @Schema(description = "创建人用户 ID。")
    private Long creatorId;
}
