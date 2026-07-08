package com.jitong.projectflow.notice.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.entity.ProjectNodeEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectStageDeadlineNoticeServiceTest {
    @Mock
    ProjectNodeMapper projectNodeMapper;
    @Mock
    ProjectMapper projectMapper;
    @Mock
    NoticeService noticeService;

    @Test
    void scanProjectStageDeadlinesNotifiesProjectManager() {
        LocalDate today = LocalDate.of(2026, 7, 8);
        ProjectNodeEntity node = new ProjectNodeEntity();
        node.setId(10L);
        node.setProjectId(1L);
        node.setNodeName("测试验收");
        node.setPlannedEndDate(today.plusDays(1));
        ProjectEntity project = new ProjectEntity();
        project.setId(1L);
        project.setName("研发平台");
        project.setManagerId(1001L);
        when(projectNodeMapper.selectList(any(Wrapper.class))).thenReturn(List.of(node));
        when(projectMapper.selectById(1L)).thenReturn(project);

        int created = new ProjectStageDeadlineNoticeService(projectNodeMapper, projectMapper, noticeService)
                .scanProjectStageDeadlines(today);

        assertThat(created).isEqualTo(1);
        verify(noticeService).create(1001L, NoticeType.PROJECT_WARNING, "项目阶段即将到期",
                "研发平台 - 测试验收 将于 2026-07-09 到期，请及时推进。", "ProjectNode", 10L);
    }
}
