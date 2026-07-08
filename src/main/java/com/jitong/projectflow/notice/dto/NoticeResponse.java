package com.jitong.projectflow.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NoticeResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "通知类型。")
    private String noticeType;
    @Schema(description = "标题。")
    private String title;
    @Schema(description = "内容。")
    private String content;
    @Schema(description = "业务类型，例如 PROJECT、TASK、BUG、REQUIREMENT。")
    private String businessType;
    @Schema(description = "业务数据 ID。")
    private Long businessId;
    @Schema(description = "是否已读。")
    private boolean read;
    @Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime createdAt;
    @Schema(description = "阅读时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime readAt;
}
