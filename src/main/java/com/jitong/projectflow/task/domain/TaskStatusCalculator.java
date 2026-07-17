package com.jitong.projectflow.task.domain;

import java.time.LocalDate;

public class TaskStatusCalculator {
    public TaskStatus calculate(LocalDate plannedStartDate, LocalDate plannedEndDate, LocalDate actualStartDate, LocalDate actualEndDate, boolean paused, LocalDate today) {
        if (paused) {
            return TaskStatus.PAUSED;
        }
        if (actualEndDate != null) {
            return TaskStatus.COMPLETED;
        }
        if (plannedEndDate != null && today.isAfter(plannedEndDate)) {
            return TaskStatus.OVERDUE;
        }
        if (plannedEndDate != null && !today.isBefore(plannedEndDate.minusDays(3))) {
            return TaskStatus.DUE_SOON;
        }
        if (actualStartDate != null || (plannedStartDate != null && !today.isBefore(plannedStartDate))) {
            return TaskStatus.IN_PROGRESS;
        }
        return TaskStatus.NOT_STARTED;
    }
}
