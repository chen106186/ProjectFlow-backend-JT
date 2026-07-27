package com.jitong.projectflow.task.domain;

import java.time.LocalDate;

public class TaskStatusCalculator {
    public TaskStatus calculate(LocalDate plannedEndDate, LocalDate actualStartDate, LocalDate actualEndDate, boolean paused, LocalDate today) {
        return calculate(null, plannedEndDate, actualStartDate, actualEndDate, paused, today);
    }

    public TaskStatus calculate(LocalDate plannedStartDate, LocalDate plannedEndDate, LocalDate actualStartDate, LocalDate actualEndDate, boolean paused, LocalDate today) {
        if (paused) {
            return TaskStatus.PAUSED;
        }
        if (actualEndDate != null && !actualEndDate.isAfter(today)) {
            return TaskStatus.COMPLETED;
        }
        if (actualStartDate == null) {
            return TaskStatus.NOT_STARTED;
        }
        if (plannedEndDate != null && today.isAfter(plannedEndDate)) {
            return TaskStatus.OVERDUE;
        }
        if (plannedEndDate != null && !today.isBefore(plannedEndDate.minusDays(3))) {
            return TaskStatus.DUE_SOON;
        }
        return TaskStatus.IN_PROGRESS;
    }
}
