package com.jitong.projectflow.project.domain;

import java.time.LocalDate;

/**
 * 甘特图节点状态计算器（纯逻辑，无 Spring 依赖）。
 * 判断优先级固定：实际结束时间 → 实际开始时间 → 当前时间与计划结束时间的关系。
 */
public class ProjectNodeStatusCalculator {

    public ProjectNodeStatus calculate(
            LocalDate plannedStartDate,
            LocalDate plannedEndDate,
            LocalDate actualStartDate,
            LocalDate actualEndDate,
            LocalDate today) {

        // 1. 实际结束时间已到达（<= 今天）→ 已完成 or 逾期完成
        if (actualEndDate != null && !actualEndDate.isAfter(today)) {
            if (plannedEndDate != null && actualEndDate.isAfter(plannedEndDate)) {
                return ProjectNodeStatus.OVERDUE_COMPLETED;
            }
            return ProjectNodeStatus.COMPLETED;
        }

        // 2. 实际开始时间为空 → 未开始 or 启动逾期
        if (actualStartDate == null) {
            if (plannedStartDate != null && today.isAfter(plannedStartDate)) {
                return ProjectNodeStatus.OVERDUE_START;
            }
            return ProjectNodeStatus.NOT_STARTED;
        }

        // 3. 已开始未结束 → 根据计划结束时间判断
        if (plannedEndDate == null) {
            return ProjectNodeStatus.IN_PROGRESS;
        }
        if (today.isAfter(plannedEndDate)) {
            return ProjectNodeStatus.OVERDUE;
        }
        if (!today.isBefore(plannedEndDate.minusDays(7))) {
            return ProjectNodeStatus.DUE_SOON;
        }
        return ProjectNodeStatus.IN_PROGRESS;
    }
}
