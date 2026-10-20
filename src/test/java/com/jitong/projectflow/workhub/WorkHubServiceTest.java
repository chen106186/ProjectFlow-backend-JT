package com.jitong.projectflow.workhub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.auth.security.CurrentUserContext;
import com.jitong.projectflow.common.error.BusinessException;
import com.jitong.projectflow.notice.service.NoticeService;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.audit.OperationLogService;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkHubServiceTest {
    private JdbcTemplate jdbc;
    private WorkHubService service;
    private RequirementMapper requirements;
    private TaskMapper tasks;
    private ProjectMapper projects;
    private NoticeService notices;
    private BusinessAccessService access;
    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:workhub_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        jdbc.execute("CREATE TABLE pf_requirement_task(requirement_id BIGINT,task_id BIGINT,PRIMARY KEY(requirement_id,task_id))");
        jdbc.execute("CREATE TABLE pf_requirement_acceptance(requirement_id BIGINT PRIMARY KEY,result VARCHAR(16),conclusion CLOB,accepted_by BIGINT,accepted_at TIMESTAMP,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_task_blocker(task_id BIGINT PRIMARY KEY,reason CLOB,reported_by BIGINT,reported_at TIMESTAMP,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_bug_task(bug_id BIGINT,task_id BIGINT)");
        jdbc.execute("CREATE TABLE pf_bug(id BIGINT,project_id BIGINT,bug_no BIGINT,title VARCHAR(160),status VARCHAR(30),priority VARCHAR(20),assignee_id BIGINT,creator_id BIGINT,solution VARCHAR(1000),created_at TIMESTAMP,deleted INT)");
        jdbc.execute("CREATE TABLE pf_requirement(id BIGINT,project_id BIGINT,requirement_no BIGINT,title VARCHAR(160),status VARCHAR(30),priority VARCHAR(20),reviewer_id BIGINT,created_by BIGINT,created_at TIMESTAMP,deleted INT)");
        jdbc.execute("CREATE TABLE pf_task(id BIGINT,project_id BIGINT,name VARCHAR(160),status VARCHAR(30),priority VARCHAR(20),assignee_id BIGINT,created_by BIGINT,planned_end_date DATE,sort_order INT,created_at TIMESTAMP,deleted INT)");
        jdbc.execute("CREATE TABLE pf_delivery_release(id VARCHAR(36) PRIMARY KEY,project_id BIGINT,name VARCHAR(160),target_date DATE,status VARCHAR(16),acceptance_note CLOB,released_at TIMESTAMP,created_by BIGINT,created_at TIMESTAMP,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_delivery_release_item(release_id VARCHAR(36),business_type VARCHAR(16),business_id BIGINT,PRIMARY KEY(release_id,business_type,business_id))");
        jdbc.execute("CREATE TABLE pf_release_stage(release_id VARCHAR(36),stage_code VARCHAR(32),owner_id BIGINT,actual_start DATE,actual_end DATE,status VARCHAR(16),conclusion CLOB,completed_by BIGINT,completed_at TIMESTAMP,updated_by BIGINT,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_release_record(id VARCHAR(36),release_id VARCHAR(36),stage_code VARCHAR(32),record_type VARCHAR(24),title VARCHAR(160),content CLOB,before_json CLOB,after_json CLOB,actor_id BIGINT,created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_release_application(release_id VARCHAR(36),status VARCHAR(16))");
        jdbc.execute("CREATE TABLE pf_work_comment(id VARCHAR(36),business_type VARCHAR(16),business_id BIGINT,author_id BIGINT,content CLOB,created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE pf_saved_work_view(id VARCHAR(36),user_id BIGINT,name VARCHAR(80),scope VARCHAR(20),filters_json CLOB,created_at TIMESTAMP,updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE sys_user(id BIGINT,real_name VARCHAR(80),enabled INT,deleted INT)");
        jdbc.execute("CREATE TABLE pf_project(id BIGINT,manager_id BIGINT,created_by BIGINT,co_manager_ids VARCHAR(200),management_project_id BIGINT,deleted INT)");
        jdbc.execute("CREATE TABLE pf_project_participant(project_id BIGINT,user_id BIGINT)");
        jdbc.execute("CREATE TABLE pf_file(id BIGINT,business_type VARCHAR(40),business_id BIGINT,original_name VARCHAR(255),uploaded_at TIMESTAMP,deleted INT)");
        jdbc.execute("CREATE TABLE sys_operation_log(id BIGINT,business_type VARCHAR(40),business_id BIGINT,operation_type VARCHAR(40),operator_name VARCHAR(80),content VARCHAR(1000),created_at TIMESTAMP)");
        CurrentUserContext.set(1L);
        access = mock(BusinessAccessService.class);
        projects = mock(ProjectMapper.class);
        requirements = mock(RequirementMapper.class);
        tasks = mock(TaskMapper.class);
        notices = mock(NoticeService.class);
        service = new WorkHubService(jdbc, new ObjectMapper(), access, projects, requirements, tasks,
                notices, mock(OperationLogService.class));
        RequirementEntity requirement = new RequirementEntity();
        requirement.setId(10L);
        requirement.setProjectId(100L);
        requirement.setStatus("ACCEPTED");
        requirement.setTitle("登录改造");
        requirement.setCreatedBy(2L);
        when(requirements.selectById(10L)).thenReturn(requirement);
        TaskEntity task = new TaskEntity();
        task.setId(20L);
        task.setProjectId(100L);
        task.setName("登录接口");
        when(tasks.selectById(20L)).thenReturn(task);
        ProjectEntity project = new ProjectEntity();
        project.setId(100L);
        when(projects.selectById(100L)).thenReturn(project);
        jdbc.update("INSERT INTO pf_requirement(id,project_id,requirement_no,title,status,created_by,deleted,created_at) VALUES (10,100,1,'登录改造','ACCEPTED',2,0,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO pf_task(id,project_id,name,status,created_by,deleted,created_at) VALUES (20,100,'登录接口','NOT_STARTED',1,0,CURRENT_TIMESTAMP)");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void rejectsCrossProjectTaskWithoutChangingLinks() {
        TaskEntity other = new TaskEntity();
        other.setId(21L);
        other.setProjectId(200L);
        when(tasks.selectById(21L)).thenReturn(other);
        assertThrows(BusinessException.class,
                () -> service.linkRequirementTasks(10L, new WorkHubRequests.TaskLinks(List.of(20L, 21L))));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM pf_requirement_task", Long.class));
    }

    @Test
    void linksTasksFromRelatedExecutionProject() {
        TaskEntity executionTask = new TaskEntity();
        executionTask.setId(21L);
        executionTask.setProjectId(200L);
        when(tasks.selectById(21L)).thenReturn(executionTask);
        ProjectEntity execution = new ProjectEntity();
        execution.setId(200L);
        execution.setManagementProjectId(100L);
        when(projects.selectById(200L)).thenReturn(execution);
        jdbc.update("INSERT INTO pf_task(id,project_id,name,status,deleted,created_at) VALUES (21,200,'执行开发','NOT_STARTED',0,CURRENT_TIMESTAMP)");
        service.linkRequirementTasks(10L, new WorkHubRequests.TaskLinks(List.of(20L, 21L)));
        assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM pf_requirement_task WHERE requirement_id=10", Long.class));
    }

    @Test
    void acceptanceRequiresCompletedTasksAndClosedBugs() {
        service.linkRequirementTasks(10L, new WorkHubRequests.TaskLinks(List.of(20L)));
        var passed = new WorkHubRequests.Acceptance("PASSED", "测试通过");
        assertThrows(BusinessException.class, () -> service.acceptRequirement(10L, passed));
        jdbc.update("UPDATE pf_task SET status='COMPLETED' WHERE id=20");
        jdbc.update("INSERT INTO pf_bug(id,project_id,title,status,deleted) VALUES (30,100,'无法登录','PENDING_FIX',0)");
        jdbc.update("INSERT INTO pf_bug_task(bug_id,task_id) VALUES (30,20)");
        assertThrows(BusinessException.class, () -> service.acceptRequirement(10L, passed));
        jdbc.update("UPDATE pf_bug SET status='CLOSED' WHERE id=30");
        Map<String, Object> delivery = service.acceptRequirement(10L, passed);
        assertEquals("PASSED", ((Map<?, ?>) delivery.get("acceptance")).get("result"));
        verify(notices).create(eq(2L), any(), any(), any(), eq("Requirement"), eq(10L));
    }

    @Test
    void releaseCannotBecomeReadyUntilDeliveryIsClear() {
        service.linkRequirementTasks(10L, new WorkHubRequests.TaskLinks(List.of(20L)));
        var planned = new WorkHubRequests.Release(100L, "十月交付", LocalDate.of(2026, 10, 30), "PLANNED", "",
                List.of(10L), List.of());
        Map<String, Object> created = service.saveRelease(null, planned);
        String id = (String) created.get("id");
        assertFalse((Boolean) created.get("ready"));
        var ready = new WorkHubRequests.Release(100L, "十月交付", planned.targetDate(), "READY", "", List.of(10L), List.of());
        assertThrows(BusinessException.class, () -> transactions.execute(status -> service.saveRelease(id, ready)));
        assertEquals("PLANNED", jdbc.queryForObject("SELECT status FROM pf_delivery_release WHERE id=?", String.class, id));
        jdbc.update("UPDATE pf_task SET status='COMPLETED' WHERE id=20");
        service.acceptRequirement(10L, new WorkHubRequests.Acceptance("PASSED", "测试通过"));
        Map<String, Object> updated = service.saveRelease(id, ready);
        assertTrue((Boolean) updated.get("ready"));
        assertThrows(BusinessException.class, () -> service.saveRelease(id,
                new WorkHubRequests.Release(100L, "十月交付", planned.targetDate(), "RELEASED", "", List.of(10L), List.of())));
        assertThrows(BusinessException.class, () -> transactions.execute(status -> service.saveRelease(id,
                new WorkHubRequests.Release(100L, "十月交付", planned.targetDate(), "RELEASED", "上线验收通过", List.of(10L), List.of()))));
        jdbc.update("INSERT INTO pf_release_application(release_id,status) VALUES (?,'APPROVED')", id);
        for (String stage : List.of("REQUIREMENT_SUBMISSION", "REQUIREMENT_REVIEW", "UI_DESIGN", "DEVELOPMENT",
                "TEST_HANDOFF", "TEST_VERIFICATION", "PRODUCT_ACCEPTANCE", "PRE_RELEASE")) {
            jdbc.update("INSERT INTO pf_release_stage(release_id,stage_code,status) VALUES (?,?,'COMPLETED')", id, stage);
        }
        assertThrows(BusinessException.class, () -> service.saveRelease(id,
                new WorkHubRequests.Release(100L, "更换上线范围", planned.targetDate(), "RELEASED",
                        "上线验收通过", List.of(10L), List.of())));
        Map<String, Object> released = service.saveRelease(id,
                new WorkHubRequests.Release(100L, "十月交付", planned.targetDate(), "RELEASED", "上线验收通过", List.of(10L), List.of()));
        assertEquals("RELEASED", released.get("status"));
        assertEquals(3L, jdbc.queryForObject("SELECT COUNT(*) FROM pf_release_record WHERE release_id=? AND record_type='RELEASE_CHANGE'", Long.class, id));
        assertTrue(jdbc.queryForObject("SELECT after_json FROM pf_release_record WHERE release_id=? AND record_type='RELEASE_CHANGE' ORDER BY created_at DESC LIMIT 1", String.class, id).contains("requirementIds"));
    }

    @Test
    void deletedLinkedTaskBlocksAcceptanceAndReleaseReadiness() {
        service.linkRequirementTasks(10L, new WorkHubRequests.TaskLinks(List.of(20L)));
        jdbc.update("UPDATE pf_task SET status='COMPLETED', deleted=1 WHERE id=20");
        assertThrows(BusinessException.class, () -> service.acceptRequirement(10L,
                new WorkHubRequests.Acceptance("PASSED", "已验收")));
        Map<String, Object> release = service.saveRelease(null, new WorkHubRequests.Release(
                100L, "删除任务检查", LocalDate.of(2026, 10, 30), "PLANNED", "", List.of(10L), List.of()));
        assertFalse((Boolean) release.get("ready"));
    }

    @Test
    void completedTaskDeletedAfterAcceptanceMakesReleaseUnready() {
        service.linkRequirementTasks(10L, new WorkHubRequests.TaskLinks(List.of(20L)));
        jdbc.update("UPDATE pf_task SET status='COMPLETED' WHERE id=20");
        service.acceptRequirement(10L, new WorkHubRequests.Acceptance("PASSED", "已验收"));
        Map<String, Object> release = service.saveRelease(null, new WorkHubRequests.Release(
                100L, "任务删除后检查", LocalDate.of(2026, 10, 30), "PLANNED", "", List.of(10L), List.of()));
        assertTrue((Boolean) release.get("ready"));
        jdbc.update("UPDATE pf_task SET deleted=1 WHERE id=20");
        Map<String, Object> refreshed = service.releases(100L).get(0);
        assertFalse((Boolean) refreshed.get("ready"));
        assertEquals(1L, refreshed.get("invalidLinkedTaskCount"));
    }

    @Test
    void mentionsOnlyProjectMembersAndSavedViewsStayPrivate() {
        jdbc.update("INSERT INTO pf_project(id,manager_id,created_by,deleted) VALUES (100,1,1,0)");
        jdbc.update("INSERT INTO sys_user(id,real_name,enabled,deleted) VALUES (2,'成员',1,0),(3,'外部人员',1,0)");
        jdbc.update("INSERT INTO pf_project_participant(project_id,user_id) VALUES (100,2)");
        assertThrows(BusinessException.class, () -> service.addComment("tasks", 20L,
                new WorkHubRequests.Comment("请检查", List.of(3L))));
        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM pf_work_comment", Long.class));
        assertEquals(1, service.addComment("tasks", 20L,
                new WorkHubRequests.Comment("请检查", List.of(2L))).size());
        verify(notices).create(eq(2L), any(), any(), any(), eq("Task"), eq(20L));
        service.saveView(new WorkHubRequests.SavedView("我的逾期", "TASK", Map.of("status", "OVERDUE")));
        assertEquals(1, service.savedViews("TASK").size());
        CurrentUserContext.set(3L);
        assertTrue(service.savedViews("TASK").isEmpty());
    }

    @Test
    void quickSearchFindsDisplayedRequirementNumbersAndTitles() {
        when(access.canViewAll()).thenReturn(true);
        assertEquals(10L, ((Number) service.search("REQ-2026-0001").get(0).get("id")).longValue());
        assertEquals("TASK", service.search("登录接口").get(0).get("type"));
    }

    @Test
    void overviewPaginatesRecentBugsAndActivityWithinTheSelectedProject() {
        when(access.canReadProject(100L)).thenReturn(true);
        for (int index = 1; index <= 10; index++) {
            jdbc.update("INSERT INTO pf_bug(id,project_id,bug_no,title,status,created_at,deleted) VALUES (?,?,?,?,?,DATEADD('MINUTE', ?, CURRENT_TIMESTAMP),0)",
                    1000L + index, 100L, index, "问题" + index, "PENDING_FIX", index);
            jdbc.update("INSERT INTO sys_operation_log(id,business_type,business_id,content,created_at) VALUES (?,'Bug',?,?,DATEADD('MINUTE', ?, CURRENT_TIMESTAMP))",
                    2000L + index, 1000L + index, "更新问题" + index, index);
            jdbc.update("INSERT INTO pf_file(id,business_type,business_id,original_name,uploaded_at,deleted) VALUES (?,'PROJECT',100,?,DATEADD('MINUTE', ?, CURRENT_TIMESTAMP),0)",
                    4000L + index, "附件" + index + ".pdf", index);
        }
        jdbc.update("INSERT INTO pf_bug(id,project_id,bug_no,title,status,created_at,deleted) VALUES (3000,200,99,'其他项目','CLOSED',CURRENT_TIMESTAMP,0)");

        Map<String, Object> first = service.projectOverview(100L, 1, 1, 1);
        Map<String, Object> second = service.projectOverview(100L, 2, 2, 2);
        assertEquals(10L, ((Map<?, ?>) first.get("counts")).get("bugs"));
        assertEquals(10L, ((Map<?, ?>) first.get("counts")).get("files"));
        assertEquals(10L, first.get("activityTotal"));
        assertEquals(8, ((List<?>) first.get("bugs")).size());
        assertEquals(2, ((List<?>) second.get("bugs")).size());
        assertEquals(2, ((List<?>) second.get("files")).size());
        assertEquals(2, ((List<?>) second.get("activity")).size());
        assertEquals(10L, ((Number) ((Map<?, ?>) ((List<?>) first.get("bugs")).get(0)).get("bugNo")).longValue());
    }

    @Test
    void quickSearchDoesNotExposeAnotherProjectsWork() {
        CurrentUserContext.set(3L);
        when(access.readableProjectIdsSql()).thenReturn("SELECT id FROM pf_project WHERE id=200");
        assertTrue(service.search("登录").isEmpty());
        jdbc.update("INSERT INTO pf_project(id,manager_id,created_by,deleted) VALUES (200,3,3,0)");
        jdbc.update("INSERT INTO pf_task(id,project_id,name,status,created_by,deleted,created_at) VALUES (21,200,'登录配置','NOT_STARTED',3,0,CURRENT_TIMESTAMP)");
        assertEquals(1, service.search("登录").size());
        assertEquals(21L, ((Number) service.search("登录").get(0).get("id")).longValue());
    }

    @Test
    void managementWorkbenchIncludesReadableExecutionProject() {
        jdbc.update("INSERT INTO pf_project(id,manager_id,created_by,deleted) VALUES (100,1,1,0)");
        jdbc.update("INSERT INTO pf_project(id,manager_id,created_by,management_project_id,deleted) VALUES (200,1,1,100,0)");
        jdbc.update("INSERT INTO pf_task(id,project_id,name,status,deleted,created_at) VALUES (21,200,'执行开发','IN_PROGRESS',0,CURRENT_TIMESTAMP)");
        jdbc.update("INSERT INTO pf_delivery_release(id,project_id,name,target_date,status,created_at) VALUES ('child-release',200,'子项目版本',DATE '2026-10-30','PLANNED',CURRENT_TIMESTAMP)");
        when(access.canReadProject(100L)).thenReturn(true);
        when(access.canReadProject(200L)).thenReturn(true);
        Map<String, Object> overview = service.projectOverview(100L);
        Map<?, ?> counts = (Map<?, ?>) overview.get("counts");
        assertEquals(2L, counts.get("tasks"));
        assertEquals(1L, counts.get("requirements"));
        assertEquals(2, ((List<?>) overview.get("tasks")).size());
        List<?> releases = (List<?>) overview.get("releases");
        assertEquals(1, releases.size());
        assertEquals(200L, ((Map<?, ?>) releases.get(0)).get("projectId"));
    }
}
