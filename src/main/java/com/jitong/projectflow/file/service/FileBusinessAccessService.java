package com.jitong.projectflow.file.service;

import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.daily.mapper.DailyReportMapper;
import com.jitong.projectflow.daily.domain.DailyReportEditPolicy;
import com.jitong.projectflow.file.domain.FileBusinessType;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.task.mapper.TaskMapper;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.report.mapper.ProjectReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Resolve authorization through the owning business record rather than trusting an attachment ID. */
@Service
@RequiredArgsConstructor
public class FileBusinessAccessService {
    private final BusinessAccessService access;
    private final ProjectMapper projects;
    private final TaskMapper tasks;
    private final BugMapper bugs;
    private final RequirementMapper requirements;
    private final DailyReportMapper dailyReports;
    private final ProjectReportMapper projectReports;

    public void requireWrite(String businessType, Long businessId) {
        requireRead(businessType, businessId);
        if (FileBusinessType.from(businessType) == FileBusinessType.DAILY_REPORT) {
            var report = require(dailyReports.selectById(businessId));
            access.requireDailyReportManage(report);
            DailyReportEditPolicy.requireEditable(report.getReportDate());
        }
    }

    public void requireRead(String businessType, Long businessId) {
        Long userId = CurrentUserContext.userId();
        FileBusinessType type = FileBusinessType.from(businessType);
        if (businessId == null || (type != FileBusinessType.RICH_TEXT && businessId <= 0)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的附件业务ID");
        }
        switch (type) {
            case PROJECT -> access.requireProjectRead(require(projects.selectById(businessId)));
            case TASK -> access.requireTaskRead(require(tasks.selectById(businessId)));
            case BUG -> access.requireBugRead(require(bugs.selectById(businessId)));
            case REQUIREMENT -> access.requireRequirementRead(require(requirements.selectById(businessId)));
            case DAILY_REPORT -> {
                var report = require(dailyReports.selectById(businessId));
                if (!access.canViewAll() && !userId.equals(report.getReporterId()) && !userId.equals(report.getCreatedBy())) deny();
            }
            case PROJECT_REPORT -> {
                var report = require(projectReports.selectById(businessId));
                if (!access.canViewAll() && !userId.equals(report.getCreatedBy()) && !access.canReadProject(report.getProjectId())) deny();
            }
            case RICH_TEXT -> { /* Unattached editor images are restricted to authenticated upload/list operations. */ }
        }
    }

    private <T> T require(T entity) {
        if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "关联业务不存在");
        return entity;
    }

    private void deny() {
        throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该数据");
    }
}
