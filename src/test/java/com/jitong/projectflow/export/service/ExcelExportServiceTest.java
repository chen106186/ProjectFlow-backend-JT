package com.jitong.projectflow.export.service;

import com.alibaba.excel.EasyExcel;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.bug.dto.BugQueryRequest;
import com.jitong.projectflow.bug.dto.BugResponse;
import com.jitong.projectflow.bug.service.BugService;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.export.dto.OperationLogExportRow;
import com.jitong.projectflow.export.dto.BugExportRow;
import com.jitong.projectflow.export.dto.TaskExportRow;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import com.jitong.projectflow.requirement.dto.RequirementQueryRequest;
import com.jitong.projectflow.requirement.dto.RequirementResponse;
import com.jitong.projectflow.requirement.service.RequirementService;
import com.jitong.projectflow.system.dto.OperationLogQueryRequest;
import com.jitong.projectflow.system.dto.OperationLogResponse;
import com.jitong.projectflow.system.service.OperationLogQueryService;
import com.jitong.projectflow.task.dto.TaskQueryRequest;
import com.jitong.projectflow.task.dto.TaskResponse;
import com.jitong.projectflow.task.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelExportServiceTest {
    @Mock OperationLogQueryService operationLogQueryService;
    @Mock TaskService taskService;
    @Mock RequirementService requirementService;
    @Mock BugService bugService;
    @Mock ProjectNodeMapper projectNodeMapper;
    @Mock BusinessAccessService businessAccessService;
    @InjectMocks ExcelExportService exportService;

    @Test
    void exportOperationLogsReusesFilteredLogQueryAndWritesRows() {
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        request.setModule("task");
        request.setKeyword("approved");
        OperationLogResponse log = OperationLogResponse.builder().id(1L).module("task")
                .operationType("CREATE").content("approved").build();
        when(operationLogQueryService.list(request)).thenReturn(new PageResult<>(1L, 1L, 200L, List.of(log)));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        exportService.exportOperationLogs(request, out);

        List<OperationLogExportRow> rows = EasyExcel.read(new ByteArrayInputStream(out.toByteArray()))
                .head(OperationLogExportRow.class).sheet().doReadSync();
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getModule()).isEqualTo("task");
        assertThat(rows.getFirst().getContent()).isEqualTo("approved");
        assertThat(request.getKeyword()).isEqualTo("approved");
        verify(operationLogQueryService).list(request);
    }

    @Test
    void exportTasksPreservesFiltersAndStreamsEveryPageFromAuthorizedList() {
        TaskQueryRequest request = new TaskQueryRequest();
        request.setProjectId(17L);
        request.setAssigneeId(9L);
        request.setRoleName("测试");
        request.setStatus("OVERDUE");
        request.setPageNo(4L);
        request.setPageSize(50L);
        List<Long> requestedPages = new ArrayList<>();
        when(taskService.list(request)).thenAnswer(invocation -> {
            requestedPages.add(request.getPageNo());
            assertThat(request.getProjectId()).isEqualTo(17L);
            assertThat(request.getAssigneeId()).isEqualTo(9L);
            assertThat(request.getRoleName()).isEqualTo("测试");
            assertThat(request.getStatus()).isEqualTo("OVERDUE");
            assertThat(request.getPageSize()).isEqualTo(200L);
            long start = (request.getPageNo() - 1L) * 200L + 1L;
            List<TaskResponse> rows = LongStream.rangeClosed(start, Math.min(start + 199L, 201L))
                    .mapToObj(id -> TaskResponse.builder().id(id).name("Task " + id).projectId(17L)
                            .assigneeId(9L).status("OVERDUE").build()).toList();
            return new PageResult<>(201L, request.getPageNo(), 200L, rows);
        });
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        exportService.exportTasks(request, out);

        List<TaskExportRow> rows = EasyExcel.read(new ByteArrayInputStream(out.toByteArray()))
                .head(TaskExportRow.class).sheet().doReadSync();
        assertThat(rows).hasSize(201);
        assertThat(rows).extracting(TaskExportRow::getId).containsExactlyElementsOf(LongStream.rangeClosed(1L, 201L).boxed().toList());
        assertThat(requestedPages).containsExactly(1L, 2L);
        assertThat(request.getPageNo()).isEqualTo(4L);
        assertThat(request.getPageSize()).isEqualTo(50L);
    }

    @Test
    void exportRequirementsAndBugsUseTheirScopedListServices() {
        RequirementQueryRequest requirements = new RequirementQueryRequest();
        requirements.setProjectId(17L);
        requirements.setStatus("PENDING_REVIEW");
        BugQueryRequest bugs = new BugQueryRequest();
        bugs.setTaskId(5L);
        bugs.setPriority("HIGH");
        when(requirementService.list(requirements)).thenReturn(new PageResult<RequirementResponse>(0, 1, 200, List.of()));
        when(bugService.list(bugs)).thenReturn(new PageResult<BugResponse>(0, 1, 200, List.of()));

        exportService.exportRequirements(requirements, new ByteArrayOutputStream());
        exportService.exportBugs(bugs, new ByteArrayOutputStream());

        verify(requirementService).list(requirements);
        verify(bugService).list(bugs);
        assertThat(requirements.getProjectId()).isEqualTo(17L);
        assertThat(requirements.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(bugs.getTaskId()).isEqualTo(5L);
        assertThat(bugs.getPriority()).isEqualTo("HIGH");
    }

    @Test
    void exportBugsUsesVisibleChineseLabels() {
        BugQueryRequest request = new BugQueryRequest();
        BugResponse bug = BugResponse.builder().id(1L).title("支付失败")
                .status("PENDING_VERIFY").priority("MEDIUM").solution("DEFERRED").build();
        when(bugService.list(request)).thenReturn(new PageResult<>(1, 1, 200, List.of(bug)));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        exportService.exportBugs(request, out);

        List<BugExportRow> rows = EasyExcel.read(new ByteArrayInputStream(out.toByteArray()))
                .head(BugExportRow.class).sheet().doReadSync();
        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().getPriority()).isEqualTo("一般");
        assertThat(rows.getFirst().getStatus()).isEqualTo("待验证");
        assertThat(rows.getFirst().getSolution()).isEqualTo("延期处理");
    }

    @Test
    void deniedGanttExportDoesNotReadNodesOrWriteOutput() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        when(businessAccessService.canReadProject(17L)).thenReturn(false);

        assertThatThrownBy(() -> exportService.exportGantt(17L, out)).isInstanceOf(BusinessException.class);

        verifyNoInteractions(projectNodeMapper);
        assertThat(out.size()).isZero();
    }

    @Test
    void queryFailureRestoresCallerPagination() {
        TaskQueryRequest request = new TaskQueryRequest();
        request.setPageNo(3L);
        request.setPageSize(10L);
        when(taskService.list(any())).thenThrow(new IllegalStateException("query failed"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertThatThrownBy(() -> exportService.exportTasks(request, out))
                .isInstanceOf(IllegalStateException.class).hasMessage("query failed");

        assertThat(request.getPageNo()).isEqualTo(3L);
        assertThat(request.getPageSize()).isEqualTo(10L);
        assertThat(out.size()).isZero();
    }
}
