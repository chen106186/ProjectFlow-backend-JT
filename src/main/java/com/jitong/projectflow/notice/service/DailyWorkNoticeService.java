package com.jitong.projectflow.notice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jitong.projectflow.notice.domain.NoticeType;
import com.jitong.projectflow.system.entity.SystemUser;
import com.jitong.projectflow.system.mapper.SystemUserMapper;
import com.jitong.projectflow.task.domain.TaskStatus;
import com.jitong.projectflow.task.entity.TaskEntity;
import com.jitong.projectflow.task.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyWorkNoticeService {

    private final TaskMapper taskMapper;
    private final NoticeService noticeService;
    private final SystemUserMapper systemUserMapper;

    /** 每天 08:00 向所有有活跃任务的用户发送当日工作提醒 */
    @Scheduled(cron = "${projectflow.notice.daily-work-cron:0 0 8 * * ?}")
    public void sendDailyWorkNotice() {
        int sent = sendDailyWorkNotice(LocalDate.now());
        log.info("[定时] 每日工作提醒发送完成，共发送 {} 条", sent);
    }

    public int sendDailyWorkNotice(LocalDate today) {
        List<Long> assigneeIds = taskMapper.selectList(
                        new LambdaQueryWrapper<TaskEntity>()
                                .isNotNull(TaskEntity::getAssigneeId)
                                .select(TaskEntity::getAssigneeId))
                .stream().map(TaskEntity::getAssigneeId).distinct().toList();

        int sent = 0;
        for (Long userId : assigneeIds) {
            long overdueCount = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                    .eq(TaskEntity::getAssigneeId, userId)
                    .eq(TaskEntity::getStatus, TaskStatus.OVERDUE.name()));

            long dueSoonCount = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                    .eq(TaskEntity::getAssigneeId, userId)
                    .eq(TaskEntity::getStatus, TaskStatus.DUE_SOON.name()));

            long inProgressCount = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                    .eq(TaskEntity::getAssigneeId, userId)
                    .eq(TaskEntity::getStatus, TaskStatus.IN_PROGRESS.name()));

            if (overdueCount == 0 && dueSoonCount == 0 && inProgressCount == 0) continue;

            String userName = resolveUserName(userId);
            long total = overdueCount + dueSoonCount + inProgressCount;
            String title = "早安，" + userName + "！今日共有 " + total + " 项工作待处理";

            StringBuilder content = new StringBuilder();
            if (overdueCount > 0) content.append("逾期任务 ").append(overdueCount).append(" 个");
            if (dueSoonCount > 0) {
                if (content.length() > 0) content.append("　｜　");
                content.append("即将到期 ").append(dueSoonCount).append(" 个");
            }
            if (inProgressCount > 0) {
                if (content.length() > 0) content.append("　｜　");
                content.append("进行中 ").append(inProgressCount).append(" 个");
            }

            noticeService.create(userId, NoticeType.SYSTEM, title, content.toString(), null, null);
            sent++;
        }
        return sent;
    }

    private String resolveUserName(Long userId) {
        SystemUser user = systemUserMapper.selectById(userId);
        return user != null && user.getRealName() != null ? user.getRealName() : "同事";
    }
}
