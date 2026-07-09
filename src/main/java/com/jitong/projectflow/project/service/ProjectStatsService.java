package com.jitong.projectflow.project.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.project.dto.ProjectStatsResponse;
import com.jitong.projectflow.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectStatsService {

    private final TaskMapper taskMapper;
    private final BugMapper bugMapper;

    public List<ProjectStatsResponse> batchStats(List<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> taskCounts = countByProjectIds(taskMapper, projectIds);
        Map<Long, Long> bugCounts  = countByProjectIds(bugMapper,  projectIds);

        return projectIds.stream()
                .map(id -> new ProjectStatsResponse(
                        id,
                        taskCounts.getOrDefault(id, 0L),
                        bugCounts.getOrDefault(id, 0L)))
                .toList();
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
