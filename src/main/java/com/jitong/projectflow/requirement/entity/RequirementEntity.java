package com.jitong.projectflow.requirement.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("pf_requirement")
public class RequirementEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long requirementNo;

    private Long projectId;

    private Long reviewerId;

    private String title;

    private String requirementType;

    private String status;

    private String priority;

    private String description;

    private String tags;

    private Long createdBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    private Long updatedBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
