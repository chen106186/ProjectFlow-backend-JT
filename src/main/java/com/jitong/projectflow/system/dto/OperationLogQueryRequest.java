package com.jitong.projectflow.system.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogQueryRequest {
    private String module;
    private String businessType;
    private Long businessId;
    private String operationType;
    private Long operatorId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
