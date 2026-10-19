package com.jitong.projectflow.bug.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.jitong.projectflow.common.api.PageQuery;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class BugQueryRequest extends PageQuery {
    @Schema(description = "状态。")
    private String status;
    @Schema(description = "优先级，例如 LOW、MEDIUM、HIGH、URGENT。")
    private String priority;
    @Schema(description = "项目 ID。")
    private Long projectId;
    @Schema(description = "关联任务 ID。")
    private Long taskId;
    @Schema(description = "关键字，支持按名称、标题或内容模糊查询。")
    private String keyword;
    @Schema(description = "指定处理人用户 ID。")
    private Long assigneeId;
    @Schema(description = "创建人用户 ID。")
    private Long creatorId;
    @Schema(description = "仅查询本人创建或本人负责的 Bug，即使有全量查看权限也生效。")
    private Boolean mineOnly;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "创建日期开始，含当天，格式 yyyy-MM-dd。")
    private LocalDate createdFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "创建日期结束，含当天，格式 yyyy-MM-dd。")
    private LocalDate createdTo;
}
