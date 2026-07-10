package com.jitong.projectflow.taskcalendar.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.task.domain.TaskPriority;
import com.jitong.projectflow.task.domain.TaskStatus;
import com.jitong.projectflow.task.domain.TaskStatusCalculator;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import com.jitong.projectflow.taskcalendar.dto.TaskCalendarDayResponse;
import com.jitong.projectflow.taskcalendar.dto.TaskCalendarMonthResponse;
import com.jitong.projectflow.taskcalendar.dto.TaskCalendarTaskResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class TaskCalendarService {
    private static final int MONTH_PREVIEW_LIMIT = 5;

    private final TaskMapper taskMapper;
    private final ProjectMapper projectMapper;
    private final SystemUserMapper systemUserMapper;
    private final TaskStatusCalculator statusCalculator = new TaskStatusCalculator();

    public TaskCalendarMonthResponse month(YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();
        List<TaskEntity> tasks = findTasksForRange(start, end);
        Map<Long, String> projectNames = loadProjectNames(tasks);
        Map<Long, String> userNames = loadUserNames(tasks);
        List<TaskCalendarDayResponse> days = IntStream.rangeClosed(1, month.lengthOfMonth())
                .mapToObj(day -> buildDay(month.atDay(day), tasks, projectNames, userNames, MONTH_PREVIEW_LIMIT))
                .toList();
        return TaskCalendarMonthResponse.builder()
                .month(month)
                .days(days)
                .build();
    }

    public TaskCalendarDayResponse day(LocalDate date) {
        List<TaskEntity> tasks = findTasksForRange(date, date);
        Map<Long, String> projectNames = loadProjectNames(tasks);
        Map<Long, String> userNames = loadUserNames(tasks);
        return buildDay(date, tasks, projectNames, userNames, Integer.MAX_VALUE);
    }

    private List<TaskEntity> findTasksForRange(LocalDate start, LocalDate end) {
        LambdaQueryWrapper<TaskEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(TaskEntity::getPlannedEndDate);
        wrapper.and(q -> q.isNull(TaskEntity::getActualEndDate).or().ge(TaskEntity::getActualEndDate, start));
        wrapper.and(q -> q.le(TaskEntity::getPlannedStartDate, end)
                .or().isNull(TaskEntity::getPlannedStartDate)
                .or().le(TaskEntity::getPlannedEndDate, end));
        return taskMapper.selectList(wrapper);
    }

    private Map<Long, String> loadProjectNames(List<TaskEntity> tasks) {
        List<Long> projectIds = tasks.stream()
                .map(TaskEntity::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (projectIds.isEmpty()) return Map.of();
        return projectMapper.selectByIds(projectIds).stream()
                .collect(Collectors.toMap(ProjectEntity::getId, ProjectEntity::getName));
    }

    private Map<Long, String> loadUserNames(List<TaskEntity> tasks) {
        List<Long> userIds = tasks.stream()
                .map(TaskEntity::getAssigneeId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (userIds.isEmpty()) return Map.of();
        return systemUserMapper.selectByIds(userIds).stream()
                .collect(Collectors.toMap(SystemUser::getId, SystemUser::getRealName));
    }

    private TaskCalendarDayResponse buildDay(LocalDate date, List<TaskEntity> sourceTasks,
                                              Map<Long, String> projectNames,
                                              Map<Long, String> userNames, int limit) {
        List<TaskCalendarTaskResponse> sorted = sourceTasks.stream()
                .filter(task -> activeOn(task, date))
                .map(task -> toResponse(task, date, projectNames, userNames))
                .sorted(calendarComparator())
                .toList();
        List<TaskCalendarTaskResponse> preview = sorted.stream().limit(limit).toList();
        return TaskCalendarDayResponse.builder()
                .date(date)
                .totalCount(sorted.size())
                .tasks(preview)
                .build();
    }

    private boolean activeOn(TaskEntity task, LocalDate date) {
        if (task.getPlannedEndDate() == null) {
            return false;
        }
        LocalDate start = task.getPlannedStartDate() == null ? task.getPlannedEndDate() : task.getPlannedStartDate();
        if (date.isBefore(start)) {
            return false;
        }
        if (task.getActualEndDate() != null) {
            return !date.isAfter(task.getActualEndDate());
        }
        return true;
    }

    private TaskCalendarTaskResponse toResponse(TaskEntity task, LocalDate date,
                                                  Map<Long, String> projectNames,
                                                  Map<Long, String> userNames) {
        String status = statusCalculator.calculate(
                task.getPlannedEndDate(),
                task.getActualStartDate(),
                task.getActualEndDate(),
                TaskStatus.PAUSED.name().equals(task.getStatus()),
                date).name();
        return TaskCalendarTaskResponse.builder()
                .id(task.getId())
                .projectId(task.getProjectId())
                .name(task.getName())
                .priority(task.getPriority())
                .status(status)
                .assigneeId(task.getAssigneeId())
                .plannedStartDate(task.getPlannedStartDate())
                .plannedEndDate(task.getPlannedEndDate())
                .actualStartDate(task.getActualStartDate())
                .actualEndDate(task.getActualEndDate())
                .overdueDays(overdueDays(task, date))
                .remainingDays(remainingDays(task, date))
                .projectName(projectNames.get(task.getProjectId()))
                .assigneeName(userNames.get(task.getAssigneeId()))
                .build();
    }

    private Comparator<TaskCalendarTaskResponse> calendarComparator() {
        return Comparator
                .comparingInt((TaskCalendarTaskResponse task) -> TaskPriority.URGENT.name().equals(task.getPriority()) ? 0 : 1)
                .thenComparing(Comparator.comparingInt((TaskCalendarTaskResponse task) -> valueOrZero(task.getOverdueDays())).reversed())
                .thenComparingInt(task -> TaskStatus.DUE_SOON.name().equals(task.getStatus()) ? 0 : 1)
                .thenComparingInt(task -> task.getRemainingDays() == null ? Integer.MAX_VALUE : task.getRemainingDays())
                .thenComparingInt(task -> priorityRank(task.getPriority()))
                .thenComparing(TaskCalendarTaskResponse::getId, Comparator.nullsLast(Long::compareTo));
    }

    private Integer overdueDays(TaskEntity task, LocalDate date) {
        if (task.getPlannedEndDate() == null || task.getActualEndDate() != null || TaskStatus.PAUSED.name().equals(task.getStatus())) {
            return null;
        }
        if (!date.isAfter(task.getPlannedEndDate())) {
            return null;
        }
        return Math.toIntExact(ChronoUnit.DAYS.between(task.getPlannedEndDate(), date));
    }

    private Integer remainingDays(TaskEntity task, LocalDate date) {
        if (task.getPlannedEndDate() == null || date.isAfter(task.getPlannedEndDate())) {
            return null;
        }
        return Math.toIntExact(ChronoUnit.DAYS.between(date, task.getPlannedEndDate()));
    }

    private int priorityRank(String priority) {
        if (TaskPriority.URGENT.name().equals(priority)) return 0;
        if (TaskPriority.HIGH.name().equals(priority)) return 1;
        if (TaskPriority.MEDIUM.name().equals(priority)) return 2;
        if (TaskPriority.LOW.name().equals(priority)) return 3;
        return 4;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }
}
