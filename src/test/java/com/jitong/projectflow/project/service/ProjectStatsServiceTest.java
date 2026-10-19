package com.jitong.projectflow.project.service;

import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectStatsServiceTest {
    @Mock TaskMapper tasks;
    @Mock BugMapper bugs;
    @Mock ProjectMapper projects;
    @Mock BusinessAccessService access;

    @Test
    void executionStatsExcludeUnreadableManagementProjectBugs() {
        ProjectEntity execution = new ProjectEntity();
        execution.setId(10L);
        execution.setProjectType("EXECUTION");
        execution.setManagementProjectId(20L);
        when(projects.selectBatchIds(List.of(10L))).thenReturn(List.of(execution));
        when(access.canReadProject(20L)).thenReturn(false);
        when(tasks.selectMaps(any())).thenReturn(List.of());
        when(bugs.selectMaps(any())).thenReturn(List.of(
                Map.of("project_id", 10L, "cnt", 2L),
                Map.of("project_id", 20L, "cnt", 99L)));

        var result = new ProjectStatsService(tasks, bugs, projects, access).batchStats(List.of(10L));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).bugCount()).isEqualTo(2L);
    }
}
