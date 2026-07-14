package com.jitong.projectflow.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.project.entity.ProjectEntity;
import com.jitong.projectflow.project.mapper.ProjectMapper;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.task.domain.TaskStatus;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class TaskDeadlineNoticeService {

    private static final int REMIND_DAYS = 3;
    private static final String BUSINESS_TYPE = "Task";

    private final TaskMapper taskMapper;
    private final NoticeService noticeService;
    private final ProjectMapper projectMapper;
    private final SystemUserMapper systemUserMapper;

    public TaskDeadlineNoticeService(TaskMapper taskMapper, NoticeService noticeService,
                                     ProjectMapper projectMapper, SystemUserMapper systemUserMapper) {
        this.taskMapper = taskMapper;
        this.noticeService = noticeService;
        this.projectMapper = projectMapper;
        this.systemUserMapper = systemUserMapper;
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
            NoticeType type = task.getPlannedEndDate().isBefore(today) ? NoticeType.TASK_OVERDUE : NoticeType.PROJECT_WARNING;
            if (!noticeService.existsBusinessNotice(task.getAssigneeId(), type,
                    payload.title(), BUSINESS_TYPE, task.getId())) {
                noticeService.create(task.getAssigneeId(), type, payload.title(),
                        payload.content(), BUSINESS_TYPE, task.getId());
                created++;
            }
        }
        return created;
    }

    private NoticePayload buildPayload(TaskEntity task, LocalDate today) {
        String projectName = task.getProjectId() != null
                ? resolveProjectName(task.getProjectId()) : "未知项目";
        String assigneeName = resolveUserName(task.getAssigneeId());

        if (task.getPlannedEndDate().isBefore(today)) {
            long days = ChronoUnit.DAYS.between(task.getPlannedEndDate(), today);
            String title = "任务「" + task.getName() + "」已逾期 " + days + " 天，请及时处理";
            String content = "所属项目：" + projectName + "　｜　任务负责人：" + assigneeName;
            return new NoticePayload(title, content);
        }
        String title = "任务「" + task.getName() + "」即将于 " + task.getPlannedEndDate() + " 到期";
        String content = "所属项目：" + projectName + "　｜　任务负责人：" + assigneeName;
        return new NoticePayload(title, content);
    }

    private String resolveProjectName(Long projectId) {
        ProjectEntity project = projectMapper.selectById(projectId);
        return project != null ? project.getName() : "未知项目";
    }

    private String resolveUserName(Long userId) {
        if (userId == null) return "未知";
        SystemUser user = systemUserMapper.selectById(userId);
        return user != null ? user.getRealName() : "未知";
    }

    private record NoticePayload(String title, String content) {
    }
}
