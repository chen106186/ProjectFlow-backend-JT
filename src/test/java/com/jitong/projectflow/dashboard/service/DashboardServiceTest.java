package com.jitong.projectflow.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.jitong.projectflow.bug.entity.BugEntity;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.dashboard.dto.DashboardSummaryResponse;
import com.jitong.projectflow.dashboard.dto.MyStatisticsResponse;
import com.jitong.projectflow.dashboard.dto.TodoItemResponse;
import com.jitong.projectflow.notice.mapper.NoticeMapper;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    ProjectMapper projectMapper;
    @Mock
    TaskMapper taskMapper;
    @Mock
    BugMapper bugMapper;
    @Mock
    RequirementMapper requirementMapper;
    @Mock
    NoticeMapper noticeMapper;
    @Mock
    SystemUserMapper systemUserMapper;
    @Mock BusinessAccessService businessAccessService;

    @InjectMocks
    DashboardService dashboardService;

    @SuppressWarnings("unchecked")
    @Test
    void aggregatesStatisticsCorrectly() {
        when(taskMapper.selectCount(any(Wrapper.class))).thenReturn(5L, 3L, 1L);
        when(bugMapper.selectCount(any(Wrapper.class))).thenReturn(4L, 2L);
        when(requirementMapper.selectCount(any(Wrapper.class))).thenReturn(6L, 2L);
        when(noticeMapper.selectCount(any(Wrapper.class))).thenReturn(3L);
        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                task(1L, 100L, "开发接口", "HIGH", "IN_PROGRESS", 1L, LocalDate.now().plusDays(10)),
                task(2L, 100L, "修复问题", "HIGH", "COMPLETED", 1L, LocalDate.now())
        ));
        when(bugMapper.selectList(any(Wrapper.class))).thenReturn(List.of(
                bug(10L, 100L, "登录失败", "HIGH", "PENDING_FIX", 1L),
                bug(11L, 100L, "保存异常", "LOW", "CLOSED", 1L)
        ));

        MyStatisticsResponse result = dashboardService.getMyStatistics(1L, "all", null);

        assertThat(result.getMyTaskTotal()).isEqualTo(5L);
        assertThat(result.getMyTaskCompleted()).isEqualTo(3L);
        assertThat(result.getMyTaskOverdue()).isEqualTo(1L);
        assertThat(result.getMyBugTotal()).isEqualTo(4L);
        assertThat(result.getMyBugOpen()).isEqualTo(2L);
        assertThat(result.getMyRequirementTotal()).isEqualTo(6L);
        assertThat(result.getMyRequirementAccepted()).isEqualTo(2L);
        assertThat(result.getUnreadNoticeCount()).isEqualTo(3L);
        assertThat(result.getTaskStatusDistribution()).containsEntry("IN_PROGRESS", 1L).containsEntry("COMPLETED", 1L);
        assertThat(result.getTaskPriorityDistribution()).containsEntry("HIGH", 2L);
        assertThat(result.getBugStatusDistribution()).containsEntry("PENDING_FIX", 1L).containsEntry("CLOSED", 1L);
    }

    @SuppressWarnings("unchecked")
    @Test
    void aggregatesProjectSummaryCorrectly() {
        when(projectMapper.selectCount(any(Wrapper.class))).thenReturn(2L, 8L, 3L, 6L);

        DashboardSummaryResponse result = dashboardService.getSummary();

        assertThat(result.managementProjectCount()).isEqualTo(2L);
        assertThat(result.executionProjectCount()).isEqualTo(8L);
        assertThat(result.inProgressProjectCount()).isEqualTo(3L);
        assertThat(result.completedProjectCount()).isEqualTo(6L);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void periodCompletedUsesCompletionDateWhileOpenBacklogIgnoresCreationDate() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskEntity.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), BugEntity.class);
        when(businessAccessService.canViewAll()).thenReturn(true);

        dashboardService.getMyStatistics(1L, "week", null);

        ArgumentCaptor<Wrapper> taskQueries = ArgumentCaptor.forClass(Wrapper.class);
        verify(taskMapper, times(3)).selectCount(taskQueries.capture());
        String created = taskQueries.getAllValues().get(0).getSqlSegment();
        String completed = taskQueries.getAllValues().get(1).getSqlSegment();
        String overdue = taskQueries.getAllValues().get(2).getSqlSegment();
        assertThat(created).contains("created_at");
        assertThat(completed).contains("actual_end_date").doesNotContain("created_at");
        assertThat(overdue).contains("planned_end_date").doesNotContain("created_at");

        ArgumentCaptor<Wrapper> bugQueries = ArgumentCaptor.forClass(Wrapper.class);
        verify(bugMapper, times(2)).selectCount(bugQueries.capture());
        assertThat(bugQueries.getAllValues().get(0).getSqlSegment()).contains("created_at");
        assertThat(bugQueries.getAllValues().get(1).getSqlSegment()).doesNotContain("created_at");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void completedProjectSummaryCountsOnlyManagementProjects() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), ProjectEntity.class);
        when(businessAccessService.canViewAll()).thenReturn(true);
        dashboardService.getSummary();
        ArgumentCaptor<Wrapper> queries = ArgumentCaptor.forClass(Wrapper.class);
        verify(projectMapper, times(4)).selectCount(queries.capture());
        LambdaQueryWrapper<ProjectEntity> completed = (LambdaQueryWrapper<ProjectEntity>) queries.getAllValues().get(3);
        assertThat(completed.getSqlSegment()).contains("project_type", "status");
        assertThat(completed.getParamNameValuePairs().values())
                .contains("MANAGEMENT", "COMPLETED", "OVERDUE_COMPLETED");
    }

    @SuppressWarnings("unchecked")
    @Test
    void listsAndSortsTodosFromTasksAndBugs() {
        TaskEntity mediumTask = task(10L, 100L, "普通任务", "MEDIUM", "IN_PROGRESS", 1L, LocalDate.now().minusDays(3));
        TaskEntity urgentTask = task(11L, 100L, "紧急任务", "URGENT", "OVERDUE", 2L, LocalDate.now().minusDays(1));
        BugEntity highBug = bug(20L, 100L, "高优先级Bug", "HIGH", "PENDING_FIX", 2L);
        ProjectEntity project = project(100L, "项目A");
        SystemUser user1 = user(1L, "张三");
        SystemUser user2 = user(2L, "李四");

        when(taskMapper.selectList(any(Wrapper.class))).thenReturn(List.of(mediumTask, urgentTask));
        when(bugMapper.selectList(any(Wrapper.class))).thenReturn(List.of(highBug));
        when(projectMapper.selectByIds(anyCollection())).thenReturn(List.of(project));
        when(systemUserMapper.selectByIds(anyCollection())).thenReturn(List.of(user1, user2));

        List<TodoItemResponse> result = dashboardService.listTodos(1L);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).itemType()).isEqualTo("TASK");
        assertThat(result.get(0).businessId()).isEqualTo(11L);
        assertThat(result.get(0).title()).isEqualTo("紧急任务");
        assertThat(result.get(0).projectName()).isEqualTo("项目A");
        assertThat(result.get(0).ownerName()).isEqualTo("李四");
        assertThat(result.get(0).overdueDays()).isEqualTo(1L);
        assertThat(result.get(1).itemType()).isEqualTo("BUG");
        assertThat(result.get(1).businessId()).isEqualTo(20L);
        assertThat(result.get(2).businessId()).isEqualTo(10L);
    }

    private TaskEntity task(Long id,
                            Long projectId,
                            String name,
                            String priority,
                            String status,
                            Long assigneeId,
                            LocalDate plannedEndDate) {
        TaskEntity entity = new TaskEntity();
        entity.setId(id);
        entity.setProjectId(projectId);
        entity.setName(name);
        entity.setPriority(priority);
        entity.setStatus(status);
        entity.setAssigneeId(assigneeId);
        entity.setPlannedEndDate(plannedEndDate);
        entity.setActualStartDate(LocalDate.now().minusDays(1));
        if ("COMPLETED".equals(status)) entity.setActualEndDate(LocalDate.now());
        return entity;
    }

    private BugEntity bug(Long id,
                          Long projectId,
                          String title,
                          String priority,
                          String status,
                          Long assigneeId) {
        BugEntity entity = new BugEntity();
        entity.setId(id);
        entity.setProjectId(projectId);
        entity.setTitle(title);
        entity.setPriority(priority);
        entity.setStatus(status);
        entity.setAssigneeId(assigneeId);
        return entity;
    }

    private ProjectEntity project(Long id, String name) {
        ProjectEntity entity = new ProjectEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }

    private SystemUser user(Long id, String realName) {
        SystemUser entity = new SystemUser();
        entity.setId(id);
        entity.setRealName(realName);
        return entity;
    }
}
