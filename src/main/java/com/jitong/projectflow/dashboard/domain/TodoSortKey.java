package com.jitong.projectflow.dashboard.domain;

import java.time.LocalDate;
import java.util.Comparator;

public record TodoSortKey(int priorityRank, long overdueDaysDesc, LocalDate plannedEndDate, Long sequenceId) implements Comparable<TodoSortKey> {
    private static final Comparator<LocalDate> DATE_NULLS_LAST = Comparator.nullsLast(LocalDate::compareTo);

    @Override
    public int compareTo(TodoSortKey other) {
        int priority = Integer.compare(this.priorityRank, other.priorityRank);
        if (priority != 0) {
            return priority;
        }
        int overdue = Long.compare(other.overdueDaysDesc, this.overdueDaysDesc);
        if (overdue != 0) {
            return overdue;
        }
        int date = DATE_NULLS_LAST.compare(this.plannedEndDate, other.plannedEndDate);
        if (date != 0) {
            return date;
        }
        return Long.compare(this.sequenceId, other.sequenceId);
    }
}
