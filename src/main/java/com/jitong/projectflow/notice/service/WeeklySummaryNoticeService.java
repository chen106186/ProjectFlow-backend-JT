package com.jitong.projectflow.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.task.domain.TaskStatus;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class WeeklySummaryNoticeService {

    private final TaskMapper taskMapper;
    private final NoticeService noticeService;

    public WeeklySummaryNoticeService(TaskMapper taskMapper, NoticeService noticeService) {
        this.taskMapper = taskMapper;
        this.noticeService = noticeService;
    }

    @Scheduled(cron = "${projectflow.notice.weekly-summary-cron:0 0 9 ? * MON}", zone = "Asia/Shanghai")
    public void sendWeeklySummary() {
        sendWeeklySummary(LocalDate.now());
    }

    public int sendWeeklySummary(LocalDate today) {
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).minusWeeks(1);
        LocalDate weekEnd = weekStart.plusDays(6);
        String title = "任务周报（" + weekStart + " 至 " + weekEnd + "）";

        List<Long> assigneeIds = taskMapper.selectList(
                new LambdaQueryWrapper<TaskEntity>().isNotNull(TaskEntity::getAssigneeId)
                        .select(TaskEntity::getAssigneeId)
        ).stream().map(TaskEntity::getAssigneeId).distinct().toList();

        int sent = 0;
        for (Long userId : assigneeIds) {
            if (noticeService.existsBusinessNotice(userId, NoticeType.SYSTEM,
                    title, "WeeklySummary", weekStart.toEpochDay())) continue;
            long completedThisWeek = taskMapper.selectCount(
                    new LambdaQueryWrapper<TaskEntity>()
                            .eq(TaskEntity::getAssigneeId, userId)
                            .eq(TaskEntity::getStatus, TaskStatus.COMPLETED.name())
                            .ge(TaskEntity::getActualEndDate, weekStart)
                            .le(TaskEntity::getActualEndDate, weekEnd));
            long overdueCount = taskMapper.selectCount(
                    new LambdaQueryWrapper<TaskEntity>()
                            .eq(TaskEntity::getAssigneeId, userId)
                            .isNotNull(TaskEntity::getPlannedEndDate)
                            .lt(TaskEntity::getPlannedEndDate, today)
                            .notIn(TaskEntity::getStatus,
                                    TaskStatus.COMPLETED.name(), TaskStatus.PAUSED.name()));

            if (completedThisWeek == 0 && overdueCount == 0) continue;

            String content = "上周已完成任务 " + completedThisWeek + " 个，当前逾期任务 " + overdueCount + " 个";
            noticeService.create(userId, NoticeType.SYSTEM, title, content, "WeeklySummary", weekStart.toEpochDay());
            sent++;
        }
        return sent;
    }
}
