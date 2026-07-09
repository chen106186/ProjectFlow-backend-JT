package com.jitong.projectflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.api.PageUtils;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.project.dto.ProjectCreateRequest;
import com.jitong.projectflow.project.dto.ProjectQueryRequest;
import com.jitong.projectflow.project.dto.ProjectResponse;
import com.jitong.projectflow.project.dto.ProjectUpdateRequest;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectMapper projectMapper;
    private final OperationLogService operationLogService;
    private final BusinessAccessService businessAccessService;

    public ProjectResponse create(ProjectCreateRequest request) {
        ProjectEntity entity = new ProjectEntity();
        entity.setProjectType(request.getProjectType());
        entity.setName(request.getName());
        entity.setStage(request.getStage());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "NOT_STARTED");
        entity.setContractStatus(request.getContractStatus());
        entity.setBusinessDepartment(request.getBusinessDepartment());
        entity.setContractorUnit(request.getContractorUnit());
        entity.setBusinessSupervisor(request.getBusinessSupervisor());
        entity.setReceivableAmount(request.getReceivableAmount());
        entity.setManagerId(request.getManagerId());
        entity.setDescription(request.getDescription());
        entity.setPlannedStartDate(request.getPlannedStartDate());
        entity.setPlannedEndDate(request.getPlannedEndDate());
        entity.setActualStartDate(request.getActualStartDate());
        entity.setActualEndDate(request.getActualEndDate());
        entity.setCreatedBy(CurrentUserContext.userIdOrNull());
        projectMapper.insert(entity);
        operationLogService.record("project", "Project", entity.getId(), "CREATE", entity.getName());
        return toResponse(entity);
    }

    public PageResult<ProjectResponse> list(ProjectQueryRequest request) {
        LambdaQueryWrapper<ProjectEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(request.getProjectType()), ProjectEntity::getProjectType, request.getProjectType());
        wrapper.eq(StringUtils.hasText(request.getStatus()), ProjectEntity::getStatus, request.getStatus());
        wrapper.eq(StringUtils.hasText(request.getContractStatus()), ProjectEntity::getContractStatus, request.getContractStatus());
        wrapper.eq(request.getManagerId() != null, ProjectEntity::getManagerId, request.getManagerId());
        wrapper.like(StringUtils.hasText(request.getKeyword()), ProjectEntity::getName, request.getKeyword());
        wrapper.eq(StringUtils.hasText(request.getStage()), ProjectEntity::getStage, request.getStage());
        applyReadScope(wrapper);
        wrapper.orderByDesc(ProjectEntity::getCreatedAt);
        Page<ProjectEntity> page = projectMapper.selectPage(PageUtils.toPage(request), wrapper);
        return PageUtils.toResult(page, page.getRecords().stream().map(this::toResponse).toList());
    }

    public ProjectResponse getById(Long id) {
        return toResponse(requireProject(id));
    }

    public ProjectResponse update(Long id, ProjectUpdateRequest request) {
        ProjectEntity entity = requireProject(id);
        businessAccessService.requireProjectManage(entity);
        if (request.getProjectType() != null) entity.setProjectType(request.getProjectType());
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getStage() != null) entity.setStage(request.getStage());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        if (request.getContractStatus() != null) entity.setContractStatus(request.getContractStatus());
        if (request.getBusinessDepartment() != null) entity.setBusinessDepartment(request.getBusinessDepartment());
        if (request.getContractorUnit() != null) entity.setContractorUnit(request.getContractorUnit());
        if (request.getBusinessSupervisor() != null) entity.setBusinessSupervisor(request.getBusinessSupervisor());
        if (request.getReceivableAmount() != null) entity.setReceivableAmount(request.getReceivableAmount());
        if (request.getManagerId() != null) entity.setManagerId(request.getManagerId());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getPlannedStartDate() != null) entity.setPlannedStartDate(request.getPlannedStartDate());
        if (request.getPlannedEndDate() != null) entity.setPlannedEndDate(request.getPlannedEndDate());
        if (request.getActualStartDate() != null) entity.setActualStartDate(request.getActualStartDate());
        if (request.getActualEndDate() != null) entity.setActualEndDate(request.getActualEndDate());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        projectMapper.updateById(entity);
        operationLogService.record("project", "Project", id, "UPDATE", entity.getName());
        return toResponse(entity);
    }

    public void delete(Long id) {
        ProjectEntity entity = requireProject(id);
        businessAccessService.requireProjectManage(entity);
        projectMapper.deleteById(id);
        operationLogService.record("project", "Project", id, "DELETE", entity.getName());
    }

    private ProjectEntity requireProject(Long id) {
        ProjectEntity entity = projectMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        return entity;
    }

    private void applyReadScope(LambdaQueryWrapper<ProjectEntity> wrapper) {
        if (businessAccessService.isSystemAdmin()) {
            return;
        }
        Long userId = CurrentUserContext.userId();
        wrapper.and(scope -> scope.eq(ProjectEntity::getManagerId, userId)
                .or()
                .eq(ProjectEntity::getCreatedBy, userId));
    }

    private ProjectResponse toResponse(ProjectEntity entity) {
        return ProjectResponse.builder()
                .id(entity.getId())
                .projectType(entity.getProjectType())
                .name(entity.getName())
                .stage(entity.getStage())
                .status(entity.getStatus())
                .contractStatus(entity.getContractStatus())
                .businessDepartment(entity.getBusinessDepartment())
                .contractorUnit(entity.getContractorUnit())
                .businessSupervisor(entity.getBusinessSupervisor())
                .receivableAmount(entity.getReceivableAmount())
                .managerId(entity.getManagerId())
                .description(entity.getDescription())
                .plannedStartDate(entity.getPlannedStartDate())
                .plannedEndDate(entity.getPlannedEndDate())
                .actualStartDate(entity.getActualStartDate())
                .actualEndDate(entity.getActualEndDate())
                .build();
    }
}
