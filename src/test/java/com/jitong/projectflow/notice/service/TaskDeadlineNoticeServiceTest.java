package com.jitong.projectflow.notice.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.task.domain.TaskStatus;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskDeadlineNoticeServiceTest {

    @Mock
    private TaskMapper taskMapper;

    @Mock
    private NoticeService noticeService;

    @InjectMocks
    private TaskDeadlineNoticeService service;

    @Test
    void scanTaskDeadlinesCreatesDueSoonAndOverdueNotices() {
        LocalDate today = LocalDate.of(2026, 7, 8);
        TaskEntity dueSoon = task(1L, "接口联调", 1001L, today.plusDays(2), TaskStatus.IN_PROGRESS);
        TaskEntity overdue = task(2L, "缺陷修复", 1002L, today.minusDays(1), TaskStatus.DUE_SOON);
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of(dueSoon, overdue));

        int created = service.scanTaskDeadlines(today);

        assertThat(created).isEqualTo(2);
        verify(noticeService).create(1001L, NoticeType.PROJECT_WARNING, "任务即将到期",
                "接口联调 将于 2026-07-10 到期，请及时推进。", "Task", 1L);
        verify(noticeService).create(1002L, NoticeType.PROJECT_WARNING, "任务已逾期",
                "缺陷修复 已于 2026-07-07 逾期，请尽快处理。", "Task", 2L);
    }

    @Test
    void scanTaskDeadlinesSkipsExistingBusinessNotice() {
        LocalDate today = LocalDate.of(2026, 7, 8);
        TaskEntity task = task(1L, "接口联调", 1001L, today.plusDays(1), TaskStatus.IN_PROGRESS);
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of(task));
        when(noticeService.existsBusinessNotice(1001L, NoticeType.PROJECT_WARNING,
                "任务即将到期", "Task", 1L)).thenReturn(true);

        int created = service.scanTaskDeadlines(today);

        assertThat(created).isZero();
        verify(noticeService, never()).create(any(), any(), any(), any(), any(), any());
    }

    @Test
    void scanTaskDeadlinesQueriesActiveTasksWithinReminderWindow() {
        LocalDate today = LocalDate.of(2026, 7, 8);
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
        ArgumentCaptor<Wrapper<TaskEntity>> captor = ArgumentCaptor.forClass(Wrapper.class);

        service.scanTaskDeadlines(today);

        verify(taskMapper).selectList(captor.capture());
        assertThat(captor.getValue()).isNotNull();
        verify(noticeService, never()).create(eq(1001L), any(), any(), any(), any(), any());
    }

    private TaskEntity task(Long id, String name, Long assigneeId, LocalDate plannedEndDate, TaskStatus status) {
        TaskEntity task = new TaskEntity();
        task.setId(id);
        task.setName(name);
        task.setAssigneeId(assigneeId);
        task.setPlannedEndDate(plannedEndDate);
        task.setStatus(status.name());
        return task;
    }
}
