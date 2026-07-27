package com.jitong.projectflow.project.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectCreateRequest {
    @NotBlank
    @Schema(description = "项目分类：MANAGEMENT（管理类）或 EXECUTION（执行类）。")
    private String projectType;

    @Schema(description = "项目业务类型：DIGITALIZATION（数字化）、INFORMATIZATION（信息化）、RESEARCH（科研）。")
    @JsonAlias("type")
    private String projectBusinessType;

    @NotBlank
    @Schema(description = "名称。")
    private String name;

    @Schema(description = "项目阶段。")
    private String stage;

    @Schema(description = "状态。")
    private String status;

    @Schema(description = "合同状态。")
    private String contractStatus;

    @Schema(description = "业务部门。")
    @JsonAlias("department")
    private String businessDepartment;

    @Schema(description = "承建单位。")
    @JsonAlias("contractor")
    private String contractorUnit;

    @Schema(description = "业务主管。")
    @JsonAlias("supervisor")
    private String businessSupervisor;

    @Schema(description = "回款金额（元）。")
    @JsonAlias("amount")
    private java.math.BigDecimal receivableAmount;

    @Schema(description = "项目经理用户 ID（已废弃，请使用 managerIds）。")
    private Long managerId;

    @Schema(description = "项目经理用户 ID 列表（多选，优先于 managerId）。")
    private List<Long> managerIds;

    @Schema(description = "关联管理类项目 ID（执行类项目专用）。")
    private Long managementProjectId;

    @Schema(description = "参与人员用户 ID 列表。")
    private List<Long> participantIds;

    @Schema(description = "项目节点编码列表（从模板中选取），与 nodes 二选一。")
    private List<String> nodeCodes;

    @Schema(description = "创建时内嵌的项目节点列表（自定义节点，无模板时使用），可为空。")
    private List<ProjectNodeCreateRequest> nodes;

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