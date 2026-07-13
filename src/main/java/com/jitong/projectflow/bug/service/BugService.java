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
import com.jitong.projectflow.bug.dto.BugQueryRequest;
import com.jitong.projectflow.bug.dto.BugResponse;
import com.jitong.projectflow.bug.dto.BugUpdateRequest;
import com.jitong.projectflow.bug.entity.BugCommentEntity;
import com.jitong.projectflow.bug.entity.BugEntity;
import com.jitong.projectflow.bug.mapper.BugCommentMapper;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.api.PageUtils;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    public BugResponse create(BugCreateRequest request) {
        BugEntity entity = new BugEntity();
        entity.setProjectId(request.getProjectId());
        entity.setTaskId(request.getTaskId());
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
        operationLogService.record("bug", "Bug", entity.getId(), "CREATE", "新建Bug：" + entity.getTitle());
        noticeService.create(entity.getAssigneeId(), NoticeType.BUG_ASSIGNED, "缺陷指派通知",
                entity.getTitle(), "Bug", entity.getId());
        return getById(entity.getId());
    }

    public PageResult<BugResponse> list(BugQueryRequest request) {
        LambdaQueryWrapper<BugEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(request.getStatus()), BugEntity::getStatus, request.getStatus());
        wrapper.eq(StringUtils.hasText(request.getPriority()), BugEntity::getPriority, request.getPriority());
        wrapper.eq(request.getProjectId() != null, BugEntity::getProjectId, request.getProjectId());
        wrapper.eq(request.getAssigneeId() != null, BugEntity::getAssigneeId, request.getAssigneeId());
        wrapper.like(StringUtils.hasText(request.getKeyword()), BugEntity::getTitle, request.getKeyword());
        applyReadScope(wrapper);
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

    public List<BugResponse> listMine() {
        Long userId = CurrentUserContext.userId();
        LambdaQueryWrapper<BugEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BugEntity::getCreatorId, userId).or().eq(BugEntity::getAssigneeId, userId);
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
        return toResponse(entity, userNames, projectNames);
    }

    public BugResponse update(Long id, BugUpdateRequest request) {
        BugEntity entity = requireBug(id);
        ensureMutable(entity);
        businessAccessService.requireBugEdit(entity);
        if (request.getProjectId() != null) entity.setProjectId(request.getProjectId());
        if (request.getTaskId() != null) entity.setTaskId(request.getTaskId());
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
        noticeService.create(request.getAssigneeId(), NoticeType.BUG_ASSIGNED, "缺陷转派通知",
                entity.getTitle(), "Bug", id);
        if (entity.getCreatorId() != null && !entity.getCreatorId().equals(request.getAssigneeId())) {
            noticeService.create(entity.getCreatorId(), NoticeType.BUG_ASSIGNED, "缺陷转派抄送",
                    entity.getTitle() + " 已转派给用户 " + request.getAssigneeId(), "Bug", id);
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
            noticeService.create(entity.getCreatorId(), NoticeType.BUG_ASSIGNED, "缺陷修复通知",
                    entity.getTitle() + " 已修复，请验证", "Bug", id);
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
        operationLogService.record("bug", "Bug", bugId, "COMMENT", "评论Bug：" + bug.getTitle());
        if (!CurrentUserContext.userId().equals(bug.getAssigneeId())) {
            noticeService.create(bug.getAssigneeId(), NoticeType.BUG_COMMENT, "缺陷评论通知",
                    request.getContent(), "Bug", bugId);
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

    private BugResponse toResponse(BugEntity entity, Map<Long, String> userNames, Map<Long, String> projectNames) {
        return BugResponse.builder()
                .id(entity.getId())
                .bugNo(entity.getBugNo())
                .projectId(entity.getProjectId())
                .taskId(entity.getTaskId())
                .title(entity.getTitle())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .creatorId(entity.getCreatorId())
                .assigneeId(entity.getAssigneeId())
                .description(entity.getDescription())
                .reproduceSteps(entity.getReproduceSteps())
                .fixAnalysis(entity.getFixAnalysis())
                .fixDetail(entity.getFixDetail())
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
