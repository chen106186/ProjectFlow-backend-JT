package com.jitong.projectflow.export.service;

import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.entity.OperationLog;
import com.jitong.projectflow.system.mapper.OperationLogMapper;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceTest {

    @Mock OperationLogMapper operationLogMapper;
    @Mock TaskMapper taskMapper;
    @Mock RequirementMapper requirementMapper;
    @Mock BugMapper bugMapper;
    @Mock ProjectNodeMapper projectNodeMapper;

    @InjectMocks ExcelExportService exportService;

    @Test
    void exportOperationLogs_writesNonEmptyBytes() throws Exception {
        OperationLog log = new OperationLog();
        log.setId(1L);
        log.setModule("test");
        log.setOperationType("CREATE");
        log.setContent("x");
        when(operationLogMapper.selectList(null)).thenReturn(List.of(log));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exportService.exportOperationLogs(baos);
        assertThat(baos.size()).isGreaterThan(0);
    }

    @Test
    void exportTasks_writesNonEmptyBytes() throws Exception {
        TaskEntity task = new TaskEntity();
        task.setId(1L);
        task.setName("Task A");
        task.setPriority("HIGH");
        task.setStatus("IN_PROGRESS");
        task.setAssigneeId(1L);
        task.setProjectId(1L);
        when(taskMapper.selectList(null)).thenReturn(List.of(task));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        exportService.exportTasks(baos);
        assertThat(baos.size()).isGreaterThan(0);
    }
}
