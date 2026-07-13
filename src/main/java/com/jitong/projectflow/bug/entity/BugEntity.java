package com.jitong.projectflow.bug.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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

    private LocalDateTime closedAt;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
