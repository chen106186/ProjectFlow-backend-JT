package com.jitong.projectflow.bug.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.bug.domain.BugStatus;
import com.jitong.projectflow.bug.dto.BugAssignRequest;
import com.jitong.projectflow.bug.dto.BugCommentCreateRequest;
import com.jitong.projectflow.bug.dto.BugCommentResponse;
import com.jitong.projectflow.bug.dto.BugCreateRequest;
import com.jitong.projectflow.bug.dto.BugFixRequest;
import com.jitong.projectflow.bug.dto.BugResolveRequest;
import com.jitong.projectflow.bug.dto.BugQueryRequest;
import com.jitong.projectflow.bug.dto.BugResponse;
import com.jitong.projectflow.bug.dto.BugRelatedTaskResponse;
import com.jitong.projectflow.bug.dto.BugSummaryResponse;
import com.jitong.projectflow.bug.dto.BugUpdateRequest;
import com.jitong.projectflow.bug.entity.BugCommentEntity;
import com.jitong.projectflow.bug.entity.BugEntity;
import com.jitong.projectflow.bug.mapper.BugCommentMapper;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.bug.mapper.BugTaskMapper;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.api.PageUtils;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.dto.OperationLogResponse;
import com.jitong.projectflow.system.entity.OperationLog;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.OperationLogMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BugService {
    private final BugMapper bugMapper;
    private final BugCommentMapper bugCommentMapper;
    private final OperationLogService operationLogService;
    private final NoticeService noticeService;
    private final BusinessAccessService businessAccessService;
    private final SystemUserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final OperationLogMapper operationLogMapper;
    private final BugTaskMapper bugTaskMapper;
    private final TaskMapper taskMapper;

    public BugResponse create(BugCreateRequest request) {
        List<Long> relatedTaskIds = normalizeRelatedTaskIds(request.getTaskId(), request.getRelatedTaskIds());
        BugEntity entity = new BugEntity();
        entity.setProjectId(request.getProjectId());
        entity.setTaskId(primaryTaskId(relatedTaskIds));
        entity.setTitle(request.getTitle());
        entity.setStatus(BugStatus.PENDING_FIX.name());
        entity.setPriority(request.getPriority());
        entity.setCreatorId(CurrentUserContext.userId());
        entity.setAssigneeId(request.getAssigneeId());
        entity.setDescription(request.getDescription());
        entity.setReproduceSteps(request.getReproduceSteps());
        entity.setCreatedBy(CurrentUserContext.userIdOrNull());
        entity.setBugNo(bugMapper.selectMaxBugNo() + 1L);
        bugMapper.insert(entity);
        syncRelatedTasks(entity.getId(), relatedTaskIds);
        operationLogService.record("bug", "Bug", entity.getId(), "CREATE", "新建Bug：" + entity.getTitle());
        if (entity.getAssigneeId() != null) {
            String projectName = resolveProjectName(entity.getProjectId());
            String creatorName = resolveUserName(entity.getCreatorId());
            noticeService.create(entity.getAssigneeId(), NoticeType.BUG_ASSIGNED,
                    "您有一条缺陷待处理：「" + entity.getTitle() + "」",
                    "所属项目：" + projectName + "　｜　提交人：" + creatorName,
                    "Bug", entity.getId());
        }
        return getById(entity.getId());
    }

    public PageResult<BugResponse> list(BugQueryRequest request) {
        LambdaQueryWrapper<BugEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(request.getStatus()), BugEntity::getStatus, request.getStatus());
        wrapper.eq(StringUtils.hasText(request.getPriority()), BugEntity::getPriority, request.getPriority());
        applyProjectScope(wrapper, request.getProjectId());
        applyTaskScope(wrapper, request.getTaskId());
        wrapper.eq(request.getAssigneeId() != null, BugEntity::getAssigneeId, request.getAssigneeId());
        wrapper.eq(request.getCreatorId() != null, BugEntity::getCreatorId, request.getCreatorId());
        wrapper.like(StringUtils.hasText(request.getKeyword()), BugEntity::getTitle, request.getKeyword());
        wrapper.orderByDesc(BugEntity::getCreatedAt);
        Page<BugEntity> page = bugMapper.selectPage(PageUtils.toPage(request), wrapper);
        List<BugEntity> records = page.getRecords();
        Map<Long, String> userNames = batchUserNames(records.stream()
                .flatMap(b -> Stream.of(b.getCreatorId(), b.getAssigneeId()))
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> projectNames = batchProjectNames(records.stream()
                .map(BugEntity::getProjectId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return PageUtils.toResult(page, records.stream().map(b -> toResponse(b, userNames, projectNames)).toList());
    }

    public BugSummaryResponse summary(Long projectId) {
        LambdaQueryWrapper<BugEntity> baseWrapper = new LambdaQueryWrapper<>();
        applyProjectScope(baseWrapper, projectId);
        List<BugEntity> bugs = safeList(bugMapper.selectList(baseWrapper));
        return new BugSummaryResponse(
                bugs.size(),
                countBy(bugs, BugEntity::getStatus),
                countBy(bugs, BugEntity::getPriority));
    }

    private void applyProjectScope(LambdaQueryWrapper<BugEntity> wrapper, Long projectId) {
        if (projectId == null) {
            return;
        }
        ProjectEntity proj = projectMapper.selectById(projectId);
        if (proj != null && "EXECUTION".equals(proj.getProjectType()) && proj.getManagementProjectId() != null) {
            wrapper.in(BugEntity::getProjectId, List.of(projectId, proj.getManagementProjectId()));
        } else {
            wrapper.eq(BugEntity::getProjectId, projectId);
        }
    }

    private void applyTaskScope(LambdaQueryWrapper<BugEntity> wrapper, Long taskId) {
        if (taskId == null) {
            return;
        }
        wrapper.and(taskScope -> taskScope.eq(BugEntity::getTaskId, taskId)
                .or()
                .inSql(BugEntity::getId, "SELECT bug_id FROM pf_bug_task WHERE task_id = " + taskId));
    }

    public List<BugResponse> listMine(BugQueryRequest request) {
        Long userId = CurrentUserContext.userId();
        List<Long> managedProjectIds = projectMapper.selectList(
                        new LambdaQueryWrapper<ProjectEntity>()
                                .and(w -> w.eq(ProjectEntity::getManagerId, userId)
                                        .or().apply("FIND_IN_SET({0}, co_manager_ids) > 0", String.valueOf(userId)))
                                .select(ProjectEntity::getId))
                .stream().map(ProjectEntity::getId).toList();
        LambdaQueryWrapper<BugEntity> wrapper = new LambdaQueryWrapper<>();
        if (!managedProjectIds.isEmpty()) {
            wrapper.and(w -> w.eq(BugEntity::getCreatorId, userId)
                    .or().eq(BugEntity::getAssigneeId, userId)
                    .or().in(BugEntity::getProjectId, managedProjectIds));
        } else {
            wrapper.and(w -> w.eq(BugEntity::getCreatorId, userId).or().eq(BugEntity::getAssigneeId, userId));
        }
        if (request != null) {
            wrapper.eq(request.getProjectId() != null, BugEntity::getProjectId, request.getProjectId());
            applyTaskScope(wrapper, request.getTaskId());
            wrapper.eq(StringUtils.hasText(request.getPriority()), BugEntity::getPriority, request.getPriority());
            wrapper.eq(StringUtils.hasText(request.getStatus()), BugEntity::getStatus, request.getStatus());
            wrapper.like(StringUtils.hasText(request.getKeyword()), BugEntity::getTitle, request.getKeyword());
            wrapper.eq(request.getCreatorId() != null, BugEntity::getCreatorId, request.getCreatorId());
        }
        wrapper.orderByDesc(BugEntity::getCreatedAt);
        List<BugEntity> records = bugMapper.selectList(wrapper);
        Map<Long, String> userNames = batchUserNames(records.stream()
                .flatMap(b -> Stream.of(b.getCreatorId(), b.getAssigneeId()))
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> projectNames = batchProjectNames(records.stream()
                .map(BugEntity::getProjectId).filter(Objects::nonNull).collect(Collectors.toSet()));
        return records.stream().map(b -> toResponse(b, userNames, projectNames)).toList();
    }

    public BugResponse getById(Long id) {
        BugEntity entity = requireBug(id);
        Map<Long, String> userNames = batchUserNames(Stream.of(entity.getCreatorId(), entity.getAssigneeId())
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, String> projectNames = entity.getProjectId() != null
                ? batchProjectNames(Set.of(entity.getProjectId())) : Map.of();
        BugResponse response = toResponse(entity, userNames, projectNames);
        List<OperationLog> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>()
                        .eq(OperationLog::getBusinessType, "Bug")
                        .eq(OperationLog::getBusinessId, id)
                        .orderByAsc(OperationLog::getCreatedAt));
        response.setLogs(logs.stream().map(log -> OperationLogResponse.builder()
                .id(log.getId())
                .module(log.getModule())
                .businessType(log.getBusinessType())
                .businessId(log.getBusinessId())
                .operationType(log.getOperationType())
                .operatorId(log.getOperatorId())
                .operatorName(log.getOperatorName())
                .content(log.getContent())
                .createdAt(log.getCreatedAt())
                .build()).toList());
        return response;
    }

    public BugResponse update(Long id, BugUpdateRequest request) {
        BugEntity entity = requireBug(id);
        ensureMutable(entity);
        businessAccessService.requireBugEdit(entity);
        if (request.getProjectId() != null) entity.setProjectId(request.getProjectId());
        if (request.getRelatedTaskIds() != null || request.getTaskId() != null) {
            List<Long> relatedTaskIds = normalizeRelatedTaskIds(request.getTaskId(), request.getRelatedTaskIds());
            entity.setTaskId(primaryTaskId(relatedTaskIds));
            syncRelatedTasks(id, relatedTaskIds);
        }
        if (request.getTitle() != null) entity.setTitle(request.getTitle());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        if (request.getAssigneeId() != null) entity.setAssigneeId(request.getAssigneeId());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getReproduceSteps() != null) entity.setReproduceSteps(request.getReproduceSteps());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        bugMapper.updateById(entity);
        operationLogService.record("bug", "Bug", id, "UPDATE", "编辑Bug：" + entity.getTitle());
        return getById(id);
    }

    public BugResponse assign(Long id, BugAssignRequest request) {
        BugEntity entity = requireBug(id);
        ensureMutable(entity);
        businessAccessService.requireBugEdit(entity);
        entity.setAssigneeId(request.getAssigneeId());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        bugMapper.updateById(entity);
        String assignReason = StringUtils.hasText(request.getReason()) ? "，原因：" + request.getReason() : "";
        operationLogService.record("bug", "Bug", id, "ASSIGN", "转派Bug：" + entity.getTitle() + assignReason);
        String currentUserName = resolveUserName(CurrentUserContext.userIdOrNull());
        String projectName = resolveProjectName(entity.getProjectId());
        String creatorName = resolveUserName(entity.getCreatorId());
        String newAssigneeName = resolveUserName(request.getAssigneeId());
        noticeService.create(request.getAssigneeId(), NoticeType.BUG_ASSIGNED,
                currentUserName + " 将缺陷「" + entity.getTitle() + "」改派给您",
                "所属项目：" + projectName + "　｜　提交人：" + creatorName,
                "Bug", id);
        if (entity.getCreatorId() != null && !entity.getCreatorId().equals(request.getAssigneeId())) {
            noticeService.create(entity.getCreatorId(), NoticeType.BUG_ASSIGNED,
                    "缺陷「" + entity.getTitle() + "」已转派给 " + newAssigneeName,
                    "所属项目：" + projectName + "　｜　转派人：" + currentUserName,
                    "Bug", id);
        }
        return getById(id);
    }

    public BugResponse close(Long id) {
        BugEntity entity = requireBug(id);
        ensureMutable(entity);
        businessAccessService.requireBugClose(entity);
        String oldStatus = entity.getStatus();
        entity.setStatus(BugStatus.CLOSED.name());
        entity.setClosedAt(LocalDateTime.now());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        bugMapper.updateById(entity);
        operationLogService.record("bug", "Bug", id, "CLOSE",
                "Bug状态由" + bugStatusLabel(oldStatus) + "变为已关闭：" + entity.getTitle());
        return getById(id);
    }

    public BugResponse reopen(Long id) {
        BugEntity entity = requireBug(id);
        if (!BugStatus.CLOSED.name().equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "仅已关闭的 Bug 可以重新打开");
        }
        businessAccessService.requireBugClose(entity);
        entity.setStatus(BugStatus.PENDING_FIX.name());
        entity.setClosedAt(null);
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        bugMapper.updateById(entity);
        operationLogService.record("bug", "Bug", id, "UPDATE_STATUS",
                "Bug已关闭状态重新打开为待修复：" + entity.getTitle());
        return getById(id);
    }

    public void delete(Long id) {
        BugEntity entity = requireBug(id);
        ensureMutable(entity);
        businessAccessService.requireBugDelete(entity);
        bugMapper.deleteById(id);
        operationLogService.record("bug", "Bug", id, "DELETE", "删除Bug：" + entity.getTitle());
    }

    public BugResponse fix(Long id, BugFixRequest request) {
        BugEntity entity = requireBug(id);
        ensureMutable(entity);
        businessAccessService.requireBugEdit(entity);
        String oldStatus = entity.getStatus();
        if (request.getFixAnalysis() != null) entity.setFixAnalysis(request.getFixAnalysis());
        if (request.getFixDetail() != null) entity.setFixDetail(request.getFixDetail());
        entity.setStatus(BugStatus.PENDING_VERIFY.name());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        bugMapper.updateById(entity);
        operationLogService.record("bug", "Bug", id, "FIX",
                "Bug状态由" + bugStatusLabel(oldStatus) + "变为待验证（已提交修复）：" + entity.getTitle());
        if (entity.getCreatorId() != null && !entity.getCreatorId().equals(CurrentUserContext.userId())) {
            String fixer = resolveUserName(CurrentUserContext.userIdOrNull());
            String projectName = resolveProjectName(entity.getProjectId());
            noticeService.create(entity.getCreatorId(), NoticeType.BUG_ASSIGNED,
                    "缺陷「" + entity.getTitle() + "」已修复，待您验证",
                    "所属项目：" + projectName + "　｜　修复人：" + fixer,
                    "Bug", id);
        }
        return getById(id);
    }

    public BugResponse resolve(Long id, BugResolveRequest request) {
        BugEntity entity = requireBug(id);
        ensureMutable(entity);
        businessAccessService.requireBugEdit(entity);
        String oldStatus = entity.getStatus();
        entity.setSolution(request.getSolution());
        entity.setResolvedDate(request.getResolvedDate());
        entity.setResolveRemark(request.getRemark());
        if (request.getAssigneeId() != null) {
            entity.setAssigneeId(request.getAssigneeId());
        }
        entity.setStatus(BugStatus.PENDING_VERIFY.name());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        bugMapper.updateById(entity);
        operationLogService.record("bug", "Bug", id, "UPDATE_STATUS",
                "Bug状态由" + bugStatusLabel(oldStatus) + "变为待验证（已解决）：" + entity.getTitle());
        if (entity.getCreatorId() != null && !entity.getCreatorId().equals(CurrentUserContext.userId())) {
            String resolver = resolveUserName(CurrentUserContext.userIdOrNull());
            String projectName = resolveProjectName(entity.getProjectId());
            noticeService.create(entity.getCreatorId(), NoticeType.BUG_ASSIGNED,
                    "缺陷「" + entity.getTitle() + "」已解决，待您验证",
                    "所属项目：" + projectName + "　｜　解决人：" + resolver,
                    "Bug", id);
        }
        return getById(id);
    }

    public BugCommentResponse addComment(Long bugId, BugCommentCreateRequest request) {
        BugEntity bug = requireBug(bugId);
        ensureMutable(bug);
        businessAccessService.requireBugEdit(bug);
        BugCommentEntity comment = new BugCommentEntity();
        comment.setBugId(bugId);
        comment.setUserId(CurrentUserContext.userId());
        comment.setContent(request.getContent());
        comment.setCreatedAt(LocalDateTime.now());
        bugCommentMapper.insert(comment);
        String commentSnippet = request.getContent() != null && request.getContent().length() > 50
                ? request.getContent().substring(0, 50) + "..."
                : request.getContent();
        operationLogService.record("bug", "Bug", bugId, "COMMENT",
                "评论缺陷「" + bug.getTitle() + "」：" + commentSnippet);
        if (bug.getAssigneeId() != null && !CurrentUserContext.userId().equals(bug.getAssigneeId())) {
            String commenter = resolveUserName(CurrentUserContext.userIdOrNull());
            String creatorName = resolveUserName(bug.getCreatorId());
            noticeService.create(bug.getAssigneeId(), NoticeType.BUG_COMMENT,
                    commenter + " 在缺陷「" + bug.getTitle() + "」下发了评论",
                    "所属缺陷：「" + bug.getTitle() + "」　｜　提交人：" + creatorName,
                    "Bug", bugId);
        }
        return toCommentResponse(comment);
    }

    public List<BugCommentResponse> listComments(Long bugId) {
        requireBug(bugId);
        LambdaQueryWrapper<BugCommentEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BugCommentEntity::getBugId, bugId).orderByDesc(BugCommentEntity::getCreatedAt);
        return bugCommentMapper.selectList(wrapper).stream().map(this::toCommentResponse).toList();
    }

    private BugEntity requireBug(Long id) {
        BugEntity entity = bugMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "缺陷不存在");
        }
        return entity;
    }

    private void ensureMutable(BugEntity bug) {
        if (BugStatus.CLOSED.name().equals(bug.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "已关闭的 Bug 仅支持查看，不允许继续操作");
        }
    }

    private void applyReadScope(LambdaQueryWrapper<BugEntity> wrapper) {
        if (businessAccessService.isSystemAdmin() || businessAccessService.canViewAll()) {
            return;
        }
        Long userId = CurrentUserContext.userId();
        wrapper.and(scope -> scope.eq(BugEntity::getCreatorId, userId)
                .or()
                .eq(BugEntity::getAssigneeId, userId));
    }

    private List<Long> normalizeRelatedTaskIds(Long taskId, List<Long> relatedTaskIds) {
        Stream<Long> explicitIds = relatedTaskIds == null ? Stream.empty() : relatedTaskIds.stream();
        return Stream.concat(Stream.of(taskId), explicitIds)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Long primaryTaskId(List<Long> relatedTaskIds) {
        return relatedTaskIds.isEmpty() ? null : relatedTaskIds.get(0);
    }

    private void syncRelatedTasks(Long bugId, List<Long> relatedTaskIds) {
        bugTaskMapper.deleteByBugId(bugId);
        for (Long taskId : relatedTaskIds) {
            bugTaskMapper.insert(bugId, taskId);
        }
    }

    private List<Long> loadRelatedTaskIds(BugEntity entity) {
        List<Long> linkedIds = safeList(bugTaskMapper.findTaskIdsByBugId(entity.getId()));
        return normalizeRelatedTaskIds(entity.getTaskId(), linkedIds);
    }

    private List<BugRelatedTaskResponse> loadRelatedTasks(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return List.of();
        }
        Map<Long, String> taskNames = taskMapper.selectBatchIds(taskIds).stream()
                .collect(Collectors.toMap(TaskEntity::getId, TaskEntity::getName));
        return taskIds.stream()
                .map(id -> BugRelatedTaskResponse.builder()
                        .id(id)
                        .name(taskNames.getOrDefault(id, "任务 " + id))
                        .build())
                .toList();
    }

    private BugResponse toResponse(BugEntity entity, Map<Long, String> userNames, Map<Long, String> projectNames) {
        List<Long> relatedTaskIds = loadRelatedTaskIds(entity);
        return BugResponse.builder()
                .id(entity.getId())
                .bugNo(entity.getBugNo())
                .projectId(entity.getProjectId())
                .taskId(entity.getTaskId())
                .relatedTaskIds(relatedTaskIds)
                .relatedTasks(loadRelatedTasks(relatedTaskIds))
                .title(entity.getTitle())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .creatorId(entity.getCreatorId())
                .assigneeId(entity.getAssigneeId())
                .description(entity.getDescription())
                .reproduceSteps(entity.getReproduceSteps())
                .fixAnalysis(entity.getFixAnalysis())
                .fixDetail(entity.getFixDetail())
                .solution(entity.getSolution())
                .resolvedDate(entity.getResolvedDate())
                .resolveRemark(entity.getResolveRemark())
                .closedAt(entity.getClosedAt())
                .createdAt(entity.getCreatedAt())
                .creatorName(userNames.get(entity.getCreatorId()))
                .assigneeName(userNames.get(entity.getAssigneeId()))
                .projectName(projectNames.get(entity.getProjectId()))
                .build();
    }

    private BugCommentResponse toCommentResponse(BugCommentEntity entity) {
        String authorName = null;
        if (entity.getUserId() != null) {
            SystemUser user = userMapper.selectById(entity.getUserId());
            if (user != null) authorName = user.getRealName();
        }
        return BugCommentResponse.builder()
                .id(entity.getId())
                .bugId(entity.getBugId())
                .userId(entity.getUserId())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .authorName(authorName)
                .build();
    }

    private Map<Long, String> batchUserNames(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SystemUser::getId, SystemUser::getRealName));
    }

    private Map<Long, String> batchProjectNames(Set<Long> ids) {
        if (ids.isEmpty()) return Map.of();
        return projectMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(ProjectEntity::getId, ProjectEntity::getName));
    }

    private String resolveUserName(Long userId) {
        if (userId == null) return "系统";
        SystemUser user = userMapper.selectById(userId);
        return user != null ? user.getRealName() : "未知用户";
    }

    private String resolveProjectName(Long projectId) {
        if (projectId == null) return "未知项目";
        ProjectEntity project = projectMapper.selectById(projectId);
        return project != null ? project.getName() : "未知项目";
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private <T> Map<String, Long> countBy(List<T> values, Function<T, String> classifier) {
        return values.stream()
                .map(classifier)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private String bugStatusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "PENDING_FIX" -> "待修复";
            case "FIXING" -> "修复中";
            case "PENDING_VERIFY" -> "待验证";
            case "CLOSED" -> "已关闭";
            default -> status;
        };
    }
}
