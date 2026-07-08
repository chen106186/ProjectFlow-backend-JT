package com.jitong.projectflow.project.service;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.project.dto.ProjectNodeUpdateRequest;
import com.jitong.projectflow.project.entity.ProjectNodeEntity;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GanttServiceTest {
    @Mock
    ProjectNodeMapper projectNodeMapper;
    @Mock
    OperationLogService operationLogService;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void updateNodeSetsCompleteWhenActualEndDateProvided() {
        CurrentUserContext.set(1001L);
        ProjectNodeEntity node = node();
        when(projectNodeMapper.selectById(10L)).thenReturn(node);
        ProjectNodeUpdateRequest request = new ProjectNodeUpdateRequest();
        request.setActualEndDate(LocalDate.of(2026, 7, 8));

        var result = new GanttService(projectNodeMapper, operationLogService).updateNode(1L, 10L, request);

        assertThat(node.getProgressPercent()).isEqualTo(100);
        assertThat(node.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getProgressPercent()).isEqualTo(100);
        verify(projectNodeMapper).updateById(node);
    }

    @Test
    void updateNodeRejectsInvalidProgress() {
        CurrentUserContext.set(1001L);
        when(projectNodeMapper.selectById(10L)).thenReturn(node());
        ProjectNodeUpdateRequest request = new ProjectNodeUpdateRequest();
        request.setProgressPercent(120);

        assertThatThrownBy(() -> new GanttService(projectNodeMapper, operationLogService).updateNode(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("节点进度必须在0到100之间");
    }

    @Test
    void updateNodeRejectsInvalidPlannedRange() {
        CurrentUserContext.set(1001L);
        when(projectNodeMapper.selectById(10L)).thenReturn(node());
        ProjectNodeUpdateRequest request = new ProjectNodeUpdateRequest();
        request.setPlannedStartDate(LocalDate.of(2026, 7, 10));
        request.setPlannedEndDate(LocalDate.of(2026, 7, 9));

        assertThatThrownBy(() -> new GanttService(projectNodeMapper, operationLogService).updateNode(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("计划开始日期不能晚于计划结束日期");
    }

    private ProjectNodeEntity node() {
        ProjectNodeEntity node = new ProjectNodeEntity();
        node.setId(10L);
        node.setProjectId(1L);
        node.setNodeName("测试验收");
        node.setStatus("IN_PROGRESS");
        node.setProgressPercent(60);
        return node;
    }
}
