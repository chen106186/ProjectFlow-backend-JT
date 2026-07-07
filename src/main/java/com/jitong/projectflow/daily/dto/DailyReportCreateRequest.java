package com.jitong.projectflow.daily.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DailyReportCreateRequest {
    @NotNull
    private Long projectId;
    @NotNull
    private LocalDate reportDate;
    @NotBlank
    private String content;
}
