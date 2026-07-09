package com.jitong.projectflow.daily.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "创建日报请求。")
public class DailyReportCreateRequest {
    @Schema(description = "项目ID（可选）。")
    private Long projectId;

    @NotNull(message = "日报日期不能为空")
    @PastOrPresent(message = "日报日期不能是未来日期")
    @Schema(description = "日报日期，格式 yyyy-MM-dd。")
    private LocalDate reportDate;

    @NotBlank(message = "日报内容不能为空")
    @Schema(description = "日报内容。")
    private String content;

    @Schema(description = "关联任务 ID 列表（可选）。")
    private java.util.List<Long> relatedTaskIds;
}
