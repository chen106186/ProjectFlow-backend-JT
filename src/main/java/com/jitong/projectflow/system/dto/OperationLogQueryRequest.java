package com.jitong.projectflow.system.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OperationLogQueryRequest extends PageQuery {
    @Schema(description = "操作模块，例如 bug、task、project。")
    private String module;
    @Schema(description = "业务类型，例如 Bug、Task、Project。")
    private String businessType;
    @Schema(description = "业务数据 ID。")
    private Long businessId;
    @Schema(description = "操作类型，例如 CREATE、UPDATE、DELETE。")
    private String operationType;
    @Schema(description = "操作人 ID。")
    private Long operatorId;
    @Schema(description = "操作人姓名（模糊匹配）。")
    private String operatorName;
    @Schema(description = "关键字，匹配日志内容。")
    private String keyword;
    @Schema(description = "开始日期，格式 yyyy-MM-dd。")
    private LocalDate startDate;
    @Schema(description = "结束日期，格式 yyyy-MM-dd。")
    private LocalDate endDate;
}
