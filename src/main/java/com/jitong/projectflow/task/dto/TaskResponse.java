package com.jitong.projectflow.task.dto;

import com.jitong.projectflow.system.dto.OperationLogResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@Schema(description = "任务响应。")
public class TaskResponse {
    @Schema(description = "主键 ID。")
    private Long id;

    @Schema(description = "项目 ID。")
    private Long projectId;

    @Schema(description = "父任务 ID，顶级任务为空。")
    private Long parentId;

    @Schema(description = "任务名称。")
    private String name;

    @Schema(description = "任务角色名称。")
    private String roleName;

    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;

    @Schema(description = "任务状态。")
    private String status;

    @Schema(description = "负责人用户 ID。")
    private Long assigneeId;

    @Schema(description = "计划开始日期，格式 yyyy-MM-dd。")
    private LocalDate plannedStartDate;

    @Schema(description = "计划结束日期，格式 yyyy-MM-dd。")
    private LocalDate plannedEndDate;

    @Schema(description = "实际开始日期，格式 yyyy-MM-dd。")
    private LocalDate actualStartDate;

    @Schema(description = "实际结束日期，格式 yyyy-MM-dd。")
    private LocalDate actualEndDate;

    @Schema(description = "详细描述。")
    private String description;

    @Schema(description = "标签，多个标签可用逗号分隔。")
    private String tags;

    @Schema(description = "备注。")
    private String remark;

    @Schema(description = "同级任务排序号。")
    private Integer sortOrder;

    @Schema(description = "负责人姓名（仅详情接口填充）。")
    private String assigneeName;

    @Schema(description = "所属项目名称（仅详情接口填充）。")
    private String projectName;

    @Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime createdAt;

    @Schema(description = "操作日志列表（仅详情接口填充）。")
    private List<OperationLogResponse> logs;
}
