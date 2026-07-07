package com.jitong.projectflow.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("pf_task")
public class TaskEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long projectId;

    private String name;

    private String roleName;

    private String priority;

    private String status;

    private Long assigneeId;

    private LocalDate plannedStartDate;

    private LocalDate plannedEndDate;

    private LocalDate actualStartDate;

    private LocalDate actualEndDate;

    private String description;

    private String tags;

    private String remark;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
