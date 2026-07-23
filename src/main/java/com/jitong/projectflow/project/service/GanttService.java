package com.jitong.projectflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.DateRangeValidator;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.project.domain.GanttNodeSummaryCalculator;
import com.jitong.projectflow.project.domain.GanttSummaryData;
import com.jitong.projectflow.project.domain.ProjectNodeStatus;
import com.jitong.projectflow.project.domain.ProjectNodeStatusCalculator;
import com.jitong.projectflow.project.domain.ProjectNodeTemplate;
import com.jitong.projectflow.project.domain.ProjectStatusCalculator;
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
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
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
    private final ProjectNodeStatusCalculator statusCalculator = new ProjectNodeStatusCalculator();
    private static final ProjectStatusCalculator PROJECT_STATUS_CALCULATOR = new ProjectStatusCalculator();
    private final OperationLogService operationLogService;
    private final NoticeService noticeService;
    private final ProjectMapper projectMapper;
    private final SystemUserMapper systemUserMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final TaskMapper taskMapper;

    public GanttService(ProjectNodeMapper projectNodeMapper, OperationLogService operationLogService,
                        NoticeService noticeService, ProjectMapper projectMapper,
                        SystemUserMapper systemUserMapper, RoleMapper roleMapper, UserRoleMapper userRoleMapper,
                        TaskMapper taskMapper) {
        this.projectNodeMapper = projectNodeMapper;
        this.operationLogService = operationLogService;
        this.noticeService = noticeService;
        this.projectMapper = projectMapper;
        this.systemUserMapper = systemUserMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.taskMapper = taskMapper;
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
        // 直接覆盖日期字段（含 null 以支持清除），不做 null 判断
        entity.setPlannedStartDate(req.getPlannedStartDate());
        entity.setPlannedEndDate(req.getPlannedEndDate());
        entity.setActualStartDate(req.getActualStartDate());
        entity.setActualEndDate(req.getActualEndDate());
        DateRangeValidator.validate(entity.getPlannedStartDate(), entity.getPlannedEndDate(), "计划");
        DateRangeValidator.validate(entity.getActualStartDate(), entity.getActualEndDate(), "实际");
        // 状态由计划/实际时间自动计算，不接受手动传入
        LocalDate today = LocalDate.now();
        ProjectNodeStatus calculated = statusCalculator.calculate(
                entity.getPlannedStartDate(), entity.getPlannedEndDate(),
                entity.getActualStartDate(), entity.getActualEndDate(), today);
        entity.setStatus(calculated.name());
        // 实际结束时间已到达时进度自动设为 100%
        if (entity.getActualEndDate() != null && !entity.getActualEndDate().isAfter(today)) {
            entity.setProgressPercent(100);
        } else if (req.getProgressPercent() != null) {
            entity.setProgressPercent(req.getProgressPercent());
        }
        validateNode(entity);
        entity.setUpdatedBy(CurrentUserContext.userId());

        // 使用 LambdaUpdateWrapper 强制写入日期字段（updateById 默认跳过 null 字段）
        projectNodeMapper.update(null, new LambdaUpdateWrapper<ProjectNodeEntity>()
                .eq(ProjectNodeEntity::getId, entity.getId())
                .set(req.getNodeName() != null, ProjectNodeEntity::getNodeName, req.getNodeName())
                .set(ProjectNodeEntity::getPlannedStartDate, entity.getPlannedStartDate())
                .set(ProjectNodeEntity::getPlannedEndDate, entity.getPlannedEndDate())
                .set(ProjectNodeEntity::getActualStartDate, entity.getActualStartDate())
                .set(ProjectNodeEntity::getActualEndDate, entity.getActualEndDate())
                .set(ProjectNodeEntity::getStatus, entity.getStatus())
                .set(ProjectNodeEntity::getProgressPercent, entity.getProgressPercent())
                .set(ProjectNodeEntity::getUpdatedBy, entity.getUpdatedBy()));
        syncToManagementNode(entity, req);
        syncToExecutionNode(entity, req);
        recalcProjectStatuses(entity.getProjectId());

        String newStatus = entity.getStatus();
        StringBuilder logContent = new StringBuilder("编辑项目节点：").append(entity.getNodeName());
        if (!newStatus.equals(oldStatus)) {
            logContent.append("　｜　阶段状态 → ").append(nodeStatusLabel(newStatus));
        }
        if (entity.getProgressPercent() != null) {
            logContent.append("　｜　完成比例 → ").append(entity.getProgressPercent()).append("%");
        }
        if (req.getPlannedEndDate() != null) {
            logContent.append("　｜　计划完成 → ").append(req.getPlannedEndDate());
        }
        operationLogService.record("project", "ProjectNode", nodeId, "UPDATE", logContent.toString());

        if (!newStatus.equals(oldStatus)) {
            sendStageChangeNotice(entity);
        }

        return toResponse(entity);
    }

    /** 重算原始项目及其关联管理类/执行类项目的状态（基于各自的甘特节点）。 */
    private void recalcProjectStatuses(Long originProjectId) {
        syncProjectStatus(originProjectId);
        ProjectEntity project = projectMapper.selectById(originProjectId);
        if (project == null) return;
        if ("EXECUTION".equals(project.getProjectType()) && project.getManagementProjectId() != null) {
            syncProjectStatus(project.getManagementProjectId());
        } else if ("MANAGEMENT".equals(project.getProjectType())) {
            ProjectEntity exec = projectMapper.selectOne(
                    new LambdaQueryWrapper<ProjectEntity>()
                            .eq(ProjectEntity::getManagementProjectId, originProjectId)
                            .eq(ProjectEntity::getProjectType, "EXECUTION"));
            if (exec != null) syncProjectStatus(exec.getId());
        }
    }

    private void syncProjectStatus(Long projectId) {
        ProjectEntity project = projectMapper.selectById(projectId);
        if (project == null
                || "PAUSED".equals(project.getStatus())
                || "CANCELLED".equals(project.getStatus())) return;
        List<ProjectNodeEntity> nodes = projectNodeMapper.selectList(
                new LambdaQueryWrapper<ProjectNodeEntity>()
                        .eq(ProjectNodeEntity::getProjectId, projectId));
        String newStatus = PROJECT_STATUS_CALCULATOR.computeFromNodes(nodes);
        if (!newStatus.equals(project.getStatus())) {
            project.setStatus(newStatus);
            projectMapper.updateById(project);
        }
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
            case "NOT_STARTED"       -> "未开始";
            case "OVERDUE_START"     -> "启动逾期";
            case "IN_PROGRESS"       -> "进行中";
            case "DUE_SOON"          -> "即将到期";
            case "OVERDUE"           -> "已逾期";
            case "COMPLETED"         -> "已完成";
            case "OVERDUE_COMPLETED" -> "逾期完成";
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

        long totalTasks = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getProjectId, projectId));
        long completedTasks = totalTasks == 0 ? 0 : taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getProjectId, projectId)
                .eq(TaskEntity::getStatus, "COMPLETED"));

        return GanttSummaryResponse.builder()
                .total(data.total())
                .completed(data.completed())
                .overdue(data.overdue())
                .dueSoon(data.dueSoon())
                .overallProgress(data.overallProgress())
                .totalTaskCount(totalTasks)
                .completedTaskCount(completedTasks)
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

    /** 当执行类项目节点更新后，同步到对应管理类项目的节点（按 nodeCode 优先、nodeName 兜底匹配）。 */
    private void syncToManagementNode(ProjectNodeEntity execNode, ProjectNodeUpdateRequest req) {
        ProjectEntity project = projectMapper.selectById(execNode.getProjectId());
        if (project == null || !"EXECUTION".equals(project.getProjectType())
                || project.getManagementProjectId() == null) {
            return;
        }
        ProjectNodeEntity mgmtNode = findMatchingManagementNode(
                project.getManagementProjectId(), execNode.getNodeCode(), execNode.getNodeName());
        if (mgmtNode == null) return;

        mgmtNode.setPlannedStartDate(req.getPlannedStartDate());
        mgmtNode.setPlannedEndDate(req.getPlannedEndDate());
        mgmtNode.setActualStartDate(req.getActualStartDate());
        mgmtNode.setActualEndDate(req.getActualEndDate());

        LocalDate today = LocalDate.now();
        ProjectNodeStatus calculated = statusCalculator.calculate(
                mgmtNode.getPlannedStartDate(), mgmtNode.getPlannedEndDate(),
                mgmtNode.getActualStartDate(), mgmtNode.getActualEndDate(), today);
        mgmtNode.setStatus(calculated.name());

        if (mgmtNode.getActualEndDate() != null && !mgmtNode.getActualEndDate().isAfter(today)) {
            mgmtNode.setProgressPercent(100);
        } else if (req.getProgressPercent() != null) {
            mgmtNode.setProgressPercent(req.getProgressPercent());
        }

        mgmtNode.setUpdatedBy(CurrentUserContext.userId());
        projectNodeMapper.update(null, new LambdaUpdateWrapper<ProjectNodeEntity>()
                .eq(ProjectNodeEntity::getId, mgmtNode.getId())
                .set(ProjectNodeEntity::getPlannedStartDate, mgmtNode.getPlannedStartDate())
                .set(ProjectNodeEntity::getPlannedEndDate, mgmtNode.getPlannedEndDate())
                .set(ProjectNodeEntity::getActualStartDate, mgmtNode.getActualStartDate())
                .set(ProjectNodeEntity::getActualEndDate, mgmtNode.getActualEndDate())
                .set(ProjectNodeEntity::getStatus, mgmtNode.getStatus())
                .set(ProjectNodeEntity::getProgressPercent, mgmtNode.getProgressPercent())
                .set(ProjectNodeEntity::getUpdatedBy, mgmtNode.getUpdatedBy()));
    }

    /** 当管理类项目节点更新后，同步到对应执行类项目的节点。 */
    private void syncToExecutionNode(ProjectNodeEntity mgmtNode, ProjectNodeUpdateRequest req) {
        ProjectEntity project = projectMapper.selectById(mgmtNode.getProjectId());
        if (project == null || !"MANAGEMENT".equals(project.getProjectType())) {
            return;
        }
        ProjectEntity execProject = projectMapper.selectOne(
                new LambdaQueryWrapper<ProjectEntity>()
                        .eq(ProjectEntity::getManagementProjectId, project.getId())
                        .eq(ProjectEntity::getProjectType, "EXECUTION"));
        if (execProject == null) return;

        ProjectNodeEntity execNode = findNodeInProject(
                execProject.getId(), mgmtNode.getNodeCode(), mgmtNode.getNodeName());
        if (execNode == null) return;

        execNode.setPlannedStartDate(req.getPlannedStartDate());
        execNode.setPlannedEndDate(req.getPlannedEndDate());
        execNode.setActualStartDate(req.getActualStartDate());
        execNode.setActualEndDate(req.getActualEndDate());

        LocalDate today = LocalDate.now();
        ProjectNodeStatus calculated = statusCalculator.calculate(
                execNode.getPlannedStartDate(), execNode.getPlannedEndDate(),
                execNode.getActualStartDate(), execNode.getActualEndDate(), today);
        execNode.setStatus(calculated.name());

        if (execNode.getActualEndDate() != null && !execNode.getActualEndDate().isAfter(today)) {
            execNode.setProgressPercent(100);
        } else if (req.getProgressPercent() != null) {
            execNode.setProgressPercent(req.getProgressPercent());
        }

        execNode.setUpdatedBy(CurrentUserContext.userId());
        projectNodeMapper.update(null, new LambdaUpdateWrapper<ProjectNodeEntity>()
                .eq(ProjectNodeEntity::getId, execNode.getId())
                .set(ProjectNodeEntity::getPlannedStartDate, execNode.getPlannedStartDate())
                .set(ProjectNodeEntity::getPlannedEndDate, execNode.getPlannedEndDate())
                .set(ProjectNodeEntity::getActualStartDate, execNode.getActualStartDate())
                .set(ProjectNodeEntity::getActualEndDate, execNode.getActualEndDate())
                .set(ProjectNodeEntity::getStatus, execNode.getStatus())
                .set(ProjectNodeEntity::getProgressPercent, execNode.getProgressPercent())
                .set(ProjectNodeEntity::getUpdatedBy, execNode.getUpdatedBy()));
    }

    private ProjectNodeEntity findMatchingManagementNode(Long mgmtProjectId, String nodeCode, String nodeName) {
        return findNodeInProject(mgmtProjectId, nodeCode, nodeName);
    }

    private ProjectNodeEntity findNodeInProject(Long projectId, String nodeCode, String nodeName) {
        if (nodeCode != null) {
            ProjectNodeEntity node = projectNodeMapper.selectOne(
                    new LambdaQueryWrapper<ProjectNodeEntity>()
                            .eq(ProjectNodeEntity::getProjectId, projectId)
                            .eq(ProjectNodeEntity::getNodeCode, nodeCode));
            if (node != null) return node;
        }
        if (nodeName != null) {
            return projectNodeMapper.selectOne(
                    new LambdaQueryWrapper<ProjectNodeEntity>()
                            .eq(ProjectNodeEntity::getProjectId, projectId)
                            .eq(ProjectNodeEntity::getNodeName, nodeName));
        }
        return null;
    }
}
