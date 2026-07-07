package com.jitong.projectflow.notice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NoticeResponse {
    private Long id;
    private String noticeType;
    private String title;
    private String content;
    private String businessType;
    private Long businessId;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
