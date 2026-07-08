package com.jitong.projectflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.project.domain.GanttNodeSummaryCalculator;
import com.jitong.projectflow.project.domain.GanttSummaryData;
import com.jitong.projectflow.project.dto.GanttNodeResponse;
import com.jitong.projectflow.project.dto.GanttSummaryResponse;
import com.jitong.projectflow.project.dto.ProjectNodeUpdateRequest;
import com.jitong.projectflow.project.entity.ProjectNodeEntity;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GanttService {

    private final ProjectNodeMapper projectNodeMapper;
    private final GanttNodeSummaryCalculator calculator = new GanttNodeSummaryCalculator();
    private final OperationLogService operationLogService;

    public GanttService(ProjectNodeMapper projectNodeMapper, OperationLogService operationLogService) {
        this.projectNodeMapper = projectNodeMapper;
        this.operationLogService = operationLogService;
    }

    public List<GanttNodeResponse> getGanttNodes(Long projectId) {
        LambdaQueryWrapper<ProjectNodeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectNodeEntity::getProjectId, projectId)
               .orderByAsc(ProjectNodeEntity::getSortOrder);
        List<ProjectNodeEntity> entities = projectNodeMapper.selectList(wrapper);
        return entities.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public GanttNodeResponse updateNode(Long projectId, Long nodeId, ProjectNodeUpdateRequest req) {
        ProjectNodeEntity entity = projectNodeMapper.selectById(nodeId);
        if (entity == null || !projectId.equals(entity.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "项目节点不存在");
        }

        if (req.getNodeName() != null) entity.setNodeName(req.getNodeName());
        if (req.getStatus() != null) entity.setStatus(req.getStatus());
        if (req.getProgressPercent() != null) entity.setProgressPercent(req.getProgressPercent());
        if (req.getPlannedStartDate() != null) entity.setPlannedStartDate(req.getPlannedStartDate());
        if (req.getPlannedEndDate() != null) entity.setPlannedEndDate(req.getPlannedEndDate());
        if (req.getActualStartDate() != null) entity.setActualStartDate(req.getActualStartDate());
        if (req.getActualEndDate() != null) entity.setActualEndDate(req.getActualEndDate());
        validateNode(entity);
        if (req.getActualEndDate() != null) {
            entity.setProgressPercent(100);
            entity.setStatus("COMPLETED");
        }
        entity.setUpdatedBy(CurrentUserContext.userId());

        projectNodeMapper.updateById(entity);

        String logContent = req.getNodeName() != null ? req.getNodeName() : nodeId.toString();
        operationLogService.record("project", "ProjectNode", nodeId, "UPDATE", logContent);

        return toResponse(entity);
    }

    public GanttSummaryResponse getSummary(Long projectId) {
        LambdaQueryWrapper<ProjectNodeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectNodeEntity::getProjectId, projectId);
        List<ProjectNodeEntity> entities = projectNodeMapper.selectList(wrapper);

        List<String> statuses = entities.stream()
                .map(ProjectNodeEntity::getStatus)
                .collect(Collectors.toList());
        List<LocalDate> actualEndDates = entities.stream()
                .map(ProjectNodeEntity::getActualEndDate)
                .collect(Collectors.toList());
        List<LocalDate> plannedEndDates = entities.stream()
                .map(ProjectNodeEntity::getPlannedEndDate)
                .collect(Collectors.toList());
        List<Integer> progressPercents = entities.stream()
                .map(ProjectNodeEntity::getProgressPercent)
                .collect(Collectors.toList());

        GanttSummaryData data = calculator.calculate(statuses, actualEndDates, plannedEndDates, progressPercents, LocalDate.now());

        return GanttSummaryResponse.builder()
                .total(data.total())
                .completed(data.completed())
                .overdue(data.overdue())
                .dueSoon(data.dueSoon())
                .overallProgress(data.overallProgress())
                .build();
    }

    private void validateNode(ProjectNodeEntity entity) {
        if (entity.getProgressPercent() != null && (entity.getProgressPercent() < 0 || entity.getProgressPercent() > 100)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "节点进度必须在0到100之间");
        }
        if (entity.getPlannedStartDate() != null && entity.getPlannedEndDate() != null
                && entity.getPlannedStartDate().isAfter(entity.getPlannedEndDate())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "计划开始日期不能晚于计划结束日期");
        }
        if (entity.getActualStartDate() != null && entity.getActualEndDate() != null
                && entity.getActualStartDate().isAfter(entity.getActualEndDate())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "实际开始日期不能晚于实际结束日期");
        }
    }

    private GanttNodeResponse toResponse(ProjectNodeEntity entity) {
        return GanttNodeResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .parentId(entity.getParentId())
                .nodeName(entity.getNodeName())
                .nodeType(entity.getNodeType())
                .plannedStartDate(entity.getPlannedStartDate())
                .plannedEndDate(entity.getPlannedEndDate())
                .actualStartDate(entity.getActualStartDate())
                .actualEndDate(entity.getActualEndDate())
                .status(entity.getStatus())
                .progressPercent(entity.getProgressPercent())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
