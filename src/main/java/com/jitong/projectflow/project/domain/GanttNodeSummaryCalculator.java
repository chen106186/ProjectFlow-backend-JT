package com.jitong.projectflow.project.domain;

import java.time.LocalDate;
import java.util.List;

public class GanttNodeSummaryCalculator {

    /**
     * @param statuses       list of status strings for all nodes in the project
     * @param actualEndDates list of actualEndDate for each node (may be null)
     * @param plannedEndDates list of plannedEndDate for each node (may be null)
     * @param progressPercents list of progressPercent for each node
     * @param today          reference date for overdue/due-soon calculations
     */
    public GanttSummaryData calculate(
            List<String> statuses,
            List<LocalDate> actualEndDates,
            List<LocalDate> plannedEndDates,
            List<Integer> progressPercents,
            LocalDate today) {

        int total = statuses.size();
        if (total == 0) {
            return new GanttSummaryData(0, 0, 0, 0, 0);
        }

        int completed = 0;
        int overdue = 0;
        int dueSoon = 0;
        int progressSum = 0;

        for (int i = 0; i < total; i++) {
            LocalDate actualEnd = actualEndDates.get(i);
            LocalDate plannedEnd = plannedEndDates.get(i);
            String status = statuses.get(i);
            int progress = progressPercents.get(i);

            progressSum += progress;

            // completed: 节点状态为已完成或逾期完成（已含实际结束时间 <= 今天的判断）
            if ("COMPLETED".equals(status) || "OVERDUE_COMPLETED".equals(status)) {
                completed++;
            }

            // overdue: actualEndDate == null AND plannedEndDate != null AND today is after plannedEndDate
            if (actualEnd == null && plannedEnd != null && today.isAfter(plannedEnd)) {
                overdue++;
            }

            // dueSoon: actualEndDate == null AND plannedEndDate != null AND !today.isAfter(plannedEnd) AND plannedEnd <= today + 7
            if (actualEnd == null && plannedEnd != null && !today.isAfter(plannedEnd)
                    && !plannedEnd.isAfter(today.plusDays(7))) {
                dueSoon++;
            }
        }

        int overallProgress = Math.floorDiv(progressSum, total);

        return new GanttSummaryData(total, completed, overdue, dueSoon, overallProgress);
    }
}
