package com.jitong.projectflow.dashboard.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "首页待办事项响应。")
public record TodoItemResponse(
        @Schema(description = "待办类型，例如 TASK、BUG。")
        String itemType,

        @Schema(description = "关联业务数据 ID。")
        @JsonSerialize(using = ToStringSerializer.class)
        Long businessId,

        @Schema(description = "待办标题。")
        String title,

        @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
        String priority,

        @Schema(description = "状态。")
        String status,

        @Schema(description = "所属项目名称。")
        String projectName,

        @Schema(description = "负责人姓名。")
        String ownerName,

        @Schema(description = "计划结束日期，格式 yyyy-MM-dd。")
        LocalDate plannedEndDate,

        @Schema(description = "逾期天数，未逾期时为 0。")
        long overdueDays) {
}
