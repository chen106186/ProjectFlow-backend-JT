package com.jitong.projectflow.daily.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DailyReportResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "日报填写人 ID。")
    private Long reporterId;
    @Schema(description = "日报日期，格式 yyyy-MM-dd。")
    private LocalDate reportDate;
    @Schema(description = "内容。")
    private String content;
    @Schema(description = "创建人 ID。")
    private Long createdBy;
    @Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime createdAt;
    @Schema(description = "更新人 ID。")
    private Long updatedBy;
    @Schema(description = "更新时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime updatedAt;

    @Schema(description = "关联任务 ID 列表。")
    private java.util.List<Long> relatedTaskIds;
}
