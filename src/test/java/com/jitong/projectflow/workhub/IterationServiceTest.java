package com.jitong.projectflow.workhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.file.dto.FileResponse;
import com.jitong.projectflow.file.service.FileService;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IterationServiceTest {
    private JdbcTemplate jdbc;
    private IterationService service;
    private WorkHubService workHub;
    private FileService files;
    private String releaseId;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:iteration_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE pf_delivery_release(id VARCHAR(36) PRIMARY KEY,project_id BIGINT,name VARCHAR(160),target_date DATE,status VARCHAR(16),acceptance_note CLOB)");
        jdbc.execute("CREATE TABLE pf_release_stage(release_id VARCHAR(36),stage_code VARCHAR(32),owner_id BIGINT,planned_start DATE,planned_end DATE,actual_start DATE,actual_end DATE,status VARCHAR(16),conclusion CLOB,completed_by BIGINT,completed_at TIMESTAMP,updated_by BIGINT,updated_at TIMESTAMP,PRIMARY KEY(release_id,stage_code))");
        jdbc.execute("CREATE TABLE pf_release_record(id VARCHAR(36),release_id VARCHAR(36),stage_code VARCHAR(32),record_type VARCHAR(24),title VARCHAR(160),content CLOB,evidence_url VARCHAR(1000),file_id BIGINT,before_json CLOB,after_json CLOB,actor_id BIGINT,created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_release_application(release_id VARCHAR(36) PRIMARY KEY,environment VARCHAR(120),window_start TIMESTAMP,window_end TIMESTAMP,change_summary CLOB,impact_scope CLOB,rollback_plan CLOB,monitoring_plan CLOB,support_plan CLOB,applicant_id BIGINT,approver_id BIGINT,status VARCHAR(16),submitted_at TIMESTAMP,decision_at TIMESTAMP,decision_note CLOB,updated_by BIGINT,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_release_execution(task_id BIGINT,release_id VARCHAR(36))");
        jdbc.execute("CREATE TABLE sys_user(id BIGINT,real_name VARCHAR(80),enabled INT,deleted INT)");
        jdbc.execute("CREATE TABLE pf_project(id BIGINT,manager_id BIGINT,created_by BIGINT,co_manager_ids VARCHAR(200),deleted INT)");
        jdbc.execute("CREATE TABLE pf_project_participant(project_id BIGINT,user_id BIGINT)");
        jdbc.update("INSERT INTO pf_project(id,manager_id,created_by,deleted) VALUES (100,1,1,0)");
        jdbc.update("INSERT INTO sys_user(id,real_name,enabled,deleted) VALUES (1,'负责人',1,0),(2,'审批人',1,0),(3,'外部人员',1,0)");
        jdbc.update("INSERT INTO pf_project_participant(project_id,user_id) VALUES (100,2)");
        releaseId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO pf_delivery_release(id,project_id,name,target_date,status) VALUES (?,100,'十月迭代',DATE '2026-10-30','READY')", releaseId);
        CurrentUserContext.set(1L);
        BusinessAccessService access = mock(BusinessAccessService.class);
        ProjectMapper projects = mock(ProjectMapper.class);
        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        when(projects.selectById(100L)).thenReturn(project);
        workHub = mock(WorkHubService.class);
        files = mock(FileService.class);
        when(workHub.releases(100L)).thenReturn(List.of(Map.of("id", releaseId, "ready", true,
                "selectedRequirementIds", List.of(10L), "selectedTaskIds", List.of(20L))));
        service = new IterationService(jdbc, new ObjectMapper().findAndRegisterModules(), access, projects,
                workHub, mock(NoticeService.class), files, mock(ReleaseExecutionService.class));
    }

    @AfterEach
    void tearDown() { CurrentUserContext.clear(); }

    @Test
    void stageCompletionRequiresEvidenceAndPreviousStage() {
        var completed = new IterationRequests.StageUpdate(1L, LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2),
                "COMPLETED", "需求范围和负责人已确认");
        assertThrows(BusinessException.class, () -> service.updateStage(releaseId, "REQUIREMENT_REVIEW", completed));
        assertThrows(BusinessException.class, () -> service.updateStage(releaseId, "REQUIREMENT_SUBMISSION",
                new IterationRequests.StageUpdate(1L, null, null, null, null, "COMPLETED", "")));
        assertThrows(BusinessException.class, () -> service.updateStage(releaseId, "REQUIREMENT_SUBMISSION",
                new IterationRequests.StageUpdate(3L, null, null, null, null, "IN_PROGRESS", "外部人员")));
        service.updateStage(releaseId, "REQUIREMENT_SUBMISSION", completed);
        service.updateStage(releaseId, "REQUIREMENT_REVIEW", completed);
        assertThrows(BusinessException.class, () -> service.updateStage(releaseId, "REQUIREMENT_SUBMISSION",
                new IterationRequests.StageUpdate(1L, null, null, null, null, "IN_PROGRESS", "需要返工")));
        assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM pf_release_record WHERE release_id=?", Long.class, releaseId));
        List<?> stages = (List<?>) service.workflow(releaseId).get("stages");
        assertEquals("COMPLETED", ((Map<?, ?>) stages.get(1)).get("status"));
    }

    @Test
    void applicationRequiresCompletedStagesAndIndependentApproval() {
        var request = new IterationRequests.Application("生产", LocalDateTime.of(2026, 10, 30, 20, 0),
                LocalDateTime.of(2026, 10, 30, 21, 0), "登录功能", "登录用户", "回滚上一个版本",
                "监控错误率", "研发和客服值守", 2L);
        assertThrows(BusinessException.class, () -> service.saveApplication(releaseId,
                new IterationRequests.Application("生产", request.windowStart(), request.windowEnd(),
                        request.changeSummary(), request.impactScope(), request.rollbackPlan(),
                        request.monitoringPlan(), request.supportPlan(), 1L)));
        service.saveApplication(releaseId, request);
        assertThrows(BusinessException.class, () -> service.submitApplication(releaseId));
        for (String stage : List.of("REQUIREMENT_SUBMISSION", "REQUIREMENT_REVIEW", "UI_DESIGN", "DEVELOPMENT",
                "TEST_HANDOFF", "TEST_VERIFICATION", "PRODUCT_ACCEPTANCE", "PRE_RELEASE")) {
            jdbc.update("INSERT INTO pf_release_stage(release_id,stage_code,status) VALUES (?,?,'COMPLETED')", releaseId, stage);
        }
        jdbc.update("UPDATE pf_delivery_release SET status='PLANNED' WHERE id=?", releaseId);
        assertThrows(BusinessException.class, () -> service.submitApplication(releaseId));
        jdbc.update("UPDATE pf_delivery_release SET status='READY' WHERE id=?", releaseId);
        when(workHub.releases(100L)).thenReturn(List.of(Map.of("id", releaseId, "ready", false)));
        assertThrows(BusinessException.class, () -> service.submitApplication(releaseId));
        when(workHub.releases(100L)).thenReturn(List.of(Map.of("id", releaseId, "ready", true)));
        service.submitApplication(releaseId);
        assertEquals("SUBMITTED", jdbc.queryForObject("SELECT status FROM pf_release_application WHERE release_id=?", String.class, releaseId));
        assertThrows(BusinessException.class, () -> service.decideApplication(releaseId,
                new IterationRequests.Decision("APPROVED", "同意")));
        assertThrows(BusinessException.class, () -> service.updateStage(releaseId, "PRE_RELEASE",
                new IterationRequests.StageUpdate(1L, null, null, null, LocalDate.now(), "IN_PROGRESS", "修改")));
        CurrentUserContext.set(2L);
        service.decideApplication(releaseId, new IterationRequests.Decision("APPROVED", "监控及回滚方案已核对"));
        assertEquals("APPROVED", jdbc.queryForObject("SELECT status FROM pf_release_application WHERE release_id=?", String.class, releaseId));
        CurrentUserContext.set(1L);
        service.withdrawApplication(releaseId, new IterationRequests.Withdrawal("发布范围需要调整"));
        assertEquals("WITHDRAWN", jdbc.queryForObject("SELECT status FROM pf_release_application WHERE release_id=?", String.class, releaseId));
        service.saveApplication(releaseId, request);
        assertEquals("DRAFT", jdbc.queryForObject("SELECT status FROM pf_release_application WHERE release_id=?", String.class, releaseId));
        assertTrue(jdbc.queryForObject("SELECT COUNT(*) FROM pf_release_record WHERE release_id=?", Long.class, releaseId) >= 3);
    }

    @Test
    void appendOnlyRecordsValidateMemberEvidenceAndPreserveHistory() {
        assertThrows(BusinessException.class, () -> service.addRecord(releaseId,
                new IterationRequests.Record("UI_DESIGN", "DOCUMENT", "设计稿", "初稿", "javascript:alert(1)")));
        Map<String, Object> result = service.addRecord(releaseId,
                new IterationRequests.Record("UI_DESIGN", "DOCUMENT", "设计稿", "初稿待评审", "https://example.com/design"));
        assertFalse(((List<?>) result.get("records")).isEmpty());
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM pf_release_record WHERE release_id=?", Long.class, releaseId));
    }

    @Test
    void uploadedEvidenceBelongsToSelectedStageAndIsLockedDuringApproval() {
        MockMultipartFile file = new MockMultipartFile("file", "评审纪要.pdf", "application/pdf", "minutes".getBytes());
        when(files.uploadIterationEvidence(100L, file)).thenReturn(
                FileResponse.builder().id(55L).originalName("评审纪要.pdf").build());

        Map<String, Object> workflow = service.uploadEvidence(releaseId, "REQUIREMENT_REVIEW", file);
        assertEquals(55L, jdbc.queryForObject("SELECT file_id FROM pf_release_record WHERE release_id=?", Long.class, releaseId));
        assertEquals("REQUIREMENT_REVIEW", jdbc.queryForObject(
                "SELECT stage_code FROM pf_release_record WHERE release_id=?", String.class, releaseId));
        assertEquals(55L, ((Map<?, ?>) ((List<?>) workflow.get("records")).get(0)).get("fileId"));
        verify(files).uploadIterationEvidence(100L, file);

        jdbc.update("INSERT INTO pf_release_application(release_id,status) VALUES (?,'SUBMITTED')", releaseId);
        assertThrows(BusinessException.class, () -> service.uploadEvidence(releaseId, "PRE_RELEASE", file));
        assertThrows(BusinessException.class, () -> service.addRecord(releaseId,
                new IterationRequests.Record("PRE_RELEASE", "NOTE", "补充", "审批后修改", null)));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM pf_release_record WHERE release_id=?", Long.class, releaseId));
    }

    @Test
    void cancelledReleaseRejectsNewStageChangesAndEvidence() {
        jdbc.update("UPDATE pf_delivery_release SET status='CANCELLED' WHERE id=?", releaseId);
        MockMultipartFile file = new MockMultipartFile("file", "result.pdf", "application/pdf", "x".getBytes());
        assertThrows(BusinessException.class, () -> service.uploadEvidence(releaseId, "TEST_VERIFICATION", file));
        assertThrows(BusinessException.class, () -> service.updateStage(releaseId, "TEST_VERIFICATION",
                new IterationRequests.StageUpdate(1L, null, null, null, null, "IN_PROGRESS", "")));
    }
}
