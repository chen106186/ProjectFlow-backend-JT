package com.jitong.projectflow.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectReportQueryRequest extends PageQuery {
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "状态。")
    private String status;
    @Schema(description = "汇报类型。")
    private String reportType;
    @Schema(description = "计划汇报开始日期，格式 yyyy-MM-dd。")
    private LocalDate plannedDateFrom;
    @Schema(description = "计划汇报结束日期，格式 yyyy-MM-dd。")
    private LocalDate plannedDateTo;
    @Schema(description = "关键字，支持按名称、标题或内容模糊查询。")
    private String keyword;
}
