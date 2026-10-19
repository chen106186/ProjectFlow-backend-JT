package com.jitong.projectflow.task.domain;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.jitong.projectflow.task.entity.TaskEntity;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class TaskStatusQueryTest {
    @Test
    void databaseFiltersMatchDisplayedStatusesAcrossDateBoundariesAndStaleStoredStates() throws Exception {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TaskEntity.class);
        LocalDate today = LocalDate.of(2026, 10, 19);
        List<TaskEntity> tasks = List.of(
                task(1, today.minusDays(1), null, null, false),
                task(2, today.plusDays(3), null, null, false),
                task(3, today.plusDays(10), today, today.plusDays(1), false),
                task(4, null, today.plusDays(1), null, false),
                task(5, today.minusDays(1), today.minusDays(3), today.minusDays(1), true),
                task(6, today.plusDays(5), today.minusDays(2), today, false),
                task(7, null, today, null, false),
                task(8, today.plusDays(4), null, null, false));
        try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:task_status_query;MODE=MySQL", "sa", "")) {
            connection.createStatement().execute("CREATE TABLE pf_task(id BIGINT, planned_end_date DATE, actual_start_date DATE, actual_end_date DATE, status VARCHAR(32))");
            for (TaskEntity task : tasks) {
                try (PreparedStatement insert = connection.prepareStatement("INSERT INTO pf_task VALUES(?,?,?,?,?)")) {
                    insert.setLong(1, task.getId());
                    insert.setObject(2, task.getPlannedEndDate());
                    insert.setObject(3, task.getActualStartDate());
                    insert.setObject(4, task.getActualEndDate());
                    insert.setString(5, task.getStatus());
                    insert.executeUpdate();
                }
            }
            TaskStatusCalculator calculator = new TaskStatusCalculator();
            for (TaskStatus status : TaskStatus.values()) {
                List<Long> expectedIds = tasks.stream().filter(task -> calculator.calculate(null, task.getPlannedEndDate(),
                                task.getActualStartDate(), task.getActualEndDate(), "PAUSED".equals(task.getStatus()), today) == status)
                        .map(TaskEntity::getId).toList();
                LambdaQueryWrapper<TaskEntity> query = new LambdaQueryWrapper<>();
                TaskStatusQuery.apply(query, status, today);
                assertThat(queryIds(connection, query)).as("database filter for %s", status).containsExactlyInAnyOrderElementsOf(expectedIds);
            }
        }
    }

    private List<Long> queryIds(Connection connection, LambdaQueryWrapper<TaskEntity> query) throws Exception {
        var matcher = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.([^}]+)}").matcher(query.getSqlSegment());
        List<Object> parameters = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        while (matcher.find()) {
            parameters.add(query.getParamNameValuePairs().get(matcher.group(1)));
            matcher.appendReplacement(sql, "?");
        }
        matcher.appendTail(sql);
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM pf_task WHERE " + sql)) {
            for (int i = 0; i < parameters.size(); i++) statement.setObject(i + 1, parameters.get(i));
            List<Long> ids = new ArrayList<>();
            try (var result = statement.executeQuery()) {
                while (result.next()) ids.add(result.getLong(1));
            }
            return ids;
        }
    }

    private TaskEntity task(long id, LocalDate plannedEnd, LocalDate actualStart, LocalDate actualEnd, boolean paused) {
        var task = new TaskEntity();
        task.setId(id);
        task.setPlannedEndDate(plannedEnd);
        task.setActualStartDate(actualStart);
        task.setActualEndDate(actualEnd);
        task.setStatus(paused ? "PAUSED" : "NOT_STARTED");
        return task;
    }
}
