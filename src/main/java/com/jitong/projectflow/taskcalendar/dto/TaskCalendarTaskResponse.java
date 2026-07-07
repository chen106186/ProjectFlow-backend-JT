package com.jitong.projectflow.taskcalendar.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class TaskCalendarTaskResponse {
    private Long id;
    private Long projectId;
    private String name;
    private String priority;
    private String status;
    private Long assigneeId;
    private LocalDate plannedStartDate;
    private LocalDate plannedEndDate;
    private LocalDate actualStartDate;
    private LocalDate actualEndDate;
    private Integer overdueDays;
    private Integer remainingDays;
}
