package com.jitong.projectflow.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.project.dto.ProjectCreateRequest;
import com.jitong.projectflow.project.dto.ProjectQueryRequest;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
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

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void createDefaultsStatusAndWritesOperationLog() {
        CurrentUserContext.set(1001L);
        ProjectService service = new ProjectService(projectMapper, operationLogService, businessAccessService);
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setProjectType("EXECUTION");
        request.setName("Project A");
        request.setContractStatus("SIGNED");
        request.setManagerId(2002L);
        request.setPlannedStartDate(LocalDate.of(2026, 7, 1));
        request.setPlannedEndDate(LocalDate.of(2026, 8, 1));

        service.create(request);

        ArgumentCaptor<ProjectEntity> captor = ArgumentCaptor.forClass(ProjectEntity.class);
        verify(projectMapper).insert(captor.capture());
        ProjectEntity inserted = captor.getValue();
        assertThat(inserted.getProjectType()).isEqualTo("EXECUTION");
        assertThat(inserted.getName()).isEqualTo("Project A");
        assertThat(inserted.getStatus()).isEqualTo("NOT_STARTED");
        assertThat(inserted.getCreatedBy()).isEqualTo(1001L);
        verify(operationLogService).record("project", "Project", inserted.getId(), "CREATE", "Project A");
    }

    @Test
    void listReturnsPagedProjects() {
        ProjectEntity project = new ProjectEntity();
        project.setId(1L);
        project.setName("Project A");
        Page<ProjectEntity> page = new Page<>(1, 20, 1);
        page.setRecords(java.util.List.of(project));
        when(projectMapper.selectPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(page);

        ProjectQueryRequest request = new ProjectQueryRequest();
        var result = new ProjectService(projectMapper, operationLogService, businessAccessService).list(request);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).extracting("name").containsExactly("Project A");
    }
}
