package com.jitong.projectflow.task.service;

import com.alibaba.excel.EasyExcel;
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
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.system.dto.OperationLogResponse;
import com.jitong.projectflow.system.entity.OperationLog;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.OperationLogMapper;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.task.domain.TaskStatus;
import com.jitong.projectflow.task.domain.TaskStatusCalculator;
import com.jitong.projectflow.task.dto.TaskActualTimeUpdateRequest;
import com.jitong.projectflow.task.dto.TaskBatchCreateRequest;
import com.jitong.projectflow.task.dto.TaskCreateRequest;
import com.jitong.projectflow.task.dto.TaskImportRow;
import com.jitong.projectflow.task.dto.TaskQueryRequest;
import com.jitong.projectflow.task.dto.TaskResponse;
import com.jitong.projectflow.task.dto.TaskRiskStatisticsResponse;
import com.jitong.projectflow.task.dto.TaskUpdateRequest;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskMapper taskMapper;
    private final OperationLogService operationLogService;
    private final BusinessAccessService businessAccessService;
    private final NoticeService noticeService;
    private final SystemUserMapper systemUserMapper;
    private final ProjectMapper projectMapper;
    private final OperationLogMapper operationLogMapper;
    private final TaskStatusCalculator statusCalculator = new TaskStatusCalculator();

    public TaskResponse create(TaskCreateRequest request) {
        TaskEntity entity = createEntity(request);
        taskMapper.insert(entity);
        afterTaskCreated(entity);
        return toResponse(entity);
    }

    public List<TaskResponse> batchCreate(TaskBatchCreateRequest request) {
        return request.getTasks().stream().map(this::create).toList();
    }

    public List<TaskResponse> importTasks(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "导入文件不能为空");
        }
        try {
            List<TaskImportRow> rows = EasyExcel.read(file.getInputStream())
                    .head(TaskImportRow.class)
                    .sheet()
                    .doReadSync();
            TaskBatchCreateRequest request = new TaskBatchCreateRequest();
            request.setTasks(rows.stream().map(this::toCreateRequest).toList());
            return batchCreate(request);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "任务导入失败");
        }
    }

    public void writeImportTemplate(OutputStream out) {
        EasyExcel.write(out, TaskImportRow.class).sheet("任务导入模板").doWrite(List.of());
    }

    private TaskEntity createEntity(TaskCreateRequest request) {
        TaskEntity entity = new TaskEntity();
        entity.setProjectId(request.getProjectId());
        entity.setParentId(request.getParentId());
        entity.setName(request.getName());
        entity.setRoleName(request.getRoleName());
        entity.setPriority(request.getPriority());
        entity.setAssigneeId(request.getAssigneeId());
        entity.setPlannedStartDate(request.getPlannedStartDate());
        entity.setPlannedEndDate(request.getPlannedEndDate());
        entity.setDescription(request.getDescription());
        entity.setTags(request.getTags());
        entity.setRemark(request.getRemark());
        entity.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        entity.setStatus(calculateStatus(entity).name());
        entity.setCreatedBy(CurrentUserContext.userIdOrNull());
        return entity;
    }

    private void afterTaskCreated(TaskEntity entity) {
        operationLogService.record("task", "Task", entity.getId(), "CREATE", "新建任务：" + entity.getName());
        if (entity.getAssigneeId() != null) {
            String operatorName = resolveUserName(entity.getCreatedBy());
            String projectName = resolveProjectName(entity.getProjectId());
            String priorityLabel = priorityLabel(entity.getPriority());
            String title = operatorName + " 将任务「" + entity.getName() + "」分配给您";
            String content = "所属项目：" + projectName + "　｜　优先级：" + priorityLabel;
            noticeService.create(entity.getAssigneeId(), NoticeType.TASK_ASSIGNED, title, content, "Task", entity.getId());
        }
    }

    private String resolveUserName(Long userId) {
        if (userId == null) return "系统";
        SystemUser user = systemUserMapper.selectById(userId);
        return user != null ? user.getRealName() : "未知用户";
    }

    private String resolveProjectName(Long projectId) {
        if (projectId == null) return "未知项目";
        ProjectEntity project = projectMapper.selectById(projectId);
        return project != null ? project.getName() : "未知项目";
    }

    private static String priorityLabel(String priority) {
        if (priority == null) return "普通";
        return switch (priority) {
            case "LOW" -> "低";
            case "MEDIUM" -> "中";
            case "HIGH" -> "高";
            case "URGENT" -> "紧急";
            default -> priority;
        };
    }

    private TaskCreateRequest toCreateRequest(TaskImportRow row) {
        TaskCreateRequest request = new TaskCreateRequest();
        request.setProjectId(row.getProjectId());
        request.setParentId(row.getParentId());
        request.setName(row.getName());
        request.setRoleName(row.getRoleName());
        request.setPriority(row.getPriority());
        request.setAssigneeId(row.getAssigneeId());
        request.setPlannedStartDate(row.getPlannedStartDate());
        request.setPlannedEndDate(row.getPlannedEndDate());
        request.setDescription(row.getDescription());
        request.setTags(row.getTags());
        request.setRemark(row.getRemark());
        request.setSortOrder(row.getSortOrder());
        return request;
    }

    public PageResult<TaskResponse> list(TaskQueryRequest request) {
        Page<TaskEntity> page = taskMapper.selectPage(PageUtils.toPage(request), buildQuery(request));
        return PageUtils.toResult(page, page.getRecords().stream().map(this::toResponse).toList());
    }

    public List<TaskResponse> listMine() {
        TaskQueryRequest request = new TaskQueryRequest();
        request.setAssigneeId(CurrentUserContext.userId());
        return taskMapper.selectList(buildQuery(request)).stream().map(this::toResponse).toList();
    }

    public TaskResponse getById(Long id) {
        TaskEntity entity = requireTask(id);
        TaskResponse response = toResponse(entity);

        if (entity.getAssigneeId() != null) {
            SystemUser assignee = systemUserMapper.selectById(entity.getAssigneeId());
            if (assignee != null) response.setAssigneeName(assignee.getRealName());
        }
        if (entity.getProjectId() != null) {
            ProjectEntity project = projectMapper.selectById(entity.getProjectId());
            if (project != null) response.setProjectName(project.getName());
        }
        response.setCreatedAt(entity.getCreatedAt());

        List<OperationLog> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>()
                        .eq(OperationLog::getBusinessType, "Task")
                        .eq(OperationLog::getBusinessId, id)
                        .orderByDesc(OperationLog::getCreatedAt));
        response.setLogs(logs.stream().map(this::toLogResponse).toList());

        return response;
    }

    public TaskResponse update(Long id, TaskUpdateRequest request) {
        TaskEntity entity = requireTask(id);
        businessAccessService.requireTaskManage(entity);
        Long oldAssigneeId = entity.getAssigneeId();
        if (request.getProjectId() != null) entity.setProjectId(request.getProjectId());
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getRoleName() != null) entity.setRoleName(request.getRoleName());
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        if (request.getAssigneeId() != null) entity.setAssigneeId(request.getAssigneeId());
        if (request.getPlannedStartDate() != null) entity.setPlannedStartDate(request.getPlannedStartDate());
        if (request.getPlannedEndDate() != null) entity.setPlannedEndDate(request.getPlannedEndDate());
        if (request.getActualStartDate() != null) entity.setActualStartDate(request.getActualStartDate());
        if (request.getActualEndDate() != null) entity.setActualEndDate(request.getActualEndDate());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getTags() != null) entity.setTags(request.getTags());
        if (request.getRemark() != null) entity.setRemark(request.getRemark());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        taskMapper.updateById(entity);
        operationLogService.record("task", "Task", id, "UPDATE", "编辑任务：" + entity.getName());
        if (request.getAssigneeId() != null && !request.getAssigneeId().equals(oldAssigneeId)) {
            String operatorName = resolveUserName(CurrentUserContext.userIdOrNull());
            String projectName = resolveProjectName(entity.getProjectId());
            String title = operatorName + " 将任务「" + entity.getName() + "」分配给您";
            String content = "所属项目：" + projectName + "　｜　优先级：" + priorityLabel(entity.getPriority());
            noticeService.create(entity.getAssigneeId(), NoticeType.TASK_ASSIGNED, title, content, "Task", entity.getId());
        }
        return toResponse(entity);
    }

    public TaskResponse updateActualTime(Long id, TaskActualTimeUpdateRequest request) {
        TaskEntity entity = requireTask(id);
        businessAccessService.requireTaskActualTimeManage(entity);
        if (request.getActualStartDate() != null) entity.setActualStartDate(request.getActualStartDate());
        if (request.getActualEndDate() != null) entity.setActualEndDate(request.getActualEndDate());
        if (request.getRemark() != null) entity.setRemark(request.getRemark());
        entity.setStatus(calculateStatus(entity).name());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        taskMapper.updateById(entity);
        operationLogService.record("task", "Task", id, "UPDATE_ACTUAL_TIME", "更新工时：" + entity.getName());
        return toResponse(entity);
    }

    public void delete(Long id) {
        TaskEntity entity = requireTask(id);
        businessAccessService.requireTaskManage(entity);
        taskMapper.deleteById(id);
        operationLogService.record("task", "Task", id, "DELETE", "删除任务：" + entity.getName());
    }

    private LambdaQueryWrapper<TaskEntity> buildQuery(TaskQueryRequest request) {
        LambdaQueryWrapper<TaskEntity> wrapper = new LambdaQueryWrapper<>();
        if (request.getProjectId() != null) {
            List<Long> projectIds = resolveProjectIds(request.getProjectId());
            if (projectIds.size() > 1) {
                wrapper.in(TaskEntity::getProjectId, projectIds);
            } else {
                wrapper.eq(TaskEntity::getProjectId, projectIds.get(0));
            }
        }
        wrapper.eq(request.getAssigneeId() != null, TaskEntity::getAssigneeId, request.getAssigneeId());
        wrapper.eq(StringUtils.hasText(request.getPriority()), TaskEntity::getPriority, request.getPriority());
        wrapper.eq(StringUtils.hasText(request.getStatus()), TaskEntity::getStatus, request.getStatus());
        wrapper.eq(request.getPlannedEndDate() != null, TaskEntity::getPlannedEndDate, request.getPlannedEndDate());
        wrapper.like(StringUtils.hasText(request.getKeyword()), TaskEntity::getName, request.getKeyword());
        applyReadScope(wrapper);
        wrapper.orderByDesc(TaskEntity::getCreatedAt);
        return wrapper;
    }

    private void applyReadScope(LambdaQueryWrapper<TaskEntity> wrapper) {
        if (businessAccessService.isSystemAdmin() || businessAccessService.canViewAll()) {
            return;
        }
        Long userId = CurrentUserContext.userId();
        wrapper.and(scope -> scope.eq(TaskEntity::getAssigneeId, userId)
                .or()
                .eq(TaskEntity::getCreatedBy, userId));
    }

    private TaskStatus calculateStatus(TaskEntity entity) {
        return statusCalculator.calculate(
                entity.getPlannedEndDate(),
                entity.getActualStartDate(),
                entity.getActualEndDate(),
                TaskStatus.PAUSED.name().equals(entity.getStatus()),
                LocalDate.now());
    }

    private TaskEntity requireTask(Long id) {
        TaskEntity entity = taskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return entity;
    }

    private TaskResponse toResponse(TaskEntity entity) {
        return TaskResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .parentId(entity.getParentId())
                .name(entity.getName())
                .roleName(entity.getRoleName())
                .priority(entity.getPriority())
                .status(entity.getStatus())
                .assigneeId(entity.getAssigneeId())
                .plannedStartDate(entity.getPlannedStartDate())
                .plannedEndDate(entity.getPlannedEndDate())
                .actualStartDate(entity.getActualStartDate())
                .actualEndDate(entity.getActualEndDate())
                .description(entity.getDescription())
                .tags(entity.getTags())
                .remark(entity.getRemark())
                .sortOrder(entity.getSortOrder())
                .build();
    }

    private OperationLogResponse toLogResponse(OperationLog log) {
        return OperationLogResponse.builder()
                .id(log.getId())
                .module(log.getModule())
                .businessType(log.getBusinessType())
                .businessId(log.getBusinessId())
                .operationType(log.getOperationType())
                .operatorId(log.getOperatorId())
                .operatorName(log.getOperatorName())
                .content(log.getContent())
                .createdAt(log.getCreatedAt())
                .build();
    }

    public TaskRiskStatisticsResponse getRiskStatistics(Long projectId) {
        List<Long> projectIds = resolveStatsProjectIds(projectId);
        long total           = taskMapper.selectCount(projectScopeQuery(projectIds));
        long overdueCount    = taskMapper.selectCount(projectScopeQuery(projectIds).eq(TaskEntity::getStatus, TaskStatus.OVERDUE.name()));
        long dueSoonCount    = taskMapper.selectCount(projectScopeQuery(projectIds).eq(TaskEntity::getStatus, TaskStatus.DUE_SOON.name()));
        long inProgressCount = taskMapper.selectCount(projectScopeQuery(projectIds).eq(TaskEntity::getStatus, TaskStatus.IN_PROGRESS.name()));
        long notStartedCount = taskMapper.selectCount(projectScopeQuery(projectIds).eq(TaskEntity::getStatus, TaskStatus.NOT_STARTED.name()));
        long pausedCount     = taskMapper.selectCount(projectScopeQuery(projectIds).eq(TaskEntity::getStatus, TaskStatus.PAUSED.name()));
        long completedCount  = taskMapper.selectCount(projectScopeQuery(projectIds).eq(TaskEntity::getStatus, TaskStatus.COMPLETED.name()));

        return TaskRiskStatisticsResponse.builder()
                .total(total)
                .overdueCount(overdueCount)
                .dueSoonCount(dueSoonCount)
                .inProgressCount(inProgressCount)
                .notStartedCount(notStartedCount)
                .pausedCount(pausedCount)
                .completedCount(completedCount)
                .build();
    }

    /** 任务列表范围：执行类 → 本项目 + 父管理类；其他 → 本项目 */
    private List<Long> resolveProjectIds(Long projectId) {
        if (projectId == null) return List.of();
        ProjectEntity proj = projectMapper.selectById(projectId);
        if (proj != null && "EXECUTION".equals(proj.getProjectType()) && proj.getManagementProjectId() != null) {
            return List.of(projectId, proj.getManagementProjectId());
        }
        return List.of(projectId);
    }

    /** 风险统计范围：执行类 → 仅父管理类（与管理类统计口径一致）；其他 → 本项目 */
    private List<Long> resolveStatsProjectIds(Long projectId) {
        if (projectId == null) return List.of();
        ProjectEntity proj = projectMapper.selectById(projectId);
        if (proj != null && "EXECUTION".equals(proj.getProjectType()) && proj.getManagementProjectId() != null) {
            return List.of(proj.getManagementProjectId());
        }
        return List.of(projectId);
    }

    private LambdaQueryWrapper<TaskEntity> projectScopeQuery(List<Long> projectIds) {
        LambdaQueryWrapper<TaskEntity> q = new LambdaQueryWrapper<>();
        if (projectIds.size() > 1) {
            q.in(TaskEntity::getProjectId, projectIds);
        } else if (!projectIds.isEmpty()) {
            q.eq(TaskEntity::getProjectId, projectIds.get(0));
        }
        return q;
    }
}
