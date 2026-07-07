package com.jitong.projectflow.daily.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DailyReportResponse {
    private Long id;
    private Long projectId;
    private Long reporterId;
    private LocalDate reportDate;
    private String content;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
}
