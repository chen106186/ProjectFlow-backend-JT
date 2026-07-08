package com.jitong.projectflow.auth.security;

import com.jitong.projectflow.bug.entity.BugEntity;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.daily.entity.DailyReportEntity;
import com.jitong.projectflow.file.entity.FileMetadata;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.report.entity.ProjectReportEntity;
import com.jitong.projectflow.task.entity.TaskEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BusinessAccessService {
    private static final String FORBIDDEN_MESSAGE = "无权操作该数据";

    private final ProjectMapper projectMapper;

    public void requireProjectManage(ProjectEntity project) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, project.getManagerId()) || same(userId, project.getCreatedBy())) {
            return;
        }
        throwForbidden();
    }

    public void requireTaskManage(TaskEntity task) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, task.getCreatedBy()) || canManageProject(task.getProjectId(), userId)) {
            return;
        }
        throwForbidden();
    }

    public void requireTaskActualTimeManage(TaskEntity task) {
        Long userId = currentUserId();
        if (same(userId, task.getAssigneeId())) {
            return;
        }
        requireTaskManage(task);
    }

    public void requireBugEdit(BugEntity bug) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, bug.getCreatorId()) || same(userId, bug.getAssigneeId())) {
            return;
        }
        throwForbidden();
    }

    public void requireBugClose(BugEntity bug) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, bug.getCreatorId())) {
            return;
        }
        throwForbidden();
    }

    public void requireRequirementManage(RequirementEntity requirement) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, requirement.getCreatedBy()) || canManageProject(requirement.getProjectId(), userId)) {
            return;
        }
        throwForbidden();
    }

    public void requireFileDelete(FileMetadata metadata) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, metadata.getUploaderId())) {
            return;
        }
        throwForbidden();
    }

    public void requireDailyReportManage(DailyReportEntity report) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, report.getReporterId()) || same(userId, report.getCreatedBy())) {
            return;
        }
        throwForbidden();
    }

    public void requireProjectReportManage(ProjectReportEntity report) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, report.getCreatedBy()) || canManageProject(report.getProjectId(), userId)) {
            return;
        }
        throwForbidden();
    }

    public boolean isSystemAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "system:user:update".equals(authority.getAuthority())
                        || "system:role:update".equals(authority.getAuthority()));
    }

    private boolean canManageProject(Long projectId, Long userId) {
        if (projectId == null) {
            return false;
        }
        ProjectEntity project = projectMapper.selectById(projectId);
        return project != null && (same(userId, project.getManagerId()) || same(userId, project.getCreatedBy()));
    }

    private Long currentUserId() {
        return CurrentUserContext.userId();
    }

    private boolean same(Long left, Long right) {
        return left != null && left.equals(right);
    }

    private void throwForbidden() {
        throw new BusinessException(ErrorCode.FORBIDDEN, FORBIDDEN_MESSAGE);
    }
}
