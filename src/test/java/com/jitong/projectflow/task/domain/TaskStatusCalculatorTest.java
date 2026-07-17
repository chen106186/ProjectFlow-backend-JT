package com.jitong.projectflow.task.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TaskStatusCalculatorTest {
    private final TaskStatusCalculator calculator = new TaskStatusCalculator();

    @Test
    void returnsCompletedWhenActualEndExists() {
        TaskStatus status = calculator.calculate(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1), LocalDate.now().minusDays(2), LocalDate.now(), false, LocalDate.now());
        assertThat(status).isEqualTo(TaskStatus.COMPLETED);
    }

    @Test
    void returnsOverdueWhenPastPlannedEndAndNotCompleted() {
        TaskStatus status = calculator.calculate(null, LocalDate.of(2026, 7, 1), null, null, false, LocalDate.of(2026, 7, 6));
        assertThat(status).isEqualTo(TaskStatus.OVERDUE);
    }

    @Test
    void returnsDueSoonWithinThreeDays() {
        TaskStatus status = calculator.calculate(null, LocalDate.of(2026, 7, 8), null, null, false, LocalDate.of(2026, 7, 6));
        assertThat(status).isEqualTo(TaskStatus.DUE_SOON);
    }

    @Test
    void returnsDueSoonOnBoundaryExactlyThreeDaysOut() {
        TaskStatus status = calculator.calculate(null, LocalDate.of(2026, 7, 9), null, null, false, LocalDate.of(2026, 7, 6));
        assertThat(status).isEqualTo(TaskStatus.DUE_SOON);
    }

    @Test
    void returnsPausedWhenPausedFlagSet() {
        TaskStatus status = calculator.calculate(null, LocalDate.of(2026, 7, 1), null, null, true, LocalDate.of(2026, 7, 6));
        assertThat(status).isEqualTo(TaskStatus.PAUSED);
    }

    @Test
    void returnsPausedEvenWhenOverdue() {
        TaskStatus status = calculator.calculate(null, LocalDate.of(2026, 7, 1), null, null, true, LocalDate.of(2026, 7, 10));
        assertThat(status).isEqualTo(TaskStatus.PAUSED);
    }

    @Test
    void returnsInProgressWhenStartedAndNotDueSoon() {
        TaskStatus status = calculator.calculate(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 7, 1), null, false, LocalDate.of(2026, 7, 6));
        assertThat(status).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void returnsNotStartedWhenNoStartDateAndNoDeadline() {
        TaskStatus status = calculator.calculate(null, null, null, null, false, LocalDate.of(2026, 7, 6));
        assertThat(status).isEqualTo(TaskStatus.NOT_STARTED);
    }

    @Test
    void returnsInProgressWhenStartedAndNullDeadline() {
        TaskStatus status = calculator.calculate(null, null, LocalDate.of(2026, 7, 1), null, false, LocalDate.of(2026, 7, 6));
        assertThat(status).isEqualTo(TaskStatus.IN_PROGRESS);
    }
}
