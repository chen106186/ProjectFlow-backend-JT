package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OperationLogResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "操作模块。")
    private String module;
    @Schema(description = "业务类型，例如 PROJECT、TASK、BUG、REQUIREMENT。")
    private String businessType;
    @Schema(description = "业务数据 ID。")
    private Long businessId;
    @Schema(description = "操作类型，例如 CREATE、UPDATE、DELETE、EXPORT。")
    private String operationType;
    @Schema(description = "操作人 ID。")
    private Long operatorId;
    @Schema(description = "操作人姓名。")
    private String operatorName;
    @Schema(description = "内容。")
    private String content;
    @Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime createdAt;
}
