package com.jitong.projectflow.daily.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.daily.dto.DailyReportCreateRequest;
import com.jitong.projectflow.daily.dto.DailyReportQueryRequest;
import com.jitong.projectflow.daily.dto.DailyReportUpdateRequest;
import com.jitong.projectflow.daily.entity.DailyReportEntity;
import com.jitong.projectflow.daily.mapper.DailyReportMapper;
import com.jitong.projectflow.file.mapper.FileMetadataMapper;
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
class DailyReportServiceTest {
    @Mock
    DailyReportMapper dailyReportMapper;

    @Mock
    OperationLogService operationLogService;

    @Mock
    BusinessAccessService businessAccessService;

    @Mock
    FileMetadataMapper fileMetadataMapper;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void createUsesCurrentUserAsReporterAndWritesLog() {
        CurrentUserContext.set(1001L);
        DailyReportCreateRequest request = new DailyReportCreateRequest();
        request.setProjectId(20L);
        request.setReportDate(LocalDate.of(2026, 7, 7));
        request.setContent("Completed API design");

        DailyReportService service = new DailyReportService(dailyReportMapper, operationLogService, businessAccessService, fileMetadataMapper);
        service.create(request);

        ArgumentCaptor<DailyReportEntity> captor = ArgumentCaptor.forClass(DailyReportEntity.class);
        verify(dailyReportMapper).insert(captor.capture());
        DailyReportEntity inserted = captor.getValue();
        assertThat(inserted.getReporterId()).isEqualTo(1001L);
        assertThat(inserted.getCreatedBy()).isEqualTo(1001L);
        assertThat(inserted.getProjectId()).isEqualTo(20L);
        verify(operationLogService).record("daily-report", "DailyReport", inserted.getId(), "CREATE", "Completed API design");
    }

    @Test
    void listReturnsPagedReports() {
        DailyReportEntity report = new DailyReportEntity();
        report.setId(1L);
        report.setProjectId(20L);
        report.setReporterId(1001L);
        report.setReportDate(LocalDate.of(2026, 7, 7));
        report.setContent("Daily note");
        Page<DailyReportEntity> page = new Page<>(1, 20, 1);
        page.setRecords(java.util.List.of(report));
        when(dailyReportMapper.selectPage(any(), any())).thenReturn(page);

        var result = new DailyReportService(dailyReportMapper, operationLogService, businessAccessService, fileMetadataMapper).list(new DailyReportQueryRequest());

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).extracting("content").containsExactly("Daily note");
    }

    @Test
    void updateChangesContentAndWritesLog() {
        CurrentUserContext.set(1001L);
        DailyReportEntity report = new DailyReportEntity();
        report.setId(10L);
        report.setProjectId(20L);
        report.setReporterId(1001L);
        report.setReportDate(LocalDate.of(2026, 7, 7));
        report.setContent("Old content");
        when(dailyReportMapper.selectById(10L)).thenReturn(report);

        DailyReportUpdateRequest request = new DailyReportUpdateRequest();
        request.setContent("New content");

        new DailyReportService(dailyReportMapper, operationLogService, businessAccessService, fileMetadataMapper).update(10L, request);

        ArgumentCaptor<DailyReportEntity> captor = ArgumentCaptor.forClass(DailyReportEntity.class);
        verify(businessAccessService).requireDailyReportManage(report);
        verify(dailyReportMapper).updateById(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("New content");
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(1001L);
        verify(operationLogService).record("daily-report", "DailyReport", 10L, "UPDATE", "New content");
    }
}
