package com.jitong.projectflow.report.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.report.dto.ProjectReportCreateRequest;
import com.jitong.projectflow.report.dto.ProjectReportItemCreateRequest;
import com.jitong.projectflow.report.dto.ProjectReportQueryRequest;
import com.jitong.projectflow.report.dto.ProjectReportStatusUpdateRequest;
import com.jitong.projectflow.report.entity.ProjectReportEntity;
import com.jitong.projectflow.report.entity.ProjectReportItemEntity;
import com.jitong.projectflow.report.mapper.ProjectReportItemMapper;
import com.jitong.projectflow.report.mapper.ProjectReportMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectReportServiceTest {
    @Mock
    ProjectReportMapper projectReportMapper;

    @Mock
    ProjectReportItemMapper projectReportItemMapper;

    @Mock
    OperationLogService operationLogService;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void createDefaultsStatusAndWritesLog() {
        CurrentUserContext.set(1001L);
        ProjectReportCreateRequest request = new ProjectReportCreateRequest();
        request.setProjectId(20L);
        request.setTitle("Q2 progress");
        request.setReportType("WEEKLY");
        request.setPlannedDate(LocalDate.of(2026, 7, 10));
        request.setTargetAudience("Leadership");
        request.setLocationMethod("Meeting room");
        request.setDescription("Prepare Q2 report");

        ProjectReportService service = new ProjectReportService(projectReportMapper, projectReportItemMapper, operationLogService);
        service.create(request);

        ArgumentCaptor<ProjectReportEntity> captor = ArgumentCaptor.forClass(ProjectReportEntity.class);
        verify(projectReportMapper).insert(captor.capture());
        ProjectReportEntity inserted = captor.getValue();
        assertThat(inserted.getStatus()).isEqualTo("PREPARING");
        assertThat(inserted.getCreatedBy()).isEqualTo(1001L);
        verify(operationLogService).record("project-report", "ProjectReport", inserted.getId(), "CREATE", "Q2 progress");
    }

    @Test
    void listReturnsPagedReports() {
        ProjectReportEntity report = new ProjectReportEntity();
        report.setId(1L);
        report.setProjectId(20L);
        report.setTitle("Q2 progress");
        report.setStatus("PREPARING");
        Page<ProjectReportEntity> page = new Page<>(1, 20, 1);
        page.setRecords(java.util.List.of(report));
        when(projectReportMapper.selectPage(any(), any())).thenReturn(page);

        var result = new ProjectReportService(projectReportMapper, projectReportItemMapper, operationLogService).list(new ProjectReportQueryRequest());

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).extracting("title").containsExactly("Q2 progress");
    }

    @Test
    void updateStatusWritesLog() {
        CurrentUserContext.set(1001L);
        ProjectReportEntity report = new ProjectReportEntity();
        report.setId(10L);
        report.setProjectId(20L);
        report.setTitle("Q2 progress");
        report.setStatus("PREPARING");
        when(projectReportMapper.selectById(10L)).thenReturn(report);

        ProjectReportStatusUpdateRequest request = new ProjectReportStatusUpdateRequest();
        request.setStatus("COMPLETED");

        new ProjectReportService(projectReportMapper, projectReportItemMapper, operationLogService).updateStatus(10L, request);

        ArgumentCaptor<ProjectReportEntity> captor = ArgumentCaptor.forClass(ProjectReportEntity.class);
        verify(projectReportMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("COMPLETED");
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(1001L);
        verify(operationLogService).record("project-report", "ProjectReport", 10L, "UPDATE_STATUS", "COMPLETED");
    }

    @Test
    void createItemWritesLog() {
        CurrentUserContext.set(1001L);
        ProjectReportEntity report = new ProjectReportEntity();
        report.setId(10L);
        report.setTitle("Q2 progress");
        when(projectReportMapper.selectById(10L)).thenReturn(report);
        ProjectReportItemCreateRequest request = new ProjectReportItemCreateRequest();
        request.setContent("Prepare slides");
        request.setOwnerId(1002L);
        request.setPriority("HIGH");
        request.setStatus("NOT_STARTED");

        new ProjectReportService(projectReportMapper, projectReportItemMapper, operationLogService).createItem(10L, request);

        ArgumentCaptor<ProjectReportItemEntity> captor = ArgumentCaptor.forClass(ProjectReportItemEntity.class);
        verify(projectReportItemMapper).insert(captor.capture());
        assertThat(captor.getValue().getReportId()).isEqualTo(10L);
        assertThat(captor.getValue().getContent()).isEqualTo("Prepare slides");
        verify(operationLogService).record("project-report", "ProjectReport", 10L, "CREATE_ITEM", "Prepare slides");
    }
}
