package com.jitong.projectflow.task.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.task.dto.TaskActualTimeUpdateRequest;
import com.jitong.projectflow.task.dto.TaskQueryRequest;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
    @Mock
    TaskMapper taskMapper;

    @Mock
    OperationLogService operationLogService;

    @Mock
    BusinessAccessService businessAccessService;

    @AfterEach
    void clearCurrentUser() {
        CurrentUserContext.clear();
    }

    @Test
    void updateActualTimeRecalculatesStatusAndWritesLog() {
        CurrentUserContext.set(1001L);
        TaskEntity task = new TaskEntity();
        task.setId(10L);
        task.setName("Develop API");
        task.setPlannedEndDate(LocalDate.now().plusDays(5));
        task.setStatus("NOT_STARTED");
        when(taskMapper.selectById(10L)).thenReturn(task);

        TaskActualTimeUpdateRequest request = new TaskActualTimeUpdateRequest();
        request.setActualStartDate(LocalDate.now());
        request.setActualEndDate(LocalDate.now());

        TaskService service = new TaskService(taskMapper, operationLogService, businessAccessService);
        service.updateActualTime(10L, request);

        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskMapper).updateById(captor.capture());
        TaskEntity updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo("COMPLETED");
        assertThat(updated.getUpdatedBy()).isEqualTo(1001L);
        verify(businessAccessService).requireTaskActualTimeManage(task);
        verify(operationLogService).record("task", "Task", 10L, "UPDATE_ACTUAL_TIME", "Develop API");
    }

    @Test
    void listReturnsPagedTasks() {
        CurrentUserContext.set(1001L);
        TaskEntity task = new TaskEntity();
        task.setId(1L);
        task.setName("Develop API");
        Page<TaskEntity> page = new Page<>(1, 20, 1);
        page.setRecords(java.util.List.of(task));
        when(taskMapper.selectPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(page);

        var result = new TaskService(taskMapper, operationLogService, businessAccessService).list(new TaskQueryRequest());

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).extracting("name").containsExactly("Develop API");
    }
}
