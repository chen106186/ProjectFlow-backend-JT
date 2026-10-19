package com.jitong.projectflow.project.domain;

import com.jitong.projectflow.project.entity.ProjectNodeEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectStatusCalculatorTest {
    private final ProjectStatusCalculator calculator = new ProjectStatusCalculator();
    private final LocalDate today = LocalDate.of(2026, 10, 19);

    @Test
    void executionStagesCanCompleteWithoutCompletionNode() {
        var nodes = List.of(node("DEVELOPMENT", today.minusDays(3)), node("TESTING", today), node("DEPLOYMENT_IMPLEMENTATION", today));
        assertThat(calculator.computeFromNodes(nodes, today, today)).isEqualTo("COMPLETED");
        assertThat(calculator.computeFromNodes(nodes, today.minusDays(1), today)).isEqualTo("OVERDUE_COMPLETED");
    }

    @Test
    void futureActualDatesDoNotCompleteOrStartProject() {
        var node = node("DEVELOPMENT", today.plusDays(2));
        node.setActualStartDate(today.plusDays(1));
        assertThat(calculator.computeFromNodes(List.of(node), today.plusDays(20), today)).isEqualTo("NOT_STARTED");
    }

    @Test
    void unfinishedProjectUsesProjectDeadlineThenLatestStageDeadline() {
        var first = node("DEVELOPMENT", null);
        var second = node("TESTING", null);
        first.setPlannedEndDate(today.minusDays(1));
        second.setPlannedEndDate(today.plusDays(10));
        assertThat(calculator.computeFromNodes(List.of(first, second), today.minusDays(1), today)).isEqualTo("OVERDUE");
        assertThat(calculator.computeFromNodes(List.of(first, second), null, today)).isEqualTo("IN_PROGRESS");
    }

    @Test
    void explicitCompletionStageStillControlsProjectCompletion() {
        var development = node("DEVELOPMENT", today);
        var completion = node("COMPLETION", null);
        completion.setPlannedEndDate(today.plusDays(2));
        assertThat(calculator.computeFromNodes(List.of(development, completion), null, today)).isEqualTo("DUE_SOON");
        completion.setActualEndDate(today.plusDays(1));
        assertThat(calculator.computeFromNodes(List.of(development, completion), null, today)).isEqualTo("DUE_SOON");
        completion.setActualEndDate(today);
        assertThat(calculator.computeFromNodes(List.of(development, completion), null, today)).isEqualTo("COMPLETED");
    }

    private ProjectNodeEntity node(String code, LocalDate actualEnd) {
        var node = new ProjectNodeEntity();
        node.setNodeCode(code);
        node.setActualStartDate(today.minusDays(5));
        node.setActualEndDate(actualEnd);
        return node;
    }
}
