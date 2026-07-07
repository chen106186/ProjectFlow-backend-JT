package com.jitong.projectflow.task.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.api.PageUtils;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.task.domain.TaskStatus;
import com.jitong.projectflow.task.domain.TaskStatusCalculator;
import com.jitong.projectflow.task.dto.TaskActualTimeUpdateRequest;
import com.jitong.projectflow.task.dto.TaskCreateRequest;
import com.jitong.projectflow.task.dto.TaskQueryRequest;
import com.jitong.projectflow.task.dto.TaskResponse;
import com.jitong.projectflow.task.dto.TaskUpdateRequest;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskMapper taskMapper;
    private final OperationLogService operationLogService;
    private final TaskStatusCalculator statusCalculator = new TaskStatusCalculator();

    public TaskResponse create(TaskCreateRequest request) {
        TaskEntity entity = new TaskEntity();
        entity.setProjectId(request.getProjectId());
        entity.setName(request.getName());
        entity.setRoleName(request.getRoleName());
        entity.setPriority(request.getPriority());
        entity.setAssigneeId(request.getAssigneeId());
        entity.setPlannedStartDate(request.getPlannedStartDate());
        entity.setPlannedEndDate(request.getPlannedEndDate());
        entity.setDescription(request.getDescription());
        entity.setTags(request.getTags());
        entity.setRemark(request.getRemark());
        entity.setStatus(calculateStatus(entity).name());
        entity.setCreatedBy(CurrentUserContext.userIdOrNull());
        taskMapper.insert(entity);
        operationLogService.record("task", "Task", entity.getId(), "CREATE", entity.getName());
        return toResponse(entity);
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
        wrapper.orderByDesc(TaskEntity::getCreatedAt);
        return wrapper;
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
                .build();
    }
}
