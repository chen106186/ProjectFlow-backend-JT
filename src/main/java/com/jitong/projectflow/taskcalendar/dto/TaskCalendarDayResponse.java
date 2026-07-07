package com.jitong.projectflow.taskcalendar.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class TaskCalendarDayResponse {
    private LocalDate date;
    private int totalCount;
    private List<TaskCalendarTaskResponse> tasks;
}
