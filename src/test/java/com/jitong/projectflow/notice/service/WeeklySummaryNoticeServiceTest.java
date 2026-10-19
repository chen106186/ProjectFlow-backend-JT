package com.jitong.projectflow.notice.service;

import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeeklySummaryNoticeServiceTest {
    @Mock TaskMapper taskMapper;
    @Mock NoticeService noticeService;
    @InjectMocks WeeklySummaryNoticeService service;

    @Test
    void weeklySummaryIsScheduledUsingConfiguredCron() throws Exception {
        Scheduled scheduled = WeeklySummaryNoticeService.class.getMethod("sendWeeklySummary").getAnnotation(Scheduled.class);
        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).contains("projectflow.notice.weekly-summary-cron");
        assertThat(scheduled.zone()).isEqualTo("Asia/Shanghai");
    }

    @Test
    void weeklyRetryUsesSameReportWeekAndDoesNotResend() {
        LocalDate monday = LocalDate.of(2026, 10, 19);
        LocalDate reportStart = LocalDate.of(2026, 10, 12);
        TaskEntity task = new TaskEntity();
        task.setAssigneeId(1001L);
        when(taskMapper.selectList(any())).thenReturn(List.of(task, task));
        when(taskMapper.selectCount(any())).thenReturn(2L, 1L);
        when(noticeService.existsBusinessNotice(1001L, NoticeType.SYSTEM, "任务周报（2026-10-12 至 2026-10-18）",
                "WeeklySummary", reportStart.toEpochDay())).thenReturn(false, true);

        assertThat(service.sendWeeklySummary(monday)).isEqualTo(1);
        assertThat(service.sendWeeklySummary(monday.plusDays(1))).isZero();

        verify(noticeService).create(1001L, NoticeType.SYSTEM, "任务周报（2026-10-12 至 2026-10-18）",
                "上周已完成任务 2 个，当前逾期任务 1 个", "WeeklySummary", reportStart.toEpochDay());
    }
}
