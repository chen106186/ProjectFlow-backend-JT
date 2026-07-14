package com.jitong.projectflow.report.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pf_project_report")
public class ProjectReportEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long projectId;
    private String title;
    private String reportType;
    private String status;
    private LocalDate plannedDate;
    private LocalDate actualDate;
    private String targetAudience;
    private String locationMethod;
    private String description;
    private String remark;
    private Long relatedTaskId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer deleted;
}
