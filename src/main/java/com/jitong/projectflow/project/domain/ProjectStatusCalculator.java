package com.jitong.projectflow.project.domain;

import com.jitong.projectflow.project.entity.ProjectNodeEntity;

import java.time.LocalDate;
import java.util.List;

public class ProjectStatusCalculator {

    public String computeFromNodes(List<ProjectNodeEntity> nodes) {
        return computeFromNodes(nodes, null, LocalDate.now());
    }

    public String computeFromNodes(List<ProjectNodeEntity> nodes, LocalDate plannedEndDate) {
        return computeFromNodes(nodes, plannedEndDate, LocalDate.now());
    }

    public String computeFromNodes(List<ProjectNodeEntity> nodes, LocalDate plannedEndDate, LocalDate today) {
        if (nodes == null || nodes.isEmpty()) return "NOT_STARTED";

        boolean anyStarted = nodes.stream().anyMatch(n ->
                (n.getActualStartDate() != null && !n.getActualStartDate().isAfter(today))
                        || (n.getActualEndDate() != null && !n.getActualEndDate().isAfter(today)));

        ProjectNodeEntity completionNode = nodes.stream()
                .filter(n -> "COMPLETION".equals(n.getNodeCode()))
                .findFirst().orElse(null);

        LocalDate deadline = completionNode == null ? null : completionNode.getPlannedEndDate();
        if (deadline == null) deadline = plannedEndDate;
        if (deadline == null) deadline = nodes.stream().map(ProjectNodeEntity::getPlannedEndDate)
                .filter(java.util.Objects::nonNull).max(LocalDate::compareTo).orElse(null);

        boolean completed = completionNode != null
                ? completionNode.getActualEndDate() != null && !completionNode.getActualEndDate().isAfter(today)
                : nodes.stream().allMatch(n -> n.getActualEndDate() != null && !n.getActualEndDate().isAfter(today));
        if (completed) {
            LocalDate actualEnd = completionNode != null ? completionNode.getActualEndDate()
                    : nodes.stream().map(ProjectNodeEntity::getActualEndDate).max(LocalDate::compareTo).orElseThrow();
            return deadline != null && actualEnd.isAfter(deadline) ? "OVERDUE_COMPLETED" : "COMPLETED";
        }
        if (deadline != null) {
            if (today.isAfter(deadline)) return "OVERDUE";
            if (!today.isBefore(deadline.minusDays(7))) return "DUE_SOON";
        }

        return anyStarted ? "IN_PROGRESS" : "NOT_STARTED";
    }
}
