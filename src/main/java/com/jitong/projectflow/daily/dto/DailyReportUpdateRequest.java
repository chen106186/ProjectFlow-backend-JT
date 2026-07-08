package com.jitong.projectflow.daily.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyReportUpdateRequest {
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "日报日期，格式 yyyy-MM-dd。")
    private LocalDate reportDate;
    @NotBlank
    @Schema(description = "内容。")
    private String content;
}
