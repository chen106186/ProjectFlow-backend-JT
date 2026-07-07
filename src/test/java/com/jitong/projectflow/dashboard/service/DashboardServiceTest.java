package com.jitong.projectflow.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.dashboard.dto.MyStatisticsResponse;
import com.jitong.projectflow.notice.mapper.NoticeMapper;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    TaskMapper taskMapper;

    @Mock
    BugMapper bugMapper;

    @Mock
    RequirementMapper requirementMapper;

    @Mock
    NoticeMapper noticeMapper;

    @InjectMocks
    DashboardService dashboardService;

    @SuppressWarnings("unchecked")
    @Test
    void aggregatesStatisticsCorrectly() {
        // taskMapper called 3 times: total=5, completed=3, overdue=1
        when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(5L, 3L, 1L);

        // bugMapper called 2 times: total=4, open=2
        when(bugMapper.selectCount(any(Wrapper.class))).thenReturn(4L, 2L);

        // requirementMapper called 2 times: total=6, accepted=2
        when(requirementMapper.selectCount(any(Wrapper.class))).thenReturn(6L, 2L);

        // noticeMapper called 1 time: unread=3
        when(noticeMapper.selectCount(any(Wrapper.class))).thenReturn(3L);

        MyStatisticsResponse result = dashboardService.getMyStatistics(1L);

        assertThat(result.getMyTaskTotal()).isEqualTo(5L);
        assertThat(result.getMyTaskCompleted()).isEqualTo(3L);
        assertThat(result.getMyTaskOverdue()).isEqualTo(1L);
        assertThat(result.getMyBugTotal()).isEqualTo(4L);
        assertThat(result.getMyBugOpen()).isEqualTo(2L);
        assertThat(result.getMyRequirementTotal()).isEqualTo(6L);
        assertThat(result.getMyRequirementAccepted()).isEqualTo(2L);
        assertThat(result.getUnreadNoticeCount()).isEqualTo(3L);
    }
}
