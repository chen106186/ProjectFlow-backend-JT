package com.jitong.projectflow.project.domain;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class GanttNodeSummaryCalculatorTest {
    private final GanttNodeSummaryCalculator calculator = new GanttNodeSummaryCalculator();
    private final LocalDate TODAY = LocalDate.of(2026, 7, 7);

    @Test
    void completedNodeCountedWhenActualEndDatePresent() {
        GanttSummaryData result = calculator.calculate(
            List.of("IN_PROGRESS"),
            List.of(TODAY.minusDays(1)),   // actualEndDate
            List.of(TODAY.plusDays(5)),     // plannedEndDate
            List.of(100),
            TODAY
        );
        assertThat(result.completed()).isEqualTo(1);
        assertThat(result.overdue()).isEqualTo(0);
    }

    @Test
    void completedNodeCountedWhenStatusIsCompleted() {
        GanttSummaryData result = calculator.calculate(
            List.of("COMPLETED"),
            Arrays.asList((LocalDate) null),
            List.of(TODAY.plusDays(5)),
            List.of(100),
            TODAY
        );
        assertThat(result.completed()).isEqualTo(1);
    }

    @Test
    void overdueNodeDetected() {
        GanttSummaryData result = calculator.calculate(
            List.of("IN_PROGRESS"),
            Arrays.asList((LocalDate) null),
            List.of(TODAY.minusDays(1)),
            List.of(50),
            TODAY
        );
        assertThat(result.overdue()).isEqualTo(1);
        assertThat(result.completed()).isEqualTo(0);
    }

    @Test
    void dueSoonNodeDetectedWithinSevenDays() {
        GanttSummaryData result = calculator.calculate(
            List.of("IN_PROGRESS"),
            Arrays.asList((LocalDate) null),
            List.of(TODAY.plusDays(3)),
            List.of(60),
            TODAY
        );
        assertThat(result.dueSoon()).isEqualTo(1);
        assertThat(result.overdue()).isEqualTo(0);
    }

    @Test
    void dueSoonNodeOnBoundaryExactlySevenDays() {
        GanttSummaryData result = calculator.calculate(
            List.of("IN_PROGRESS"),
            Arrays.asList((LocalDate) null),
            List.of(TODAY.plusDays(7)),
            List.of(60),
            TODAY
        );
        assertThat(result.dueSoon()).isEqualTo(1);
    }

    @Test
    void overallProgressIsAverageRoundedDown() {
        GanttSummaryData result = calculator.calculate(
            List.of("IN_PROGRESS", "IN_PROGRESS", "IN_PROGRESS"),
            Arrays.asList(null, null, null),
            Arrays.asList(null, null, null),
            List.of(10, 20, 30),
            TODAY
        );
        assertThat(result.overallProgress()).isEqualTo(20); // (10+20+30)/3 = 20
    }

    @Test
    void emptyNodeListReturnsZeros() {
        GanttSummaryData result = calculator.calculate(
            List.of(), List.of(), List.of(), List.of(), TODAY
        );
        assertThat(result.total()).isEqualTo(0);
        assertThat(result.overallProgress()).isEqualTo(0);
    }
}
