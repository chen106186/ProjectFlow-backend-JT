package com.jitong.projectflow.notice.service;

import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyWorkNoticeServiceTest {
    @Mock TaskMapper taskMapper;
    @Mock BugMapper bugMapper;
    @Mock NoticeService noticeService;
    @Mock SystemUserMapper systemUserMapper;
    @InjectMocks DailyWorkNoticeService service;

    @Test
    void dailySummaryIsScheduledUsingConfiguredCron() throws Exception {
        Scheduled scheduled = DailyWorkNoticeService.class.getMethod("sendDailyWorkNotice").getAnnotation(Scheduled.class);
        assertThat(scheduled).isNotNull();
        assertThat(scheduled.cron()).contains("projectflow.notice.daily-work-cron");
        assertThat(scheduled.zone()).isEqualTo("Asia/Shanghai");
    }

    @Test
    void sameDayRetryIsSkippedButFollowingDayCanSendAgain() {
        LocalDate today = LocalDate.of(2026, 10, 19);
        SystemUser user = new SystemUser();
        user.setId(1001L);
        user.setRealName("张三");
        when(systemUserMapper.selectList(any())).thenReturn(List.of(user));
        when(taskMapper.selectCount(any())).thenReturn(1L);
        when(bugMapper.selectCount(any())).thenReturn(2L);
        when(noticeService.existsBusinessNotice(1001L, NoticeType.SYSTEM, "今日工作提醒（2026-10-19）",
                "DailyWorkSummary", today.toEpochDay())).thenReturn(false, true);

        assertThat(service.sendDailyWorkNotice(today)).isEqualTo(1);
        assertThat(service.sendDailyWorkNotice(today)).isZero();
        assertThat(service.sendDailyWorkNotice(today.plusDays(1))).isEqualTo(1);

        verify(noticeService).create(1001L, NoticeType.SYSTEM, "今日工作提醒（2026-10-19）",
                "早安，张三！今日共有 5 项工作待处理。　逾期任务 1 个　｜　即将到期任务 1 个　｜　进行中任务 1 个　｜　待处理缺陷 2 个",
                "DailyWorkSummary", today.toEpochDay());
        verify(taskMapper, times(6)).selectCount(any());
    }

    @Test
    void usersWithoutPendingWorkDoNotReceiveEmptySummaries() {
        SystemUser user = new SystemUser();
        user.setId(1001L);
        when(systemUserMapper.selectList(any())).thenReturn(List.of(user));

        assertThat(service.sendDailyWorkNotice(LocalDate.of(2026, 10, 19))).isZero();

        verify(noticeService, never()).create(any(), any(), any(), any(), any(), any());
    }
}
