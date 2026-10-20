package com.jitong.projectflow.workhub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.task.dto.TaskCreateRequest;
import com.jitong.projectflow.task.dto.TaskUpdateRequest;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReleaseExecutionService {
    private static final Set<String> WORK_TYPES = Set.of("GENERAL", "FEATURE", "RELEASE_EXECUTION");
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final BusinessAccessService access;
    private final TaskMapper tasks;
    private final ProjectMapper projects;
    private final RequirementMapper requirements;
    private final OperationLogService logs;

    public void validateTaskCreate(TaskCreateRequest request) {
        String type = request.getWorkType() == null ? "GENERAL" : request.getWorkType();
        if (!WORK_TYPES.contains(type)) bad("无效的任务类型");
        if ("FEATURE".equals(type)) {
            if (request.getRequirementId() == null) bad("功能开发任务必须关联需求");
            RequirementEntity requirement = requirements.selectById(request.getRequirementId());
            if (requirement == null || !sameScope(request.getProjectId(), requirement.getProjectId())) {
                bad("功能开发任务必须关联同一交付范围内的需求");
            }
            access.requireRequirementRead(requirement);
            if (!"ACCEPTED".equals(requirement.getStatus())) bad("需求评审采纳后才能新建功能开发任务");
        }
        if (!"RELEASE_EXECUTION".equals(type)) return;
        if (!member(request.getProjectId(), CurrentUserContext.userId())) bad("只有项目成员可以创建上线执行任务");
        if (request.getParentId() != null) bad("上线执行任务不能作为子任务");
        if (!StringUtils.hasText(request.getReleaseId()) || !StringUtils.hasText(request.getReleaseEnvironment())
                || request.getReleaseWindowStart() == null || request.getReleaseWindowEnd() == null
                || request.getReleaseLeadId() == null || request.getReleaseOperatorIds() == null
                || request.getReleaseOperatorIds().isEmpty()) bad("请完整填写上线版本、环境、时间和执行人员");
        if (!request.getReleaseWindowEnd().isAfter(request.getReleaseWindowStart())) bad("上线结束时间必须晚于开始时间");
        if (request.getReleaseOperatorIds().size() > 30 || request.getReleaseSupportIds() != null
                && request.getReleaseSupportIds().size() > 30) bad("上线参与人员过多");
        Map<String, Object> release = release(request.getReleaseId());
        if (!sameScope(request.getProjectId(), ((Number) release.get("projectId")).longValue())) bad("上线版本不属于当前项目");
        if ("RELEASED".equals(release.get("status")) || "CANCELLED".equals(release.get("status"))) bad("不能为已发布或已取消版本创建上线任务");
        if (!jdbc.queryForList("SELECT task_id FROM pf_release_execution WHERE release_id=?", Long.class,
                request.getReleaseId()).isEmpty()) bad("该版本已有上线执行任务");
        if (!member(request.getProjectId(), request.getReleaseLeadId())
                || request.getReleaseOperatorIds().stream().anyMatch(id -> !member(request.getProjectId(), id))
                || request.getReleaseVerifierId() != null && !member(request.getProjectId(), request.getReleaseVerifierId())
                || request.getReleaseSupportIds() != null && request.getReleaseSupportIds().stream().anyMatch(id -> !member(request.getProjectId(), id))) {
            bad("上线人员必须属于项目成员");
        }
    }

    public void afterTaskCreated(TaskEntity task, TaskCreateRequest request) {
        if ("FEATURE".equals(task.getWorkType())) {
            jdbc.update("INSERT INTO pf_requirement_task(requirement_id,task_id) VALUES (?,?)",
                    request.getRequirementId(), task.getId());
            jdbc.update("DELETE FROM pf_requirement_acceptance WHERE requirement_id=?", request.getRequirementId());
            logs.record("requirement", "Requirement", request.getRequirementId(), "LINK_TASK", "新建并关联开发任务：" + task.getName());
        }
        if (!"RELEASE_EXECUTION".equals(task.getWorkType())) return;
        jdbc.update("""
                INSERT INTO pf_release_execution(task_id,release_id,environment,window_start,window_end,
                lead_id,operator_ids,verifier_id,support_ids,meeting_planned_at,plan_revision,
                created_by,created_at,updated_by,updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,1,?,?,?,?)
                """, task.getId(), request.getReleaseId(), request.getReleaseEnvironment().trim(),
                request.getReleaseWindowStart(), request.getReleaseWindowEnd(), request.getReleaseLeadId(),
                encode(request.getReleaseOperatorIds()), request.getReleaseVerifierId(),
                encode(request.getReleaseSupportIds() == null ? List.of() : request.getReleaseSupportIds()),
                request.getMeetingPlannedAt(), CurrentUserContext.userId(), LocalDateTime.now(),
                CurrentUserContext.userId(), LocalDateTime.now());
        audit(request.getReleaseId(), "PRE_RELEASE", "创建上线执行任务", "任务 #" + task.getId() + " · " + task.getName());
    }

    public void validateTaskUpdate(TaskEntity task, TaskUpdateRequest request) {
        if (!"RELEASE_EXECUTION".equals(task.getWorkType())) return;
        if (request.getProjectId() != null && !Objects.equals(request.getProjectId(), task.getProjectId()))
            bad("上线执行任务不能更换项目");
        if (request.getActualEndDate() != null) requireReportBeforeClose(task);
    }

    public void requireReportBeforeClose(TaskEntity task) {
        if ("RELEASE_EXECUTION".equals(task.getWorkType()) && jdbc.queryForObject(
                "SELECT COUNT(*) FROM pf_release_report WHERE task_id=? AND status='VERIFIED'", Long.class, task.getId()) == 0) {
            bad("请先提交上线报告单并由验证人确认，再完成上线执行任务");
        }
    }

    public void requireNotReleaseTask(TaskEntity task) {
        if ("RELEASE_EXECUTION".equals(task.getWorkType())) bad("上线执行任务属于正式迭代档案，不能删除");
    }

    public Map<String, Object> detail(Long taskId) {
        TaskEntity task = task(taskId);
        access.requireTaskRead(task);
        Map<String, Object> plan = plan(taskId);
        Map<String, Object> result = new LinkedHashMap<>(plan);
        result.put("operatorIds", decode((String) plan.get("operatorIds")));
        result.put("supportIds", decode((String) plan.get("supportIds")));
        result.put("meetings", jdbc.queryForList("""
                SELECT id,held_at AS heldAt,chair_id AS chairId,attendee_ids AS attendeeIds,
                agenda,decisions,risks,action_items AS actionItems,conclusion,
                plan_revision AS planRevision,
                recorded_by AS recordedBy,recorded_at AS recordedAt
                FROM pf_release_meeting WHERE task_id=? ORDER BY recorded_at DESC,id DESC
                """, taskId));
        ((List<Map<String, Object>>) result.get("meetings")).forEach(row -> {
            row.put("attendeeIds", decode((String) row.get("attendeeIds")));
            row.put("confirmedIds", jdbc.queryForList(
                    "SELECT user_id FROM pf_release_meeting_ack WHERE meeting_id=?", Long.class, row.get("id")));
        });
        List<Map<String, Object>> reports = jdbc.queryForList("""
                SELECT actual_start AS actualStart,actual_end AS actualEnd,package_ref AS packageRef,
                execution_steps AS executionSteps,smoke_result AS smokeResult,
                monitoring_result AS monitoringResult,incident_notes AS incidentNotes,
                rolled_back AS rolledBack,outcome,residual_issues AS residualIssues,
                verifier_id AS verifierId,status,verified_by AS verifiedBy,verified_at AS verifiedAt,
                verification_note AS verificationNote,submitted_by AS submittedBy,submitted_at AS submittedAt
                FROM pf_release_report WHERE task_id=?
                """, taskId);
        result.put("report", reports.isEmpty() ? null : reports.get(0));
        result.put("canManage", canManage(task));
        result.put("currentUserId", CurrentUserContext.userId());
        return result;
    }

    @Transactional
    public Map<String, Object> updatePlan(Long taskId, ReleaseExecutionRequests.Plan request) {
        TaskEntity task = task(taskId);
        access.requireTaskManage(task);
        Map<String, Object> before = plan(taskId);
        if (!Set.of("PLANNED", "READY").contains(before.get("releaseStatus"))) bad("版本已发布或取消，不能调整上线计划");
        List<String> statuses = jdbc.queryForList(
                "SELECT status FROM pf_release_application WHERE release_id=?", String.class, before.get("releaseId"));
        if (!statuses.isEmpty() && Set.of("SUBMITTED", "APPROVED").contains(statuses.get(0)))
            bad("请先撤回上线申请，再调整上线计划");
        if (!request.windowEnd().isAfter(request.windowStart())) bad("上线结束时间必须晚于开始时间");
        if (request.operatorIds().isEmpty() || request.operatorIds().size() > 30
                || request.supportIds() != null && request.supportIds().size() > 30) bad("请正确填写执行与值守人员");
        if (!member(task.getProjectId(), request.leadId())
                || request.operatorIds().stream().anyMatch(id -> !member(task.getProjectId(), id))
                || request.verifierId() != null && !member(task.getProjectId(), request.verifierId())
                || request.supportIds() != null && request.supportIds().stream().anyMatch(id -> !member(task.getProjectId(), id)))
            bad("上线人员必须属于项目成员");
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                UPDATE pf_release_execution SET environment=?,window_start=?,window_end=?,lead_id=?,
                operator_ids=?,verifier_id=?,support_ids=?,meeting_planned_at=?,plan_revision=plan_revision+1,
                updated_by=?,updated_at=? WHERE task_id=?
                """, request.environment().trim(), request.windowStart(), request.windowEnd(), request.leadId(),
                encode(request.operatorIds()), request.verifierId(),
                encode(request.supportIds() == null ? List.of() : request.supportIds()),
                request.meetingPlannedAt(), CurrentUserContext.userId(), now, taskId);
        int reopened = jdbc.update("""
                UPDATE pf_release_stage SET status='IN_PROGRESS',conclusion=NULL,actual_end=NULL,
                completed_by=NULL,completed_at=NULL,updated_by=?,updated_at=?
                WHERE release_id=? AND stage_code='PRE_RELEASE' AND status='COMPLETED'
                """, CurrentUserContext.userId(), now, before.get("releaseId"));
        if (reopened > 0) audit((String) before.get("releaseId"), "PRE_RELEASE",
                "重新评估上线前准备", "上线计划变更，原上线前评估结论已重新打开");
        Map<String, Object> after = plan(taskId);
        jdbc.update("""
                INSERT INTO pf_release_record(id,release_id,stage_code,record_type,title,content,
                before_json,after_json,actor_id,created_at)
                VALUES (?,?,'PRE_RELEASE','RELEASE_CHANGE','调整上线计划',?,?,?,?,?)
                """, UUID.randomUUID().toString(), before.get("releaseId"),
                "上线计划已调整，原会议结论需重新确认", snapshot(before), snapshot(after),
                CurrentUserContext.userId(), now);
        logs.record("task", "Task", taskId, "RELEASE_PLAN", "调整上线计划，需重新召开确认会议");
        return detail(taskId);
    }

    @Transactional
    public Map<String, Object> addMeeting(Long taskId, ReleaseExecutionRequests.Meeting request) {
        TaskEntity task = task(taskId);
        access.requireTaskManage(task);
        Map<String, Object> plan = plan(taskId);
        if (!"PLANNED".equals(plan.get("releaseStatus")) && !"READY".equals(plan.get("releaseStatus")))
            bad("已发布或已取消的版本不能补写上线前会议");
        List<String> applicationStatuses = jdbc.queryForList(
                "SELECT status FROM pf_release_application WHERE release_id=?", String.class, plan.get("releaseId"));
        if (!applicationStatuses.isEmpty() && Set.of("SUBMITTED", "APPROVED").contains(applicationStatuses.get(0)))
            bad("申请审批中或已批准，请先撤回申请再补充会议纪要");
        if (!Set.of("APPROVED", "CONDITIONAL", "DEFERRED").contains(request.conclusion())) bad("无效的会议结论");
        if (request.heldAt().isAfter(LocalDateTime.now())) bad("会议实际时间不能晚于现在");
        if (request.attendeeIds().isEmpty() || request.attendeeIds().size() > 50) bad("请选择参会人");
        if (!request.attendeeIds().contains(request.chairId())) bad("主持人必须在参会人名单中");
        if (!member(task.getProjectId(), request.chairId()) || request.attendeeIds().stream().anyMatch(id -> !member(task.getProjectId(), id)))
            bad("会议人员必须属于项目成员");
        if ("CONDITIONAL".equals(request.conclusion()) && !StringUtils.hasText(request.actionItems())) bad("有条件同意时必须填写待完成事项");
        jdbc.update("""
                INSERT INTO pf_release_meeting(id,task_id,held_at,chair_id,attendee_ids,agenda,decisions,
                risks,action_items,conclusion,plan_revision,recorded_by,recorded_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, UUID.randomUUID().toString(), taskId, request.heldAt(), request.chairId(),
                encode(request.attendeeIds()), request.agenda().trim(), request.decisions().trim(),
                request.risks().trim(), request.actionItems().trim(), request.conclusion(), plan.get("planRevision"),
                CurrentUserContext.userId(), LocalDateTime.now());
        audit((String) plan.get("releaseId"), "PRE_RELEASE", "上线会议纪要",
                "任务 #" + taskId + " · 会议结论：" + request.conclusion() + " · " + request.decisions());
        logs.record("task", "Task", taskId, "RELEASE_MEETING", "登记上线会议纪要：" + request.conclusion());
        return detail(taskId);
    }

    @Transactional
    public Map<String, Object> confirmMeeting(Long taskId, String meetingId) {
        TaskEntity task = task(taskId);
        access.requireTaskRead(task);
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT attendee_ids AS attendeeIds,plan_revision AS planRevision FROM pf_release_meeting WHERE id=? AND task_id=?", meetingId, taskId);
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "会议纪要不存在");
        if (!Objects.equals(((Number) rows.get(0).get("planRevision")).intValue(),
                ((Number) plan(taskId).get("planRevision")).intValue())) bad("上线计划已调整，请确认最新会议纪要");
        if (!decode((String) rows.get(0).get("attendeeIds")).contains(CurrentUserContext.userId()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有参会人可以确认纪要");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM pf_release_meeting_ack WHERE meeting_id=? AND user_id=?",
                Long.class, meetingId, CurrentUserContext.userId()) > 0) bad("您已确认该会议纪要");
        jdbc.update("INSERT INTO pf_release_meeting_ack(meeting_id,user_id,confirmed_at) VALUES (?,?,?)",
                meetingId, CurrentUserContext.userId(), LocalDateTime.now());
        audit((String) plan(taskId).get("releaseId"), "PRE_RELEASE", "参会人确认会议纪要",
                "任务 #" + taskId + " · 会议 " + meetingId + " · 用户 " + CurrentUserContext.userId());
        return detail(taskId);
    }

    @Transactional
    public Map<String, Object> submitReport(Long taskId, ReleaseExecutionRequests.Report request) {
        TaskEntity task = task(taskId);
        access.requireTaskManage(task);
        Map<String, Object> plan = plan(taskId);
        if (!"RELEASED".equals(plan.get("releaseStatus"))) bad("版本发布后才能填写上线报告单");
        if (!request.actualEnd().isAfter(request.actualStart()) || request.actualEnd().isAfter(LocalDateTime.now()))
            bad("实际执行时间不合法");
        if (!Set.of("SUCCESS", "ISSUES", "ROLLED_BACK").contains(request.outcome())) bad("无效的上线结论");
        if (request.rolledBack() != "ROLLED_BACK".equals(request.outcome())) bad("回滚状态与上线结论不一致");
        if (!member(task.getProjectId(), request.verifierId())) bad("验证人必须属于项目成员");
        List<Map<String, Object>> previous = jdbc.queryForList("SELECT * FROM pf_release_report WHERE task_id=?", taskId);
        if (!previous.isEmpty() && !"REJECTED".equals(previous.get(0).get("status")))
            bad("上线报告单已提交；后续补充请在迭代档案追加记录");
        if (previous.isEmpty()) jdbc.update("""
                INSERT INTO pf_release_report(task_id,actual_start,actual_end,package_ref,execution_steps,
                smoke_result,monitoring_result,incident_notes,rolled_back,outcome,residual_issues,
                verifier_id,status,submitted_by,submitted_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """, taskId, request.actualStart(), request.actualEnd(), request.packageRef().trim(),
                request.executionSteps().trim(), request.smokeResult().trim(), request.monitoringResult().trim(),
                request.incidentNotes().trim(), request.rolledBack(), request.outcome(),
                request.residualIssues().trim(), request.verifierId(), "SUBMITTED",
                CurrentUserContext.userId(), LocalDateTime.now());
        else {
            jdbc.update("""
                    UPDATE pf_release_report SET actual_start=?,actual_end=?,package_ref=?,execution_steps=?,
                    smoke_result=?,monitoring_result=?,incident_notes=?,rolled_back=?,outcome=?,
                    residual_issues=?,verifier_id=?,status='SUBMITTED',verified_by=NULL,verified_at=NULL,
                    verification_note=NULL,submitted_by=?,submitted_at=? WHERE task_id=?
                    """, request.actualStart(), request.actualEnd(), request.packageRef().trim(),
                    request.executionSteps().trim(), request.smokeResult().trim(), request.monitoringResult().trim(),
                    request.incidentNotes().trim(), request.rolledBack(), request.outcome(),
                    request.residualIssues().trim(), request.verifierId(), CurrentUserContext.userId(),
                    LocalDateTime.now(), taskId);
            jdbc.update("""
                    INSERT INTO pf_release_record(id,release_id,stage_code,record_type,title,content,
                    before_json,after_json,actor_id,created_at)
                    VALUES (?,?,'PRODUCTION_RELEASE','RELEASE_CHANGE','重提上线报告',?,?,?,?,?)
                    """, UUID.randomUUID().toString(), plan.get("releaseId"), "驳回后重新提交上线报告单",
                    snapshot(previous.get(0)), snapshot(jdbc.queryForMap("SELECT * FROM pf_release_report WHERE task_id=?", taskId)),
                    CurrentUserContext.userId(), LocalDateTime.now());
        }
        audit((String) plan.get("releaseId"), "PRODUCTION_RELEASE", "上线报告单已提交",
                "任务 #" + taskId + " · 结果：" + request.outcome() + " · 发布包：" + request.packageRef());
        logs.record("task", "Task", taskId, "RELEASE_REPORT", "提交上线报告单：" + request.outcome());
        return detail(taskId);
    }

    @Transactional
    public Map<String, Object> verifyReport(Long taskId, ReleaseExecutionRequests.Verification request) {
        TaskEntity task = task(taskId);
        access.requireTaskRead(task);
        Map<String, Object> plan = plan(taskId);
        List<Long> verifierIds = jdbc.queryForList(
                "SELECT verifier_id FROM pf_release_report WHERE task_id=? AND status='SUBMITTED'", Long.class, taskId);
        if (verifierIds.isEmpty()) bad("上线报告不存在或已经验证");
        if (!Objects.equals(verifierIds.get(0), CurrentUserContext.userId()))
            throw new BusinessException(ErrorCode.FORBIDDEN, "只有报告指定的验证人可以确认");
        if (!Set.of("APPROVED", "REJECTED").contains(request.decision())) bad("无效的验证结论");
        jdbc.update("""
                UPDATE pf_release_report SET status=?,verified_by=?,verified_at=?,verification_note=?
                WHERE task_id=? AND status='SUBMITTED'
                """, "APPROVED".equals(request.decision()) ? "VERIFIED" : "REJECTED",
                CurrentUserContext.userId(), LocalDateTime.now(), request.note().trim(), taskId);
        audit((String) plan.get("releaseId"), "PRODUCTION_RELEASE",
                "APPROVED".equals(request.decision()) ? "上线报告验证通过" : "上线报告验证驳回", request.note().trim());
        logs.record("task", "Task", taskId, "RELEASE_VERIFY", "验证上线报告：" + request.decision());
        return detail(taskId);
    }

    public void requireMeetingBeforeApplication(String releaseId) {
        List<Long> ids = jdbc.queryForList("SELECT task_id FROM pf_release_execution WHERE release_id=?", Long.class, releaseId);
        if (ids.size() != 1) bad("请先创建并关联该版本的上线执行任务");
        List<Map<String, Object>> meetings = jdbc.queryForList("""
                SELECT id,conclusion,attendee_ids AS attendeeIds,plan_revision AS planRevision FROM pf_release_meeting
                WHERE task_id=? ORDER BY recorded_at DESC,id DESC LIMIT 1
                """, ids.get(0));
        if (meetings.isEmpty() || !"APPROVED".equals(meetings.get(0).get("conclusion")))
            bad("上线会议尚未形成同意上线的结论");
        Map<String, Object> latest = meetings.get(0);
        Map<String, Object> plan = plan(ids.get(0));
        if (!Objects.equals(((Number) latest.get("planRevision")).intValue(),
                ((Number) plan.get("planRevision")).intValue())) bad("上线计划已调整，请重新召开确认会议");
        List<Long> attendees = decode((String) latest.get("attendeeIds"));
        List<Long> confirmed = jdbc.queryForList(
                "SELECT user_id FROM pf_release_meeting_ack WHERE meeting_id=?", Long.class, latest.get("id"));
        if (attendees.isEmpty() || !confirmed.containsAll(attendees)) bad("上线会议纪要尚未由所有参会人确认");
        List<Map<String, Object>> applications = jdbc.queryForList("""
                SELECT environment,window_start AS windowStart,window_end AS windowEnd
                FROM pf_release_application WHERE release_id=?
                """, releaseId);
        if (applications.isEmpty()) bad("请先填写上线申请单");
        Map<String, Object> app = applications.get(0);
        if (!Objects.equals(plan.get("environment"), app.get("environment"))
                || !Objects.equals(plan.get("windowStart"), app.get("windowStart"))
                || !Objects.equals(plan.get("windowEnd"), app.get("windowEnd"))) {
            bad("上线任务与申请单的环境、时间不一致，请先核对");
        }
    }

    public void requireVerifiedReportBeforeFollowup(String releaseId) {
        Long verified = jdbc.queryForObject("""
                SELECT COUNT(*) FROM pf_release_execution e JOIN pf_release_report r ON r.task_id=e.task_id
                WHERE e.release_id=? AND r.status='VERIFIED'
                """, Long.class, releaseId);
        if (verified == null || verified == 0) bad("请先提交上线报告并由验证人确认，再完成上线后跟进阶段");
    }

    private Map<String, Object> plan(Long taskId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT e.task_id AS taskId,e.release_id AS releaseId,r.name AS releaseName,
                r.status AS releaseStatus,e.environment,e.window_start AS windowStart,
                e.window_end AS windowEnd,e.lead_id AS leadId,e.operator_ids AS operatorIds,
                e.verifier_id AS verifierId,e.support_ids AS supportIds,
                e.meeting_planned_at AS meetingPlannedAt,e.plan_revision AS planRevision
                FROM pf_release_execution e JOIN pf_delivery_release r ON r.id=e.release_id
                WHERE e.task_id=?
                """, taskId);
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "上线执行计划不存在");
        return rows.get(0);
    }

    private Map<String, Object> release(String releaseId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT project_id AS projectId,status FROM pf_delivery_release WHERE id=?", releaseId);
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "交付版本不存在");
        return rows.get(0);
    }

    private TaskEntity task(Long taskId) {
        TaskEntity task = tasks.selectById(taskId);
        if (task == null || !"RELEASE_EXECUTION".equals(task.getWorkType()))
            throw new BusinessException(ErrorCode.NOT_FOUND, "上线执行任务不存在");
        return task;
    }

    private boolean sameScope(Long firstId, Long secondId) {
        if (Objects.equals(firstId, secondId)) return true;
        ProjectEntity first = projects.selectById(firstId);
        ProjectEntity second = projects.selectById(secondId);
        return first != null && second != null &&
                (Objects.equals(first.getManagementProjectId(), secondId)
                        || Objects.equals(second.getManagementProjectId(), firstId));
    }

    private boolean member(Long projectId, Long userId) {
        if (userId == null) return false;
        if (memberDirect(projectId, userId)) return true;
        ProjectEntity project = projects == null ? null : projects.selectById(projectId);
        return project != null && project.getManagementProjectId() != null
                && memberDirect(project.getManagementProjectId(), userId);
    }

    private boolean memberDirect(Long projectId, Long userId) {
        return !jdbc.queryForList("""
                SELECT u.id FROM sys_user u JOIN pf_project p ON p.id=?
                WHERE u.id=? AND u.enabled=1 AND u.deleted=0
                AND (u.id=p.manager_id OR u.id=p.created_by
                OR CONCAT(',',REPLACE(COALESCE(p.co_manager_ids,''),' ',''),',') LIKE CONCAT('%,',u.id,',%')
                OR EXISTS (SELECT 1 FROM pf_project_participant pp WHERE pp.project_id=p.id AND pp.user_id=u.id))
                """, Long.class, projectId, userId).isEmpty();
    }

    private boolean canManage(TaskEntity task) {
        try { access.requireTaskManage(task); return true; }
        catch (BusinessException exception) { return false; }
    }

    private String encode(List<Long> ids) {
        try { return json.writeValueAsString(ids); }
        catch (JsonProcessingException exception) { throw new BusinessException(ErrorCode.BAD_REQUEST, "人员列表不合法"); }
    }

    private String snapshot(Map<String, Object> data) {
        try { return json.writeValueAsString(data); }
        catch (JsonProcessingException exception) { throw new BusinessException(ErrorCode.BAD_REQUEST, "上线计划留痕失败"); }
    }

    private List<Long> decode(String value) {
        if (!StringUtils.hasText(value)) return List.of();
        try { return json.readValue(value, new TypeReference<List<Long>>() {}); }
        catch (JsonProcessingException exception) { return List.of(); }
    }

    private void audit(String releaseId, String stage, String title, String content) {
        jdbc.update("""
                INSERT INTO pf_release_record(id,release_id,stage_code,record_type,title,content,actor_id,created_at)
                VALUES (?,?,?,?,?,?,?,?)
                """, UUID.randomUUID().toString(), releaseId, stage, "NOTE", title, content,
                CurrentUserContext.userId(), LocalDateTime.now());
    }

    private void bad(String message) { throw new BusinessException(ErrorCode.BAD_REQUEST, message); }
}
