package com.jitong.projectflow.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.project.dto.ProjectCreateRequest;
import com.jitong.projectflow.project.dto.ProjectQueryRequest;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import com.jitong.projectflow.project.mapper.ProjectParticipantMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.task.mapper.TaskMapper;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {
    @Mock
    ProjectMapper projectMapper;

    @Mock
    OperationLogService operationLogService;

    @Mock
    BusinessAccessService businessAccessService;
    @Mock ProjectNodeMapper projectNodeMapper;
    @Mock ProjectParticipantMapper participantMapper;
    @Mock SystemUserMapper systemUserMapper;
    @Mock TaskMapper taskMapper;
    @Mock RequirementMapper requirementMapper;
    @InjectMocks ProjectService projectService;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void createDefaultsStatusAndWritesOperationLog() {
        CurrentUserContext.set(1001L);
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setProjectType("EXECUTION");
        request.setName("Project A");
        request.setContractStatus("SIGNED");
        request.setManagerId(2002L);
        request.setPlannedStartDate(LocalDate.of(2026, 7, 1));
        request.setPlannedEndDate(LocalDate.of(2026, 8, 1));

        projectService.create(request);

        ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectMapper).insert(captor.capture());
        ProjectEntity inserted = captor.getValue();
        assertThat(inserted.getProjectType()).isEqualTo("EXECUTION");
        assertThat(inserted.getName()).isEqualTo("Project A");
        assertThat(inserted.getStatus()).isEqualTo("NOT_STARTED");
        assertThat(inserted.getCreatedBy()).isEqualTo(1001L);
        verify(operationLogService).record("project", "Project", inserted.getId(), "CREATE", "新建项目：Project A");
    }

    @Test
    void listReturnsPagedProjects() {
        CurrentUserContext.set(1001L);
        ProjectEntity project = new ProjectEntity();
        project.setId(1L);
        project.setName("Project A");
        Page<ProjectEntity> page = new Page<>(1, 20, 1);
        page.setRecords(java.util.List.of(project));
        when(projectMapper.selectPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(page);

        ProjectQueryRequest request = new ProjectQueryRequest();
        var result = projectService.list(request);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).extracting("name").containsExactly("Project A");
    }

    @Test
    void completedSummaryFilterIncludesOverdueCompletedProjects() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProjectEntity.class);
        when(businessAccessService.canViewAll()).thenReturn(true);
        when(projectMapper.selectPage(any(), any())).thenReturn(new Page<ProjectEntity>(1, 20, 0));
        ProjectQueryRequest request = new ProjectQueryRequest();
        request.setProjectType("MANAGEMENT");
        request.setStatus("COMPLETED_OR_OVERDUE_COMPLETED");

        projectService.list(request);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<ProjectEntity>> query = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(projectMapper).selectPage(any(), query.capture());
        assertThat(query.getValue().getSqlSegment()).contains("project_type", "status");
        assertThat(query.getValue().getParamNameValuePairs().values())
                .contains("MANAGEMENT", "COMPLETED", "OVERDUE_COMPLETED");
    }

    @Test
    void participantCannotCreateExecutionProjectLinkedToReadOnlyManagementProject() {
        ProjectEntity parent = new ProjectEntity();
        parent.setId(20L);
        parent.setProjectType("MANAGEMENT");
        when(projectMapper.selectById(20L)).thenReturn(parent);
        doThrow(new com.jitong.projectflow.common.error.BusinessException(
                com.jitong.projectflow.common.error.ErrorCode.FORBIDDEN, "无权操作该数据"))
                .when(businessAccessService).requireProjectManage(parent);
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setProjectType("EXECUTION");
        request.setManagementProjectId(20L);

        assertThatThrownBy(() -> projectService.create(request))
                .isInstanceOf(com.jitong.projectflow.common.error.BusinessException.class);
        verify(projectMapper, never()).insert(any(ProjectEntity.class));
    }
}
