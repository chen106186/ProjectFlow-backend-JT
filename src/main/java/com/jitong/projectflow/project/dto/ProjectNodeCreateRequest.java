package com.jitong.projectflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "创建项目时内嵌的节点请求。")
public class ProjectNodeCreateRequest {

    @NotBlank(message = "节点名称不能为空")
    @Schema(description = "节点名称。")
    private String nodeName;

    @Schema(description = "计划开始日期，格式 yyyy-MM-dd。")
    private LocalDate plannedStartDate;

    @Schema(description = "计划结束日期，格式 yyyy-MM-dd。")
    private LocalDate plannedEndDate;

    @Schema(description = "排序序号，从 1 开始，不传则按提交顺序自动赋值。")
    private Integer sortOrder;
}
