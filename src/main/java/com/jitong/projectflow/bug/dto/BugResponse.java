package com.jitong.projectflow.bug.dto;

import com.jitong.projectflow.system.dto.OperationLogResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class BugResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "Bug 序号，从 1 开始递增。")
    private Long bugNo;
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "任务 ID。")
    private Long taskId;
    @Schema(description = "关联任务 ID 列表。")
    private List<Long> relatedTaskIds;
    @Schema(description = "关联任务列表。")
    private List<BugRelatedTaskResponse> relatedTasks;
    @Schema(description = "标题。")
    private String title;
    @Schema(description = "状态。")
    private String status;
    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;
    @Schema(description = "创建人 ID。")
    private Long creatorId;
    @Schema(description = "负责人用户 ID。")
    private Long assigneeId;
    @Schema(description = "详细描述。")
    private String description;
    @Schema(description = "复现步骤。")
    private String reproduceSteps;
    @Schema(description = "问题分析（开发填写）。")
    private String fixAnalysis;
    @Schema(description = "修复细节（开发填写）。")
    private String fixDetail;
    @Schema(description = "解决方案枚举值。")
    private String solution;
    @Schema(description = "解决日期。")
    private LocalDate resolvedDate;
    @Schema(description = "解决备注（富文本 HTML）。")
    private String resolveRemark;
    @Schema(description = "关闭时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime closedAt;
    @Schema(description = "创建时间。")
    private java.time.LocalDateTime createdAt;
    @Schema(description = "创建人姓名。")
    private String creatorName;
    @Schema(description = "负责人姓名。")
    private String assigneeName;
    @Schema(description = "项目名称。")
    private String projectName;
    @Schema(description = "操作日志列表（仅详情接口填充）。")
    private List<OperationLogResponse> logs;
}
