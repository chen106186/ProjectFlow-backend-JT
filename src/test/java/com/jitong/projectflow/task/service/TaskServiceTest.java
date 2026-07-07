package com.jitong.projectflow.task.service;

import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.task.dto.TaskActualTimeUpdateRequest;
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

        TaskService service = new TaskService(taskMapper, operationLogService);
        service.updateActualTime(10L, request);

        ArgumentCaptor<TaskEntity> captor = ArgumentCaptor.forClass(TaskEntity.class);
        verify(taskMapper).updateById(captor.capture());
        TaskEntity updated = captor.getValue();
        assertThat(updated.getStatus()).isEqualTo("COMPLETED");
        assertThat(updated.getUpdatedBy()).isEqualTo(1001L);
        verify(operationLogService).record("task", "Task", 10L, "UPDATE_ACTUAL_TIME", "Develop API");
    }
}
