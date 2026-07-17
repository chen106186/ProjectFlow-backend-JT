package com.jitong.projectflow.bug.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pf_bug")
public class BugEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long bugNo;

    private Long projectId;

    private Long taskId;

    private String title;

    private String status;

    private String priority;

    private Long creatorId;

    private Long assigneeId;

    private String description;

    private String reproduceSteps;

    private String fixAnalysis;

    private String fixDetail;

    private String solution;

    private LocalDate resolvedDate;

    private String resolveRemark;

    private LocalDateTime closedAt;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
