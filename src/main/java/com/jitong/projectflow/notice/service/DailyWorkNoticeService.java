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

    /** 每天 08:00 向全体在职用户发送当日工作提醒 */
    @Scheduled(cron = "${projectflow.notice.daily-work-cron:0 0 8 * * ?}")
    public void sendDailyWorkNotice() {
        int sent = sendDailyWorkNotice(LocalDate.now());
        log.info("[定时] 每日工作提醒发送完成，共发送 {} 条", sent);
    }

    public int sendDailyWorkNotice(LocalDate today) {
        // 向所有启用状态的用户发送，不论是否有任务
        List<SystemUser> allUsers = systemUserMapper.selectList(
                new LambdaQueryWrapper<SystemUser>()
                        .eq(SystemUser::getEnabled, true)
                        .eq(SystemUser::getDeleted, false));

        int sent = 0;
        for (SystemUser user : allUsers) {
            long overdueCount = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                    .eq(TaskEntity::getAssigneeId, user.getId())
                    .eq(TaskEntity::getStatus, TaskStatus.OVERDUE.name()));

            long dueSoonCount = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                    .eq(TaskEntity::getAssigneeId, user.getId())
                    .eq(TaskEntity::getStatus, TaskStatus.DUE_SOON.name()));

            long inProgressCount = taskMapper.selectCount(new LambdaQueryWrapper<TaskEntity>()
                    .eq(TaskEntity::getAssigneeId, user.getId())
                    .eq(TaskEntity::getStatus, TaskStatus.IN_PROGRESS.name()));

            String userName = user.getRealName() != null ? user.getRealName() : user.getUsername();
            String title;
            String content;

            long total = overdueCount + dueSoonCount + inProgressCount;
            if (total > 0) {
                title = "早安，" + userName + "！今日共有 " + total + " 项工作待处理";
                StringBuilder sb = new StringBuilder();
                if (overdueCount > 0) sb.append("逾期任务 ").append(overdueCount).append(" 个");
                if (dueSoonCount > 0) {
                    if (sb.length() > 0) sb.append("　｜　");
                    sb.append("即将到期 ").append(dueSoonCount).append(" 个");
                }
                if (inProgressCount > 0) {
                    if (sb.length() > 0) sb.append("　｜　");
                    sb.append("进行中 ").append(inProgressCount).append(" 个");
                }
                content = sb.toString();
            } else {
                title = "早安，" + userName + "！祝您今日工作顺利";
                content = "今日暂无待处理任务，请关注新任务分配";
            }

            noticeService.create(user.getId(), NoticeType.SYSTEM, title, content, null, null);
            sent++;
        }
        return sent;
    }
}
