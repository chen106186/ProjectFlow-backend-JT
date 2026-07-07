package com.jitong.projectflow.bug.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BugCommentResponse {
    private Long id;
    private Long bugId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
}
