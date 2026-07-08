package com.jitong.projectflow.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.task.domain.TaskStatus;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskDeadlineNoticeService {

    private static final int REMIND_DAYS = 3;
    private static final String BUSINESS_TYPE = "Task";
    private static final String OVERDUE_TITLE = "任务已逾期";
    private static final String DUE_SOON_TITLE = "任务即将到期";

    private final TaskMapper taskMapper;
    private final NoticeService noticeService;

    public TaskDeadlineNoticeService(TaskMapper taskMapper, NoticeService noticeService) {
        this.taskMapper = taskMapper;
        this.noticeService = noticeService;
    }

    @Scheduled(cron = "${projectflow.notice.task-deadline-cron:0 0 9 * * ?}")
    public void scanTaskDeadlines() {
        scanTaskDeadlines(LocalDate.now());
    }

    public int scanTaskDeadlines(LocalDate today) {
        LocalDate remindEndDate = today.plusDays(REMIND_DAYS);
        LambdaQueryWrapper<TaskEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(TaskEntity::getAssigneeId)
               .isNotNull(TaskEntity::getPlannedEndDate)
               .le(TaskEntity::getPlannedEndDate, remindEndDate)
               .notIn(TaskEntity::getStatus, TaskStatus.COMPLETED.name(), TaskStatus.PAUSED.name());

        List<TaskEntity> tasks = taskMapper.selectList(wrapper);
        int created = 0;
        for (TaskEntity task : tasks) {
            NoticePayload payload = buildPayload(task, today);
            if (!noticeService.existsBusinessNotice(task.getAssigneeId(), NoticeType.PROJECT_WARNING,
                    payload.title(), BUSINESS_TYPE, task.getId())) {
                noticeService.create(task.getAssigneeId(), NoticeType.PROJECT_WARNING, payload.title(),
                        payload.content(), BUSINESS_TYPE, task.getId());
                created++;
            }
        }
        return created;
    }

    private NoticePayload buildPayload(TaskEntity task, LocalDate today) {
        if (task.getPlannedEndDate().isBefore(today)) {
            return new NoticePayload(OVERDUE_TITLE,
                    task.getName() + " 已于 " + task.getPlannedEndDate() + " 逾期，请尽快处理。");
        }
        return new NoticePayload(DUE_SOON_TITLE,
                task.getName() + " 将于 " + task.getPlannedEndDate() + " 到期，请及时推进。");
    }

    private record NoticePayload(String title, String content) {
    }
}
