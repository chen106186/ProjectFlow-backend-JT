package com.jitong.projectflow.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class OperationLog {
    private Long id;
    private String module;
    private String businessType;
    private Long businessId;
    private String operationType;
    private Long operatorId;
    private String operatorName;
    private String beforeValue;
    private String afterValue;
    private String content;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
