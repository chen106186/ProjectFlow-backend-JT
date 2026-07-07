package com.jitong.projectflow.system.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OperationLogResponse {
    private Long id;
    private String module;
    private String businessType;
    private Long businessId;
    private String operationType;
    private Long operatorId;
    private String operatorName;
    private String content;
    private LocalDateTime createdAt;
}
