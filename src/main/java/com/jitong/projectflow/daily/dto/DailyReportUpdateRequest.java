package com.jitong.projectflow.daily.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyReportUpdateRequest {
    private Long projectId;
    private LocalDate reportDate;
    @NotBlank
    private String content;
}
