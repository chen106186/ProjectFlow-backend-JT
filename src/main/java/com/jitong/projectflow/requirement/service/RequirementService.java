package com.jitong.projectflow.requirement.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.api.PageUtils;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.requirement.domain.RequirementStatus;
import com.jitong.projectflow.requirement.domain.RequirementStatusPolicy;
import com.jitong.projectflow.requirement.dto.RequirementCreateRequest;
import com.jitong.projectflow.requirement.dto.RequirementLogResponse;
import com.jitong.projectflow.requirement.dto.RequirementQueryRequest;
import com.jitong.projectflow.requirement.dto.RequirementResponse;
import com.jitong.projectflow.requirement.dto.RequirementStatusUpdateRequest;
import com.jitong.projectflow.requirement.dto.RequirementUpdateRequest;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.entity.OperationLog;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.OperationLogMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RequirementService {

    private final RequirementMapper requirementMapper;
    private final SystemUserMapper systemUserMapper;
    private final ProjectMapper projectMapper;
    private final OperationLogService operationLogService;
    private final OperationLogMapper operationLogMapper;
    private final NoticeService noticeService;
    private final BusinessAccessService businessAccessService;
    private final RequirementStatusPolicy statusPolicy = new RequirementStatusPolicy();

    public RequirementService(RequirementMapper requirementMapper,
                              SystemUserMapper systemUserMapper,
                              ProjectMapper projectMapper,
                              OperationLogService operationLogService,
                              OperationLogMapper operationLogMapper,
                              NoticeService noticeService,
                              BusinessAccessService businessAccessService) {
        this.requirementMapper = requirementMapper;
        this.systemUserMapper = systemUserMapper;
        this.projectMapper = projectMapper;
        this.operationLogService = operationLogService;
        this.operationLogMapper = operationLogMapper;
        this.noticeService = noticeService;
        this.businessAccessService = businessAccessService;
    }

    public RequirementResponse create(RequirementCreateRequest req) {
        RequirementEntity entity = new RequirementEntity();
        entity.setRequirementNo(requirementMapper.selectMaxRequirementNo() + 1L);
        entity.setTitle(req.title());
        entity.setRequirementType(req.requirementType());
        entity.setPriority(req.priority());
        entity.setProjectId(req.projectId());
        entity.setReviewerId(req.reviewerId());
        entity.setDescription(req.description());
        entity.setTags(req.tags());
        entity.setStatus(RequirementStatus.PENDING_REVIEW.name());
        entity.setCreatedBy(CurrentUserContext.userId());

        requirementMapper.insert(entity);

        operationLogService.record("requirement", "Requirement", entity.getId(), "CREATE",
                "提交需求：「" + entity.getTitle() + "」，状态待评审");

        return toResponse(entity, loadUserNames(java.util.Arrays.asList(entity.getCreatedBy(), entity.getReviewerId())));
    }

    public PageResult<RequirementResponse> list(RequirementQueryRequest request) {
        LambdaQueryWrapper<RequirementEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getProjectId() != null, RequirementEntity::getProjectId, request.getProjectId());
        wrapper.and(StringUtils.hasText(request.getKeyword()), w -> w
                .like(RequirementEntity::getTitle, request.getKeyword())
                .or()
                .like(RequirementEntity::getDescription, request.getKeyword()));
        wrapper.eq(StringUtils.hasText(request.getRequirementType()), RequirementEntity::getRequirementType, request.getRequirementType());
        wrapper.eq(StringUtils.hasText(request.getPriority()), RequirementEntity::getPriority, request.getPriority());
        wrapper.eq(StringUtils.hasText(request.getStatus()), RequirementEntity::getStatus, request.getStatus());
        applyReadScope(wrapper);
        wrapper.orderByDesc(RequirementEntity::getCreatedAt);
        Page<RequirementEntity> page = requirementMapper.selectPage(PageUtils.toPage(request), wrapper);
        List<RequirementEntity> records = page.getRecords();
        Map<Long, String> nameMap = loadUserNames(records.stream()
                .flatMap(entity -> java.util.stream.Stream.of(entity.getCreatedBy(), entity.getReviewerId()))
                .collect(Collectors.toList()));
        return PageUtils.toResult(page, records.stream().map(e -> toResponse(e, nameMap)).collect(Collectors.toList()));
    }

    public RequirementResponse getById(Long id) {
        RequirementEntity entity = requirementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        businessAccessService.requireRequirementManage(entity);
        return toResponse(entity, loadUserNames(java.util.Arrays.asList(entity.getCreatedBy(), entity.getReviewerId())));
    }

    public RequirementResponse update(Long id, RequirementUpdateRequest req) {
        RequirementEntity entity = requirementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        businessAccessService.requireRequirementOwner(entity);

        RequirementEntity before = copyRequirement(entity);

        if (req.title() != null) entity.setTitle(req.title());
        if (req.requirementType() != null) entity.setRequirementType(req.requirementType());
        if (req.priority() != null) entity.setPriority(req.priority());
        if (req.projectId() != null) entity.setProjectId(req.projectId());
        if (req.reviewerId() != null) entity.setReviewerId(req.reviewerId());
        if (req.description() != null) entity.setDescription(req.description());
        if (req.tags() != null) entity.setTags(req.tags());
        if (req.status() != null) entity.setStatus(req.status());
        entity.setUpdatedBy(CurrentUserContext.userId());

        requirementMapper.updateById(entity);

        operationLogService.record("requirement", "Requirement", id, "UPDATE", buildUpdateContent(before, entity));

        return toResponse(entity, loadUserNames(java.util.Arrays.asList(entity.getCreatedBy(), entity.getReviewerId())));
    }

    public RequirementResponse updateStatus(Long id, RequirementStatusUpdateRequest req) {
        RequirementEntity entity = requirementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        businessAccessService.requireRequirementOwner(entity);

        RequirementStatus from = RequirementStatus.valueOf(entity.getStatus());
        RequirementStatus to;
        try {
            to = RequirementStatus.valueOf(req.status());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的状态值: " + req.status());
        }

        if (!statusPolicy.canTransition(from, to)) {
            throw new BusinessException(ErrorCode.CONFLICT, "需求状态流转不合法");
        }

        entity.setStatus(to.name());
        entity.setUpdatedBy(CurrentUserContext.userId());

        requirementMapper.updateById(entity);

        operationLogService.record("requirement", "Requirement", id, "STATUS_CHANGE",
                "「" + entity.getTitle() + "」需求状态：" + statusLabel(from.name()) + " → " + statusLabel(to.name()));

        Long currentUserId = CurrentUserContext.userIdOrNull();
        if (entity.getCreatedBy() != null && !entity.getCreatedBy().equals(currentUserId)) {
            noticeService.create(entity.getCreatedBy(),
                    NoticeType.REQUIREMENT_STATUS_CHANGED,
                    "需求状态变更",
                    entity.getTitle() + "：" + statusLabel(from.name()) + " -> " + statusLabel(to.name()),
                    "Requirement",
                    id);
        }

        return toResponse(entity, loadUserNames(java.util.Arrays.asList(entity.getCreatedBy(), entity.getReviewerId())));
    }

    public List<RequirementResponse> listMine() {
        Long userId = CurrentUserContext.userId();
        LambdaQueryWrapper<RequirementEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RequirementEntity::getCreatedBy, userId);
        wrapper.orderByDesc(RequirementEntity::getCreatedAt);
        List<RequirementEntity> entities = requirementMapper.selectList(wrapper);
        Map<Long, String> nameMap = loadUserNames(entities.stream()
                .flatMap(entity -> java.util.stream.Stream.of(entity.getCreatedBy(), entity.getReviewerId()))
                .collect(Collectors.toList()));
        return entities.stream().map(e -> toResponse(e, nameMap)).collect(Collectors.toList());
    }

    public List<RequirementLogResponse> listLogs(Long id) {
        RequirementEntity entity = requirementMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "需求不存在");
        }
        businessAccessService.requireRequirementManage(entity);

        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OperationLog::getBusinessType, "Requirement");
        wrapper.eq(OperationLog::getBusinessId, id);
        wrapper.orderByAsc(OperationLog::getCreatedAt);
        List<OperationLog> logs = operationLogMapper.selectList(wrapper);

        List<Long> opIds = logs.stream()
                .map(OperationLog::getOperatorId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> nameMap = opIds.isEmpty() ? Collections.emptyMap()
                : systemUserMapper.selectBatchIds(opIds).stream()
                        .collect(Collectors.toMap(SystemUser::getId, u -> u.getRealName() != null ? u.getRealName() : ""));

        return logs.stream().map(log -> RequirementLogResponse.builder()
                .id(log.getId())
                .operationType(log.getOperationType())
                .operatorId(log.getOperatorId())
                .operatorName(nameMap.getOrDefault(log.getOperatorId(), ""))
                .content(log.getContent())
                .createdAt(log.getCreatedAt())
                .build())
                .collect(Collectors.toList());
    }

    private void applyReadScope(LambdaQueryWrapper<RequirementEntity> wrapper) {
        if (businessAccessService.isSystemAdmin() || businessAccessService.canViewAll()) {
            return;
        }
        Long userId = CurrentUserContext.userId();
        wrapper.and(scope -> scope.eq(RequirementEntity::getCreatedBy, userId)
                .or()
                .eq(RequirementEntity::getReviewerId, userId));
    }

    private Map<Long, String> loadUserNames(List<Long> userIds) {
        List<Long> distinctIds = userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (distinctIds.isEmpty()) return Collections.emptyMap();
        return systemUserMapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(SystemUser::getId, u -> u.getRealName() != null ? u.getRealName() : ""));
    }

    private RequirementEntity copyRequirement(RequirementEntity source) {
        RequirementEntity copy = new RequirementEntity();
        copy.setId(source.getId());
        copy.setRequirementNo(source.getRequirementNo());
        copy.setProjectId(source.getProjectId());
        copy.setReviewerId(source.getReviewerId());
        copy.setTitle(source.getTitle());
        copy.setRequirementType(source.getRequirementType());
        copy.setStatus(source.getStatus());
        copy.setPriority(source.getPriority());
        copy.setDescription(source.getDescription());
        copy.setTags(source.getTags());
        copy.setCreatedBy(source.getCreatedBy());
        copy.setCreatedAt(source.getCreatedAt());
        copy.setUpdatedBy(source.getUpdatedBy());
        copy.setUpdatedAt(source.getUpdatedAt());
        copy.setDeleted(source.getDeleted());
        return copy;
    }

    private String buildUpdateContent(RequirementEntity before, RequirementEntity after) {
        List<String> changes = new java.util.ArrayList<>();
        appendChange(changes, "需求标题", before.getTitle(), after.getTitle());
        appendChange(changes, "所属项目", projectName(before.getProjectId()), projectName(after.getProjectId()));
        appendChange(changes, "审核人", userName(before.getReviewerId()), userName(after.getReviewerId()));
        appendChange(changes, "需求类型", requirementTypeLabel(before.getRequirementType()), requirementTypeLabel(after.getRequirementType()));
        appendChange(changes, "优先级", priorityLabel(before.getPriority()), priorityLabel(after.getPriority()));
        appendChange(changes, "状态", statusLabel(before.getStatus()), statusLabel(after.getStatus()));
        appendChange(changes, "标签", before.getTags(), after.getTags());
        appendChange(changes, "需求描述", before.getDescription(), after.getDescription());

        if (changes.isEmpty()) {
            return "提交了需求更新，但内容未发生变化";
        }
        return "更新需求信息：" + String.join("；", changes);
    }

    private void appendChange(List<String> changes, String label, String before, String after) {
        String oldValue = displayValue(before);
        String newValue = displayValue(after);
        if (!Objects.equals(oldValue, newValue)) {
            changes.add(label + "由「" + oldValue + "」改为「" + newValue + "」");
        }
    }

    private String displayValue(String value) {
        return StringUtils.hasText(value) ? value : "空";
    }

    private String projectName(Long projectId) {
        if (projectId == null) return null;
        ProjectEntity project = projectMapper.selectById(projectId);
        return project == null ? String.valueOf(projectId) : project.getName();
    }

    private String userName(Long userId) {
        if (userId == null) return null;
        SystemUser user = systemUserMapper.selectById(userId);
        if (user == null) return String.valueOf(userId);
        return StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername();
    }

    private String requirementTypeLabel(String type) {
        return switch (type == null ? "" : type) {
            case "FUNCTION" -> "功能需求";
            case "OPTIMIZATION" -> "优化需求";
            case "INTEGRATION" -> "集成需求";
            case "OTHER" -> "其他";
            default -> type;
        };
    }

    private String priorityLabel(String priority) {
        return switch (priority == null ? "" : priority) {
            case "URGENT" -> "紧急";
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> priority;
        };
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "PENDING_REVIEW" -> "待评审";
            case "ACCEPTED" -> "已采纳";
            case "REJECTED" -> "已拒绝";
            default -> status;
        };
    }

    private RequirementResponse toResponse(RequirementEntity entity, Map<Long, String> nameMap) {
        return RequirementResponse.builder()
                .id(entity.getId())
                .requirementNo(entity.getRequirementNo())
                .projectId(entity.getProjectId())
                .projectName(projectName(entity.getProjectId()))
                .reviewerId(entity.getReviewerId())
                .reviewerName(nameMap.getOrDefault(entity.getReviewerId(), ""))
                .title(entity.getTitle())
                .requirementType(entity.getRequirementType())
                .status(entity.getStatus())
                .priority(entity.getPriority())
                .description(entity.getDescription())
                .tags(entity.getTags())
                .createdBy(entity.getCreatedBy())
                .creatorName(nameMap.getOrDefault(entity.getCreatedBy(), ""))
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
