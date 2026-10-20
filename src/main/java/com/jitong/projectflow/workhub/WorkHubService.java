package com.jitong.projectflow.workhub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.common.error.ErrorCode;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkHubService {
    private static final Set<String> ACCEPTANCE_RESULTS = Set.of("PENDING", "PASSED", "FAILED");
    private static final Set<String> RELEASE_STATUSES = Set.of("PLANNED", "READY", "RELEASED", "CANCELLED");
    private static final Set<String> VIEW_SCOPES = Set.of("PROJECT", "TASK", "REQUIREMENT", "BUG", "GLOBAL");
    private static final Set<String> FILTER_KEYS = Set.of("keyword", "projectId", "status", "priority",
            "assigneeId", "creatorId", "createdFrom", "createdTo", "plannedEndDate", "requirementType",
            "roleName", "mineOnly", "taskId");

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final BusinessAccessService access;
    private final ProjectMapper projects;
    private final RequirementMapper requirements;
    private final TaskMapper tasks;
    private final NoticeService notices;
    private final OperationLogService logs;

    public Map<String, Object> requirementDelivery(Long id) {
        RequirementEntity requirement = requirement(id);
        access.requireRequirementRead(requirement);
        List<Map<String, Object>> linkedTasks = jdbc.queryForList("""
                SELECT t.id, t.name, t.status, t.assignee_id AS assigneeId,
                       t.planned_end_date AS plannedEndDate, b.reason AS blockerReason,
                       (SELECT COUNT(*) FROM pf_bug_task bt JOIN pf_bug bug ON bug.id=bt.bug_id
                        WHERE bt.task_id=t.id AND bug.deleted=0 AND bug.status<>'CLOSED') AS openBugCount
                FROM pf_requirement_task rt JOIN pf_task t ON t.id=rt.task_id
                LEFT JOIN pf_task_blocker b ON b.task_id=t.id
                WHERE rt.requirement_id=? AND t.deleted=0 ORDER BY t.sort_order, t.id
                """, id).stream().filter(row -> canReadTask(((Number) row.get("id")).longValue())).toList();
        List<Map<String, Object>> acceptance = jdbc.queryForList("""
                SELECT result, conclusion, accepted_by AS acceptedBy, accepted_at AS acceptedAt
                FROM pf_requirement_acceptance WHERE requirement_id=?
                """, id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("requirementId", id);
        result.put("tasks", linkedTasks);
        result.put("acceptance", acceptance.isEmpty() ? Map.of("result", "PENDING") : acceptance.get(0));
        result.put("completedCount", linkedTasks.stream().filter(t -> "COMPLETED".equals(t.get("status"))).count());
        return result;
    }

    @Transactional
    public Map<String, Object> linkRequirementTasks(Long id, WorkHubRequests.TaskLinks request) {
        RequirementEntity requirement = requirement(id);
        access.requireRequirementManage(requirement);
        List<Long> ids = distinctIds(request.taskIds(), 200);
        for (Long taskId : ids) {
            TaskEntity task = task(taskId);
            access.requireTaskRead(task);
            if (!sameDeliveryScope(requirement.getProjectId(), task.getProjectId())) bad("关联任务必须属于当前项目或其关联项目");
        }
        Set<Long> oldIds = new LinkedHashSet<>(jdbc.queryForList(
                "SELECT task_id FROM pf_requirement_task WHERE requirement_id=?", Long.class, id));
        if (!oldIds.equals(new LinkedHashSet<>(ids))) {
            jdbc.update("DELETE FROM pf_requirement_task WHERE requirement_id=?", id);
            for (Long taskId : ids) {
                jdbc.update("INSERT INTO pf_requirement_task(requirement_id,task_id) VALUES (?,?)", id, taskId);
            }
            jdbc.update("DELETE FROM pf_requirement_acceptance WHERE requirement_id=?", id);
        }
        logs.record("requirement", "Requirement", id, "LINK_TASK", "更新需求关联任务，共 " + ids.size() + " 项");
        return requirementDelivery(id);
    }

    @Transactional
    public Map<String, Object> acceptRequirement(Long id, WorkHubRequests.Acceptance request) {
        RequirementEntity requirement = requirement(id);
        access.requireRequirementReview(requirement);
        if (!ACCEPTANCE_RESULTS.contains(request.result())) bad("无效的验收结论");
        if (!"ACCEPTED".equals(requirement.getStatus())) bad("只有已采纳的需求可以验收");
        if (!"PENDING".equals(request.result()) && !StringUtils.hasText(request.conclusion())) bad("请填写验收说明");
        if ("PASSED".equals(request.result())) {
            long unfinished = count("""
                    SELECT COUNT(*) FROM pf_requirement_task rt LEFT JOIN pf_task t ON t.id=rt.task_id
                    WHERE rt.requirement_id=? AND (t.id IS NULL OR t.deleted<>0 OR t.status<>'COMPLETED')
                    """, id);
            long openBugs = count("""
                    SELECT COUNT(DISTINCT bug.id) FROM pf_requirement_task rt
                    JOIN pf_bug_task bt ON bt.task_id=rt.task_id
                    JOIN pf_bug bug ON bug.id=bt.bug_id
                    WHERE rt.requirement_id=? AND bug.deleted=0 AND bug.status<>'CLOSED'
                    """, id);
            if (unfinished > 0 || openBugs > 0) bad("仍有关联任务未完成或关联 Bug 未关闭，不能通过验收");
        }
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO pf_requirement_acceptance(requirement_id,result,conclusion,accepted_by,accepted_at,updated_at)
                VALUES (?,?,?,?,?,?) ON DUPLICATE KEY UPDATE result=VALUES(result), conclusion=VALUES(conclusion),
                accepted_by=VALUES(accepted_by), accepted_at=VALUES(accepted_at), updated_at=VALUES(updated_at)
                """, id, request.result(), request.conclusion(),
                "PENDING".equals(request.result()) ? null : CurrentUserContext.userId(),
                "PENDING".equals(request.result()) ? null : now, now);
        logs.record("requirement", "Requirement", id, "ACCEPT", "需求验收：" + request.result());
        if (requirement.getCreatedBy() != null && !requirement.getCreatedBy().equals(CurrentUserContext.userId())) {
            notices.create(requirement.getCreatedBy(), NoticeType.REQUIREMENT_STATUS_CHANGED,
                    "需求「" + requirement.getTitle() + "」已有验收结论", request.result(), "Requirement", id);
        }
        return requirementDelivery(id);
    }

    public Map<String, Object> projectOverview(Long projectId) {
        access.requireProjectRead(projectId);
        List<Long> projectIds = overviewProjectIds(projectId);
        NamedParameterJdbcTemplate scoped = new NamedParameterJdbcTemplate(jdbc);
        Map<String, Object> parameters = Map.of("ids", projectIds);
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("requirements", scopedCount(scoped, "SELECT COUNT(*) FROM pf_requirement WHERE project_id IN (:ids) AND deleted=0", parameters));
        counts.put("tasks", scopedCount(scoped, "SELECT COUNT(*) FROM pf_task WHERE project_id IN (:ids) AND deleted=0", parameters));
        counts.put("bugs", scopedCount(scoped, "SELECT COUNT(*) FROM pf_bug WHERE project_id IN (:ids) AND deleted=0", parameters));
        counts.put("openBugs", scopedCount(scoped, "SELECT COUNT(*) FROM pf_bug WHERE project_id IN (:ids) AND deleted=0 AND status<>'CLOSED'", parameters));
        counts.put("blockedTasks", scopedCount(scoped, """
                SELECT COUNT(*) FROM pf_task t JOIN pf_task_blocker b ON b.task_id=t.id
                WHERE t.project_id IN (:ids) AND t.deleted=0 AND NULLIF(TRIM(b.reason),'') IS NOT NULL
                """, parameters));
        counts.put("overdueTasks", scopedCount(scoped, """
                SELECT COUNT(*) FROM pf_task WHERE project_id IN (:ids) AND deleted=0 AND planned_end_date<CURRENT_DATE
                AND status NOT IN ('COMPLETED','PAUSED')
                """, parameters));
        counts.put("files", scopedCount(scoped, """
                SELECT COUNT(*) FROM pf_file WHERE business_id IN (:ids) AND deleted=0
                AND business_type IN ('PROJECT','Project','ManagementProject','ExecutionProject','management-project','execution-project')
                """, parameters));
        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("counts", counts);
        overview.put("requirements", scoped.queryForList("""
                SELECT id, requirement_no AS requirementNo, title, status, priority, reviewer_id AS reviewerId
                FROM pf_requirement WHERE project_id IN (:ids) AND deleted=0 ORDER BY created_at DESC LIMIT 20
                """, parameters));
        overview.put("tasks", scoped.queryForList("""
                SELECT t.id, t.name, t.status, t.priority, t.assignee_id AS assigneeId,
                       t.planned_end_date AS plannedEndDate, b.reason AS blockerReason
                FROM pf_task t LEFT JOIN pf_task_blocker b ON b.task_id=t.id
                WHERE t.project_id IN (:ids) AND t.deleted=0 ORDER BY t.created_at DESC LIMIT 20
                """, parameters));
        overview.put("bugs", scoped.queryForList("""
                SELECT id, bug_no AS bugNo, title, status, priority, assignee_id AS assigneeId, solution
                FROM pf_bug WHERE project_id IN (:ids) AND deleted=0 ORDER BY created_at DESC LIMIT 20
                """, parameters));
        overview.put("releases", releases(projectId));
        overview.put("activity", scoped.queryForList("""
                SELECT id, business_type AS businessType, business_id AS businessId,
                       operation_type AS operationType, operator_name AS operatorName,
                       content, created_at AS createdAt
                FROM sys_operation_log WHERE (business_type='Project' AND business_id IN (:ids))
                  OR (business_type='Task' AND business_id IN (SELECT id FROM pf_task WHERE project_id IN (:ids)))
                  OR (business_type='Requirement' AND business_id IN (SELECT id FROM pf_requirement WHERE project_id IN (:ids)))
                  OR (business_type='Bug' AND business_id IN (SELECT id FROM pf_bug WHERE project_id IN (:ids)))
                ORDER BY created_at DESC LIMIT 15
                """, parameters));
        return overview;
    }

    private List<Long> overviewProjectIds(Long projectId) {
        ProjectEntity selected = project(projectId);
        List<Long> candidates = new ArrayList<>();
        candidates.add(projectId);
        if (selected.getManagementProjectId() != null) {
            candidates.add(selected.getManagementProjectId());
        } else {
            candidates.addAll(jdbc.queryForList(
                    "SELECT id FROM pf_project WHERE management_project_id=? AND deleted=0", Long.class, projectId));
        }
        return candidates.stream().distinct().filter(access::canReadProject).toList();
    }

    private long scopedCount(NamedParameterJdbcTemplate scoped, String sql, Map<String, Object> parameters) {
        Long value = scoped.queryForObject(sql, parameters, Long.class);
        return value == null ? 0 : value;
    }

    public Map<String, Object> taskBlocker(Long id) {
        TaskEntity task = task(id);
        access.requireTaskRead(task);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT reason, reported_by AS reportedBy, reported_at AS reportedAt, updated_at AS updatedAt
                FROM pf_task_blocker WHERE task_id=?
                """, id);
        Map<String, Object> result = new LinkedHashMap<>(rows.isEmpty() ? Map.of() : rows.get(0));
        try {
            access.requireTaskManage(task);
            result.put("canEdit", true);
        } catch (BusinessException forbidden) {
            result.put("canEdit", false);
        }
        return result;
    }

    public List<Map<String, Object>> taskRequirements(Long id) {
        TaskEntity task = task(id);
        access.requireTaskRead(task);
        return jdbc.queryForList("""
                SELECT r.id, r.requirement_no AS requirementNo, r.title, r.status,
                       COALESCE(a.result,'PENDING') AS acceptanceResult
                FROM pf_requirement_task rt JOIN pf_requirement r ON r.id=rt.requirement_id
                LEFT JOIN pf_requirement_acceptance a ON a.requirement_id=r.id
                WHERE rt.task_id=? AND r.deleted=0 ORDER BY r.created_at DESC
                """, id).stream().filter(row -> canReadRequirement(((Number) row.get("id")).longValue())).toList();
    }

    @Transactional
    public Map<String, Object> updateTaskBlocker(Long id, WorkHubRequests.Blocker request) {
        TaskEntity task = task(id);
        access.requireTaskManage(task);
        String reason = request.reason() == null ? null : request.reason().trim();
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO pf_task_blocker(task_id,reason,reported_by,reported_at,updated_at)
                VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE reason=VALUES(reason),
                reported_by=VALUES(reported_by), reported_at=VALUES(reported_at), updated_at=VALUES(updated_at)
                """, id, reason, StringUtils.hasText(reason) ? CurrentUserContext.userId() : null,
                StringUtils.hasText(reason) ? now : null, now);
        logs.record("task", "Task", id, "BLOCKER", StringUtils.hasText(reason) ? "登记阻塞：" + reason : "清除阻塞");
        return taskBlocker(id);
    }

    public List<Map<String, Object>> comments(String type, Long id) {
        readWork(type, id);
        return jdbc.queryForList("""
                SELECT c.id, c.content, c.author_id AS authorId, u.real_name AS authorName,
                       c.created_at AS createdAt FROM pf_work_comment c
                LEFT JOIN sys_user u ON u.id=c.author_id
                WHERE c.business_type=? AND c.business_id=? ORDER BY c.created_at ASC
                """, workType(type), id);
    }

    public List<Map<String, Object>> mentionCandidates(String type, Long id) {
        String normalizedType = workType(type);
        Long projectId = readWork(normalizedType, id);
        return jdbc.queryForList("""
                SELECT u.id, u.real_name AS name FROM sys_user u JOIN pf_project p ON p.id=?
                WHERE u.enabled=1 AND u.deleted=0 AND (
                    u.id=p.manager_id OR u.id=p.created_by
                    OR CONCAT(',',REPLACE(COALESCE(p.co_manager_ids,''),' ',''),',') LIKE CONCAT('%,',u.id,',%')
                    OR EXISTS (SELECT 1 FROM pf_project_participant pp WHERE pp.project_id=p.id AND pp.user_id=u.id)
                    OR (?='TASK' AND EXISTS (SELECT 1 FROM pf_task t WHERE t.id=? AND (t.assignee_id=u.id OR t.created_by=u.id)))
                    OR (?='REQUIREMENT' AND EXISTS (SELECT 1 FROM pf_requirement r WHERE r.id=? AND (r.reviewer_id=u.id OR r.created_by=u.id)))
                ) ORDER BY u.real_name
                """, projectId, normalizedType, id, normalizedType, id);
    }

    @Transactional
    public List<Map<String, Object>> addComment(String type, Long id, WorkHubRequests.Comment request) {
        String normalizedType = workType(type);
        Long projectId = readWork(normalizedType, id);
        String content = request.content().trim();
        if (content.isEmpty()) bad("评论内容不能为空");
        Set<Long> mentioned = new LinkedHashSet<>(distinctIds(request.mentionedUserIds(), 20));
        for (Long userId : mentioned) {
            if (!canMention(projectId, normalizedType, id, userId)) bad("被提及人不属于该项目或无权查看该工作项");
        }
        jdbc.update("INSERT INTO pf_work_comment(id,business_type,business_id,author_id,content,created_at) VALUES (?,?,?,?,?,?)",
                UUID.randomUUID().toString(), normalizedType, id, CurrentUserContext.userId(), content, LocalDateTime.now());
        for (Long userId : mentioned) {
            if (!userId.equals(CurrentUserContext.userId())) notices.create(userId, NoticeType.SYSTEM,
                    "您在" + ("TASK".equals(normalizedType) ? "任务" : "需求") + "评论中被提及",
                    content.length() > 100 ? content.substring(0, 100) : content,
                    "TASK".equals(normalizedType) ? "Task" : "Requirement", id);
        }
        logs.record(normalizedType.toLowerCase(), "TASK".equals(normalizedType) ? "Task" : "Requirement",
                id, "COMMENT", "新增评论");
        return comments(normalizedType, id);
    }

    public List<Map<String, Object>> releases(Long projectId) {
        access.requireProjectRead(projectId);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, project_id AS projectId, name, target_date AS targetDate, status,
                       acceptance_note AS acceptanceNote, released_at AS releasedAt,
                       created_at AS createdAt FROM pf_delivery_release
                WHERE project_id=? ORDER BY target_date DESC, created_at DESC
                """, projectId);
        rows.forEach(row -> row.putAll(releaseProgress((String) row.get("id"))));
        return rows;
    }

    @Transactional
    public Map<String, Object> saveRelease(String id, WorkHubRequests.Release request) {
        ProjectEntity project = project(request.projectId());
        access.requireProjectManage(project);
        if (!RELEASE_STATUSES.contains(request.status())) bad("无效的发布状态");
        List<Long> requirementIds = distinctIds(request.requirementIds(), 200);
        List<Long> taskIds = distinctIds(request.taskIds(), 500);
        if (requirementIds.isEmpty() && taskIds.isEmpty()) bad("交付版本至少包含一项需求或任务");
        for (Long requirementId : requirementIds) {
            RequirementEntity requirement = requirement(requirementId);
            access.requireRequirementRead(requirement);
            if (!sameDeliveryScope(request.projectId(), requirement.getProjectId())) bad("需求不属于当前项目或其关联项目");
        }
        for (Long taskId : taskIds) {
            TaskEntity task = task(taskId);
            access.requireTaskRead(task);
            if (!sameDeliveryScope(request.projectId(), task.getProjectId())) bad("任务不属于当前项目或其关联项目");
        }
        String releaseId = id == null ? UUID.randomUUID().toString() : id;
        Map<String, Object> existing = id == null ? null : release(id);
        Map<String, Object> previousScope = existing == null ? null : releaseProgress(releaseId);
        if (existing != null) {
            if (!request.projectId().equals(((Number) existing.get("projectId")).longValue())) bad("版本不属于当前项目");
            if ("RELEASED".equals(existing.get("status"))) bad("已发布的版本不能修改");
            if ("RELEASED".equals(request.status()) && !"READY".equals(existing.get("status"))) bad("请先确认版本就绪");
            List<String> applicationStatuses = jdbc.queryForList(
                    "SELECT status FROM pf_release_application WHERE release_id=?", String.class, releaseId);
            if (!applicationStatuses.isEmpty() && ("SUBMITTED".equals(applicationStatuses.get(0))
                    || "APPROVED".equals(applicationStatuses.get(0)))) {
                if (!"RELEASED".equals(request.status())) bad("上线申请审批中或已通过，版本范围不可修改");
                Map<String, Object> selected = releaseProgress(releaseId);
                if (!Objects.equals(existing.get("name"), request.name().trim())
                        || !Objects.equals(String.valueOf(existing.get("targetDate")), request.targetDate().toString())
                        || !new LinkedHashSet<>((List<Long>) selected.get("selectedRequirementIds")).equals(new LinkedHashSet<>(requirementIds))
                        || !new LinkedHashSet<>((List<Long>) selected.get("selectedTaskIds")).equals(new LinkedHashSet<>(taskIds))) {
                    bad("上线申请通过后不能更改版本名称、日期或交付范围");
                }
            }
        } else if ("RELEASED".equals(request.status())) {
            bad("请先创建并确认版本就绪");
        }
        if ("RELEASED".equals(request.status()) && !StringUtils.hasText(request.acceptanceNote())) bad("发布前请填写验收说明");
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            jdbc.update("""
                    INSERT INTO pf_delivery_release(id,project_id,name,target_date,status,acceptance_note,released_at,
                    created_by,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?,?,?)
                    """, releaseId, request.projectId(), request.name().trim(), request.targetDate(), request.status(),
                    request.acceptanceNote(), null, CurrentUserContext.userId(), now, now);
        } else {
            jdbc.update("""
                    UPDATE pf_delivery_release SET name=?,target_date=?,status=?,acceptance_note=?,released_at=?,updated_at=? WHERE id=?
                    """, request.name().trim(), request.targetDate(), request.status(), request.acceptanceNote(),
                    "RELEASED".equals(request.status()) ? now : null, now, releaseId);
            jdbc.update("DELETE FROM pf_delivery_release_item WHERE release_id=?", releaseId);
        }
        for (Long requirementId : requirementIds) {
            jdbc.update("INSERT INTO pf_delivery_release_item(release_id,business_type,business_id) VALUES (?,'REQUIREMENT',?)", releaseId, requirementId);
        }
        for (Long taskId : taskIds) {
            jdbc.update("INSERT INTO pf_delivery_release_item(release_id,business_type,business_id) VALUES (?,'TASK',?)", releaseId, taskId);
        }
        Map<String, Object> progress = releaseProgress(releaseId);
        if (("READY".equals(request.status()) || "RELEASED".equals(request.status()))
                && !Boolean.TRUE.equals(progress.get("ready"))) {
            bad("还有未验收需求、未完成任务、阻塞任务或未关闭 Bug，不能标记就绪或发布");
        }
        if ("RELEASED".equals(request.status())) {
            long approved = count("SELECT COUNT(*) FROM pf_release_application WHERE release_id=? AND status='APPROVED'", releaseId);
            long completed = count("SELECT COUNT(*) FROM pf_release_stage WHERE release_id=? AND status='COMPLETED' AND stage_code IN ("
                    + "'REQUIREMENT_SUBMISSION','REQUIREMENT_REVIEW','UI_DESIGN','DEVELOPMENT','TEST_HANDOFF',"
                    + "'TEST_VERIFICATION','PRODUCT_ACCEPTANCE','PRE_RELEASE')", releaseId);
            if (approved != 1 || completed != 8) bad("上线申请需完成审批，且上线前八个阶段必须完成");
            jdbc.update("""
                    INSERT INTO pf_release_stage(release_id,stage_code,owner_id,actual_start,actual_end,status,
                    conclusion,completed_by,completed_at,updated_by,updated_at)
                    VALUES (?,'PRODUCTION_RELEASE',?,?,?,'COMPLETED',?,?,?,?,?)
                    """, releaseId, CurrentUserContext.userId(), now.toLocalDate(), now.toLocalDate(),
                    request.acceptanceNote(), CurrentUserContext.userId(), now, CurrentUserContext.userId(), now);
            jdbc.update("""
                    INSERT INTO pf_release_record(id,release_id,stage_code,record_type,title,content,
                    actor_id,created_at) VALUES (?,?,'PRODUCTION_RELEASE','STAGE_CHANGE','版本上线发布',?,?,?)
                    """, UUID.randomUUID().toString(), releaseId, request.acceptanceNote(), CurrentUserContext.userId(), now);
        }
        Map<String, Object> before = existing == null ? null : releaseAuditSnapshot(
                String.valueOf(existing.get("name")), String.valueOf(existing.get("targetDate")),
                String.valueOf(existing.get("status")), (String) existing.get("acceptanceNote"),
                (List<Long>) previousScope.get("selectedRequirementIds"),
                (List<Long>) previousScope.get("selectedTaskIds"));
        Map<String, Object> after = releaseAuditSnapshot(request.name().trim(), request.targetDate().toString(),
                request.status(), request.acceptanceNote(), requirementIds, taskIds);
        jdbc.update("""
                INSERT INTO pf_release_record(id,release_id,stage_code,record_type,title,content,
                before_json,after_json,actor_id,created_at)
                VALUES (?,?,'REQUIREMENT_SUBMISSION','RELEASE_CHANGE',?,?,?,?,?,?)
                """, UUID.randomUUID().toString(), releaseId, existing == null ? "创建交付版本" : "变更交付版本",
                "版本范围、日期与状态变更", releaseAuditJson(before), releaseAuditJson(after),
                CurrentUserContext.userId(), now);
        logs.record("project", "Project", request.projectId(), "RELEASE", "更新交付版本「" + request.name().trim() + "」：" + request.status());
        Map<String, Object> result = release(releaseId);
        result.putAll(progress);
        return result;
    }

    private Map<String, Object> releaseAuditSnapshot(String name, String targetDate, String status,
                                                      String note, List<Long> requirements, List<Long> tasks) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("name", name);
        snapshot.put("targetDate", targetDate);
        snapshot.put("status", status);
        snapshot.put("acceptanceNote", note);
        snapshot.put("requirementIds", requirements);
        snapshot.put("taskIds", tasks);
        return snapshot;
    }

    private String releaseAuditJson(Map<String, Object> snapshot) {
        if (snapshot == null) return null;
        try { return json.writeValueAsString(snapshot); }
        catch (JsonProcessingException exception) { throw new BusinessException(ErrorCode.BAD_REQUEST, "版本记录序列化失败"); }
    }

    private Map<String, Object> releaseProgress(String releaseId) {
        List<Map<String, Object>> selectedItems = jdbc.queryForList(
                "SELECT business_type AS type,business_id AS id FROM pf_delivery_release_item WHERE release_id=?", releaseId);
        List<Map<String, Object>> requirementRows = jdbc.queryForList("""
                SELECT r.id, r.requirement_no AS requirementNo, r.title, r.status,
                       COALESCE(a.result,'PENDING') AS acceptanceResult
                FROM pf_delivery_release_item i JOIN pf_requirement r ON r.id=i.business_id
                LEFT JOIN pf_requirement_acceptance a ON a.requirement_id=r.id
                WHERE i.release_id=? AND i.business_type='REQUIREMENT' AND r.deleted=0
                """, releaseId);
        List<Map<String, Object>> taskRows = jdbc.queryForList("""
                SELECT t.id, t.name, t.status, b.reason AS blockerReason
                FROM pf_task t LEFT JOIN pf_task_blocker b ON b.task_id=t.id
                WHERE t.deleted=0 AND (
                    t.id IN (SELECT business_id FROM pf_delivery_release_item WHERE release_id=? AND business_type='TASK')
                    OR t.id IN (SELECT rt.task_id FROM pf_requirement_task rt JOIN pf_delivery_release_item i
                        ON i.business_id=rt.requirement_id AND i.business_type='REQUIREMENT' WHERE i.release_id=?))
                GROUP BY t.id,t.name,t.status,b.reason
                """, releaseId, releaseId);
        long openBugs = count("""
                SELECT COUNT(DISTINCT bug.id) FROM pf_bug bug JOIN pf_bug_task bt ON bt.bug_id=bug.id
                WHERE bug.deleted=0 AND bug.status<>'CLOSED' AND bt.task_id IN (
                    SELECT business_id FROM pf_delivery_release_item WHERE release_id=? AND business_type='TASK'
                    UNION SELECT rt.task_id FROM pf_requirement_task rt JOIN pf_delivery_release_item i
                    ON i.business_id=rt.requirement_id AND i.business_type='REQUIREMENT' WHERE i.release_id=?)
                """, releaseId, releaseId);
        long invalidLinkedTasks = count("""
                SELECT COUNT(*) FROM pf_delivery_release_item i
                JOIN pf_requirement_task rt ON rt.requirement_id=i.business_id
                LEFT JOIN pf_task t ON t.id=rt.task_id
                WHERE i.release_id=? AND i.business_type='REQUIREMENT'
                  AND (t.id IS NULL OR t.deleted<>0 OR t.status<>'COMPLETED')
                """, releaseId);
        long selectedRequirements = selectedItems.stream().filter(item -> "REQUIREMENT".equals(item.get("type"))).count();
        long selectedTasks = selectedItems.stream().filter(item -> "TASK".equals(item.get("type"))).count();
        boolean ready = (!requirementRows.isEmpty() || !taskRows.isEmpty())
                && requirementRows.size() == selectedRequirements
                && taskRows.stream().filter(t -> selectedItems.stream().anyMatch(item -> "TASK".equals(item.get("type"))
                    && ((Number) item.get("id")).longValue() == ((Number) t.get("id")).longValue())).count() == selectedTasks;
        ready &= requirementRows.stream().allMatch(r -> "ACCEPTED".equals(r.get("status"))
                && "PASSED".equals(r.get("acceptanceResult")));
        ready &= taskRows.stream().allMatch(t -> "COMPLETED".equals(t.get("status"))
                && !StringUtils.hasText((String) t.get("blockerReason")));
        ready &= openBugs == 0;
        ready &= invalidLinkedTasks == 0;
        Map<String, Object> progress = new LinkedHashMap<>();
        progress.put("requirements", requirementRows);
        progress.put("tasks", taskRows);
        progress.put("selectedRequirementIds", selectedItems.stream().filter(item -> "REQUIREMENT".equals(item.get("type")))
                .map(item -> ((Number) item.get("id")).longValue()).toList());
        progress.put("selectedTaskIds", selectedItems.stream().filter(item -> "TASK".equals(item.get("type")))
                .map(item -> ((Number) item.get("id")).longValue()).toList());
        progress.put("openBugCount", openBugs);
        progress.put("invalidLinkedTaskCount", invalidLinkedTasks);
        progress.put("ready", ready);
        return progress;
    }

    public List<Map<String, Object>> search(String text) {
        if (!StringUtils.hasText(text)) return List.of();
        String term = text.trim();
        if (term.length() > 80) bad("搜索词不能超过80字");
        String like = "%" + term.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        long number = -1;
        String digits = term.replaceFirst("(?i)^(?:REQ-\\d{4}-|BUG[-#]?|TASK[-#]?|#)", "");
        if (digits.matches("\\d{1,18}")) number = Long.parseLong(digits);
        List<Map<String, Object>> results = new ArrayList<>();
        boolean all = access.canViewAll();
        long userId = CurrentUserContext.userId();
        String requirementScope = all ? "" : " AND (project_id IN (" + access.readableProjectIdsSql() + ") OR created_by=" + userId + " OR reviewer_id=" + userId + ")";
        String taskScope = all ? "" : " AND (project_id IN (" + access.readableProjectIdsSql() + ") OR created_by=" + userId + " OR assignee_id=" + userId + ")";
        String bugScope = all ? "" : " AND (project_id IN (" + access.readableProjectIdsSql() + ") OR creator_id=" + userId + " OR assignee_id=" + userId + ")";
        results.addAll(jdbc.queryForList("SELECT 'REQUIREMENT' AS type,id,requirement_no AS number,title,project_id AS projectId,status FROM pf_requirement WHERE deleted=0 AND (title LIKE ? ESCAPE '!' OR requirement_no=?)" + requirementScope + " ORDER BY created_at DESC LIMIT 20", like, number));
        results.addAll(jdbc.queryForList("SELECT 'TASK' AS type,id,id AS number,name AS title,project_id AS projectId,status FROM pf_task WHERE deleted=0 AND (name LIKE ? ESCAPE '!' OR id=?)" + taskScope + " ORDER BY created_at DESC LIMIT 20", like, number));
        results.addAll(jdbc.queryForList("SELECT 'BUG' AS type,id,bug_no AS number,title,project_id AS projectId,status FROM pf_bug WHERE deleted=0 AND (title LIKE ? ESCAPE '!' OR bug_no=?)" + bugScope + " ORDER BY created_at DESC LIMIT 20", like, number));
        return results;
    }

    public List<Map<String, Object>> savedViews(String scope) {
        if (StringUtils.hasText(scope) && !VIEW_SCOPES.contains(scope)) bad("无效的视图范围");
        String sql = "SELECT id,name,scope,filters_json AS filtersJson,created_at AS createdAt FROM pf_saved_work_view WHERE user_id=?";
        return StringUtils.hasText(scope)
                ? jdbc.queryForList(sql + " AND scope=? ORDER BY updated_at DESC", CurrentUserContext.userId(), scope)
                : jdbc.queryForList(sql + " ORDER BY updated_at DESC", CurrentUserContext.userId());
    }

    public List<Map<String, Object>> saveView(WorkHubRequests.SavedView request) {
        if (!VIEW_SCOPES.contains(request.scope())) bad("无效的视图范围");
        if (request.filters().size() > 10 || !FILTER_KEYS.containsAll(request.filters().keySet())
                || request.filters().values().stream().anyMatch(v -> v == null || v.length() > 100)) {
            bad("筛选条件不合法");
        }
        String encoded;
        try {
            encoded = json.writeValueAsString(request.filters());
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "筛选条件不合法");
        }
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("INSERT INTO pf_saved_work_view(id,user_id,name,scope,filters_json,created_at,updated_at) VALUES (?,?,?,?,?,?,?)",
                UUID.randomUUID().toString(), CurrentUserContext.userId(), request.name().trim(), request.scope(), encoded, now, now);
        return savedViews(request.scope());
    }

    public void deleteView(String id) {
        if (jdbc.update("DELETE FROM pf_saved_work_view WHERE id=? AND user_id=?", id, CurrentUserContext.userId()) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "筛选视图不存在");
        }
    }

    private Long readWork(String type, Long id) {
        return switch (workType(type)) {
            case "TASK" -> {
                TaskEntity task = task(id);
                access.requireTaskRead(task);
                yield task.getProjectId();
            }
            case "REQUIREMENT" -> {
                RequirementEntity requirement = requirement(id);
                access.requireRequirementRead(requirement);
                yield requirement.getProjectId();
            }
            default -> throw new IllegalStateException();
        };
    }

    private boolean canMention(Long projectId, String type, Long id, Long userId) {
        if (userId == null || count("SELECT COUNT(*) FROM sys_user WHERE id=? AND enabled=1 AND deleted=0", userId) == 0) return false;
        if (userId.equals(CurrentUserContext.userId())) return true;
        if (count("""
                SELECT COUNT(*) FROM pf_project p WHERE p.id=? AND p.deleted=0 AND
                  (p.manager_id=? OR p.created_by=? OR CONCAT(',',REPLACE(COALESCE(p.co_manager_ids,''),' ',''),',') LIKE ?)
                """, projectId, userId, userId, "%," + userId + ",%") > 0) return true;
        if (count("SELECT COUNT(*) FROM pf_project_participant WHERE project_id=? AND user_id=?", projectId, userId) > 0) return true;
        return "TASK".equals(type)
                ? count("SELECT COUNT(*) FROM pf_task WHERE id=? AND (assignee_id=? OR created_by=?)", id, userId, userId) > 0
                : count("SELECT COUNT(*) FROM pf_requirement WHERE id=? AND (reviewer_id=? OR created_by=?)", id, userId, userId) > 0;
    }

    private boolean canReadTask(Long id) {
        try {
            access.requireTaskRead(task(id));
            return true;
        } catch (BusinessException denied) {
            return false;
        }
    }

    private boolean canReadRequirement(Long id) {
        try {
            access.requireRequirementRead(requirement(id));
            return true;
        } catch (BusinessException denied) {
            return false;
        }
    }

    private boolean sameDeliveryScope(Long firstProjectId, Long secondProjectId) {
        if (firstProjectId == null || secondProjectId == null) return false;
        if (firstProjectId.equals(secondProjectId)) return true;
        ProjectEntity first = projects.selectById(firstProjectId);
        ProjectEntity second = projects.selectById(secondProjectId);
        return first != null && second != null
                && (secondProjectId.equals(first.getManagementProjectId())
                    || firstProjectId.equals(second.getManagementProjectId()));
    }

    private Map<String, Object> release(String id) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id,project_id AS projectId,name,target_date AS targetDate,status,
                       acceptance_note AS acceptanceNote,released_at AS releasedAt FROM pf_delivery_release WHERE id=?
                """, id);
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "交付版本不存在");
        Map<String, Object> result = rows.get(0);
        access.requireProjectRead(((Number) result.get("projectId")).longValue());
        return result;
    }

    private ProjectEntity project(Long id) {
        ProjectEntity entity = projects.selectById(id);
        if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        return entity;
    }

    private RequirementEntity requirement(Long id) {
        RequirementEntity entity = requirements.selectById(id);
        if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "需求不存在");
        return entity;
    }

    private TaskEntity task(Long id) {
        TaskEntity entity = tasks.selectById(id);
        if (entity == null) throw new BusinessException(ErrorCode.NOT_FOUND, "任务不存在");
        return entity;
    }

    private List<Long> distinctIds(List<Long> values, int max) {
        if (values == null) return List.of();
        if (values.size() > max || values.stream().anyMatch(v -> v == null || v <= 0)) bad("关联项数量或 ID 不合法");
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private String workType(String type) {
        if ("TASK".equalsIgnoreCase(type) || "TASKS".equalsIgnoreCase(type)) return "TASK";
        if ("REQUIREMENT".equalsIgnoreCase(type) || "REQUIREMENTS".equalsIgnoreCase(type)) return "REQUIREMENT";
        bad("仅支持任务和需求评论");
        return "";
    }

    private long count(String sql, Object... args) {
        Long result = jdbc.queryForObject(sql, Long.class, args);
        return result == null ? 0 : result;
    }

    private void bad(String message) {
        throw new BusinessException(ErrorCode.BAD_REQUEST, message);
    }
}
