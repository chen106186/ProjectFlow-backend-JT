package com.jitong.projectflow.project.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "项目分类。")
    private String projectType;
    @Schema(description = "项目业务类型。")
    private String projectBusinessType;
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
    @Schema(description = "关联管理类项目 ID（执行类项目专用）。")
    private Long managementProjectId;
    @Schema(description = "参与人员用户 ID 列表。")
    private List<Long> participantIds;
    @Schema(description = "项目类型标签，兼容前端 type 字段。")
    private String type;
    @Schema(description = "业务部门，兼容前端 department 字段。")
    private String department;
    @Schema(description = "承建单位，兼容前端 contractor 字段。")
    private String contractor;
    @Schema(description = "业务主管，兼容前端 supervisor 字段。")
    private String supervisor;
    @Schema(description = "回款金额，兼容前端 amount 字段。")
    private BigDecimal amount;
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