package com.jitong.projectflow.taskcalendar.dto;

import lombok.Builder;
import lombok.Data;

import java.time.YearMonth;
import java.util.List;

@Data
@Builder
public class TaskCalendarMonthResponse {
    private YearMonth month;
    private List<TaskCalendarDayResponse> days;
}
