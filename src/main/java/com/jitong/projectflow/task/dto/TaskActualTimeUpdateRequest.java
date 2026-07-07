package com.jitong.projectflow.task.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskActualTimeUpdateRequest {
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private String remark;
}
