package com.jitong.projectflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.project.domain.GanttNodeSummaryCalculator;
import com.jitong.projectflow.project.domain.GanttSummaryData;
import com.jitong.projectflow.project.domain.ProjectNodeTemplate;
import com.jitong.projectflow.project.dto.GanttNodeResponse;
import com.jitong.projectflow.project.dto.GanttSummaryResponse;
import com.jitong.projectflow.project.dto.ProjectNodeUpdateRequest;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.entity.ProjectNodeEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.entity.RoleEntity;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.RoleMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.system.mapper.UserRoleMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GanttService {

    private final ProjectNodeMapper projectNodeMapper;
    private final GanttNodeSummaryCalculator calculator = new GanttNodeSummaryCalculator();
    private final OperationLogService operationLogService;
    private final NoticeService noticeService;
    private final ProjectMapper projectMapper;
    private final SystemUserMapper systemUserMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;

    public GanttService(ProjectNodeMapper projectNodeMapper, OperationLogService operationLogService,
                        NoticeService noticeService, ProjectMapper projectMapper,
                        SystemUserMapper systemUserMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper) {
        this.projectNodeMapper = projectNodeMapper;
        this.operationLogService = operationLogService;
        this.noticeService = noticeService;
        this.projectMapper = projectMapper;
        this.systemUserMapper = systemUserMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
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

        String oldStatus = entity.getStatus();
        if (req.getNodeName() != null) entity.setNodeName(req.getNodeName());
        if (req.getStatus() != null) entity.setStatus(req.getStatus());
        if (req.getProgressPercent() != null) entity.setProgressPercent(req.getProgressPercent());
        if (req.getPlannedStartDate() != null) entity.setPlannedStartDate(req.getPlannedStartDate());
        if (req.getPlannedEndDate() != null) entity.setPlannedEndDate(req.getPlannedEndDate());
        if (req.getActualStartDate() != null) entity.setActualStartDate(req.getActualStartDate());
        if (req.getActualEndDate() != null) entity.setActualEndDate(req.getActualEndDate());
        validateNode(entity);
        entity.setUpdatedBy(CurrentUserContext.userId());

        projectNodeMapper.updateById(entity);

        String logContent = req.getNodeName() != null ? req.getNodeName() : nodeId.toString();
        operationLogService.record("project", "ProjectNode", nodeId, "UPDATE", logContent);

        if (req.getStatus() != null && !req.getStatus().equals(oldStatus)) {
            sendStageChangeNotice(entity);
        }

        return toResponse(entity);
    }

    private void sendStageChangeNotice(ProjectNodeEntity node) {
        ProjectEntity project = projectMapper.selectById(node.getProjectId());
        if (project == null) return;
        String projectName = project.getName();
        String pmName = resolvePmName(project);
        String statusLabel = nodeStatusLabel(node.getStatus());
        String nodeLabel = resolveNodeLabel(node);
        String title = "项目「" + projectName + "」的「" + nodeLabel + "」阶段状态已更新为" + statusLabel;
        String content = "项目经理：" + pmName;
        String businessType = "EXECUTION".equals(project.getProjectType()) ? "ExecutionProject" : "ManagementProject";

        Set<Long> recipients = new LinkedHashSet<>();
        RoleEntity adminRole = roleMapper.selectOne(
                new LambdaQueryWrapper<RoleEntity>().eq(RoleEntity::getCode, "ADMIN").eq(RoleEntity::getDeleted, 0));
        if (adminRole != null) {
            recipients.addAll(userRoleMapper.selectUserIdsByRoleId(adminRole.getId()));
        }
        if (project.getManagerId() != null) {
            recipients.add(project.getManagerId());
        }
        for (Long recipientId : recipients) {
            noticeService.create(recipientId, NoticeType.SYSTEM, title, content, businessType, project.getId());
        }
    }

    private String resolveNodeLabel(ProjectNodeEntity node) {
        if (node.getNodeCode() != null) {
            String label = ProjectNodeTemplate.resolveLabel(node.getNodeCode());
            if (label != null) return label;
        }
        return node.getNodeName() != null ? node.getNodeName() : "未知阶段";
    }

    private String resolvePmName(ProjectEntity project) {
        Long pmId = project.getManagerId() != null ? project.getManagerId() : project.getCreatedBy();
        if (pmId == null) return "未知";
        SystemUser user = systemUserMapper.selectById(pmId);
        return user != null ? user.getRealName() : "未知";
    }

    private static String nodeStatusLabel(String status) {
        if (status == null) return "未知";
        return switch (status) {
            case "NOT_STARTED" -> "未开始";
            case "IN_PROGRESS" -> "进行中";
            case "COMPLETED" -> "已完成";
            case "OVERDUE" -> "已逾期";
            default -> status;
        };
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
        String resolvedLabel = entity.getNodeCode() != null
                ? ProjectNodeTemplate.resolveLabel(entity.getNodeCode())
                : null;
        return GanttNodeResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .parentId(entity.getParentId())
                .nodeCode(entity.getNodeCode())
                .nodeName(entity.getNodeName())
                .label(resolvedLabel != null ? resolvedLabel : entity.getNodeName())
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
