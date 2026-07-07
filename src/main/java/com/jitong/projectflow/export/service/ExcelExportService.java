package com.jitong.projectflow.export.service;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.bug.entity.BugEntity;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.export.dto.BugExportRow;
import com.jitong.projectflow.export.dto.GanttExportRow;
import com.jitong.projectflow.export.dto.OperationLogExportRow;
import com.jitong.projectflow.export.dto.RequirementExportRow;
import com.jitong.projectflow.export.dto.TaskExportRow;
import com.jitong.projectflow.project.entity.ProjectNodeEntity;
import com.jitong.projectflow.project.mapper.ProjectNodeMapper;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.entity.OperationLog;
import com.jitong.projectflow.system.mapper.OperationLogMapper;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.util.List;

@Service
public class ExcelExportService {

    private final OperationLogMapper operationLogMapper;
    private final TaskMapper taskMapper;
    private final RequirementMapper requirementMapper;
    private final BugMapper bugMapper;
    private final ProjectNodeMapper projectNodeMapper;

    public ExcelExportService(OperationLogMapper operationLogMapper,
                               TaskMapper taskMapper,
                               RequirementMapper requirementMapper,
                               BugMapper bugMapper,
                               ProjectNodeMapper projectNodeMapper) {
        this.operationLogMapper = operationLogMapper;
        this.taskMapper = taskMapper;
        this.requirementMapper = requirementMapper;
        this.bugMapper = bugMapper;
        this.projectNodeMapper = projectNodeMapper;
    }

    public void exportOperationLogs(OutputStream out) {
        List<OperationLog> entities = operationLogMapper.selectList(null);
        List<OperationLogExportRow> rows = entities.stream().map(e -> {
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
        }).toList();
        EasyExcel.write(out, OperationLogExportRow.class).sheet("操作日志").doWrite(rows);
    }

    public void exportTasks(OutputStream out) {
        List<TaskEntity> entities = taskMapper.selectList(null);
        List<TaskExportRow> rows = entities.stream().map(e -> {
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
        }).toList();
        EasyExcel.write(out, TaskExportRow.class).sheet("任务").doWrite(rows);
    }

    public void exportRequirements(OutputStream out) {
        List<RequirementEntity> entities = requirementMapper.selectList(null);
        List<RequirementExportRow> rows = entities.stream().map(e -> {
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
        }).toList();
        EasyExcel.write(out, RequirementExportRow.class).sheet("需求").doWrite(rows);
    }

    public void exportBugs(OutputStream out) {
        List<BugEntity> entities = bugMapper.selectList(null);
        List<BugExportRow> rows = entities.stream().map(e -> {
            BugExportRow row = new BugExportRow();
            row.setId(e.getId());
            row.setProjectId(e.getProjectId());
            row.setTitle(e.getTitle());
            row.setStatus(e.getStatus());
            row.setPriority(e.getPriority());
            row.setCreatorId(e.getCreatorId());
            row.setAssigneeId(e.getAssigneeId());
            row.setCreatedAt(e.getCreatedAt());
            return row;
        }).toList();
        EasyExcel.write(out, BugExportRow.class).sheet("缺陷").doWrite(rows);
    }

    public void exportGantt(Long projectId, OutputStream out) {
        LambdaQueryWrapper<ProjectNodeEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ProjectNodeEntity::getProjectId, projectId);
        List<ProjectNodeEntity> entities = projectNodeMapper.selectList(wrapper);
        List<GanttExportRow> rows = entities.stream().map(e -> {
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
        EasyExcel.write(out, GanttExportRow.class).sheet("甘特图").doWrite(rows);
    }
}
