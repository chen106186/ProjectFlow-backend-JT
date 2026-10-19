package com.jitong.projectflow.task.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.file.service.FileService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.DateRangeValidator;
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
import com.jitong.projectflow.task.domain.TaskStatusQuery;
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
import org.springframework.transaction.annotation.Transactional;
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
    private final FileService fileService;
    private final TaskStatusCalculator statusCalculator = new TaskStatusCalculator();

    @Transactional
    public TaskResponse create(TaskCreateRequest request) {
        requireProjectRead(request.getProjectId());
        if (request.getParentId() != null) {
            TaskEntity parent = requireTask(request.getParentId());
            businessAccessService.requireTaskRead(parent);
            if (!request.getProjectId().equals(parent.getProjectId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "父任务必须属于同一项目");
            }
            if (computeDepth(request.getParentId()) >= 2) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "子任务最多支持三级");
            }
        }
        DateRangeValidator.validate(request.getPlannedStartDate(), request.getPlannedEndDate(), "计划");
        TaskEntity entity = createEntity(request);
        taskMapper.insert(entity);
        afterTaskCreated(entity);
        return toResponse(entity);
    }

    @Transactional
    public List<TaskResponse> batchCreate(TaskBatchCreateRequest request) {
        return request.getTasks().stream().map(this::create).toList();
    }

    @Transactional
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
        Long userId = CurrentUserContext.userId();
        LambdaQueryWrapper<TaskEntity> wrapper = new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getAssigneeId, userId);
        wrapper.orderByDesc(TaskEntity::getCreatedAt);
        return taskMapper.selectList(wrapper).stream().map(this::toResponse).toList();
    }

    public TaskResponse getById(Long id) {
        TaskEntity entity = requireTask(id);
        businessAccessService.requireTaskRead(entity);
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

    @Transactional
    public TaskResponse update(Long id, TaskUpdateRequest request) {
        TaskEntity entity = requireTask(id);
        businessAccessService.requireTaskManage(entity);
        Long oldAssigneeId = entity.getAssigneeId();
        if (request.getProjectId() != null && !request.getProjectId().equals(entity.getProjectId())) {
            requireProjectRead(request.getProjectId());
            if (entity.getParentId() != null || taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                    .eq(TaskEntity::getParentId, id)) > 0) {
                throw new BusinessException(ErrorCode.CONFLICT, "包含父子关联的任务不能直接更换项目");
            }
            entity.setProjectId(request.getProjectId());
        }
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getRoleName() != null) entity.setRoleName(request.getRoleName());
        if (request.getPriority() != null) entity.setPriority(request.getPriority());
        if (request.getStatus() != null) entity.setStatus(request.getStatus());
        if (request.getAssigneeId() != null) entity.setAssigneeId(request.getAssigneeId());
        if (request.getPlannedStartDate() != null) entity.setPlannedStartDate(request.getPlannedStartDate());
        if (request.getPlannedEndDate() != null) entity.setPlannedEndDate(request.getPlannedEndDate());
        if (request.isActualStartDatePresent()) entity.setActualStartDate(request.getActualStartDate());
        if (request.isActualEndDatePresent()) entity.setActualEndDate(request.getActualEndDate());
        DateRangeValidator.validate(entity.getPlannedStartDate(), entity.getPlannedEndDate(), "计划");
        DateRangeValidator.validate(entity.getActualStartDate(), entity.getActualEndDate(), "实际");
        validateActualDates(entity);
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getTags() != null) entity.setTags(request.getTags());
        if (request.getRemark() != null) entity.setRemark(request.getRemark());
        entity.setStatus(calculateStatus(entity).name());
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

    @Transactional
    public TaskResponse updateActualTime(Long id, TaskActualTimeUpdateRequest request) {
        TaskEntity entity = requireTask(id);
        businessAccessService.requireTaskActualTimeManage(entity);
        if (request.isActualStartDatePresent()) entity.setActualStartDate(request.getActualStartDate());
        if (request.isActualEndDatePresent()) entity.setActualEndDate(request.getActualEndDate());
        DateRangeValidator.validate(entity.getActualStartDate(), entity.getActualEndDate(), "实际");
        validateActualDates(entity);
        if (request.getRemark() != null) entity.setRemark(request.getRemark());
        entity.setStatus(calculateStatus(entity).name());
        entity.setUpdatedBy(CurrentUserContext.userIdOrNull());
        taskMapper.updateById(entity);
        operationLogService.record("task", "Task", id, "UPDATE_ACTUAL_TIME", "更新工时：" + entity.getName());
        return toResponse(entity);
    }

    @Transactional
    public void delete(Long id) {
        TaskEntity entity = requireTask(id);
        businessAccessService.requireTaskManage(entity);
        deleteWithDescendants(id);
        operationLogService.record("task", "Task", id, "DELETE", "删除任务：" + entity.getName());
    }

    public List<TaskResponse> listSubtasks(Long id) {
        businessAccessService.requireTaskRead(requireTask(id));
        LambdaQueryWrapper<TaskEntity> query = new LambdaQueryWrapper<TaskEntity>()
                .eq(TaskEntity::getParentId, id)
                .orderByAsc(TaskEntity::getSortOrder);
        applyReadScope(query);
        return taskMapper.selectList(query)
                .stream().map(this::toResponse).toList();
    }

    private void deleteWithDescendants(Long id) {
        taskMapper.selectList(new LambdaQueryWrapper<TaskEntity>().eq(TaskEntity::getParentId, id))
                .forEach(child -> {
                    businessAccessService.requireTaskManage(child);
                    deleteWithDescendants(child.getId());
                });
        fileService.purgeForBusiness("TASK", id);
        taskMapper.deleteById(id);
    }

    private int computeDepth(Long taskId) {
        int depth = 0;
        Long current = taskId;
        while (current != null && depth <= 4) {
            TaskEntity entity = taskMapper.selectById(current);
            if (entity == null) break;
            current = entity.getParentId();
            if (current != null) depth++;
        }
        return depth;
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
        wrapper.eq(StringUtils.hasText(request.getRoleName()), TaskEntity::getRoleName, request.getRoleName());
        wrapper.eq(StringUtils.hasText(request.getPriority()), TaskEntity::getPriority, request.getPriority());
        applyStatusQuery(wrapper, request.getStatus());
        wrapper.eq(request.getPlannedEndDate() != null, TaskEntity::getPlannedEndDate, request.getPlannedEndDate());
        wrapper.like(StringUtils.hasText(request.getKeyword()), TaskEntity::getName, request.getKeyword());
        applyReadScope(wrapper);
        wrapper.orderByDesc(TaskEntity::getCreatedAt);
        return wrapper;
    }

    private void applyReadScope(LambdaQueryWrapper<TaskEntity> wrapper) {
        if (businessAccessService.canViewAll()) {
            return;
        }
        Long userId = CurrentUserContext.userId();
        wrapper.and(scope -> scope.eq(TaskEntity::getAssigneeId, userId)
                .or()
                .eq(TaskEntity::getCreatedBy, userId)
                .or().inSql(TaskEntity::getProjectId, businessAccessService.readableProjectIdsSql()));
    }

    private TaskStatus calculateStatus(TaskEntity entity) {
        return statusCalculator.calculate(
                entity.getPlannedStartDate(),
                entity.getPlannedEndDate(),
                entity.getActualStartDate(),
                entity.getActualEndDate(),
                TaskStatus.PAUSED.name().equals(entity.getStatus()),
                LocalDate.now());
    }

    private void applyStatusQuery(LambdaQueryWrapper<TaskEntity> wrapper, String status) {
        if (!StringUtils.hasText(status)) {
            return;
        }
        try {
            TaskStatusQuery.apply(wrapper, TaskStatus.valueOf(status), LocalDate.now());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的任务状态");
        }
    }

    private void requireProjectRead(Long projectId) {
        ProjectEntity project = projectMapper.selectById(projectId);
        if (project == null) throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        businessAccessService.requireProjectRead(project);
    }

    private void validateActualDates(TaskEntity task) {
        LocalDate today = LocalDate.now();
        if ((task.getActualStartDate() != null && task.getActualStartDate().isAfter(today))
                || (task.getActualEndDate() != null && task.getActualEndDate().isAfter(today))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "实际日期不能晚于今天");
        }
        if (task.getActualEndDate() != null && task.getActualStartDate() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "填写实际结束日期前须填写实际开始日期");
        }
    }

    private TaskEntity requireTask(Long id) {
        TaskEntity entity = taskMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        }
        return entity;
    }

    private TaskResponse toResponse(TaskEntity entity) {
        TaskStatus displayStatus = calculateStatus(entity);
        return TaskResponse.builder()
                .id(entity.getId())
                .projectId(entity.getProjectId())
                .parentId(entity.getParentId())
                .name(entity.getName())
                .roleName(entity.getRoleName())
                .priority(entity.getPriority())
                .status(displayStatus.name())
                .assigneeId(entity.getAssigneeId())
                .plannedStartDate(entity.getPlannedStartDate())
                .plannedEndDate(entity.getPlannedEndDate())
                .actualStartDate(entity.getActualStartDate())
                .actualEndDate(entity.getActualEndDate())
                .description(entity.getDescription())
                .tags(entity.getTags())
                .remark(entity.getRemark())
                .sortOrder(entity.getSortOrder())
                .createdAt(entity.getCreatedAt())
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
        List<Long> projectIds = resolveProjectIds(projectId);
        long total           = taskMapper.selectCount(projectScopeQuery(projectIds));
        long overdueCount    = countStatus(projectIds, TaskStatus.OVERDUE);
        long dueSoonCount    = countStatus(projectIds, TaskStatus.DUE_SOON);
        long inProgressCount = countStatus(projectIds, TaskStatus.IN_PROGRESS);
        long notStartedCount = countStatus(projectIds, TaskStatus.NOT_STARTED);
        long pausedCount     = countStatus(projectIds, TaskStatus.PAUSED);
        long completedCount  = countStatus(projectIds, TaskStatus.COMPLETED);

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

    /** 任务列表范围：执行类 → 本项目 + 父管理类；管理类 → 本项目 + 子执行类；其他 → 本项目 */
    private List<Long> resolveProjectIds(Long projectId) {
        if (projectId == null) return List.of();
        ProjectEntity proj = projectMapper.selectById(projectId);
        if (proj == null) return List.of(projectId);
        if ("EXECUTION".equals(proj.getProjectType()) && proj.getManagementProjectId() != null) {
            return List.of(projectId, proj.getManagementProjectId());
        }
        if ("MANAGEMENT".equals(proj.getProjectType())) {
            ProjectEntity exec = projectMapper.selectOne(
                    new LambdaQueryWrapper<ProjectEntity>()
                            .eq(ProjectEntity::getManagementProjectId, projectId)
                            .eq(ProjectEntity::getProjectType, "EXECUTION"));
            if (exec != null) {
                return List.of(projectId, exec.getId());
            }
        }
        return List.of(projectId);
    }

    private long countStatus(List<Long> projectIds, TaskStatus status) {
        LambdaQueryWrapper<TaskEntity> query = projectScopeQuery(projectIds);
        TaskStatusQuery.apply(query, status, LocalDate.now());
        return taskMapper.selectCount(query);
    }

    private LambdaQueryWrapper<TaskEntity> projectScopeQuery(List<Long> projectIds) {
        LambdaQueryWrapper<TaskEntity> q = new LambdaQueryWrapper<>();
        if (projectIds.size() > 1) {
            q.in(TaskEntity::getProjectId, projectIds);
        } else if (!projectIds.isEmpty()) {
            q.eq(TaskEntity::getProjectId, projectIds.get(0));
        }
        applyReadScope(q);
        return q;
    }
}
