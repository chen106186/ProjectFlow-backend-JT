package com.jitong.projectflow.task.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TaskStatusCalculatorTest {
    private final TaskStatusCalculator calculator = new TaskStatusCalculator();

    @Test
    void returnsCompletedWhenActualEndExists() {
        TaskStatus status = calculator.calculate(LocalDate.now().minusDays(1), LocalDate.now().minusDays(2), LocalDate.now(), false, LocalDate.now());
        assertThat(status).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void returnsOverdueWhenPastPlannedEndAndNotCompleted() {
        TaskStatus status = calculator.calculate(LocalDate.of(2026, 7, 1), null, null, false, LocalDate.of(2026, 7, 6));
        assertThat(status).isEqualTo(TaskStatus.OVERDUE);
    }

    @Test
    void returnsDueSoonWithinThreeDays() {
        TaskStatus status = calculator.calculate(LocalDate.of(2026, 7, 8), null, null, false, LocalDate.of(2026, 7, 6));
        assertThat(status).isEqualTo(TaskStatus.DUE_SOON);
    }
}
