package com.jitong.projectflow.taskcalendar.service;

import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskCalendarServiceTest {
    @Mock
    TaskMapper taskMapper;

    @Test
    void dayReturnsOnlyTasksActiveOnDate() {
        TaskEntity activeRange = task(1L, "Active range", "HIGH", "IN_PROGRESS",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 3));
        TaskEntity dueOnly = task(2L, "Due only", "LOW", "NOT_STARTED",
                null, LocalDate.of(2026, 7, 2));
        TaskEntity inactive = task(3L, "Inactive", "HIGH", "NOT_STARTED",
                LocalDate.of(2026, 7, 5), LocalDate.of(2026, 7, 6));
        when(taskMapper.selectList(any())).thenReturn(List.of(activeRange, dueOnly, inactive));

        var result = new TaskCalendarService(taskMapper).day(LocalDate.of(2026, 7, 2));

        assertThat(result.getDate()).isEqualTo(LocalDate.of(2026, 7, 2));
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getTasks()).extracting("id").containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void daySortsUrgentThenOverdueThenDueSoonThenPriority() {
        TaskEntity normalHigh = task(4L, "Normal high", "HIGH", "IN_PROGRESS",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 20));
        TaskEntity dueSoon = task(3L, "Due soon", "MEDIUM", "IN_PROGRESS",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 9));
        TaskEntity overdueTwoDays = task(2L, "Overdue two days", "LOW", "IN_PROGRESS",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 5));
        TaskEntity urgent = task(1L, "Urgent", "URGENT", "NOT_STARTED",
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 30));
        when(taskMapper.selectList(any())).thenReturn(List.of(normalHigh, dueSoon, overdueTwoDays, urgent));

        var result = new TaskCalendarService(taskMapper).day(LocalDate.of(2026, 7, 7));

        assertThat(result.getTasks()).extracting("id").containsExactly(1L, 2L, 3L, 4L);
        assertThat(result.getTasks().get(1).getOverdueDays()).isEqualTo(2);
        assertThat(result.getTasks().get(2).getRemainingDays()).isEqualTo(2);
    }

    @Test
    void monthLimitsPreviewTasksToFiveButKeepsTotalCount() {
        when(taskMapper.selectList(any())).thenReturn(List.of(
                task(1L, "Task 1", "URGENT", "IN_PROGRESS", null, LocalDate.of(2026, 7, 7)),
                task(2L, "Task 2", "HIGH", "IN_PROGRESS", null, LocalDate.of(2026, 7, 7)),
                task(3L, "Task 3", "HIGH", "IN_PROGRESS", null, LocalDate.of(2026, 7, 7)),
                task(4L, "Task 4", "MEDIUM", "IN_PROGRESS", null, LocalDate.of(2026, 7, 7)),
                task(5L, "Task 5", "LOW", "IN_PROGRESS", null, LocalDate.of(2026, 7, 7)),
                task(6L, "Task 6", "LOW", "IN_PROGRESS", null, LocalDate.of(2026, 7, 7))
        ));

        var result = new TaskCalendarService(taskMapper).month(YearMonth.of(2026, 7));

        var day = result.getDays().stream()
                .filter(item -> item.getDate().equals(LocalDate.of(2026, 7, 7)))
                .findFirst()
                .orElseThrow();
        assertThat(day.getTotalCount()).isEqualTo(6);
        assertThat(day.getTasks()).hasSize(5);
        assertThat(day.getTasks()).extracting("id").containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    private TaskEntity task(Long id, String name, String priority, String status, LocalDate start, LocalDate end) {
        TaskEntity entity = new TaskEntity();
        entity.setId(id);
        entity.setProjectId(100L);
        entity.setName(name);
        entity.setPriority(priority);
        entity.setStatus(status);
        entity.setAssigneeId(200L);
        entity.setPlannedStartDate(start);
        entity.setPlannedEndDate(end);
        return entity;
    }
}
