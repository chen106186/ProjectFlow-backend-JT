package com.jitong.projectflow.workhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDateTime;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReleaseExecutionServiceTest {
    private JdbcTemplate jdbc;
    private ReleaseExecutionService service;
    private String releaseId;

    @AfterEach
    void clearUser() { CurrentUserContext.clear(); }

    @BeforeEach
    void setUp() {
        DriverManagerDataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:release_execution_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        source.setDriverClassName("org.h2.Driver");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE pf_delivery_release(id VARCHAR(36),name VARCHAR(160),status VARCHAR(16))");
        jdbc.execute("CREATE TABLE pf_release_execution(task_id BIGINT,release_id VARCHAR(36),environment VARCHAR(120),window_start TIMESTAMP,window_end TIMESTAMP,lead_id BIGINT,operator_ids CLOB,verifier_id BIGINT,support_ids CLOB,meeting_planned_at TIMESTAMP,plan_revision INT,updated_by BIGINT,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_release_application(release_id VARCHAR(36),environment VARCHAR(120),window_start TIMESTAMP,window_end TIMESTAMP,status VARCHAR(16))");
        jdbc.execute("CREATE TABLE pf_release_meeting(id VARCHAR(36),task_id BIGINT,held_at TIMESTAMP,chair_id BIGINT,conclusion VARCHAR(16),attendee_ids CLOB,agenda CLOB,decisions CLOB,risks CLOB,action_items CLOB,plan_revision INT,recorded_by BIGINT,recorded_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_release_meeting_ack(meeting_id VARCHAR(36),user_id BIGINT,confirmed_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_release_report(task_id BIGINT,status VARCHAR(16),actual_start TIMESTAMP,actual_end TIMESTAMP,package_ref VARCHAR(500),execution_steps CLOB,smoke_result CLOB,monitoring_result CLOB,incident_notes CLOB,rolled_back BOOLEAN,outcome VARCHAR(16),residual_issues CLOB,verifier_id BIGINT,submitted_by BIGINT,submitted_at TIMESTAMP,verified_by BIGINT,verified_at TIMESTAMP,verification_note CLOB)");
        jdbc.execute("CREATE TABLE pf_release_record(id VARCHAR(36),release_id VARCHAR(36),stage_code VARCHAR(32),record_type VARCHAR(24),title VARCHAR(160),content CLOB,before_json CLOB,after_json CLOB,actor_id BIGINT,created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_release_stage(release_id VARCHAR(36),stage_code VARCHAR(32),status VARCHAR(16),conclusion CLOB,actual_end DATE,completed_by BIGINT,completed_at TIMESTAMP,updated_by BIGINT,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_project(id BIGINT,manager_id BIGINT,created_by BIGINT,co_manager_ids VARCHAR(200))");
        jdbc.execute("CREATE TABLE pf_project_participant(project_id BIGINT,user_id BIGINT)");
        jdbc.execute("CREATE TABLE sys_user(id BIGINT,enabled INT,deleted INT)");
        jdbc.update("INSERT INTO pf_project(id,manager_id,created_by) VALUES (100,1,1)");
        jdbc.update("INSERT INTO pf_project_participant(project_id,user_id) VALUES (100,2)");
        jdbc.update("INSERT INTO sys_user(id,enabled,deleted) VALUES (1,1,0),(2,1,0)");
        releaseId = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO pf_delivery_release(id,name,status) VALUES (?,'十月版本','READY')", releaseId);
        service = new ReleaseExecutionService(jdbc, new ObjectMapper(), null, null, null, null, null);
    }

    @Test
    void applicationRequiresExecutionTaskAndApprovedMeetingWithMatchingWindow() {
        assertThrows(BusinessException.class, () -> service.requireMeetingBeforeApplication(releaseId));
        LocalDateTime start = LocalDateTime.of(2026, 10, 21, 20, 0);
        LocalDateTime end = start.plusHours(1);
        jdbc.update("INSERT INTO pf_release_execution(task_id,release_id,environment,window_start,window_end,plan_revision) VALUES (1,?,'生产',?,?,1)",
                releaseId, start, end);
        jdbc.update("INSERT INTO pf_release_application(release_id,environment,window_start,window_end,status) VALUES (?,'生产',?,?,'DRAFT')",
                releaseId, start, end);
        assertThrows(BusinessException.class, () -> service.requireMeetingBeforeApplication(releaseId));
        jdbc.update("INSERT INTO pf_release_meeting(id,task_id,conclusion,attendee_ids,plan_revision,recorded_at) VALUES ('first',1,'DEFERRED','[1,2]',1,CURRENT_TIMESTAMP)");
        assertThrows(BusinessException.class, () -> service.requireMeetingBeforeApplication(releaseId));
        jdbc.update("INSERT INTO pf_release_meeting(id,task_id,conclusion,attendee_ids,plan_revision,recorded_at) VALUES ('second',1,'APPROVED','[1,2]',1,DATEADD('SECOND',1,CURRENT_TIMESTAMP))");
        assertThrows(BusinessException.class, () -> service.requireMeetingBeforeApplication(releaseId));
        jdbc.update("INSERT INTO pf_release_meeting_ack(meeting_id,user_id,confirmed_at) VALUES ('second',1,CURRENT_TIMESTAMP)");
        assertThrows(BusinessException.class, () -> service.requireMeetingBeforeApplication(releaseId));
        jdbc.update("INSERT INTO pf_release_meeting_ack(meeting_id,user_id,confirmed_at) VALUES ('second',2,CURRENT_TIMESTAMP)");
        assertDoesNotThrow(() -> service.requireMeetingBeforeApplication(releaseId));
        jdbc.update("UPDATE pf_release_execution SET plan_revision=2 WHERE task_id=1");
        assertThrows(BusinessException.class, () -> service.requireMeetingBeforeApplication(releaseId));
        jdbc.update("UPDATE pf_release_execution SET plan_revision=1 WHERE task_id=1");
        jdbc.update("UPDATE pf_release_application SET environment='预发' WHERE release_id=?", releaseId);
        assertThrows(BusinessException.class, () -> service.requireMeetingBeforeApplication(releaseId));
    }

    @Test
    void releaseTaskCannotCloseWithoutReport() {
        TaskEntity task = new TaskEntity();
        task.setId(1L);
        task.setWorkType("RELEASE_EXECUTION");
        assertThrows(BusinessException.class, () -> service.requireReportBeforeClose(task));
        jdbc.update("INSERT INTO pf_release_report(task_id,status) VALUES (1,'SUBMITTED')");
        assertThrows(BusinessException.class, () -> service.requireReportBeforeClose(task));
        jdbc.update("UPDATE pf_release_report SET status='VERIFIED' WHERE task_id=1");
        assertDoesNotThrow(() -> service.requireReportBeforeClose(task));
    }

    @Test
    void meetingAcknowledgementAndReportVerificationCompleteReleaseRecord() {
        TaskEntity task = new TaskEntity();
        task.setId(1L);
        task.setProjectId(100L);
        task.setWorkType("RELEASE_EXECUTION");
        TaskMapper taskMapper = (TaskMapper) Proxy.newProxyInstance(TaskMapper.class.getClassLoader(),
                new Class<?>[]{TaskMapper.class}, (proxy, method, args) ->
                        "selectById".equals(method.getName()) ? task : null);
        BusinessAccessService access = new BusinessAccessService(null) {
            @Override public void requireTaskRead(TaskEntity ignored) {}
            @Override public void requireTaskManage(TaskEntity ignored) {}
        };
        service = new ReleaseExecutionService(jdbc, new ObjectMapper(), access, taskMapper,
                null, null, (module, businessType, businessId, operationType, content) -> {});
        LocalDateTime start = LocalDateTime.of(2026, 10, 21, 20, 0);
        LocalDateTime end = start.plusHours(1);
        jdbc.update("INSERT INTO pf_release_execution(task_id,release_id,environment,window_start,window_end,lead_id,operator_ids,support_ids,plan_revision) VALUES (1,?,'生产',?,?,1,'[1]','[]',1)", releaseId, start, end);
        jdbc.update("INSERT INTO pf_release_application(release_id,environment,window_start,window_end,status) VALUES (?,'生产',?,?,'DRAFT')", releaseId, start, end);
        jdbc.update("INSERT INTO pf_release_stage(release_id,stage_code,status,conclusion) VALUES (?,'PRE_RELEASE','COMPLETED','检查通过')", releaseId);
        CurrentUserContext.set(1L);
        service.updatePlan(1L, new ReleaseExecutionRequests.Plan("生产", start, end,
                1L, List.of(1L), 2L, List.of(2L), start.minusHours(2)));
        assertEquals("IN_PROGRESS", jdbc.queryForObject("SELECT status FROM pf_release_stage WHERE release_id=?", String.class, releaseId));
        service.addMeeting(1L, new ReleaseExecutionRequests.Meeting(LocalDateTime.now().minusMinutes(30),
                1L, List.of(1L, 2L), "发布评估", "同意上线", "无", "无", "APPROVED"));
        String meetingId = jdbc.queryForObject("SELECT id FROM pf_release_meeting WHERE task_id=1", String.class);
        assertThrows(BusinessException.class, () -> service.requireMeetingBeforeApplication(releaseId));
        service.confirmMeeting(1L, meetingId);
        CurrentUserContext.set(2L);
        service.confirmMeeting(1L, meetingId);
        assertDoesNotThrow(() -> service.requireMeetingBeforeApplication(releaseId));

        jdbc.update("UPDATE pf_delivery_release SET status='RELEASED' WHERE id=?", releaseId);
        CurrentUserContext.set(1L);
        service.submitReport(1L, new ReleaseExecutionRequests.Report(LocalDateTime.now().minusMinutes(20),
                LocalDateTime.now().minusMinutes(10), "commit-123", "已部署", "冒烟通过", "监控正常",
                "", false, "SUCCESS", "", 2L));
        assertThrows(BusinessException.class, () -> service.requireReportBeforeClose(task));
        assertThrows(BusinessException.class, () -> service.requireVerifiedReportBeforeFollowup(releaseId));
        CurrentUserContext.set(2L);
        service.verifyReport(1L, new ReleaseExecutionRequests.Verification("REJECTED", "请补充监控结论"));
        assertThrows(BusinessException.class, () -> service.requireReportBeforeClose(task));
        CurrentUserContext.set(1L);
        service.submitReport(1L, new ReleaseExecutionRequests.Report(LocalDateTime.now().minusMinutes(20),
                LocalDateTime.now().minusMinutes(10), "commit-123", "已部署", "冒烟通过", "监控复核正常",
                "", false, "SUCCESS", "", 2L));
        CurrentUserContext.set(2L);
        service.verifyReport(1L, new ReleaseExecutionRequests.Verification("APPROVED", "生产验证通过"));
        assertDoesNotThrow(() -> service.requireReportBeforeClose(task));
        assertDoesNotThrow(() -> service.requireVerifiedReportBeforeFollowup(releaseId));
    }
}
