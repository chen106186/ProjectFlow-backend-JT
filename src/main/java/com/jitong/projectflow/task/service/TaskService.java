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
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.task.domain.TaskStatus;
import com.jitong.projectflow.task.domain.TaskStatusCalculator;
import com.jitong.projectflow.task.dto.TaskActualTimeUpdateRequest;
import com.jitong.projectflow.task.dto.TaskBatchCreateRequest;
import com.jitong.projectflow.task.dto.TaskCreateRequest;
import com.jitong.projectflow.task.dto.TaskImportRow;
import com.jitong.projectflow.task.dto.TaskQueryRequest;
import com.jitong.projectflow.task.dto.TaskResponse;
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
        operationLogService.record("task", "Task", entity.getId(), "CREATE", entity.getName());
        if (entity.getAssigneeId() != null) {
            noticeService.create(entity.getAssigneeId(), NoticeType.TASK_ASSIGNED,
                    "任务分配通知", entity.getName(), "Task", entity.getId());
        }
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
        return toResponse(requireTask(id));
    }

    public TaskResponse update(Long id, TaskUpdateRequest request) {
        TaskEntity entity = requireTask(id);
        businessAccessService.requireTaskManage(entity);
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
        operationLogService.record("task", "Task", id, "UPDATE", entity.getName());
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
        operationLogService.record("task", "Task", id, "UPDATE_ACTUAL_TIME", entity.getName());
        return toResponse(entity);
    }

    public void delete(Long id) {
        TaskEntity entity = requireTask(id);
        businessAccessService.requireTaskManage(entity);
        taskMapper.deleteById(id);
        operationLogService.record("task", "Task", id, "DELETE", entity.getName());
    }

    private LambdaQueryWrapper<TaskEntity> buildQuery(TaskQueryRequest request) {
        LambdaQueryWrapper<TaskEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(request.getProjectId() != null, TaskEntity::getProjectId, request.getProjectId());
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
        if (businessAccessService.isSystemAdmin()) {
            return;
        }
        Long userId = CurrentUserContext.userId();
        wrapper.and(scope -> scope.eq(TaskEntity::getAssigneeId, userId)
                .or()
                .eq(TaskEntity::getCreatedBy, userId)
                .or()
                .inSql(TaskEntity::getProjectId, managedProjectSql(userId)));
    }

    private String managedProjectSql(Long userId) {
        return "select id from pf_project where deleted = 0 and (manager_id = " + userId + " or created_by = " + userId + ")";
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
}
