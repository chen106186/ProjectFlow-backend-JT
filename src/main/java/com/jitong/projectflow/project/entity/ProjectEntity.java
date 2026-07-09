package com.jitong.projectflow.project.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pf_project")
public class ProjectEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String projectType;
    private String projectBusinessType;
    private String name;
    private String stage;
    private String status;
    private String contractStatus;
    private String businessDepartment;
    private String contractorUnit;
    private String businessSupervisor;
    private java.math.BigDecimal receivableAmount;
    private Long managerId;
    private String description;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
