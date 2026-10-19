package com.jitong.projectflow.project.service;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.project.dto.ProjectNodeUpdateRequest;
import com.jitong.projectflow.project.entity.ProjectNodeEntity;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.system.mapper.RoleMapper;
import com.jitong.projectflow.system.mapper.UserRoleMapper;
import com.jitong.projectflow.task.mapper.TaskMapper;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GanttServiceTest {
    @Mock
    ProjectNodeMapper projectNodeMapper;
    @Mock
    OperationLogService operationLogService;
    @Mock NoticeService noticeService;
    @Mock ProjectMapper projectMapper;
    @Mock SystemUserMapper systemUserMapper;
    @Mock RoleMapper roleMapper;
    @Mock UserRoleMapper userRoleMapper;
    @Mock TaskMapper taskMapper;
    @Mock BusinessAccessService businessAccessService;
    @InjectMocks GanttService service;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void updateNodeSetsCompleteWhenActualEndDateProvided() {
        readableProject();
        CurrentUserContext.set(1001L);
        ProjectNodeEntity node = node();
        when(projectNodeMapper.selectById(10L)).thenReturn(node);
        ProjectNodeUpdateRequest request = new ProjectNodeUpdateRequest();
        request.setActualEndDate(LocalDate.of(2026, 7, 8));

        var result = service.updateNode(1L, 10L, request);

        assertThat(node.getProgressPercent()).isEqualTo(100);
        assertThat(node.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getProgressPercent()).isEqualTo(100);
        verify(projectNodeMapper).update(isNull(), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
        verify(businessAccessService).requireProjectManage(any(ProjectEntity.class));
    }

    @Test
    void updateNodeRejectsInvalidProgress() {
        readableProject();
        CurrentUserContext.set(1001L);
        when(projectNodeMapper.selectById(10L)).thenReturn(node());
        ProjectNodeUpdateRequest request = new ProjectNodeUpdateRequest();
        request.setProgressPercent(120);

        assertThatThrownBy(() -> service.updateNode(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("节点进度必须在0到100之间");
    }

    @Test
    void updateNodeRejectsInvalidPlannedRange() {
        readableProject();
        CurrentUserContext.set(1001L);
        when(projectNodeMapper.selectById(10L)).thenReturn(node());
        ProjectNodeUpdateRequest request = new ProjectNodeUpdateRequest();
        request.setPlannedStartDate(LocalDate.of(2026, 7, 10));
        request.setPlannedEndDate(LocalDate.of(2026, 7, 9));

        assertThatThrownBy(() -> service.updateNode(1L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("计划结束日期不能早于开始日期");
    }

    @Test
    void deniedProjectReadCannotLoadGanttNodesOrSummary() {
        readableProject();
        doThrow(new BusinessException(com.jitong.projectflow.common.error.ErrorCode.FORBIDDEN, "无权操作该数据"))
                .when(businessAccessService).requireProjectRead(any(ProjectEntity.class));

        assertThatThrownBy(() -> service.getGanttNodes(1L)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.getSummary(1L)).isInstanceOf(BusinessException.class);

        verify(projectNodeMapper, never()).selectList(any());
        verify(taskMapper, never()).selectCount(any());
    }

    @Test
    void deniedProjectManageCannotUpdateNode() {
        readableProject();
        doThrow(new BusinessException(com.jitong.projectflow.common.error.ErrorCode.FORBIDDEN, "无权操作该数据"))
                .when(businessAccessService).requireProjectManage(any());

        assertThatThrownBy(() -> service.updateNode(1L, 10L, new ProjectNodeUpdateRequest()))
                .isInstanceOf(BusinessException.class);

        verify(projectNodeMapper, never()).selectById(any());
        verify(projectNodeMapper, never()).update(any(), any(com.baomidou.mybatisplus.core.conditions.Wrapper.class));
    }

    @Test
    void executionNodeCannotWriteReadOnlyManagementProject() {
        CurrentUserContext.set(1001L);
        ProjectEntity execution = new ProjectEntity();
        execution.setId(1L);
        execution.setProjectType("EXECUTION");
        execution.setManagementProjectId(20L);
        ProjectEntity management = new ProjectEntity();
        management.setId(20L);
        management.setProjectType("MANAGEMENT");
        when(projectMapper.selectById(1L)).thenReturn(execution);
        when(projectMapper.selectById(20L)).thenReturn(management);
        ProjectNodeEntity executionNode = node();
        executionNode.setNodeCode("TEST");
        when(projectNodeMapper.selectById(10L)).thenReturn(executionNode);
        ProjectNodeEntity managementNode = node();
        managementNode.setId(30L);
        managementNode.setProjectId(20L);
        when(projectNodeMapper.selectOne(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class)))
                .thenReturn(managementNode);
        org.mockito.Mockito.doAnswer(invocation -> {
            ProjectEntity target = invocation.getArgument(0);
            if (Long.valueOf(20L).equals(target.getId())) {
                throw new BusinessException(com.jitong.projectflow.common.error.ErrorCode.FORBIDDEN, "无权操作该数据");
            }
            return null;
        }).when(businessAccessService).requireProjectManage(any(ProjectEntity.class));

        assertThatThrownBy(() -> service.updateNode(1L, 10L, new ProjectNodeUpdateRequest()))
                .isInstanceOf(BusinessException.class);
        verify(businessAccessService).requireProjectManage(management);
    }

    private void readableProject() {
        ProjectEntity project = new ProjectEntity();
        project.setId(1L);
        project.setStatus("PAUSED");
        when(projectMapper.selectById(1L)).thenReturn(project);
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
