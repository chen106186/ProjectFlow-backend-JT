package com.jitong.projectflow.bug.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.bug.domain.BugStatus;
import com.jitong.projectflow.bug.dto.BugAssignRequest;
import com.jitong.projectflow.bug.dto.BugCommentCreateRequest;
import com.jitong.projectflow.bug.dto.BugCommentResponse;
import com.jitong.projectflow.bug.dto.BugCreateRequest;
import com.jitong.projectflow.bug.dto.BugResponse;
import com.jitong.projectflow.bug.dto.BugUpdateRequest;
import com.jitong.projectflow.bug.entity.BugCommentEntity;
import com.jitong.projectflow.bug.entity.BugEntity;
import com.jitong.projectflow.bug.mapper.BugCommentMapper;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.system.audit.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BugService {
    private final BugMapper bugMapper;
    private final BugCommentMapper bugCommentMapper;
    private final OperationLogService operationLogService;
    private final NoticeService noticeService;

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
        bugMapper.insert(entity);
        operationLogService.record("bug", "Bug", entity.getId(), "CREATE", entity.getTitle());
        noticeService.create(entity.getAssigneeId(), NoticeType.BUG_ASSIGNED, "BUG assigned", entity.getTitle(), "Bug", entity.getId());
        return toResponse(entity);
    }

    public List<BugResponse> list(String status, String priority, Long projectId, String keyword) {
        LambdaQueryWrapper<BugEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(status), BugEntity::getStatus, status);
        wrapper.eq(StringUtils.hasText(priority), BugEntity::getPriority, priority);
        wrapper.eq(projectId != null, BugEntity::getProjectId, projectId);
        wrapper.like(StringUtils.hasText(keyword), BugEntity::getTitle, keyword);
        wrapper.orderByDesc(BugEntity::getCreatedAt);
        return bugMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    public List<BugResponse> listMine() {
        Long userId = CurrentUserContext.userId();
        LambdaQueryWrapper<BugEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BugEntity::getCreatorId, userId).or().eq(BugEntity::getAssigneeId, userId);
        wrapper.orderByDesc(BugEntity::getCreatedAt);
        return bugMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    public BugResponse getById(Long id) {
        return toResponse(requireBug(id));
    }

    public BugResponse update(Long id, BugUpdateRequest request) {
        BugEntity entity = requireBug(id);
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
        operationLogService.record("bug", "Bug", id, "UPDATE", entity.getTitle());
        return toResponse(entity);
    }

    public BugResponse assign(Long id, BugAssignRequest request) {
        BugEntity entity = requireBug(id);
        entity.setAssigneeId(request.getAssigneeId());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        bugMapper.updateById(entity);
        String content = StringUtils.hasText(request.getReason()) ? request.getReason() : entity.getTitle();
        operationLogService.record("bug", "Bug", id, "ASSIGN", content);
        noticeService.create(request.getAssigneeId(), NoticeType.BUG_ASSIGNED, "BUG assigned", entity.getTitle(), "Bug", id);
        return toResponse(entity);
    }

    public BugResponse close(Long id) {
        BugEntity entity = requireBug(id);
        entity.setStatus(BugStatus.CLOSED.name());
        entity.setClosedAt(LocalDateTime.now());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        bugMapper.updateById(entity);
        operationLogService.record("bug", "Bug", id, "CLOSE", entity.getTitle());
        return toResponse(entity);
    }

    public BugCommentResponse addComment(Long bugId, BugCommentCreateRequest request) {
        BugEntity bug = requireBug(bugId);
        BugCommentEntity comment = new BugCommentEntity();
        comment.setBugId(bugId);
        comment.setUserId(CurrentUserContext.userId());
        comment.setContent(request.getContent());
        comment.setCreatedAt(LocalDateTime.now());
        bugCommentMapper.insert(comment);
        operationLogService.record("bug", "Bug", bugId, "COMMENT", request.getContent());
        if (!CurrentUserContext.userId().equals(bug.getAssigneeId())) {
            noticeService.create(bug.getAssigneeId(), NoticeType.BUG_COMMENT, "BUG comment", request.getContent(), "Bug", bugId);
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
            throw new BusinessException(ErrorCode.NOT_FOUND, "Bug not found");
        }
        return entity;
    }

    private BugResponse toResponse(BugEntity entity) {
        return BugResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .taskId(entity.getTaskId())
                .title(entity.getTitle())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .creatorId(entity.getCreatorId())
                .assigneeId(entity.getAssigneeId())
                .description(entity.getDescription())
                .reproduceSteps(entity.getReproduceSteps())
                .closedAt(entity.getClosedAt())
                .build();
    }

    private BugCommentResponse toCommentResponse(BugCommentEntity entity) {
        return BugCommentResponse.builder()
                .id(entity.getId())
                .bugId(entity.getBugId())
                .userId(entity.getUserId())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
