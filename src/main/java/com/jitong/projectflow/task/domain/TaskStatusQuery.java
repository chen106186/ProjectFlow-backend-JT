package com.jitong.projectflow.task.domain;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.task.entity.TaskEntity;

import java.time.LocalDate;

/** SQL equivalent of TaskStatusCalculator, shared by lists and statistics. */
public final class TaskStatusQuery {
    private TaskStatusQuery() { }

    public static void apply(LambdaQueryWrapper<TaskEntity> query, TaskStatus status, LocalDate today) {
        if (status == TaskStatus.PAUSED) {
            query.eq(TaskEntity::getStatus, status.name());
            return;
        }
        query.ne(TaskEntity::getStatus, TaskStatus.PAUSED.name());
        if (status == TaskStatus.COMPLETED) {
            query.le(TaskEntity::getActualEndDate, today);
            return;
        }
        query.and(q -> q.isNull(TaskEntity::getActualEndDate).or().gt(TaskEntity::getActualEndDate, today));
        switch (status) {
            case OVERDUE -> query.lt(TaskEntity::getPlannedEndDate, today);
            case DUE_SOON -> query.ge(TaskEntity::getPlannedEndDate, today)
                    .le(TaskEntity::getPlannedEndDate, today.plusDays(3));
            case IN_PROGRESS, NOT_STARTED -> {
                query.and(q -> q.isNull(TaskEntity::getPlannedEndDate).or().gt(TaskEntity::getPlannedEndDate, today.plusDays(3)));
                if (status == TaskStatus.IN_PROGRESS) {
                    query.le(TaskEntity::getActualStartDate, today);
                } else {
                    query.and(q -> q.isNull(TaskEntity::getActualStartDate).or().gt(TaskEntity::getActualStartDate, today));
                }
            }
            default -> throw new IllegalArgumentException("Unsupported task status: " + status);
        }
    }
}
