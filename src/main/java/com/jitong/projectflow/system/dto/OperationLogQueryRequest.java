package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OperationLogQueryRequest extends PageQuery {
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
    @Schema(description = "开始时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime startTime;
    @Schema(description = "结束时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime endTime;
}
