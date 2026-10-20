package com.jitong.projectflow.export.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.bug.dto.BugQueryRequest;
import com.jitong.projectflow.bug.service.BugService;
import com.jitong.projectflow.common.api.PageQuery;
import com.jitong.projectflow.common.api.PageResult;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.export.dto.BugExportRow;
import com.jitong.projectflow.export.dto.GanttExportRow;
import com.jitong.projectflow.export.dto.OperationLogExportRow;
import com.jitong.projectflow.export.dto.RequirementExportRow;
import com.jitong.projectflow.export.dto.TaskExportRow;
import com.jitong.projectflow.project.entity.ProjectNodeEntity;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import com.jitong.projectflow.requirement.dto.RequirementQueryRequest;
import com.jitong.projectflow.requirement.service.RequirementService;
import com.jitong.projectflow.system.dto.OperationLogQueryRequest;
import com.jitong.projectflow.system.service.OperationLogQueryService;
import com.jitong.projectflow.task.dto.TaskQueryRequest;
import com.jitong.projectflow.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class ExcelExportService {
    private static final long EXPORT_PAGE_SIZE = 200L;

    private final OperationLogQueryService operationLogQueryService;
    private final TaskService taskService;
    private final RequirementService requirementService;
    private final BugService bugService;
    private final ProjectNodeMapper projectNodeMapper;
    private final BusinessAccessService businessAccessService;

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or hasAuthority('system:log:view')")
    public void exportOperationLogs(OperationLogQueryRequest request, OutputStream out) {
        writePages(out, OperationLogExportRow.class, "操作日志", request, () -> operationLogQueryService.list(request), e -> {
            OperationLogExportRow row = new OperationLogExportRow();
            row.setId(e.getId());
            row.setModule(e.getModule());
            row.setBusinessType(e.getBusinessType());
            row.setBusinessId(e.getBusinessId());
            row.setOperationType(e.getOperationType());
            row.setOperatorId(e.getOperatorId());
            row.setContent(e.getContent());
            row.setCreatedAt(e.getCreatedAt());
            return row;
        });
    }

    public void exportTasks(TaskQueryRequest request, OutputStream out) {
        writePages(out, TaskExportRow.class, "任务", request, () -> taskService.list(request), e -> {
            TaskExportRow row = new TaskExportRow();
            row.setId(e.getId());
            row.setProjectId(e.getProjectId());
            row.setName(e.getName());
            row.setPriority(e.getPriority());
            row.setStatus(e.getStatus());
            row.setAssigneeId(e.getAssigneeId());
            row.setPlannedStartDate(e.getPlannedStartDate());
            row.setPlannedEndDate(e.getPlannedEndDate());
            row.setActualStartDate(e.getActualStartDate());
            row.setActualEndDate(e.getActualEndDate());
            return row;
        });
    }

    public void exportRequirements(RequirementQueryRequest request, OutputStream out) {
        writePages(out, RequirementExportRow.class, "需求", request, () -> requirementService.list(request), e -> {
            RequirementExportRow row = new RequirementExportRow();
            row.setId(e.getId());
            row.setProjectId(e.getProjectId());
            row.setTitle(e.getTitle());
            row.setRequirementType(e.getRequirementType());
            row.setStatus(e.getStatus());
            row.setPriority(e.getPriority());
            row.setCreatedBy(e.getCreatedBy());
            row.setCreatedAt(e.getCreatedAt());
            return row;
        });
    }

    public void exportBugs(BugQueryRequest request, OutputStream out) {
        writePages(out, BugExportRow.class, "缺陷", request, () -> bugService.list(request), e -> {
            BugExportRow row = new BugExportRow();
            row.setId(e.getId());
            row.setProjectId(e.getProjectId());
            row.setTitle(e.getTitle());
            row.setStatus(bugStatusLabel(e.getStatus()));
            row.setSolution(bugSolutionLabel(e.getSolution()));
            row.setPriority(bugPriorityLabel(e.getPriority()));
            row.setCreatorId(e.getCreatorId());
            row.setAssigneeId(e.getAssigneeId());
            row.setCreatedAt(e.getCreatedAt());
            return row;
        });
    }

    private String bugSolutionLabel(String solution) {
        if (solution == null) {
            return null;
        }
        return switch (solution) {
            case "RESOLVED" -> "已解决";
            case "BY_DESIGN" -> "设计如此";
            case "DUPLICATE" -> "重复bug";
            case "EXTERNAL_CAUSE" -> "外部原因";
            case "CODE_BUG" -> "代码bug";
            case "CANNOT_REPRODUCE" -> "无法重现";
            case "DEFERRED" -> "延期处理";
            case "WONT_FIX" -> "不予解决";
            default -> solution;
        };
    }

    private String bugPriorityLabel(String priority) {
        if (priority == null) return null;
        return switch (priority) {
            case "LOW" -> "轻微";
            case "MEDIUM" -> "一般";
            case "HIGH" -> "严重";
            case "URGENT" -> "致命";
            default -> priority;
        };
    }

    private String bugStatusLabel(String status) {
        if (status == null) return null;
        return switch (status) {
            case "PENDING_FIX" -> "待修复";
            case "FIXING" -> "修复中";
            case "PENDING_VERIFY" -> "待验证";
            case "CLOSED" -> "已关闭";
            default -> status;
        };
    }

    public void exportGantt(Long projectId, OutputStream out) {
        if (!businessAccessService.canReadProject(projectId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查看该项目");
        }
        LambdaQueryWrapper<ProjectNodeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectNodeEntity::getProjectId, projectId)
                .orderByAsc(ProjectNodeEntity::getSortOrder, ProjectNodeEntity::getId);
        List<GanttExportRow> rows = projectNodeMapper.selectList(wrapper).stream().map(e -> {
            GanttExportRow row = new GanttExportRow();
            row.setId(e.getId());
            row.setProjectId(e.getProjectId());
            row.setNodeName(e.getNodeName());
            row.setNodeType(e.getNodeType());
            row.setStatus(e.getStatus());
            row.setProgressPercent(e.getProgressPercent());
            row.setPlannedStartDate(e.getPlannedStartDate());
            row.setPlannedEndDate(e.getPlannedEndDate());
            row.setActualStartDate(e.getActualStartDate());
            row.setActualEndDate(e.getActualEndDate());
            return row;
        }).toList();
        EasyExcel.write(out, GanttExportRow.class).autoCloseStream(false).sheet("甘特图").doWrite(rows);
    }

    private <T, R> void writePages(OutputStream out, Class<R> rowType, String sheetName, PageQuery request,
                                   Supplier<PageResult<T>> query, Function<T, R> mapRow) {
        Long originalPageNo = request.getPageNo();
        Long originalPageSize = request.getPageSize();
        request.setPageNo(1L);
        request.setPageSize(EXPORT_PAGE_SIZE);
        try {
            // Resolve access checks before opening the response workbook.
            PageResult<T> page = query.get();
            try (ExcelWriter writer = EasyExcel.write(out, rowType).autoCloseStream(false).build()) {
                WriteSheet sheet = EasyExcel.writerSheet(sheetName).build();
                while (true) {
                    List<R> rows = page.records().stream().map(mapRow).toList();
                    writer.write(rows, sheet);
                    if (page.records().isEmpty() || request.getPageNo() * EXPORT_PAGE_SIZE >= page.total()) break;
                    request.setPageNo(request.getPageNo() + 1L);
                    page = query.get();
                }
            }
        } finally {
            request.setPageNo(originalPageNo);
            request.setPageSize(originalPageSize);
        }
    }
}
