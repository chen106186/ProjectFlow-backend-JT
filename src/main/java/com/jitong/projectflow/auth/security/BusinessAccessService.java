package com.jitong.projectflow.auth.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.bug.entity.BugEntity;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.daily.entity.DailyReportEntity;
import com.jitong.projectflow.file.entity.FileMetadata;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.report.entity.ProjectReportEntity;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.task.entity.TaskEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BusinessAccessService {
    private static final String FORBIDDEN_MESSAGE = "无权操作该数据";

    private final ProjectMapper projectMapper;

    public void requireProjectManage(ProjectEntity project) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, project.getManagerId()) || same(userId, project.getCreatedBy())
                || isCoManager(userId, project.getCoManagerIds())) {
            return;
        }
        throwForbidden();
    }

    public void requireTaskManage(TaskEntity task) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, task.getAssigneeId()) || same(userId, task.getCreatedBy()) || canManageProject(task.getProjectId(), userId)) {
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

    public void requireBugDelete(BugEntity bug) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, bug.getCreatorId())) {
            return;
        }
        throwForbidden();
    }

    public void requireRequirementManage(RequirementEntity requirement) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, requirement.getCreatedBy()) || same(userId, requirement.getReviewerId()) || canManageProject(requirement.getProjectId(), userId)) {
            return;
        }
        throwForbidden();
    }

    public void requireRequirementOwner(RequirementEntity requirement) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, requirement.getCreatedBy())) {
            return;
        }
        throwForbidden();
    }

    public void requireRequirementReview(RequirementEntity requirement) {
        Long userId = currentUserId();
        if (isSystemAdmin() || same(userId, requirement.getReviewerId())
                || (requirement.getReviewerId() == null
                    && (same(userId, requirement.getCreatedBy()) || canManageProject(requirement.getProjectId(), userId)))) {
            return;
        }
        throwForbidden();
    }

    /** Only the authenticated numeric user ID is embedded; no request text is accepted by this query. */
    public String readableProjectIdsSql() {
        long userId = currentUserId();
        return "SELECT p.id FROM pf_project p WHERE p.deleted = 0 AND (p.manager_id = " + userId
                + " OR p.created_by = " + userId
                + " OR CONCAT(',', REPLACE(COALESCE(p.co_manager_ids, ''), ' ', ''), ',') LIKE '%," + userId + ",%'"
                + " OR EXISTS (SELECT 1 FROM pf_project_participant pp WHERE pp.project_id = p.id AND pp.user_id = " + userId + "))";
    }

    public boolean canReadProject(Long projectId) {
        if (projectId == null) return false;
        if (canViewAll()) return true;
        return projectMapper.selectCount(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getId, projectId)
                .inSql(ProjectEntity::getId, readableProjectIdsSql())) > 0;
    }

    public void requireProjectRead(ProjectEntity project) {
        Long userId = currentUserId();
        if (canViewAll() || same(userId, project.getCreatedBy()) || same(userId, project.getManagerId())
                || isCoManager(userId, project.getCoManagerIds()) || canReadProject(project.getId())) return;
        throwForbidden();
    }

    public void requireTaskRead(TaskEntity task) {
        Long userId = currentUserId();
        if (canViewAll() || same(userId, task.getAssigneeId()) || same(userId, task.getCreatedBy())
                || canReadProject(task.getProjectId())) return;
        throwForbidden();
    }

    public void requireProjectRead(Long projectId) {
        ProjectEntity project = projectId == null ? null : projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        requireProjectRead(project);
    }

    public void requireDailyReportRead(DailyReportEntity report) {
        Long userId = currentUserId();
        if (canViewAll() || same(userId, report.getReporterId()) || same(userId, report.getCreatedBy())) return;
        throwForbidden();
    }

    public void requireProjectReportRead(ProjectReportEntity report) {
        Long userId = currentUserId();
        if (canViewAll() || same(userId, report.getCreatedBy()) || canReadProject(report.getProjectId())) return;
        throwForbidden();
    }

    public void requireBugRead(BugEntity bug) {
        Long userId = currentUserId();
        if (canViewAll() || same(userId, bug.getCreatorId()) || same(userId, bug.getAssigneeId())
                || canReadProject(bug.getProjectId())) return;
        throwForbidden();
    }

    public void requireRequirementRead(RequirementEntity requirement) {
        Long userId = currentUserId();
        if (canViewAll() || same(userId, requirement.getCreatedBy()) || same(userId, requirement.getReviewerId())
                || canReadProject(requirement.getProjectId())) return;
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

    public boolean isGmOffice() {
        return CurrentUserContext.isGmOffice();
    }

    public boolean isSystemAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_SUPER_ADMIN".equals(authority.getAuthority()));
    }

    /** 是否拥有"查看全部业务数据"权限：总经办、系统管理员或拥有 data:view:all 权限码的角色（如部门领导）。 */
    public boolean canViewAll() {
        if (isGmOffice() || isSystemAdmin()) {
            return true;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "data:view:all".equals(a.getAuthority()));
    }

    private boolean canManageProject(Long projectId, Long userId) {
        if (projectId == null) {
            return false;
        }
        ProjectEntity project = projectMapper.selectById(projectId);
        return project != null && (same(userId, project.getManagerId()) || same(userId, project.getCreatedBy())
                || isCoManager(userId, project.getCoManagerIds()));
    }

    private boolean isCoManager(Long userId, String coManagerIds) {
        if (userId == null || !StringUtils.hasText(coManagerIds)) return false;
        String idStr = String.valueOf(userId);
        for (String s : coManagerIds.split(",")) {
            if (idStr.equals(s.trim())) return true;
        }
        return false;
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
