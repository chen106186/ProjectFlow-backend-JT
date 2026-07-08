package com.jitong.projectflow.bug.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BugCommentResponse {
    @Schema(description = "主键 ID。")
    private Long id;
    @Schema(description = "Bug ID。")
    private Long bugId;
    @Schema(description = "用户 ID。")
    private Long userId;
    @Schema(description = "内容。")
    private String content;
    @Schema(description = "创建时间，格式 yyyy-MM-dd HH:mm:ss。")
    private LocalDateTime createdAt;
}
