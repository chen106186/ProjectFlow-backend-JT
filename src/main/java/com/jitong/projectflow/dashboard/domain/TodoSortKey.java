package com.jitong.projectflow.dashboard.domain;

import java.time.LocalDate;

public record TodoSortKey(int priorityRank, long overdueDaysDesc, LocalDate plannedEndDate, Long sequenceId) implements Comparable<TodoSortKey> {
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
        int date = this.plannedEndDate.compareTo(other.plannedEndDate);
        if (date != 0) {
            return date;
        }
        return this.sequenceId.compareTo(other.sequenceId);
    }
}
