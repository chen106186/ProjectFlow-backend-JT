package com.jitong.projectflow.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.bug.domain.BugStatus;
import com.jitong.projectflow.bug.entity.BugEntity;
import com.jitong.projectflow.bug.mapper.BugMapper;
import com.jitong.projectflow.dashboard.domain.TodoSortKey;
import com.jitong.projectflow.dashboard.dto.DashboardSummaryResponse;
import com.jitong.projectflow.dashboard.dto.MyStatisticsResponse;
import com.jitong.projectflow.dashboard.dto.TodoItemResponse;
import com.jitong.projectflow.dashboard.dto.TrendDataPoint;
import com.jitong.projectflow.notice.entity.NoticeEntity;
import com.jitong.projectflow.notice.mapper.NoticeMapper;
import com.jitong.projectflow.auth.security.BusinessAccessService;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.requirement.domain.RequirementStatus;
import com.jitong.projectflow.requirement.entity.RequirementEntity;
import com.jitong.projectflow.requirement.mapper.RequirementMapper;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.task.domain.TaskStatus;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final BugMapper bugMapper;
    private final RequirementMapper requirementMapper;
    private final NoticeMapper noticeMapper;
    private final SystemUserMapper systemUserMapper;
    private final BusinessAccessService businessAccessService;

    public DashboardSummaryResponse getSummary() {
        long managementProjectCount = projectMapper.selectCount(
                new LambdaQueryWrapper<ProjectEntity>()
                        .eq(ProjectEntity::getProjectType, "MANAGEMENT"));
        long executionProjectCount = projectMapper.selectCount(
                new LambdaQueryWrapper<ProjectEntity>()
                        .eq(ProjectEntity::getProjectType, "EXECUTION"));
        long inProgressProjectCount = projectMapper.selectCount(
                new LambdaQueryWrapper<ProjectEntity>()
                        .eq(ProjectEntity::getStatus, "IN_PROGRESS"));
        long completedProjectCount = projectMapper.selectCount(
                new LambdaQueryWrapper<ProjectEntity>()
                        .eq(ProjectEntity::getStatus, "COMPLETED"));

        return new DashboardSummaryResponse(
                managementProjectCount,
                executionProjectCount,
                inProgressProjectCount,
                completedProjectCount);
    }

    public List<TodoItemResponse> listTodos(Long userId) {
        boolean viewAll = businessAccessService.canViewAll();
        LambdaQueryWrapper<TaskEntity> taskWrapper = new LambdaQueryWrapper<TaskEntity>()
                .ne(TaskEntity::getStatus, TaskStatus.COMPLETED.name());
        if (!viewAll) {
            taskWrapper.eq(TaskEntity::getAssigneeId, userId);
        }
        List<TaskEntity> tasks = taskMapper.selectList(taskWrapper);

        LambdaQueryWrapper<BugEntity> bugWrapper = new LambdaQueryWrapper<BugEntity>()
                .ne(BugEntity::getStatus, BugStatus.CLOSED.name());
        if (!viewAll) {
            bugWrapper.and(wrapper -> wrapper.eq(BugEntity::getAssigneeId, userId)
                    .or().eq(BugEntity::getCreatorId, userId));
        }
        List<BugEntity> bugs = bugMapper.selectList(bugWrapper);

        LambdaQueryWrapper<RequirementEntity> reqWrapper = new LambdaQueryWrapper<RequirementEntity>()
                .eq(RequirementEntity::getStatus, RequirementStatus.PENDING_REVIEW.name());
        if (!viewAll) {
            reqWrapper.eq(RequirementEntity::getCreatedBy, userId);
        }
        List<RequirementEntity> requirements = requirementMapper.selectList(reqWrapper);

        Map<Long, String> projectNames = loadProjectNames(tasks, bugs, requirements);
        Map<Long, String> userNames = loadUserNames(tasks, bugs, requirements);
        LocalDate today = LocalDate.now();

        List<TodoWithSortKey> todos = new ArrayList<>();
        tasks.stream()
                .map(task -> taskTodo(task, projectNames, userNames, today))
                .forEach(todos::add);
        bugs.stream()
                .map(bug -> bugTodo(bug, projectNames, userNames))
                .forEach(todos::add);
        requirements.stream()
                .map(req -> requirementTodo(req, projectNames, userNames))
                .forEach(todos::add);

        return todos.stream()
                .sorted()
                .map(TodoWithSortKey::item)
                .toList();
    }

    public MyStatisticsResponse getMyStatistics(Long userId, String period, Long targetUserId) {
        // 总经办：targetUserId=null 表示全员，targetUserId!=null 表示指定用户；普通用户强制用自己
        final Long effectiveUserId = businessAccessService.canViewAll() ? targetUserId : userId;
        final boolean hasUserFilter = effectiveUserId != null;
        // 计算时间范围
        LocalDate today = LocalDate.now();
        LocalDate startDate = null;
        LocalDate endDate = null;
        if ("today".equals(period)) {
            startDate = today;
            endDate = today;
        } else if ("week".equals(period)) {
            startDate = today.minusDays(6);
            endDate = today;
        } else if ("month".equals(period)) {
            startDate = today.minusDays(29);
            endDate = today;
        } else if ("quarter".equals(period)) {
            startDate = today.minusDays(89);
            endDate = today;
        } else if ("year".equals(period)) {
            startDate = today.minusDays(364);
            endDate = today;
        }
        boolean hasPeriod = startDate != null;
        final LocalDate fStart = startDate;
        final LocalDate fEnd = endDate;

        // 聚合统计（带时间过滤）
        long myTaskTotal = taskMapper.selectCount(
                new LambdaQueryWrapper<TaskEntity>()
                        .eq(hasUserFilter, TaskEntity::getAssigneeId, effectiveUserId)
                        .ge(hasPeriod, TaskEntity::getCreatedAt, hasPeriod ? startOfDay(fStart) : null)
                        .le(hasPeriod, TaskEntity::getCreatedAt, hasPeriod ? endOfDay(fEnd) : null));

        long myTaskCompleted = taskMapper.selectCount(
                new LambdaQueryWrapper<TaskEntity>()
                        .eq(hasUserFilter, TaskEntity::getAssigneeId, effectiveUserId)
                        .eq(TaskEntity::getStatus, TaskStatus.COMPLETED.name())
                        .ge(hasPeriod, TaskEntity::getCreatedAt, hasPeriod ? startOfDay(fStart) : null)
                        .le(hasPeriod, TaskEntity::getCreatedAt, hasPeriod ? endOfDay(fEnd) : null));

        long myTaskOverdue = taskMapper.selectCount(
                new LambdaQueryWrapper<TaskEntity>()
                        .eq(hasUserFilter, TaskEntity::getAssigneeId, effectiveUserId)
                        .eq(TaskEntity::getStatus, TaskStatus.OVERDUE.name())
                        .ge(hasPeriod, TaskEntity::getCreatedAt, hasPeriod ? startOfDay(fStart) : null)
                        .le(hasPeriod, TaskEntity::getCreatedAt, hasPeriod ? endOfDay(fEnd) : null));

        LambdaQueryWrapper<BugEntity> bugTotalW = new LambdaQueryWrapper<BugEntity>()
                .ge(hasPeriod, BugEntity::getCreatedAt, hasPeriod ? startOfDay(fStart) : null)
                .le(hasPeriod, BugEntity::getCreatedAt, hasPeriod ? endOfDay(fEnd) : null);
        if (hasUserFilter) {
            bugTotalW.and(w -> w.eq(BugEntity::getAssigneeId, effectiveUserId).or().eq(BugEntity::getCreatorId, effectiveUserId));
        }
        long myBugTotal = bugMapper.selectCount(bugTotalW);

        LambdaQueryWrapper<BugEntity> bugOpenW = new LambdaQueryWrapper<BugEntity>()
                .ne(BugEntity::getStatus, BugStatus.CLOSED.name())
                .ge(hasPeriod, BugEntity::getCreatedAt, hasPeriod ? startOfDay(fStart) : null)
                .le(hasPeriod, BugEntity::getCreatedAt, hasPeriod ? endOfDay(fEnd) : null);
        if (hasUserFilter) {
            bugOpenW.and(w -> w.eq(BugEntity::getAssigneeId, effectiveUserId).or().eq(BugEntity::getCreatorId, effectiveUserId));
        }
        long myBugOpen = bugMapper.selectCount(bugOpenW);

        long myRequirementTotal = requirementMapper.selectCount(
                new LambdaQueryWrapper<RequirementEntity>()
                        .eq(hasUserFilter, RequirementEntity::getCreatedBy, effectiveUserId)
                        .ge(hasPeriod, RequirementEntity::getCreatedAt, hasPeriod ? startOfDay(fStart) : null)
                        .le(hasPeriod, RequirementEntity::getCreatedAt, hasPeriod ? endOfDay(fEnd) : null));

        long myRequirementAccepted = requirementMapper.selectCount(
                new LambdaQueryWrapper<RequirementEntity>()
                        .eq(hasUserFilter, RequirementEntity::getCreatedBy, effectiveUserId)
                        .eq(RequirementEntity::getStatus, RequirementStatus.ACCEPTED.name())
                        .ge(hasPeriod, RequirementEntity::getCreatedAt, hasPeriod ? startOfDay(fStart) : null)
                        .le(hasPeriod, RequirementEntity::getCreatedAt, hasPeriod ? endOfDay(fEnd) : null));

        // 未读通知：始终按当前登录用户过滤
        long unreadNoticeCount = noticeMapper.selectCount(
                new LambdaQueryWrapper<NoticeEntity>()
                        .eq(NoticeEntity::getReceiverId, userId)
                        .eq(NoticeEntity::getReadFlag, 0));

        // 状态/优先级分布（全量，不受时间过滤，反映当前整体工作状态）
        List<TaskEntity> myTasks = safeList(taskMapper.selectList(
                new LambdaQueryWrapper<TaskEntity>()
                        .eq(hasUserFilter, TaskEntity::getAssigneeId, effectiveUserId)));

        LambdaQueryWrapper<BugEntity> bugDistW = new LambdaQueryWrapper<>();
        if (hasUserFilter) {
            bugDistW.and(wrapper -> wrapper.eq(BugEntity::getAssigneeId, effectiveUserId)
                    .or().eq(BugEntity::getCreatorId, effectiveUserId));
        }
        List<BugEntity> myBugs = safeList(bugMapper.selectList(bugDistW));

        // 任务完成趋势（按 actualEndDate 分组，仅在 period != "all" 时计算）
        List<TrendDataPoint> completionTrend;
        if (hasPeriod) {
            List<TaskEntity> completedInPeriod = safeList(taskMapper.selectList(
                    new LambdaQueryWrapper<TaskEntity>()
                            .eq(hasUserFilter, TaskEntity::getAssigneeId, effectiveUserId)
                            .eq(TaskEntity::getStatus, TaskStatus.COMPLETED.name())
                            .ge(TaskEntity::getActualEndDate, startDate)
                            .le(TaskEntity::getActualEndDate, endDate)));
            Map<LocalDate, Long> byDate = completedInPeriod.stream()
                    .filter(t -> t.getActualEndDate() != null)
                    .collect(Collectors.groupingBy(TaskEntity::getActualEndDate, Collectors.counting()));
            List<TrendDataPoint> trend = new ArrayList<>();
            for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
                trend.add(new TrendDataPoint(d, byDate.getOrDefault(d, 0L)));
            }
            completionTrend = trend;
        } else {
            completionTrend = List.of();
        }

        // 项目分布（全量任务按项目名分布，反映当前工作覆盖的项目）
        Map<Long, Long> byProjectId = myTasks.stream()
                .filter(t -> t.getProjectId() != null)
                .collect(Collectors.groupingBy(TaskEntity::getProjectId, Collectors.counting()));

        Map<String, Long> projectDistribution = new LinkedHashMap<>();
        if (!byProjectId.isEmpty()) {
            List<ProjectEntity> projects = safeList(projectMapper.selectByIds(byProjectId.keySet()));
            for (ProjectEntity p : projects) {
                projectDistribution.put(p.getName(), byProjectId.getOrDefault(p.getId(), 0L));
            }
        }

        return MyStatisticsResponse.builder()
                .myTaskTotal(myTaskTotal)
                .myTaskCompleted(myTaskCompleted)
                .myTaskOverdue(myTaskOverdue)
                .myBugTotal(myBugTotal)
                .myBugOpen(myBugOpen)
                .myRequirementTotal(myRequirementTotal)
                .myRequirementAccepted(myRequirementAccepted)
                .unreadNoticeCount(unreadNoticeCount)
                .taskStatusDistribution(countBy(myTasks, TaskEntity::getStatus))
                .taskPriorityDistribution(countBy(myTasks, TaskEntity::getPriority))
                .bugStatusDistribution(countBy(myBugs, BugEntity::getStatus))
                .completionTrend(completionTrend)
                .projectDistribution(projectDistribution)
                .build();
    }

    private static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    private static LocalDateTime endOfDay(LocalDate date) {
        return date.atTime(23, 59, 59);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private <T> Map<String, Long> countBy(List<T> values, Function<T, String> classifier) {
        return values.stream()
                .map(classifier)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private TodoWithSortKey taskTodo(TaskEntity task,
                                     Map<Long, String> projectNames,
                                     Map<Long, String> userNames,
                                     LocalDate today) {
        long overdueDays = overdueDays(task.getPlannedEndDate(), today);
        TodoItemResponse item = new TodoItemResponse(
                "TASK",
                task.getId(),
                task.getName(),
                task.getPriority(),
                task.getStatus(),
                projectNames.get(task.getProjectId()),
                userNames.get(task.getAssigneeId()),
                task.getPlannedEndDate(),
                overdueDays);
        return new TodoWithSortKey(
                item,
                new TodoSortKey(priorityRank(task.getPriority()), overdueDays, task.getPlannedEndDate(), task.getId()));
    }

    private TodoWithSortKey bugTodo(BugEntity bug,
                                    Map<Long, String> projectNames,
                                    Map<Long, String> userNames) {
        TodoItemResponse item = new TodoItemResponse(
                "BUG",
                bug.getId(),
                bug.getTitle(),
                bug.getPriority(),
                bug.getStatus(),
                projectNames.get(bug.getProjectId()),
                userNames.get(bug.getAssigneeId()),
                null,
                0);
        return new TodoWithSortKey(
                item,
                new TodoSortKey(priorityRank(bug.getPriority()), 0, null, bug.getId()));
    }

    private TodoWithSortKey requirementTodo(RequirementEntity req,
                                            Map<Long, String> projectNames,
                                            Map<Long, String> userNames) {
        TodoItemResponse item = new TodoItemResponse(
                "REQUIREMENT",
                req.getId(),
                req.getTitle(),
                req.getPriority(),
                req.getStatus(),
                projectNames.get(req.getProjectId()),
                userNames.get(req.getCreatedBy()),
                null,
                0);
        return new TodoWithSortKey(
                item,
                new TodoSortKey(priorityRank(req.getPriority()), 0, null, req.getId()));
    }

    private Map<Long, String> loadProjectNames(List<TaskEntity> tasks, List<BugEntity> bugs,
                                               List<RequirementEntity> requirements) {
        List<Long> projectIds = new ArrayList<>();
        tasks.stream().map(TaskEntity::getProjectId).filter(Objects::nonNull).forEach(projectIds::add);
        bugs.stream().map(BugEntity::getProjectId).filter(Objects::nonNull).forEach(projectIds::add);
        requirements.stream().map(RequirementEntity::getProjectId).filter(Objects::nonNull).forEach(projectIds::add);
        return projectIds.isEmpty() ? Map.of() : toProjectNameMap(projectMapper.selectByIds(distinct(projectIds)));
    }

    private Map<Long, String> loadUserNames(List<TaskEntity> tasks, List<BugEntity> bugs,
                                            List<RequirementEntity> requirements) {
        List<Long> userIds = new ArrayList<>();
        tasks.stream().map(TaskEntity::getAssigneeId).filter(Objects::nonNull).forEach(userIds::add);
        bugs.stream().map(BugEntity::getAssigneeId).filter(Objects::nonNull).forEach(userIds::add);
        requirements.stream().map(RequirementEntity::getCreatedBy).filter(Objects::nonNull).forEach(userIds::add);
        return userIds.isEmpty() ? Map.of() : toUserNameMap(systemUserMapper.selectByIds(distinct(userIds)));
    }

    private Collection<Long> distinct(List<Long> ids) {
        return ids.stream().distinct().toList();
    }

    private Map<Long, String> toProjectNameMap(List<ProjectEntity> projects) {
        Map<Long, String> names = new HashMap<>();
        for (ProjectEntity project : projects) {
            names.put(project.getId(), project.getName());
        }
        return names;
    }

    private Map<Long, String> toUserNameMap(List<SystemUser> users) {
        Map<Long, String> names = new HashMap<>();
        for (SystemUser user : users) {
            names.put(user.getId(), user.getRealName());
        }
        return names;
    }

    private long overdueDays(LocalDate plannedEndDate, LocalDate today) {
        if (plannedEndDate == null || !plannedEndDate.isBefore(today)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(plannedEndDate, today);
    }

    private int priorityRank(String priority) {
        if ("URGENT".equals(priority)) {
            return 0;
        }
        if ("HIGH".equals(priority)) {
            return 1;
        }
        if ("MEDIUM".equals(priority)) {
            return 2;
        }
        if ("LOW".equals(priority)) {
            return 3;
        }
        return 4;
    }

    private record TodoWithSortKey(TodoItemResponse item, TodoSortKey sortKey) implements Comparable<TodoWithSortKey> {
        @Override
        public int compareTo(TodoWithSortKey other) {
            return this.sortKey.compareTo(other.sortKey);
        }
    }
}
