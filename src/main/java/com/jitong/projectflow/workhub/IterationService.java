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
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IterationService {
    private static final List<String> STAGES = List.of("REQUIREMENT_SUBMISSION", "REQUIREMENT_REVIEW",
            "UI_DESIGN", "DEVELOPMENT", "TEST_HANDOFF", "TEST_VERIFICATION", "PRODUCT_ACCEPTANCE",
            "PRE_RELEASE", "PRODUCTION_RELEASE", "POST_RELEASE");
    private static final List<String> LABELS = List.of("需求提出", "需求评审", "UI设计", "技术开发", "提测",
            "测试验证", "产品验收", "上线前评估", "上线发布", "上线后跟进");
    private static final Set<String> STAGE_STATUSES = Set.of("NOT_STARTED", "IN_PROGRESS", "COMPLETED");
    private static final Set<String> RECORD_TYPES = Set.of("NOTE", "DECISION", "RISK", "DOCUMENT",
            "TEST_REPORT", "DEFECT", "FOLLOW_UP");

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final BusinessAccessService access;
    private final ProjectMapper projects;
    private final WorkHubService workHub;
    private final NoticeService notices;

    public Map<String, Object> workflow(String releaseId) {
        Map<String, Object> release = release(releaseId);
        Long projectId = ((Number) release.get("projectId")).longValue();
        workHub.releases(projectId).stream().filter(item -> releaseId.equals(item.get("id")))
                .findFirst().ifPresent(release::putAll);
        List<Map<String, Object>> saved = jdbc.queryForList("""
                SELECT stage_code AS stageCode, owner_id AS ownerId, planned_start AS plannedStart,
                       planned_end AS plannedEnd, actual_start AS actualStart, actual_end AS actualEnd,
                       status, conclusion, completed_by AS completedBy, completed_at AS completedAt,
                       updated_by AS updatedBy, updated_at AS updatedAt
                FROM pf_release_stage WHERE release_id=?
                """, releaseId);
        List<Map<String, Object>> stages = new ArrayList<>();
        for (int i = 0; i < STAGES.size(); i++) {
            String code = STAGES.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("stageCode", code);
            row.put("label", LABELS.get(i));
            row.put("step", i + 1);
            row.put("status", "NOT_STARTED");
            saved.stream().filter(item -> code.equals(item.get("stageCode"))).findFirst().ifPresent(item -> {
                for (String key : List.of("ownerId", "plannedStart", "plannedEnd", "actualStart", "actualEnd",
                        "status", "conclusion", "completedBy", "completedAt", "updatedBy", "updatedAt")) {
                    row.put(key, item.get(key));
                }
            });
            stages.add(row);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("release", release);
        result.put("stages", stages);
        result.put("application", application(releaseId));
        result.put("records", jdbc.queryForList("""
                SELECT r.id, r.stage_code AS stageCode, r.record_type AS recordType,
                       r.title, r.content, r.evidence_url AS evidenceUrl,
                       r.before_json AS beforeJson, r.after_json AS afterJson,
                       r.actor_id AS actorId, u.real_name AS actorName, r.created_at AS createdAt
                FROM pf_release_record r LEFT JOIN sys_user u ON u.id=r.actor_id
                WHERE r.release_id=? ORDER BY r.created_at DESC, r.id DESC
                """, releaseId));
        result.put("members", members(projectId));
        result.put("canManage", canManage(projectId));
        return result;
    }

    @Transactional
    public Map<String, Object> updateStage(String releaseId, String code, IterationRequests.StageUpdate request) {
        Map<String, Object> release = release(releaseId);
        int index = STAGES.indexOf(code);
        if (index < 0 || index == 8) bad("无效或不可手工更新的阶段");
        if ("RELEASED".equals(release.get("status")) && index != 9) bad("已发布版本的历史阶段不可修改");
        if (!"RELEASED".equals(release.get("status")) && index == 9) bad("发布后才能登记上线跟进");
        if (!STAGE_STATUSES.contains(request.status())) bad("无效的阶段状态");
        String applicationStatus = (String) application(releaseId).get("status");
        if (index != 9 && ("SUBMITTED".equals(applicationStatus) || "APPROVED".equals(applicationStatus))) {
            bad("上线申请审批中或已通过，阶段内容不可修改");
        }
        Long projectId = ((Number) release.get("projectId")).longValue();
        Map<String, Object> before = stage(releaseId, code);
        boolean manager = canManage(projectId);
        Long oldOwner = number(before.get("ownerId"));
        if (!manager && !Objects.equals(oldOwner, CurrentUserContext.userId())) forbidden();
        if (!manager && (!Objects.equals(oldOwner, request.ownerId())
                || !Objects.equals(date(before.get("plannedStart")), request.plannedStart())
                || !Objects.equals(date(before.get("plannedEnd")), request.plannedEnd()))) forbidden();
        if ("COMPLETED".equals(before.get("status")) && !manager) forbidden();
        if ("COMPLETED".equals(before.get("status")) && "COMPLETED".equals(request.status())) {
            bad("已完成阶段需先由项目负责人重新打开再修改");
        }
        if ("COMPLETED".equals(before.get("status")) && !"COMPLETED".equals(request.status())) {
            for (int later = index + 1; later < STAGES.size(); later++) {
                if ("COMPLETED".equals(stage(releaseId, STAGES.get(later)).get("status"))) {
                    bad("请先从后续阶段依次重新打开，保留每次变更记录");
                }
            }
        }
        if (request.ownerId() != null && !member(projectId, request.ownerId())) bad("阶段负责人必须是项目成员");
        checkDates(request.plannedStart(), request.plannedEnd(), "计划时间");
        checkDates(request.actualStart(), request.actualEnd(), "实际时间");
        if ("COMPLETED".equals(request.status())) {
            if (request.ownerId() == null || !StringUtils.hasText(request.conclusion()) || request.actualEnd() == null) {
                bad("完成阶段需填写负责人、实际完成日期和结论");
            }
            if (index > 0 && !"COMPLETED".equals(stage(releaseId, STAGES.get(index - 1)).get("status"))) {
                bad("请先完成上一阶段并留存结论");
            }
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDate actualStart = request.actualStart();
        if (actualStart == null && !"NOT_STARTED".equals(request.status())) {
            actualStart = date(before.get("actualStart"));
            if (actualStart == null) actualStart = request.actualEnd() == null ? LocalDate.now() : request.actualEnd();
        }
        checkDates(actualStart, request.actualEnd(), "实际时间");
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("ownerId", request.ownerId());
        after.put("plannedStart", request.plannedStart());
        after.put("plannedEnd", request.plannedEnd());
        after.put("actualStart", actualStart);
        after.put("actualEnd", request.actualEnd());
        after.put("status", request.status());
        after.put("conclusion", request.conclusion());
        after.put("completedBy", "COMPLETED".equals(request.status()) ? CurrentUserContext.userId() : null);
        after.put("completedAt", "COMPLETED".equals(request.status()) ? now : null);
        jdbc.update("""
                INSERT INTO pf_release_stage(release_id,stage_code,owner_id,planned_start,planned_end,
                actual_start,actual_end,status,conclusion,completed_by,completed_at,updated_by,updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE
                owner_id=VALUES(owner_id),planned_start=VALUES(planned_start),planned_end=VALUES(planned_end),
                actual_start=VALUES(actual_start),actual_end=VALUES(actual_end),status=VALUES(status),
                conclusion=VALUES(conclusion),completed_by=VALUES(completed_by),completed_at=VALUES(completed_at),
                updated_by=VALUES(updated_by),updated_at=VALUES(updated_at)
                """, releaseId, code, request.ownerId(), request.plannedStart(), request.plannedEnd(),
                actualStart, request.actualEnd(), request.status(), request.conclusion(), after.get("completedBy"),
                after.get("completedAt"), CurrentUserContext.userId(), now);
        record(releaseId, code, "STAGE_CHANGE", "阶段状态变更", "阶段「" + LABELS.get(index) + "」更新为 " + request.status(),
                null, before, after);
        return workflow(releaseId);
    }

    @Transactional
    public Map<String, Object> addRecord(String releaseId, IterationRequests.Record request) {
        release(releaseId);
        if (!STAGES.contains(request.stageCode()) || !RECORD_TYPES.contains(request.recordType())) bad("记录类型或阶段不合法");
        if (StringUtils.hasText(request.evidenceUrl()) && !request.evidenceUrl().matches("(?i)^https?://[^\\s]+$")) {
            bad("凭证链接必须是 http 或 https 地址");
        }
        record(releaseId, request.stageCode(), request.recordType(), request.title().trim(),
                request.content().trim(), request.evidenceUrl(), null, null);
        return workflow(releaseId);
    }

    @Transactional
    public Map<String, Object> saveApplication(String releaseId, IterationRequests.Application request) {
        Map<String, Object> release = release(releaseId);
        Long projectId = ((Number) release.get("projectId")).longValue();
        manage(projectId);
        if ("RELEASED".equals(release.get("status"))) bad("已发布版本不能修改上线申请");
        Map<String, Object> before = application(releaseId);
        if ("SUBMITTED".equals(before.get("status")) || "APPROVED".equals(before.get("status"))) bad("申请审批中或已通过，不能修改");
        if (!request.windowEnd().isAfter(request.windowStart())) bad("上线结束时间必须晚于开始时间");
        if (request.approverId().equals(CurrentUserContext.userId())) bad("申请人与审批人不能相同");
        if (!member(projectId, request.approverId())) bad("审批人必须是项目成员");
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO pf_release_application(release_id,environment,window_start,window_end,change_summary,
                impact_scope,rollback_plan,monitoring_plan,support_plan,applicant_id,approver_id,status,
                submitted_at,decision_at,decision_note,updated_by,updated_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,'DRAFT',NULL,NULL,NULL,?,?) ON DUPLICATE KEY UPDATE
                environment=VALUES(environment),window_start=VALUES(window_start),window_end=VALUES(window_end),
                change_summary=VALUES(change_summary),impact_scope=VALUES(impact_scope),
                rollback_plan=VALUES(rollback_plan),monitoring_plan=VALUES(monitoring_plan),
                support_plan=VALUES(support_plan),applicant_id=VALUES(applicant_id),approver_id=VALUES(approver_id),
                status='DRAFT',submitted_at=NULL,decision_at=NULL,decision_note=NULL,
                updated_by=VALUES(updated_by),updated_at=VALUES(updated_at)
                """, releaseId, request.environment().trim(), request.windowStart(), request.windowEnd(),
                request.changeSummary().trim(), request.impactScope().trim(), request.rollbackPlan().trim(),
                request.monitoringPlan().trim(), request.supportPlan().trim(), CurrentUserContext.userId(),
                request.approverId(), CurrentUserContext.userId(), now);
        record(releaseId, "PRE_RELEASE", "APPLICATION_CHANGE", "上线申请草稿更新", "申请内容已更新",
                null, before, application(releaseId));
        return workflow(releaseId);
    }

    @Transactional
    public Map<String, Object> submitApplication(String releaseId) {
        Map<String, Object> release = release(releaseId);
        Long projectId = ((Number) release.get("projectId")).longValue();
        manage(projectId);
        Map<String, Object> before = application(releaseId);
        if (!"DRAFT".equals(before.get("status")) && !"REJECTED".equals(before.get("status"))) bad("请先填写上线申请草稿");
        if (!"READY".equals(release.get("status"))) bad("请先将版本标记为已就绪");
        for (int i = 0; i < 8; i++) {
            if (!"COMPLETED".equals(stage(releaseId, STAGES.get(i)).get("status"))) {
                bad("请先完成上线前的八个阶段并留存结论");
            }
        }
        boolean ready = workHub.releases(projectId).stream().anyMatch(item -> releaseId.equals(item.get("id"))
                && Boolean.TRUE.equals(item.get("ready")));
        if (!ready) bad("需求、任务或 Bug 尚未满足交付条件");
        jdbc.update("""
                UPDATE pf_release_application SET status='SUBMITTED',submitted_at=?,decision_at=NULL,
                decision_note=NULL,updated_by=?,updated_at=? WHERE release_id=?
                """, LocalDateTime.now(), CurrentUserContext.userId(), LocalDateTime.now(), releaseId);
        record(releaseId, "PRE_RELEASE", "APPLICATION_CHANGE", "提交上线申请", "申请提交审批",
                null, before, application(releaseId));
        Long approverId = number(before.get("approverId"));
        notices.create(approverId, NoticeType.SYSTEM, "上线申请待审批：「" + release.get("name") + "」",
                "请在版本迭代档案中查看上线申请与评估记录", "Project", projectId);
        return workflow(releaseId);
    }

    @Transactional
    public Map<String, Object> decideApplication(String releaseId, IterationRequests.Decision request) {
        Map<String, Object> release = release(releaseId);
        Map<String, Object> before = application(releaseId);
        if (!"SUBMITTED".equals(before.get("status"))) bad("上线申请尚未提交或已处理");
        if (!Objects.equals(number(before.get("approverId")), CurrentUserContext.userId())) forbidden();
        if (!Set.of("APPROVED", "REJECTED").contains(request.decision())) bad("无效的审批结论");
        access.requireProjectRead(((Number) release.get("projectId")).longValue());
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                UPDATE pf_release_application SET status=?,decision_at=?,decision_note=?,updated_by=?,updated_at=?
                WHERE release_id=?
                """, request.decision(), now, request.note().trim(), CurrentUserContext.userId(), now, releaseId);
        record(releaseId, "PRE_RELEASE", "APPLICATION_CHANGE", "上线申请审批", request.note().trim(),
                null, before, application(releaseId));
        notices.create(number(before.get("applicantId")), NoticeType.SYSTEM,
                "上线申请审批结果：「" + release.get("name") + "」",
                request.decision() + "：" + request.note().trim(), "Project",
                ((Number) release.get("projectId")).longValue());
        return workflow(releaseId);
    }

    @Transactional
    public Map<String, Object> withdrawApplication(String releaseId, IterationRequests.Withdrawal request) {
        Map<String, Object> release = release(releaseId);
        Long projectId = ((Number) release.get("projectId")).longValue();
        manage(projectId);
        if ("RELEASED".equals(release.get("status"))) bad("已发布版本不能撤回上线申请");
        Map<String, Object> before = application(releaseId);
        if (!"SUBMITTED".equals(before.get("status")) && !"APPROVED".equals(before.get("status"))) {
            bad("只有审批中或已批准的申请可以撤回");
        }
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                UPDATE pf_release_application SET status='WITHDRAWN',decision_at=?,decision_note=?,
                updated_by=?,updated_at=? WHERE release_id=?
                """, now, request.reason().trim(), CurrentUserContext.userId(), now, releaseId);
        record(releaseId, "PRE_RELEASE", "APPLICATION_CHANGE", "撤回上线申请", request.reason().trim(),
                null, before, application(releaseId));
        Long approverId = number(before.get("approverId"));
        if (approverId != null && !approverId.equals(CurrentUserContext.userId())) {
            notices.create(approverId, NoticeType.SYSTEM, "上线申请已撤回：「" + release.get("name") + "」",
                    request.reason().trim(), "Project", projectId);
        }
        return workflow(releaseId);
    }

    private Map<String, Object> release(String releaseId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, project_id AS projectId, name, target_date AS targetDate,
                       status, acceptance_note AS acceptanceNote FROM pf_delivery_release WHERE id=?
                """, releaseId);
        if (rows.isEmpty()) throw new BusinessException(ErrorCode.NOT_FOUND, "交付版本不存在");
        Map<String, Object> row = rows.get(0);
        access.requireProjectRead(((Number) row.get("projectId")).longValue());
        return row;
    }

    private Map<String, Object> stage(String releaseId, String code) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT owner_id AS ownerId,planned_start AS plannedStart,planned_end AS plannedEnd,
                actual_start AS actualStart,actual_end AS actualEnd,status,conclusion,
                completed_by AS completedBy,completed_at AS completedAt
                FROM pf_release_stage WHERE release_id=? AND stage_code=?
                """, releaseId, code);
        return rows.isEmpty() ? Map.of("status", "NOT_STARTED") : rows.get(0);
    }

    private Map<String, Object> application(String releaseId) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT environment,window_start AS windowStart,window_end AS windowEnd,
                change_summary AS changeSummary,impact_scope AS impactScope,rollback_plan AS rollbackPlan,
                monitoring_plan AS monitoringPlan,support_plan AS supportPlan,applicant_id AS applicantId,
                approver_id AS approverId,status,submitted_at AS submittedAt,decision_at AS decisionAt,
                decision_note AS decisionNote,updated_by AS updatedBy,updated_at AS updatedAt
                FROM pf_release_application WHERE release_id=?
                """, releaseId);
        return rows.isEmpty() ? Map.of() : rows.get(0);
    }

    private List<Map<String, Object>> members(Long projectId) {
        return jdbc.queryForList("""
                SELECT u.id,u.real_name AS name FROM sys_user u JOIN pf_project p ON p.id=?
                WHERE u.enabled=1 AND u.deleted=0 AND (u.id=p.manager_id OR u.id=p.created_by
                OR CONCAT(',',REPLACE(COALESCE(p.co_manager_ids,''),' ',''),',') LIKE CONCAT('%,',u.id,',%')
                OR EXISTS (SELECT 1 FROM pf_project_participant pp WHERE pp.project_id=p.id AND pp.user_id=u.id))
                ORDER BY u.real_name
                """, projectId);
    }

    private boolean member(Long projectId, Long userId) {
        return members(projectId).stream().anyMatch(row -> Objects.equals(number(row.get("id")), userId));
    }

    private boolean canManage(Long projectId) {
        try {
            manage(projectId);
            return true;
        } catch (BusinessException denied) {
            return false;
        }
    }

    private void manage(Long projectId) {
        ProjectEntity project = projects.selectById(projectId);
        if (project == null) throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        access.requireProjectManage(project);
    }

    private void record(String releaseId, String code, String type, String title, String content,
                        String url, Object before, Object after) {
        jdbc.update("""
                INSERT INTO pf_release_record(id,release_id,stage_code,record_type,title,content,evidence_url,
                before_json,after_json,actor_id,created_at) VALUES (?,?,?,?,?,?,?,?,?,?,?)
                """, UUID.randomUUID().toString(), releaseId, code, type, title, content, url,
                encode(before), encode(after), CurrentUserContext.userId(), LocalDateTime.now());
    }

    private String encode(Object value) {
        if (value == null) return null;
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new BusinessException(ErrorCode.BAD_REQUEST, "记录序列化失败"); }
    }

    private Long number(Object value) { return value instanceof Number n ? n.longValue() : null; }

    private LocalDate date(Object value) {
        if (value instanceof LocalDate localDate) return localDate;
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate();
        return null;
    }

    private void checkDates(LocalDate start, LocalDate end, String label) {
        if (start != null && end != null && end.isBefore(start)) bad(label + "的结束日期不能早于开始日期");
    }

    private void bad(String message) { throw new BusinessException(ErrorCode.BAD_REQUEST, message); }
    private void forbidden() { throw new BusinessException(ErrorCode.FORBIDDEN, "无权修改此阶段"); }
}
