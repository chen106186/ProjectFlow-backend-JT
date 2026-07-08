package com.jitong.projectflow.daily.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "创建日报请求。")
public class DailyReportCreateRequest {
    @NotNull(message = "项目ID不能为空")
    @Schema(description = "项目ID。")
    private Long projectId;

    @NotNull(message = "日报日期不能为空")
    @Schema(description = "日报日期，格式 yyyy-MM-dd。")
    private LocalDate reportDate;

    @NotBlank(message = "日报内容不能为空")
    @Schema(description = "日报内容。")
    private String content;
}
