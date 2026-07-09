package com.jitong.projectflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ProjectResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "项目类型：DIGITALIZATION（数字化）、INFORMATIZATION（信息化）、RESEARCH（科研）。")
    private String projectType;
    @Schema(description = "名称。")
    private String name;
    @Schema(description = "项目阶段。")
    private String stage;
    @Schema(description = "状态。")
    private String status;
    @Schema(description = "合同状态。")
    private String contractStatus;
    @Schema(description = "业务部门。")
    private String businessDepartment;
    @Schema(description = "承建单位。")
    private String contractorUnit;
    @Schema(description = "业务主管。")
    private String businessSupervisor;
    @Schema(description = "回款金额（元）。")
    private BigDecimal receivableAmount;
    @Schema(description = "项目经理用户 ID。")
    private Long managerId;
    @Schema(description = "详细描述。")
    private String description;
    @Schema(description = "计划开始日期，格式 yyyy-MM-dd。")
    private LocalDate plannedStartDate;
    @Schema(description = "计划结束日期，格式 yyyy-MM-dd。")
    private LocalDate plannedEndDate;
    @Schema(description = "实际开始日期，格式 yyyy-MM-dd。")
    private LocalDate actualStartDate;
    @Schema(description = "实际结束日期，格式 yyyy-MM-dd。")
    private LocalDate actualEndDate;
}
