package com.jitong.projectflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.DateRangeValidator;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.api.PageUtils;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.project.domain.ProjectNodeTemplate;
import com.jitong.projectflow.project.domain.ProjectStatusCalculator;
import com.jitong.projectflow.project.dto.ProjectCreateRequest;
import com.jitong.projectflow.project.dto.ProjectNodeCreateRequest;
import com.jitong.projectflow.project.dto.ProjectQueryRequest;
import com.jitong.projectflow.project.dto.ProjectResponse;
import com.jitong.projectflow.project.dto.ProjectUpdateRequest;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.entity.ProjectNodeEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import com.jitong.projectflow.project.mapper.ProjectParticipantMapper;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {
    private final ProjectMapper projectMapper;
    private final ProjectNodeMapper projectNodeMapper;
    private final ProjectStatusCalculator projectStatusCalculator = new ProjectStatusCalculator();
    private final ProjectParticipantMapper participantMapper;
    private final OperationLogService operationLogService;
    private final BusinessAccessService businessAccessService;
    private final SystemUserMapper systemUserMapper;
    private final TaskMapper taskMapper;
    private final RequirementMapper requirementMapper;

    @Transactional
    public ProjectResponse create(ProjectCreateRequest request) {
        if ("EXECUTION".equals(request.getProjectType()) && request.getManagementProjectId() != null) {
            long existing = projectMapper.selectCount(new LambdaQueryWrapper<ProjectEntity>()
                    .eq(ProjectEntity::getProjectType, "EXECUTION")
                    .eq(ProjectEntity::getManagementProjectId, request.getManagementProjectId()));
            if (existing > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "该管理类项目已存在执行类项目，不可重复创建");
            }
        }
        ProjectEntity entity = new ProjectEntity();
        entity.setProjectType(request.getProjectType());
        entity.setProjectBusinessType(request.getProjectBusinessType());
        entity.setName(request.getName());
        entity.setStage(request.getStage());
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "NOT_STARTED");
        entity.setContractStatus(request.getContractStatus());
        entity.setBusinessDepartment(request.getBusinessDepartment());
        entity.setContractorUnit(request.getContractorUnit());
        entity.setBusinessSupervisor(request.getBusinessSupervisor());
        entity.setReceivableAmount(request.getReceivableAmount());
        entity.setManagerId(request.getManagerId());
        entity.setManagementProjectId(request.getManagementProjectId());
        entity.setDescription(request.getDescription());
        entity.setPlannedStartDate(request.getPlannedStartDate());
        entity.setPlannedEndDate(request.getPlannedEndDate());
        entity.setActualStartDate(request.getActualStartDate());
        entity.setActualEndDate(request.getActualEndDate());
        DateRangeValidator.validate(entity.getPlannedStartDate(), entity.getPlannedEndDate(), "计划");
        DateRangeValidator.validate(entity.getActualStartDate(), entity.getActualEndDate(), "实际");
        entity.setStatus(calculateProjectStatus(entity, request.getStatus()));
        entity.setCreatedBy(CurrentUserContext.userIdOrNull());
        projectMapper.insert(entity);
        saveParticipants(entity.getId(), request.getParticipantIds());
        List<String> nodeCodes = request.getNodeCodes();
        if ("EXECUTION".equals(entity.getProjectType())
                && CollectionUtils.isEmpty(nodeCodes)
                && CollectionUtils.isEmpty(request.getNodes())) {
            nodeCodes = executionDefaultNodeCodes(entity.getProjectBusinessType());
        }
        saveNodes(entity.getId(), entity.getProjectBusinessType(), nodeCodes, request.getNodes());
        operationLogService.record("project", "Project", entity.getId(), "CREATE", "新建项目：" + entity.getName());
        return toResponse(entity);
    }

    public PageResult<ProjectResponse> list(ProjectQueryRequest request) {
        LambdaQueryWrapper<ProjectEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StringUtils.hasText(request.getProjectType()), ProjectEntity::getProjectType, request.getProjectType());
        wrapper.eq(StringUtils.hasText(request.getProjectBusinessType()), ProjectEntity::getProjectBusinessType, request.getProjectBusinessType());
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

    @Transactional
    public ProjectResponse update(Long id, ProjectUpdateRequest request) {
        ProjectEntity entity = requireProject(id);
        businessAccessService.requireProjectManage(entity);
        if (request.getProjectType() != null) entity.setProjectType(request.getProjectType());
        if (request.getProjectBusinessType() != null) entity.setProjectBusinessType(request.getProjectBusinessType());
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getStage() != null) entity.setStage(request.getStage());
        if (request.getContractStatus() != null) entity.setContractStatus(request.getContractStatus());
        if (request.getBusinessDepartment() != null) entity.setBusinessDepartment(request.getBusinessDepartment());
        if (request.getContractorUnit() != null) entity.setContractorUnit(request.getContractorUnit());
        if (request.getBusinessSupervisor() != null) entity.setBusinessSupervisor(request.getBusinessSupervisor());
        if (request.getReceivableAmount() != null) entity.setReceivableAmount(request.getReceivableAmount());
        if (request.getManagerId() != null) entity.setManagerId(request.getManagerId());
        if (request.getManagementProjectId() != null) entity.setManagementProjectId(request.getManagementProjectId());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getPlannedStartDate() != null) entity.setPlannedStartDate(request.getPlannedStartDate());
        if (request.getPlannedEndDate() != null) entity.setPlannedEndDate(request.getPlannedEndDate());
        if (request.getActualStartDate() != null) entity.setActualStartDate(request.getActualStartDate());
        if (request.getActualEndDate() != null) entity.setActualEndDate(request.getActualEndDate());
        DateRangeValidator.validate(entity.getPlannedStartDate(), entity.getPlannedEndDate(), "计划");
        DateRangeValidator.validate(entity.getActualStartDate(), entity.getActualEndDate(), "实际");
        entity.setStatus(calculateProjectStatus(entity, request.getStatus()));
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        projectMapper.updateById(entity);
        if (request.getParticipantIds() != null) {
            saveParticipants(id, request.getParticipantIds());
        }
        if (request.getNodeCodes() != null) {
            syncNodesByCodes(id, request.getNodeCodes(), entity.getProjectBusinessType());
        }
        operationLogService.record("project", "Project", id, "UPDATE", "编辑项目：" + entity.getName());
        return toResponse(entity);
    }

    public void delete(Long id) {
        ProjectEntity entity = requireProject(id);
        businessAccessService.requireProjectManage(entity);

        if ("MANAGEMENT".equals(entity.getProjectType())) {
            long execCount = projectMapper.selectCount(
                    new LambdaQueryWrapper<ProjectEntity>()
                            .eq(ProjectEntity::getProjectType, "EXECUTION")
                            .eq(ProjectEntity::getManagementProjectId, id));
            if (execCount > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "该管理类项目已关联执行类项目，无法删除");
            }
        }

        long taskCount = taskMapper.selectCount(
                new LambdaQueryWrapper<TaskEntity>().eq(TaskEntity::getProjectId, id));
        if (taskCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "项目下存在 " + taskCount + " 个任务，无法删除");
        }

        long reqCount = requirementMapper.selectCount(
                new LambdaQueryWrapper<RequirementEntity>().eq(RequirementEntity::getProjectId, id));
        if (reqCount > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "项目下存在 " + reqCount + " 个需求，无法删除");
        }

        projectMapper.deleteById(id);
        operationLogService.record("project", "Project", id, "DELETE", "删除项目：" + entity.getName());
    }

    private ProjectEntity requireProject(Long id) {
        ProjectEntity entity = projectMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        }
        return entity;
    }

    /** 计算项目状态。只有"已暂停"可由用户手动传入；其余状态由甘特节点数据自动判定。 */
    String calculateProjectStatus(ProjectEntity entity, String requestedStatus) {
        if ("PAUSED".equals(requestedStatus)) {
            return "PAUSED";
        }
        if (entity.getId() == null) {
            return "NOT_STARTED";
        }
        List<ProjectNodeEntity> nodes = projectNodeMapper.selectList(
                new LambdaQueryWrapper<ProjectNodeEntity>()
                        .eq(ProjectNodeEntity::getProjectId, entity.getId()));
        return projectStatusCalculator.computeFromNodes(nodes);
    }

    private void applyReadScope(LambdaQueryWrapper<ProjectEntity> wrapper) {
    }

    private ProjectResponse toResponse(ProjectEntity entity) {
        ProjectResponse response = new ProjectResponse();
        response.setId(entity.getId());
        response.setProjectType(entity.getProjectType());
        response.setProjectBusinessType(entity.getProjectBusinessType());
        response.setName(entity.getName());
        response.setStage(entity.getStage());
        response.setStatus(entity.getStatus());
        response.setContractStatus(entity.getContractStatus());
        response.setBusinessDepartment(entity.getBusinessDepartment());
        response.setContractorUnit(entity.getContractorUnit());
        response.setBusinessSupervisor(entity.getBusinessSupervisor());
        response.setReceivableAmount(entity.getReceivableAmount());
        response.setManagerId(entity.getManagerId());
        response.setManagementProjectId(entity.getManagementProjectId());
        response.setParticipantIds(participantMapper.selectUserIdsByProjectId(entity.getId()));
        response.setType(projectBusinessTypeLabel(entity.getProjectBusinessType()));
        response.setDepartment(entity.getBusinessDepartment());
        response.setContractor(entity.getContractorUnit());
        response.setSupervisor(entity.getBusinessSupervisor());
        response.setAmount(entity.getReceivableAmount());
        response.setDescription(entity.getDescription());
        response.setPlannedStartDate(entity.getPlannedStartDate());
        response.setPlannedEndDate(entity.getPlannedEndDate());
        response.setActualStartDate(entity.getActualStartDate());
        response.setActualEndDate(entity.getActualEndDate());
        return response;
    }

    private void saveParticipants(Long projectId, List<Long> participantIds) {
        participantMapper.deleteByProjectId(projectId);
        if (!CollectionUtils.isEmpty(participantIds)) {
            participantMapper.batchInsert(projectId, participantIds);
        }
    }

    private List<String> executionDefaultNodeCodes(String businessType) {
        return switch (businessType == null ? "" : businessType) {
            case "DIGITALIZATION" -> List.of("DEVELOPMENT", "TESTING", "THIRD_PARTY_TESTING", "DEPLOYMENT", "TRIAL_RUN");
            case "EXTERNAL"       -> List.of("DEVELOPMENT", "TESTING", "DEPLOYMENT_IMPLEMENTATION");
            default               -> List.of();
        };
    }

    private void saveNodes(Long projectId, String businessType, List<String> nodeCodes, List<ProjectNodeCreateRequest> manualNodes) {
        Long createdBy = CurrentUserContext.userIdOrNull();
        Optional<ProjectNodeTemplate> template = ProjectNodeTemplate.forBusinessType(businessType);

        if (!CollectionUtils.isEmpty(nodeCodes) && template.isPresent()) {
            Map<String, ProjectNodeTemplate.NodeDef> defMap = template.get().getNodes().stream()
                    .collect(Collectors.toMap(ProjectNodeTemplate.NodeDef::code, d -> d));
            for (String code : nodeCodes) {
                ProjectNodeTemplate.NodeDef def = defMap.get(code);
                if (def == null) continue;
                ProjectNodeEntity node = new ProjectNodeEntity();
                node.setProjectId(projectId);
                node.setNodeCode(def.code());
                node.setNodeName(def.label());
                node.setNodeType("TASK");
                node.setSortOrder(def.sortOrder());
                node.setStatus("NOT_STARTED");
                node.setProgressPercent(0);
                node.setCreatedBy(createdBy);
                projectNodeMapper.insert(node);
            }
            return;
        }

        if (template.isPresent() && CollectionUtils.isEmpty(manualNodes)) {
            for (ProjectNodeTemplate.NodeDef def : template.get().getNodes()) {
                ProjectNodeEntity node = new ProjectNodeEntity();
                node.setProjectId(projectId);
                node.setNodeCode(def.code());
                node.setNodeName(def.label());
                node.setNodeType("TASK");
                node.setSortOrder(def.sortOrder());
                node.setStatus("NOT_STARTED");
                node.setProgressPercent(0);
                node.setCreatedBy(createdBy);
                projectNodeMapper.insert(node);
            }
            return;
        }

        if (CollectionUtils.isEmpty(manualNodes)) return;
        for (int i = 0; i < manualNodes.size(); i++) {
            ProjectNodeCreateRequest req = manualNodes.get(i);
            if (!StringUtils.hasText(req.getNodeName())) continue;
            ProjectNodeEntity node = new ProjectNodeEntity();
            node.setProjectId(projectId);
            node.setNodeName(req.getNodeName());
            node.setNodeType("TASK");
            node.setPlannedStartDate(req.getPlannedStartDate());
            node.setPlannedEndDate(req.getPlannedEndDate());
            node.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : i + 1);
            node.setStatus("NOT_STARTED");
            node.setProgressPercent(0);
            node.setCreatedBy(createdBy);
            projectNodeMapper.insert(node);
        }
    }

    private void syncNodesByCodes(Long projectId, List<String> newCodes, String businessType) {
        List<ProjectNodeEntity> existing = projectNodeMapper.selectList(
                new LambdaQueryWrapper<ProjectNodeEntity>().eq(ProjectNodeEntity::getProjectId, projectId));
        Set<String> existingCodes = existing.stream()
                .filter(n -> n.getNodeCode() != null)
                .map(ProjectNodeEntity::getNodeCode)
                .collect(Collectors.toSet());
        // delete nodes not in newCodes (code-based or legacy name-based)
        existing.stream()
                .filter(n -> n.getNodeCode() == null || !newCodes.contains(n.getNodeCode()))
                .forEach(n -> projectNodeMapper.deleteById(n.getId()));
        // insert newly selected nodes from template
        Optional<ProjectNodeTemplate> template = ProjectNodeTemplate.forBusinessType(businessType);
        if (template.isEmpty()) return;
        Map<String, ProjectNodeTemplate.NodeDef> defMap = template.get().getNodes().stream()
                .collect(Collectors.toMap(ProjectNodeTemplate.NodeDef::code, d -> d));
        Long createdBy = CurrentUserContext.userIdOrNull();
        for (String code : newCodes) {
            if (existingCodes.contains(code)) continue;
            ProjectNodeTemplate.NodeDef def = defMap.get(code);
            if (def == null) continue;
            ProjectNodeEntity node = new ProjectNodeEntity();
            node.setProjectId(projectId);
            node.setNodeCode(def.code());
            node.setNodeName(def.label());
            node.setNodeType("TASK");
            node.setSortOrder(def.sortOrder());
            node.setStatus("NOT_STARTED");
            node.setProgressPercent(0);
            node.setCreatedBy(createdBy);
            projectNodeMapper.insert(node);
        }
    }

    private String projectBusinessTypeLabel(String projectBusinessType) {
        if (!StringUtils.hasText(projectBusinessType)) {
            return null;
        }
        return switch (projectBusinessType) {
            case "DIGITALIZATION" -> "数字化项目";
            case "INFORMATIZATION" -> "信息化项目";
            case "RESEARCH" -> "科研项目";
            case "EXTERNAL" -> "外部项目";
            default -> projectBusinessType;
        };
    }
}