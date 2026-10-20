package com.jitong.projectflow.file.service;

import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.daily.entity.DailyReportEntity;
import com.jitong.projectflow.daily.mapper.DailyReportMapper;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.report.mapper.ProjectReportMapper;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileBusinessAccessServiceTest {
    @Mock BusinessAccessService access;
    @Mock ProjectMapper projects;
    @Mock TaskMapper tasks;
    @Mock BugMapper bugs;
    @Mock RequirementMapper requirements;
    @Mock DailyReportMapper dailyReports;
    @Mock ProjectReportMapper projectReports;

    @AfterEach
    void clearUser() { CurrentUserContext.clear(); }

    @Test
    void historicalDailyReportAttachmentCannotBeChanged() {
        CurrentUserContext.set(12L);
        DailyReportEntity report = report(LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(2));
        when(dailyReports.selectById(5L)).thenReturn(report);

        assertThatCode(() -> service().requireRead("DAILY_REPORT", 5L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service().requireWrite("DAILY_REPORT", 5L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("仅今天和昨天");
    }

    @Test
    void ownerCanAddAttachmentToYesterdayReport() {
        CurrentUserContext.set(12L);
        DailyReportEntity report = report(LocalDate.now(ZoneId.of("Asia/Shanghai")).minusDays(1));
        when(dailyReports.selectById(5L)).thenReturn(report);

        assertThatCode(() -> service().requireWrite("daily-report", 5L)).doesNotThrowAnyException();
        verify(access).requireDailyReportManage(report);
    }

    private DailyReportEntity report(LocalDate date) {
        DailyReportEntity report = new DailyReportEntity();
        report.setId(5L);
        report.setReporterId(12L);
        report.setReportDate(date);
        return report;
    }

    private FileBusinessAccessService service() {
        return new FileBusinessAccessService(access, projects, tasks, bugs, requirements, dailyReports, projectReports);
    }
}
