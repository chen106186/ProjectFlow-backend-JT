package com.jitong.projectflow.requirement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "需求操作日志响应。")
public class RequirementLogResponse {
    @Schema(description = "日志 ID。")
    private Long id;
    @Schema(description = "操作类型，如 CREATE、UPDATE、STATUS_CHANGE。")
    private String operationType;
    @Schema(description = "操作人 ID。")
    private Long operatorId;
    @Schema(description = "操作人姓名。")
    private String operatorName;
    @Schema(description = "日志内容。")
    private String content;
    @Schema(description = "操作时间。")
    private LocalDateTime createdAt;
}
