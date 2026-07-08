package com.jitong.projectflow.daily.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class DailyReportQueryRequest extends PageQuery {
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "日报填写人 ID。")
    private Long reporterId;
    @Schema(description = "开始日期，格式 yyyy-MM-dd。")
    private LocalDate dateFrom;
    @Schema(description = "结束日期，格式 yyyy-MM-dd。")
    private LocalDate dateTo;
    @Schema(description = "关键字，支持按名称、标题或内容模糊查询。")
    private String keyword;
}
