package com.jitong.projectflow.daily.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.file.service.FileService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.daily.dto.DailyReportCreateRequest;
import com.jitong.projectflow.daily.dto.DailyReportQueryRequest;
import com.jitong.projectflow.daily.dto.DailyReportUpdateRequest;
import com.jitong.projectflow.daily.entity.DailyReportEntity;
import com.jitong.projectflow.daily.mapper.DailyReportMapper;
import com.jitong.projectflow.daily.mapper.DailyReportTaskMapper;
import com.jitong.projectflow.file.mapper.FileMetadataMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyReportServiceTest {
    private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Asia/Shanghai"));
    @Mock
    DailyReportMapper dailyReportMapper;

    @Mock
    OperationLogService operationLogService;

    @Mock
    BusinessAccessService businessAccessService;

    @Mock
    FileMetadataMapper fileMetadataMapper;
    @Mock
    FileService fileService;

    @Mock
    DailyReportTaskMapper dailyReportTaskMapper;
    @Mock
    SystemUserMapper systemUserMapper;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void createUsesCurrentUserAsReporterAndWritesLog() {
        CurrentUserContext.set(1001L);
        DailyReportCreateRequest request = new DailyReportCreateRequest();
        request.setProjectId(20L);
        request.setReportDate(TODAY);
        request.setContent("Completed API design");

        DailyReportService service = service();
        service.create(request);

        ArgumentCaptor<DailyReportEntity> captor = ArgumentCaptor.forClass(DailyReportEntity.class);
        verify(dailyReportMapper).insert(captor.capture());
        DailyReportEntity inserted = captor.getValue();
        assertThat(inserted.getReporterId()).isEqualTo(1001L);
        assertThat(inserted.getCreatedBy()).isEqualTo(1001L);
        assertThat(inserted.getProjectId()).isEqualTo(20L);
        verify(operationLogService).record("daily-report", "DailyReport", inserted.getId(), "CREATE", "提交日报：" + TODAY);
    }

    @Test
    void listReturnsPagedReports() {
        CurrentUserContext.set(1001L);
        DailyReportEntity report = new DailyReportEntity();
        report.setId(1L);
        report.setProjectId(20L);
        report.setReporterId(1001L);
        report.setReportDate(TODAY);
        report.setContent("Daily note");
        Page<DailyReportEntity> page = new Page<>(1, 20, 1);
        page.setRecords(java.util.List.of(report));
        when(dailyReportMapper.selectPage(any(), any())).thenReturn(page);

        var result = service().list(new DailyReportQueryRequest());

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
        report.setReportDate(TODAY.minusDays(1));
        report.setContent("Old content");
        when(dailyReportMapper.selectById(10L)).thenReturn(report);

        DailyReportUpdateRequest request = new DailyReportUpdateRequest();
        request.setContent("New content");

        service().update(10L, request);

        ArgumentCaptor<DailyReportEntity> captor = ArgumentCaptor.forClass(DailyReportEntity.class);
        verify(businessAccessService).requireDailyReportManage(report);
        verify(dailyReportMapper).updateById(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("New content");
        assertThat(captor.getValue().getUpdatedBy()).isEqualTo(1001L);
        verify(operationLogService).record("daily-report", "DailyReport", 10L, "UPDATE", "编辑日报：" + TODAY.minusDays(1));
    }

    @Test
    void olderReportCanBeReadButCannotBeChanged() {
        CurrentUserContext.set(1001L);
        DailyReportEntity report = new DailyReportEntity();
        report.setId(10L);
        report.setReporterId(1001L);
        report.setReportDate(TODAY.minusDays(2));
        when(dailyReportMapper.selectById(10L)).thenReturn(report);

        assertThat(service().getById(10L).getReportDate()).isEqualTo(TODAY.minusDays(2));
        assertThatThrownBy(() -> service().update(10L, new DailyReportUpdateRequest()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("仅今天和昨天");
        assertThatThrownBy(() -> service().delete(10L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("仅今天和昨天");
    }

    @Test
    void cannotCreateHistoricalReportOrMoveRecentReportToOldDate() {
        CurrentUserContext.set(1001L);
        DailyReportCreateRequest create = new DailyReportCreateRequest();
        create.setReportDate(TODAY.minusDays(2));
        assertThatThrownBy(() -> service().create(create))
                .isInstanceOf(BusinessException.class).hasMessageContaining("仅今天和昨天");

        DailyReportEntity report = new DailyReportEntity();
        report.setId(10L);
        report.setReportDate(TODAY);
        when(dailyReportMapper.selectById(10L)).thenReturn(report);
        DailyReportUpdateRequest update = new DailyReportUpdateRequest();
        update.setReportDate(TODAY.minusDays(2));
        assertThatThrownBy(() -> service().update(10L, update))
                .isInstanceOf(BusinessException.class).hasMessageContaining("仅今天和昨天");
    }

    private DailyReportService service() {
        return new DailyReportService(dailyReportMapper, operationLogService, businessAccessService,
                fileMetadataMapper, dailyReportTaskMapper, systemUserMapper, fileService);
    }
}
