package com.jitong.projectflow.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.api.PageUtils;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.report.dto.ProjectReportCreateRequest;
import com.jitong.projectflow.report.dto.ProjectReportItemCreateRequest;
import com.jitong.projectflow.report.dto.ProjectReportItemResponse;
import com.jitong.projectflow.report.dto.ProjectReportItemUpdateRequest;
import com.jitong.projectflow.report.dto.ProjectReportQueryRequest;
import com.jitong.projectflow.report.dto.ProjectReportResponse;
import com.jitong.projectflow.report.dto.ProjectReportStatusUpdateRequest;
import com.jitong.projectflow.report.dto.ProjectReportUpdateRequest;
import com.jitong.projectflow.report.entity.ProjectReportEntity;
import com.jitong.projectflow.report.entity.ProjectReportItemEntity;
import com.jitong.projectflow.report.mapper.ProjectReportItemMapper;
import com.jitong.projectflow.report.mapper.ProjectReportMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectReportService {
    private static final String DEFAULT_STATUS = "PREPARING";

    private final ProjectReportMapper projectReportMapper;
    private final ProjectReportItemMapper projectReportItemMapper;
    private final OperationLogService operationLogService;
    private final BusinessAccessService businessAccessService;

    public ProjectReportResponse create(ProjectReportCreateRequest request) {
        ProjectReportEntity entity = new ProjectReportEntity();
        entity.setProjectId(request.getProjectId());
        entity.setTitle(request.getTitle());
        entity.setReportType(request.getReportType());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : DEFAULT_STATUS);
        entity.setPlannedDate(request.getPlannedDate());
        entity.setActualDate(request.getActualDate());
        entity.setTargetAudience(request.getTargetAudience());
        entity.setLocationMethod(request.getLocationMethod());
        entity.setDescription(request.getDescription());
        entity.setRelatedTaskId(request.getRelatedTaskId());
        entity.setCreatedBy(CurrentUserContext.userIdOrNull());
        projectReportMapper.insert(entity);
        operationLogService.record("project-report", "ProjectReport", entity.getId(), "CREATE", "新建汇报：" + entity.getTitle());
        return toResponse(entity, List.of());
    }

    public PageResult<ProjectReportResponse> list(ProjectReportQueryRequest request) {
        Page<ProjectReportEntity> page = projectReportMapper.selectPage(PageUtils.toPage(request), buildQuery(request));
        return PageUtils.toResult(page, page.getRecords().stream().map(entity -> toResponse(entity, List.of())).toList());
    }

    public ProjectReportResponse getById(Long id) {
        ProjectReportEntity entity = requireReport(id);
        return toResponse(entity, listItemEntities(id).stream().map(this::toItemResponse).toList());
    }

    public ProjectReportResponse update(Long id, ProjectReportUpdateRequest request) {
        ProjectReportEntity entity = requireReport(id);
        businessAccessService.requireProjectReportManage(entity);
        if (request.getProjectId() != null) entity.setProjectId(request.getProjectId());
        if (request.getTitle() != null) entity.setTitle(request.getTitle());
        if (request.getReportType() != null) entity.setReportType(request.getReportType());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        if (request.getPlannedDate() != null) entity.setPlannedDate(request.getPlannedDate());
        if (request.getActualDate() != null) entity.setActualDate(request.getActualDate());
        if (request.getTargetAudience() != null) entity.setTargetAudience(request.getTargetAudience());
        if (request.getLocationMethod() != null) entity.setLocationMethod(request.getLocationMethod());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getRemark() != null) entity.setRemark(request.getRemark());
        if (request.getRelatedTaskId() != null) entity.setRelatedTaskId(request.getRelatedTaskId());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        projectReportMapper.updateById(entity);
        operationLogService.record("project-report", "ProjectReport", id, "UPDATE", "编辑汇报：" + entity.getTitle());
        return getById(id);
    }

    public ProjectReportResponse updateStatus(Long id, ProjectReportStatusUpdateRequest request) {
        ProjectReportEntity entity = requireReport(id);
        businessAccessService.requireProjectReportManage(entity);
        String oldStatus = entity.getStatus();
        entity.setStatus(request.getStatus());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        projectReportMapper.updateById(entity);
        operationLogService.record("project-report", "ProjectReport", id, "UPDATE_STATUS",
                "汇报状态由" + reportStatusLabel(oldStatus) + "变为" + reportStatusLabel(request.getStatus()) + "：" + entity.getTitle());
        return toResponse(entity, listItemEntities(id).stream().map(this::toItemResponse).toList());
    }

    public void delete(Long id) {
        ProjectReportEntity entity = requireReport(id);
        businessAccessService.requireProjectReportManage(entity);
        projectReportMapper.deleteById(id);
        operationLogService.record("project-report", "ProjectReport", id, "DELETE", "删除汇报：" + entity.getTitle());
    }

    public ProjectReportItemResponse createItem(Long reportId, ProjectReportItemCreateRequest request) {
        ProjectReportEntity report = requireReport(reportId);
        businessAccessService.requireProjectReportManage(report);
        ProjectReportItemEntity entity = new ProjectReportItemEntity();
        entity.setReportId(reportId);
        entity.setContent(request.getContent());
        entity.setOwnerId(request.getOwnerId());
        entity.setPriority(request.getPriority());
        entity.setStatus(request.getStatus());
        entity.setPlannedDate(request.getPlannedDate());
        entity.setDescription(request.getDescription());
        entity.setRelatedTaskId(request.getRelatedTaskId());
        entity.setCreatedBy(CurrentUserContext.userIdOrNull());
        projectReportItemMapper.insert(entity);
        operationLogService.record("project-report", "ProjectReport", reportId, "CREATE_ITEM", "新增准备项：" + entity.getContent());
        return toItemResponse(entity);
    }

    public ProjectReportItemResponse updateItem(Long reportId, Long itemId, ProjectReportItemUpdateRequest request) {
        ProjectReportEntity report = requireReport(reportId);
        businessAccessService.requireProjectReportManage(report);
        ProjectReportItemEntity entity = requireItem(reportId, itemId);
        if (request.getContent() != null) entity.setContent(request.getContent());
        if (request.getOwnerId() != null) entity.setOwnerId(request.getOwnerId());
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        if (request.getPlannedDate() != null) entity.setPlannedDate(request.getPlannedDate());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getRelatedTaskId() != null) entity.setRelatedTaskId(request.getRelatedTaskId());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        projectReportItemMapper.updateById(entity);
        operationLogService.record("project-report", "ProjectReport", reportId, "UPDATE_ITEM", "编辑准备项：" + entity.getContent());
        return toItemResponse(entity);
    }

    public void deleteItem(Long reportId, Long itemId) {
        ProjectReportEntity report = requireReport(reportId);
        businessAccessService.requireProjectReportManage(report);
        ProjectReportItemEntity entity = requireItem(reportId, itemId);
        projectReportItemMapper.deleteById(itemId);
        operationLogService.record("project-report", "ProjectReport", reportId, "DELETE_ITEM", "删除准备项：" + entity.getContent());
    }

    private LambdaQueryWrapper<ProjectReportEntity> buildQuery(ProjectReportQueryRequest request) {
        LambdaQueryWrapper<ProjectReportEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getProjectId() != null, ProjectReportEntity::getProjectId, request.getProjectId());
        wrapper.eq(StringUtils.hasText(request.getStatus()), ProjectReportEntity::getStatus, request.getStatus());
        wrapper.eq(StringUtils.hasText(request.getReportType()), ProjectReportEntity::getReportType, request.getReportType());
        wrapper.ge(request.getPlannedDateFrom() != null, ProjectReportEntity::getPlannedDate, request.getPlannedDateFrom());
        wrapper.le(request.getPlannedDateTo() != null, ProjectReportEntity::getPlannedDate, request.getPlannedDateTo());
        wrapper.like(StringUtils.hasText(request.getKeyword()), ProjectReportEntity::getTitle, request.getKeyword());
        wrapper.orderByDesc(ProjectReportEntity::getPlannedDate);
        wrapper.orderByDesc(ProjectReportEntity::getCreatedAt);
        return wrapper;
    }

    private ProjectReportEntity requireReport(Long id) {
        ProjectReportEntity entity = projectReportMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目汇报不存在");
        }
        return entity;
    }

    private ProjectReportItemEntity requireItem(Long reportId, Long itemId) {
        ProjectReportItemEntity entity = projectReportItemMapper.selectById(itemId);
        if (entity == null || !reportId.equals(entity.getReportId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "汇报准备项不存在");
        }
        return entity;
    }

    private List<ProjectReportItemEntity> listItemEntities(Long reportId) {
        LambdaQueryWrapper<ProjectReportItemEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectReportItemEntity::getReportId, reportId).orderByAsc(ProjectReportItemEntity::getCreatedAt);
        return projectReportItemMapper.selectList(wrapper);
    }

    private ProjectReportResponse toResponse(ProjectReportEntity entity, List<ProjectReportItemResponse> items) {
        return ProjectReportResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .title(entity.getTitle())
                .reportType(entity.getReportType())
                .status(entity.getStatus())
                .plannedDate(entity.getPlannedDate())
                .actualDate(entity.getActualDate())
                .targetAudience(entity.getTargetAudience())
                .locationMethod(entity.getLocationMethod())
                .description(entity.getDescription())
                .remark(entity.getRemark())
                .relatedTaskId(entity.getRelatedTaskId())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .items(items)
                .build();
    }

    private ProjectReportItemResponse toItemResponse(ProjectReportItemEntity entity) {
        return ProjectReportItemResponse.builder()
                .id(entity.getId())
                .reportId(entity.getReportId())
                .content(entity.getContent())
                .ownerId(entity.getOwnerId())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .plannedDate(entity.getPlannedDate())
                .description(entity.getDescription())
                .relatedTaskId(entity.getRelatedTaskId())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String reportStatusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "PREPARING" -> "准备中";
            case "IN_PROGRESS" -> "进行中";
            case "COMPLETED" -> "已完成";
            case "CANCELLED" -> "已取消";
            default -> status;
        };
    }
}
