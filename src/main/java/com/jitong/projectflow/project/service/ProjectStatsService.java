package com.jitong.projectflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.dto.ProjectStatsResponse;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectStatsService {

    private final TaskMapper taskMapper;
    private final BugMapper bugMapper;
    private final ProjectMapper projectMapper;

    public List<ProjectStatsResponse> batchStats(List<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> taskCounts = countByProjectIds(taskMapper, projectIds);
        Map<Long, ProjectEntity> projectMap = projectMapper.selectBatchIds(projectIds).stream()
                .collect(Collectors.toMap(ProjectEntity::getId, project -> project));
        Map<Long, Long> bugCounts = countBugsByProjectScope(projectIds, projectMap);

        return projectIds.stream()
                .map(id -> new ProjectStatsResponse(
                        id,
                        taskCounts.getOrDefault(id, 0L),
                        bugCounts.getOrDefault(id, 0L)))
                .toList();
    }

    private Map<Long, Long> countBugsByProjectScope(List<Long> projectIds, Map<Long, ProjectEntity> projectMap) {
        Set<Long> bugProjectIds = new HashSet<>(projectIds);
        projectMap.values().stream()
                .filter(project -> "EXECUTION".equals(project.getProjectType()))
                .map(ProjectEntity::getManagementProjectId)
                .filter(id -> id != null && id > 0)
                .forEach(bugProjectIds::add);

        Map<Long, Long> rawCounts = countByProjectIds(bugMapper, bugProjectIds.stream().toList());
        return projectIds.stream().collect(Collectors.toMap(
                id -> id,
                id -> {
                    long count = rawCounts.getOrDefault(id, 0L);
                    ProjectEntity project = projectMap.get(id);
                    Long managementProjectId = project == null ? null : project.getManagementProjectId();
                    if (project != null && "EXECUTION".equals(project.getProjectType())
                            && managementProjectId != null && !managementProjectId.equals(id)) {
                        count += rawCounts.getOrDefault(managementProjectId, 0L);
                    }
                    return count;
                }
        ));
    }

    private <T> Map<Long, Long> countByProjectIds(
            com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper,
            List<Long> projectIds) {

        QueryWrapper<T> wrapper = new QueryWrapper<>();
        wrapper.select("project_id, COUNT(*) AS cnt")
               .in("project_id", projectIds)
               .groupBy("project_id");

        return mapper.selectMaps(wrapper).stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("project_id")).longValue(),
                        m -> ((Number) m.get("cnt")).longValue()));
    }
}
