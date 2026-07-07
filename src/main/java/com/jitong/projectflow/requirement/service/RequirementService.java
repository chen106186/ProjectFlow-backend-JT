package com.jitong.projectflow.requirement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.api.PageUtils;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.requirement.domain.RequirementStatus;
import com.jitong.projectflow.requirement.domain.RequirementStatusPolicy;
import com.jitong.projectflow.requirement.dto.RequirementCreateRequest;
import com.jitong.projectflow.requirement.dto.RequirementQueryRequest;
import com.jitong.projectflow.requirement.dto.RequirementResponse;
import com.jitong.projectflow.requirement.dto.RequirementStatusUpdateRequest;
import com.jitong.projectflow.requirement.dto.RequirementUpdateRequest;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RequirementService {

    private final RequirementMapper requirementMapper;
    private final OperationLogService operationLogService;
    private final RequirementStatusPolicy statusPolicy = new RequirementStatusPolicy();

    public RequirementService(RequirementMapper requirementMapper, OperationLogService operationLogService) {
        this.requirementMapper = requirementMapper;
        this.operationLogService = operationLogService;
    }

    public RequirementResponse create(RequirementCreateRequest req) {
        RequirementEntity entity = new RequirementEntity();
        entity.setTitle(req.title());
        entity.setRequirementType(req.requirementType());
        entity.setPriority(req.priority());
        entity.setProjectId(req.projectId());
        entity.setDescription(req.description());
        entity.setTags(req.tags());
        entity.setStatus(RequirementStatus.PENDING_REVIEW.name());
        entity.setCreatedBy(CurrentUserContext.userId());

        requirementMapper.insert(entity);

        operationLogService.record("requirement", "Requirement", entity.getId(), "CREATE", req.title());

        return toResponse(entity);
    }

    public PageResult<RequirementResponse> list(RequirementQueryRequest request) {
        LambdaQueryWrapper<RequirementEntity> wrapper = new LambdaQueryWrapper<>();
        if (request.getProjectId() != null) {
            wrapper.eq(RequirementEntity::getProjectId, request.getProjectId());
        }
        Page<RequirementEntity> page = requirementMapper.selectPage(PageUtils.toPage(request), wrapper);
        return PageUtils.toResult(page, page.getRecords().stream().map(this::toResponse).collect(Collectors.toList()));
    }

    public RequirementResponse getById(Long id) {
        RequirementEntity entity = requirementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Requirement not found: " + id);
        }
        return toResponse(entity);
    }

    public RequirementResponse update(Long id, RequirementUpdateRequest req) {
        RequirementEntity entity = requirementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Requirement not found: " + id);
        }

        if (req.title() != null) entity.setTitle(req.title());
        if (req.requirementType() != null) entity.setRequirementType(req.requirementType());
        if (req.priority() != null) entity.setPriority(req.priority());
        if (req.projectId() != null) entity.setProjectId(req.projectId());
        if (req.description() != null) entity.setDescription(req.description());
        if (req.tags() != null) entity.setTags(req.tags());
        entity.setUpdatedBy(CurrentUserContext.userId());

        requirementMapper.updateById(entity);

        operationLogService.record("requirement", "Requirement", id, "UPDATE", entity.getTitle());

        return toResponse(entity);
    }

    public RequirementResponse updateStatus(Long id, RequirementStatusUpdateRequest req) {
        RequirementEntity entity = requirementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Requirement not found: " + id);
        }

        RequirementStatus from = RequirementStatus.valueOf(entity.getStatus());
        RequirementStatus to;
        try {
            to = RequirementStatus.valueOf(req.status());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid status: " + req.status());
        }

        if (!statusPolicy.canTransition(from, to)) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "Cannot transition from " + from + " to " + to);
        }

        entity.setStatus(to.name());
        entity.setUpdatedBy(CurrentUserContext.userId());

        requirementMapper.updateById(entity);

        operationLogService.record("requirement", "Requirement", id, "STATUS_CHANGE",
                from.name() + " -> " + to.name());

        return toResponse(entity);
    }

    public List<RequirementResponse> listMine() {
        Long userId = CurrentUserContext.userId();
        LambdaQueryWrapper<RequirementEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequirementEntity::getCreatedBy, userId);
        List<RequirementEntity> entities = requirementMapper.selectList(wrapper);
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    private RequirementResponse toResponse(RequirementEntity entity) {
        return RequirementResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .title(entity.getTitle())
                .requirementType(entity.getRequirementType())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .description(entity.getDescription())
                .tags(entity.getTags())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
