package com.jitong.projectflow.project.domain;

import com.jitong.projectflow.project.entity.ProjectNodeEntity;

import java.time.LocalDate;
import java.util.List;

public class ProjectStatusCalculator {

    public String computeFromNodes(List<ProjectNodeEntity> nodes) {
        if (nodes == null || nodes.isEmpty()) return "NOT_STARTED";

        boolean anyStarted = nodes.stream().anyMatch(n -> n.getActualStartDate() != null);
        LocalDate today = LocalDate.now();

        ProjectNodeEntity completionNode = nodes.stream()
                .filter(n -> "COMPLETION".equals(n.getNodeCode()))
                .findFirst().orElse(null);

        if (completionNode != null && anyStarted) {
            LocalDate actualEnd = completionNode.getActualEndDate();
            LocalDate plannedEnd = completionNode.getPlannedEndDate();
            if (actualEnd != null) {
                return (plannedEnd != null && actualEnd.isAfter(plannedEnd)) ? "OVERDUE_COMPLETED" : "COMPLETED";
            }
            if (plannedEnd != null) {
                if (today.isAfter(plannedEnd)) return "OVERDUE";
                if (!today.isBefore(plannedEnd.minusDays(7))) return "DUE_SOON";
            }
        }

        return anyStarted ? "IN_PROGRESS" : "NOT_STARTED";
    }
}
